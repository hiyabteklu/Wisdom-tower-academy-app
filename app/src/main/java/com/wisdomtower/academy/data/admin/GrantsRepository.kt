package com.wisdomtower.academy.data.admin

import android.net.Uri
import com.wisdomtower.academy.data.auth.AuthStateManager
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** One row of the `access_grants` table (website-reference/src/lib/access-grants.ts, type AccessGrant). */
data class AccessGrantItem(
    val id: String,
    val email: String,
    val packageId: String,
    val packageName: String?,
    val userId: String?,
    val source: String,
    val note: String?,
    val grantedBy: String?,
    val createdAt: String
)

sealed class GrantsResult {
    /** truncated = the server returned as many rows as were asked for, so older grants may not be listed. */
    class Success(val items: List<AccessGrantItem>, val truncated: Boolean) : GrantsResult()
    class Failure(val message: String) : GrantsResult()
}

/**
 * ADMIN-native N6 (Access Grants). Same data and steps as the website AccessGrantsPanel and lib/access-grants.ts
 * (listAccessGrants, grantAccess, grantAllPackages, revokeAccess, revokeAllForEmail), with the admin's own login.
 *  - list: newest 500 rows
 *  - grant: the e-mail does NOT need an account (user_id stays empty then); one access_grants row per package (single
 *    package, or the __all__ sentinel plus every package), source beta, each mirrored into enrollments
 *  - revoke: delete the access_grants row(s), then delete the enrollment copies whose source is whitelist, admin,
 *    beta or comp
 * Differences, on purpose: the write reads the existing rows once and inserts the new ones in one request (no
 * ON CONFLICT rule needed, fewer requests); a refusal or a zero-row delete is reported; enrollment copies with an
 * EMPTY source are NOT deleted on revoke (the website deletes them, which could also remove a paid enrollment of the
 * same e-mail and package).
 * The helper code below is a copy of the one in UsersRepository.kt (kept separate so that file was not resent);
 * merge them in the regression slice.
 *
 * STATUS: `[~]` written, NOT compiled, NOT tested on a device.
 */
object GrantsRepository {
    const val LIST_LIMIT = 500
    private val EMAIL_SHAPE = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    private val REMOVABLE_SOURCES = setOf("whitelist", "admin", "beta", "comp")

    private fun JSONObject.text(key: String): String? =
        if (isNull(key)) null else optString(key, "").takeIf { it.isNotEmpty() }

    private fun likeEscape(s: String): String =
        s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    private fun nowIso(): String {
        val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        f.timeZone = TimeZone.getTimeZone("UTC")
        return f.format(Date())
    }

    private fun countRows(body: String): Int = try {
        JSONArray(body).length()
    } catch (e: Exception) {
        0
    }

