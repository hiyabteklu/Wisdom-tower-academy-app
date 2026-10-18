package com.wisdomtower.academy.data.admin

import android.net.Uri
import com.wisdomtower.academy.data.auth.AuthStateManager
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** One row of the `profiles` table (columns as in website-reference/src/lib/admin-data.ts listProfiles). */
data class ScholarItem(
    val id: String,
    val email: String?,
    val fullName: String?,
    val phone: String?,
    val educationLevel: String?,
    val schoolName: String?,
    val townRegion: String?,
    val stream: String?,
    val studentId: String?,
    val targetExam: String?,
    val createdAt: String
)

sealed class ScholarsResult {
    /** truncated = the server returned as many rows as were asked for, so older scholars may not be listed. */
    class Success(val items: List<ScholarItem>, val truncated: Boolean) : ScholarsResult()
    class Failure(val message: String) : ScholarsResult()
}

class StudentScore(
    val title: String,
    val createdAt: String,
    val correct: String,
    val total: String,
    val percent: String
)

/** enrollments and grants hold one display name per row. */
class StudentDetail(
    val enrollments: List<String>,
    val grants: List<String>,
    val scores: List<StudentScore>
)

sealed class StudentDetailResult {
    class Success(val detail: StudentDetail) : StudentDetailResult()
    class Failure(val message: String) : StudentDetailResult()
}

/** ok = everything was saved. message is the toast text. */
class GrantOutcome(val ok: Boolean, val message: String)

/**
 * ADMIN-native N4. Same data and steps as the website UsersPanel (components/admin/UsersPanel.tsx,
 * lib/admin-data.ts listProfiles and getStudentDetail, lib/access-grants.ts grantAccess and grantAllPackages):
 *  - list: newest 500 profiles (rich columns; if the server rejects a column with HTTP 400, the short column list)
 *  - detail: newest 25 academic_results of the user, all enrollments of the user, access_grants of the e-mail
 *  - grant: write one access_grants row per package (single package: source whitelist; all packages: the __all__
 *    sentinel plus every package, source beta) and mirror each into enrollments, like the website
 * Differences, on purpose: the user id comes from the profile row (the website looks it up by e-mail), the write
 * does not rely on an ON CONFLICT rule (it reads the existing rows once, updates those and inserts the rest in one
 * request), and every refusal or zero-row change is reported instead of ignored.
 *
 * STATUS: `[~]` written, NOT compiled, NOT tested on a device.
 */
object UsersRepository {
    const val LIST_LIMIT = 500
    private const val BASIC_LIMIT = 300
    private const val RICH_COLUMNS =
        "id,email,full_name,phone,avatar_url,avatar_preset,education_level,school_name,town_region,stream," +
            "student_id_number,student_folio_number,target_exam,created_at,updated_at"
    private const val BASIC_COLUMNS = "id,email,full_name,avatar_url,created_at,updated_at"
    private val EMAIL_SHAPE = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    private fun JSONObject.text(key: String): String? =
        if (isNull(key)) null else optString(key, "").takeIf { it.isNotEmpty() }

    /** Numbers print without a trailing .0 (the website prints them with String(value)). A missing value is a dash. */
    private fun JSONObject.numText(key: String): String {
        if (isNull(key)) return "-"
        val v = opt(key)
        if (v is Number) {
            val d = v.toDouble()
            return if (!d.isInfinite() && !d.isNaN() && d == Math.floor(d)) d.toLong().toString() else d.toString()
        }
        val s = v?.toString().orEmpty()
        return if (s.isEmpty()) "-" else s
    }

    private fun packageLabel(row: JSONObject): String =
        row.text("package_name") ?: row.text("package_id") ?: "Package"

    suspend fun list(): ScholarsResult {
        var limit = LIST_LIMIT
        var reply = AdminSupabase.get("profiles?select=$RICH_COLUMNS&order=created_at.desc&limit=$limit")
        if (!reply.ok && reply.code == 400) {
            limit = BASIC_LIMIT
            reply = AdminSupabase.get("profiles?select=$BASIC_COLUMNS&order=created_at.desc&limit=$limit")
        }
        if (!reply.ok) return ScholarsResult.Failure(reply.errorText())
        return try {
            val arr = JSONArray(reply.body)
            val out = ArrayList<ScholarItem>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    ScholarItem(
                        id = o.text("id") ?: continue,
                        email = o.text("email"),
                        fullName = o.text("full_name"),
                        phone = o.text("phone"),
                        educationLevel = o.text("education_level"),
                        schoolName = o.text("school_name"),
                        townRegion = o.text("town_region"),
                        stream = o.text("stream"),
                        studentId = o.text("student_id_number"),
                        targetExam = o.text("target_exam"),
                        createdAt = o.text("created_at") ?: ""
                    )
                )
            }
            ScholarsResult.Success(out, arr.length() >= limit)
        } catch (e: Exception) {
            ScholarsResult.Failure("Unexpected reply from the server")
        }
    }

    /** A failed read is a failure, never an empty list: an empty list would claim the student has nothing. */
    suspend fun detail(userId: String, email: String?): StudentDetailResult {
        val uid = Uri.encode(userId)
        val results = AdminSupabase.get("academic_results?select=*&user_id=eq.$uid&order=created_at.desc&limit=25")
        if (!results.ok) return StudentDetailResult.Failure(results.errorText())
        val enrolled = AdminSupabase.get("enrollments?select=*&user_id=eq.$uid&order=created_at.desc")
        if (!enrolled.ok) return StudentDetailResult.Failure(enrolled.errorText())
        var grantsBody = "[]"
        val mail = email?.trim()?.lowercase().orEmpty()
        if (mail.isNotEmpty()) {
            val grants = AdminSupabase.get("access_grants?select=*&email=eq.${Uri.encode(mail)}")
            if (!grants.ok) return StudentDetailResult.Failure(grants.errorText())
            grantsBody = grants.body
        }
        return try {
            val scores = ArrayList<StudentScore>()
            val scoreRows = JSONArray(results.body)
            for (i in 0 until scoreRows.length()) {
                val r = scoreRows.getJSONObject(i)
                scores.add(
                    StudentScore(
                        title = r.text("title") ?: r.text("scope_id") ?: "Result",
                        createdAt = r.text("created_at") ?: "",
                        correct = r.numText("correct"),
                        total = r.numText("total"),
                        percent = r.numText("percent")
                    )
                )
            }
            val enrollments = ArrayList<String>()
            val enrollRows = JSONArray(enrolled.body)
            for (i in 0 until enrollRows.length()) {
                enrollments.add(packageLabel(enrollRows.getJSONObject(i)))
            }
            val grantLabels = ArrayList<String>()
            val grantRows = JSONArray(grantsBody)
            for (i in 0 until grantRows.length()) {
                grantLabels.add(packageLabel(grantRows.getJSONObject(i)))
            }
            StudentDetailResult.Success(StudentDetail(enrollments, grantLabels, scores))
        } catch (e: Exception) {
            StudentDetailResult.Failure("Unexpected reply from the server")
        }
    }

    private fun nowIso(): String {
        val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        f.timeZone = TimeZone.getTimeZone("UTC")
        return f.format(Date())
    }

    /** Escapes the three LIKE characters so an e-mail such as first_last@x.com matches only itself. */
    private fun likeEscape(s: String): String =
        s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    private class PendingRow(val packageId: String, val insertBody: JSONObject, val updateBody: JSONObject)

    private fun buildRow(
        email: String,
        packageId: String,
        packageName: String,
        userId: String,
        source: String,
        note: String,
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
        o.put("user_id", userId)
        o.put("source", source)
        o.put("note", note)
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
                val changed = try {
                    JSONArray(reply.body).length()
                } catch (e: Exception) {
                    0
                }
                if (changed == 0) return "Not saved: no permission to change $table"
            }
        }
        if (fresh.length() > 0) {
            val reply = AdminSupabase.post(table, fresh.toString(), "return=minimal")
            if (!reply.ok) return reply.errorText()
        }
        return null
    }

    /** packageId is ALL_PACKAGES_ID or one id from ADMIN_PACKAGES. Never throws. */
    suspend fun grant(scholar: ScholarItem, packageId: String, note: String): GrantOutcome {
        val email = scholar.email?.trim()?.lowercase().orEmpty()
        if (email.isEmpty()) return GrantOutcome(false, "User has no email associated with account")
        if (!EMAIL_SHAPE.matches(email)) return GrantOutcome(false, "This account's email address is not valid")
        val pid = packageId.trim()
        if (pid.isEmpty()) return GrantOutcome(false, "Choose a package")

        val isAll = pid == ALL_PACKAGES_ID
        val source = if (isAll) "beta" else "whitelist"
        val targets = ArrayList<Pair<String, String>>()
        if (isAll) {
            targets.add(ALL_PACKAGES_ID to "All packages")
            for (p in ADMIN_PACKAGES) targets.add(p.id to p.name)
        } else {
            val label = ADMIN_PACKAGES.firstOrNull { it.id == pid }?.name ?: pid
            targets.add(pid to label)
        }
        val noteText = note.trim().ifEmpty { "Direct Admin Grant" }
        val adminEmail = AuthStateManager.currentUser.value?.email
        val userId = scholar.id

        val grantReply =
            AdminSupabase.get("access_grants?select=id,email,package_id&email=ilike.${Uri.encode(likeEscape(email))}")
        if (!grantReply.ok) return GrantOutcome(false, grantReply.errorText())
        val enrollReply = AdminSupabase.get("enrollments?select=id,package_id&user_id=eq.${Uri.encode(userId)}")
        if (!enrollReply.ok) return GrantOutcome(false, enrollReply.errorText())

        val existingGrants = HashMap<String, String>()
        val existingEnrollments = HashMap<String, String>()
        try {
            val grantArr = JSONArray(grantReply.body)
            for (i in 0 until grantArr.length()) {
                val row = grantArr.getJSONObject(i)
                val rowEmail = row.text("email")?.lowercase()
                val rowPackage = row.text("package_id")
                val rowId = row.text("id")
                if (rowEmail == email && rowPackage != null && rowId != null) existingGrants[rowPackage] = rowId
            }
            val enrollArr = JSONArray(enrollReply.body)
            for (i in 0 until enrollArr.length()) {
                val row = enrollArr.getJSONObject(i)
                val rowPackage = row.text("package_id")
                val rowId = row.text("id")
                if (rowPackage != null && rowId != null) existingEnrollments[rowPackage] = rowId
            }
        } catch (ex: Exception) {
            return GrantOutcome(false, "Unexpected reply from the server")
        }

        val now = nowIso()
        val grantRows = ArrayList<PendingRow>()
        val enrollRows = ArrayList<PendingRow>()
        for (t in targets) {
            grantRows.add(
                PendingRow(
                    t.first,
                    buildRow(email, t.first, t.second, userId, source, noteText, adminEmail, true, null),
                    buildRow(email, t.first, t.second, userId, source, noteText, adminEmail, false, now)
                )
            )
            enrollRows.add(
                PendingRow(
                    t.first,
                    buildRow(email, t.first, t.second, userId, source, noteText, adminEmail, true, null),
                    buildRow(email, t.first, t.second, userId, source, noteText, adminEmail, false, null)
                )
            )
        }

        val grantError = saveRows("access_grants", existingGrants, grantRows)
        if (grantError != null) return GrantOutcome(false, grantError)
        val enrollError = saveRows("enrollments", existingEnrollments, enrollRows)
        if (enrollError != null) {
            return GrantOutcome(false, "Grant saved, but the enrollment copy failed: $enrollError")
        }
        return if (isAll) {
            GrantOutcome(true, "Granted full platform pass to $email (${targets.size} packages)")
        } else {
            GrantOutcome(true, "Granted ${targets[0].second} to $email")
        }
    }
}