    suspend fun list(): GrantsResult {
        val reply = AdminSupabase.get("access_grants?select=*&order=created_at.desc&limit=$LIST_LIMIT")
        if (!reply.ok) {
            val msg = reply.errorText()
            val missingTable = msg.contains("relation") || msg.contains("does not exist")
            return GrantsResult.Failure(
                if (missingTable) "Table missing: run docs/access-grants-setup.sql in Supabase SQL Editor." else msg
            )
        }
        return try {
            val arr = JSONArray(reply.body)
            val out = ArrayList<AccessGrantItem>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    AccessGrantItem(
                        id = o.text("id") ?: continue,
                        email = o.text("email") ?: continue,
                        packageId = o.text("package_id") ?: continue,
                        packageName = o.text("package_name"),
                        userId = o.text("user_id"),
                        source = o.text("source") ?: "whitelist",
                        note = o.text("note"),
                        grantedBy = o.text("granted_by"),
                        createdAt = o.text("created_at") ?: ""
                    )
                )
            }
            GrantsResult.Success(out, arr.length() >= LIST_LIMIT)
        } catch (e: Exception) {
            GrantsResult.Failure("Unexpected reply from the server")
        }
    }

    /** Account id of this e-mail from profiles, best effort like the website: null when there is no account or the read fails. */
    private suspend fun findUserId(email: String): String? {
        val reply = AdminSupabase.get("profiles?select=id,email&email=ilike.${Uri.encode(likeEscape(email))}&limit=5")
        if (!reply.ok) return null
        return try {
            val arr = JSONArray(reply.body)
            var found: String? = null
            for (i in 0 until arr.length()) {
                val row = arr.getJSONObject(i)
                if (found == null && row.text("email")?.lowercase() == email) found = row.text("id")
            }
            found
        } catch (e: Exception) {
            null
        }
    }

    private class RowsReply(val rows: List<Pair<String, String>>, val error: String?)

    /** (row id, package id) of every access_grants row of this e-mail, compared ignoring case. */
    private suspend fun grantRowsFor(email: String): RowsReply {
        val reply = AdminSupabase.get("access_grants?select=id,email,package_id&email=ilike.${Uri.encode(likeEscape(email))}")
        if (!reply.ok) return RowsReply(emptyList(), reply.errorText())
        return try {
            val arr = JSONArray(reply.body)
            val rows = ArrayList<Pair<String, String>>()
            for (i in 0 until arr.length()) {
                val row = arr.getJSONObject(i)
                val id = row.text("id")
                val pkg = row.text("package_id")
                if (row.text("email")?.lowercase() == email && id != null && pkg != null) rows.add(id to pkg)
            }
            RowsReply(rows, null)
        } catch (e: Exception) {
            RowsReply(emptyList(), "Unexpected reply from the server")
        }
    }

    private class PendingRow(val packageId: String, val insertBody: JSONObject, val updateBody: JSONObject)

    private fun buildRow(
        email: String,
        packageId: String,
        packageName: String,
        userId: String?,
        source: String,
        note: String?,
        grantedBy: String?,
        forInsert: Boolean,
        updatedAt: String?
    ): JSONObject {
        val o = JSONObject()
        if (forInsert) {
            o.put("email", email)
            o.put("package_id", packageId)
        }
        o.put("package_name", packageName)
        o.put("user_id", userId ?: JSONObject.NULL)
        o.put("source", source)
        o.put("note", note ?: JSONObject.NULL)
        o.put("granted_by", grantedBy ?: JSONObject.NULL)
        if (updatedAt != null) o.put("updated_at", updatedAt)
        return o
    }

    /** Updates the rows that already exist (one request each) and inserts the new ones in ONE request. Null = saved. */
    private suspend fun saveRows(table: String, existing: Map<String, String>, rows: List<PendingRow>): String? {
        val fresh = JSONArray()
        for (row in rows) {
            val existingId = existing[row.packageId]
            if (existingId == null) {
                fresh.put(row.insertBody)
            } else {
                val reply = AdminSupabase.patch("$table?id=eq.${Uri.encode(existingId)}", row.updateBody.toString())
                if (!reply.ok) return reply.errorText()
                if (countRows(reply.body) == 0) return "Not saved: no permission to change $table"
            }
        }
        if (fresh.length() > 0) {
            val reply = AdminSupabase.post(table, fresh.toString(), "return=minimal")
            if (!reply.ok) return reply.errorText()
        }
        return null
    }

    /** packageId is ALL_PACKAGES_ID or one id from ADMIN_PACKAGES. Never throws. */
    suspend fun grant(emailRaw: String, packageId: String, noteRaw: String): GrantOutcome {
        val email = emailRaw.trim().lowercase()
        if (!EMAIL_SHAPE.matches(email)) return GrantOutcome(false, "Enter a valid email address")
        val pid = packageId.trim()
        if (pid.isEmpty()) return GrantOutcome(false, "Choose a package")

        val isAll = pid == ALL_PACKAGES_ID
        val targets = ArrayList<Pair<String, String>>()
        if (isAll) {
            targets.add(ALL_PACKAGES_ID to "All packages")
            for (p in ADMIN_PACKAGES) targets.add(p.id to p.name)
        } else {
            targets.add(pid to (ADMIN_PACKAGES.firstOrNull { it.id == pid }?.name ?: pid))
        }
        val trimmedNote = noteRaw.trim()
        val note: String? = if (isAll) trimmedNote.ifEmpty { "Beta / full access" } else trimmedNote.ifEmpty { null }
        val source = "beta"
        val adminEmail = AuthStateManager.currentUser.value?.email

        val userId = findUserId(email)

        val grantRows = grantRowsFor(email)
        if (grantRows.error != null) return GrantOutcome(false, grantRows.error)
        val existingGrants = HashMap<String, String>()
        for (r in grantRows.rows) existingGrants[r.second] = r.first

        val enrollReply = if (userId != null) {
            AdminSupabase.get("enrollments?select=id,email,package_id&user_id=eq.${Uri.encode(userId)}")
        } else {
            AdminSupabase.get("enrollments?select=id,email,package_id&email=ilike.${Uri.encode(likeEscape(email))}")
        }
        if (!enrollReply.ok) return GrantOutcome(false, enrollReply.errorText())
        val existingEnrollments = HashMap<String, String>()
        try {
            val arr = JSONArray(enrollReply.body)
            for (i in 0 until arr.length()) {
                val row = arr.getJSONObject(i)
                val rowPackage = row.text("package_id")
                val rowId = row.text("id")
                val emailOk = userId != null || row.text("email")?.lowercase() == email
                if (emailOk && rowPackage != null && rowId != null) existingEnrollments[rowPackage] = rowId
            }
        } catch (ex: Exception) {
            return GrantOutcome(false, "Unexpected reply from the server")
        }

        val now = nowIso()
        val grantPending = ArrayList<PendingRow>()
        val enrollPending = ArrayList<PendingRow>()
        for (t in targets) {
            grantPending.add(
                PendingRow(
                    t.first,
                    buildRow(email, t.first, t.second, userId, source, note, adminEmail, true, null),
                    buildRow(email, t.first, t.second, userId, source, note, adminEmail, false, now)
                )
            )
            enrollPending.add(
                PendingRow(
                    t.first,
                    buildRow(email, t.first, t.second, userId, source, note, adminEmail, true, null),
                    buildRow(email, t.first, t.second, userId, source, note, adminEmail, false, null)
                )
            )
        }

        val grantError = saveRows("access_grants", existingGrants, grantPending)
        if (grantError != null) return GrantOutcome(false, grantError)
        val enrollError = saveRows("enrollments", existingEnrollments, enrollPending)
        if (enrollError != null) {
            return GrantOutcome(false, "Grant saved, but the enrollment copy failed: $enrollError")
        }
        return if (isAll) {
            GrantOutcome(true, "Granted full access to $email (${targets.size} rows)")
        } else {
            GrantOutcome(true, "Granted ${targets[0].second} \u2192 $email")
        }
    }

    private class MirrorResult(val removed: Int, val error: String?)

    /** Deletes the enrollment copies of these packages for this e-mail, only when their source marks them as granted. */
    private suspend fun removeMirrors(email: String, packageIds: Set<String>): MirrorResult {
        val reply = AdminSupabase.get(
            "enrollments?select=id,email,package_id,source&email=ilike.${Uri.encode(likeEscape(email))}"
        )
        if (!reply.ok) return MirrorResult(0, reply.errorText())
        val ids = ArrayList<String>()
        try {
            val arr = JSONArray(reply.body)
            for (i in 0 until arr.length()) {
                val row = arr.getJSONObject(i)
                val id = row.text("id")
                val pkg = row.text("package_id")
                val source = row.text("source")
                if (id != null && pkg != null && source != null &&
                    row.text("email")?.lowercase() == email &&
                    packageIds.contains(pkg) && REMOVABLE_SOURCES.contains(source)
                ) {
                    ids.add(id)
                }
            }
        } catch (e: Exception) {
            return MirrorResult(0, "Unexpected reply from the server")
        }
        if (ids.isEmpty()) return MirrorResult(0, null)
        val del = AdminSupabase.delete("enrollments?id=in.(${ids.joinToString(",") { Uri.encode(it) }})")
        if (!del.ok) return MirrorResult(0, del.errorText())
        return MirrorResult(countRows(del.body), null)
    }

    suspend fun revoke(item: AccessGrantItem): GrantOutcome {
        val del = AdminSupabase.delete("access_grants?id=eq.${Uri.encode(item.id)}")
        if (!del.ok) return GrantOutcome(false, del.errorText())
        if (countRows(del.body) == 0) return GrantOutcome(false, "Not revoked: grant not found or no permission")
        val mirror = removeMirrors(item.email.trim().lowercase(), setOf(item.packageId))
        if (mirror.error != null) {
            return GrantOutcome(false, "Grant removed, but the enrollment copy could not be removed: ${mirror.error}")
        }
        return GrantOutcome(true, "Revoked")
    }

    suspend fun revokeAllForEmail(emailRaw: String): GrantOutcome {
        val email = emailRaw.trim().lowercase()
        val rows = grantRowsFor(email)
        if (rows.error != null) return GrantOutcome(false, rows.error)
        if (rows.rows.isEmpty()) return GrantOutcome(true, "Removed 0 grant(s) for $email")
        val ids = rows.rows.map { it.first }
        val del = AdminSupabase.delete("access_grants?id=in.(${ids.joinToString(",") { Uri.encode(it) }})")
        if (!del.ok) return GrantOutcome(false, del.errorText())
        val removed = countRows(del.body)
        if (removed == 0) return GrantOutcome(false, "Not revoked: no permission")
        val mirror = removeMirrors(email, rows.rows.map { it.second }.toSet())
        if (mirror.error != null) {
            return GrantOutcome(
                false,
                "Removed $removed grant(s), but the enrollment copies could not be removed: ${mirror.error}"
            )
        }
        return GrantOutcome(true, "Removed $removed grant(s) for $email")
    }
}
