package com.wisdomtower.academy.data.admin

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** One row of the `content_locks` table (website-reference/src/lib/content-locks.ts, type ContentLockRow). */
data class LockItem(val lockKey: String, val mode: String, val label: String?)

sealed class LocksResult {
    class Success(val items: List<LockItem>) : LocksResult()
    class Failure(val message: String) : LocksResult()
}

/** One material (a `learning_resources` row) that can get its own lock. [chapter] is null when it has no chapter. */
data class LockableItem(
    val id: String,
    val title: String,
    val chapter: Int?,
    val published: Boolean
)

sealed class HubItemsResult {
    class Success(val items: List<LockableItem>) : HubItemsResult()
    class Failure(val message: String) : HubItemsResult()
}

/**
 * GRANULAR LOCK KEYS (2026-10-20, owner order; the app reads them in `ContentAccess.resolveLocks`, keep both in sync):
 *  - `chapter:<scope path>:<hub id>:<chapter number>`  one chapter of one hub
 *  - `item:<learning_resources.id>`                    one material
 * (`hubLockKey`, `scopeLockKey`, `packageLockKey` already exist next to the content tree.)
 */
fun chapterLockKey(scopePath: String, hubId: String, chapter: Int): String = "chapter:$scopePath:$hubId:$chapter"

fun itemLockKey(resourceId: String): String = "item:$resourceId"

/**
 * ADMIN-native N6b (Content Locks). Same data and steps as the website LocksPanel and lib/content-locks.ts
 * (listAllLocks, setLock, clearLock), with the admin's own login. Plain Supabase REST, no API route, no secret.
 *  - list: every row of content_locks, ordered by lock_key
 *  - set: upsert on lock_key with mode and label; note and updated_by are written as null, exactly like the website
 *  - clear: delete the row (the code default applies again)
 * Difference, on purpose: clear reports an error when no row was deleted (a silent refusal), the website does not.
 * 2026-10-19: the missing-table message no longer tells admins to run the old setup script on the live database.
 * 2026-10-20: [listHubItems] lists the chapters and materials of one hub so the admin can lock them one by one
 * ([chapterLockKey], [itemLockKey]); `set` and `clear` already work with any key.
 *
 * STATUS: `[~]` written, NOT compiled, NOT tested on a device.
 */
object LocksRepository {
    private val ECE_SCOPE = Regex("^ece/(sem-[12])/([^/]+)$")

    private fun JSONObject.text(key: String): String? =
        if (isNull(key)) null else optString(key, "").takeIf { it.isNotEmpty() }

    private fun nowIso(): String {
        val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        f.timeZone = TimeZone.getTimeZone("UTC")
        return f.format(Date())
    }

    suspend fun list(): LocksResult {
        val reply = AdminSupabase.get("content_locks?select=*&order=lock_key.asc")
        if (!reply.ok) {
            val msg = reply.errorText()
            val missingTable = msg.contains("relation") || msg.contains("does not exist")
            return LocksResult.Failure(
                if (missingTable) "Table missing: tell the database owner. Do not run the old setup scripts." else msg
            )
        }
        return try {
            val arr = JSONArray(reply.body)
            val out = ArrayList<LockItem>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    LockItem(
                        lockKey = o.text("lock_key") ?: continue,
                        mode = o.text("mode") ?: "",
                        label = o.text("label")
                    )
                )
            }
            LocksResult.Success(out)
        } catch (e: Exception) {
            LocksResult.Failure("Unexpected reply from the server")
        }
    }

    suspend fun set(lockKey: String, mode: String, label: String?): GrantOutcome {
        val body = JSONObject()
        body.put("lock_key", lockKey)
        body.put("mode", mode)
        body.put("label", label ?: JSONObject.NULL)
        body.put("note", JSONObject.NULL)
        body.put("updated_at", nowIso())
        body.put("updated_by", JSONObject.NULL)
        val reply = AdminSupabase.post(
            "content_locks?on_conflict=lock_key",
            body.toString(),
            "resolution=merge-duplicates,return=minimal"
        )
        if (!reply.ok) return GrantOutcome(false, reply.errorText())
        return GrantOutcome(true, "Saved \u00B7 $lockKey \u2192 $mode")
    }

    suspend fun clear(lockKey: String): GrantOutcome {
        val reply = AdminSupabase.delete("content_locks?lock_key=eq.${android.net.Uri.encode(lockKey)}")
        if (!reply.ok) return GrantOutcome(false, reply.errorText())
        val removed = try {
            JSONArray(reply.body).length()
        } catch (e: Exception) {
            0
        }
        if (removed == 0) return GrantOutcome(false, "Not cleared: no such lock or no permission")
        return GrantOutcome(true, "Cleared \u00B7 $lockKey (uses code default)")
    }

    /**
     * Every material of one hub of one course, published or not, ordered by chapter (no chapter last) and sort order.
     * Same scope rules as the student app (`LearningContentRepository`): an ECE course is stored under both
     * semesters, `freshman/math-natural` and `freshman/mathematics` are one subject, hub `short-notes` also holds the
     * old hub name `references`.
     */
    suspend fun listHubItems(scopePath: String, hubId: String): HubItemsResult {
        val hubFilter = if (hubId == "short-notes") "in.(short-notes,references)" else "eq.${android.net.Uri.encode(hubId)}"
        var reply = queryHubItems(scopeFilter(scopePath), hubFilter)
        if (!reply.ok) return HubItemsResult.Failure(reply.errorText())
        var rows = parseItems(reply.body) ?: return HubItemsResult.Failure("Unexpected reply from the server")
        val ece = ECE_SCOPE.matchEntire(scopePath)
        if (rows.isEmpty() && ece != null) {
            reply = queryHubItems("ilike.*${android.net.Uri.encode(ece.groupValues[2])}*", hubFilter)
            if (!reply.ok) return HubItemsResult.Failure(reply.errorText())
            rows = parseItems(reply.body) ?: return HubItemsResult.Failure("Unexpected reply from the server")
        }
        return HubItemsResult.Success(rows)
    }

    private suspend fun queryHubItems(scopeFilter: String, hubFilter: String): AdminReply =
        AdminSupabase.get(
            "learning_resources?select=id,title,chapter,sort_order,published" +
                "&scope_path=$scopeFilter&hub=$hubFilter" +
                "&order=chapter.asc.nullslast,sort_order.asc"
        )

    private fun scopeFilter(scopePath: String): String {
        val ece = ECE_SCOPE.matchEntire(scopePath)
        return when {
            ece != null -> {
                val other = if (ece.groupValues[1] == "sem-1") "sem-2" else "sem-1"
                "in.(${android.net.Uri.encode(scopePath)},${android.net.Uri.encode("ece/$other/${ece.groupValues[2]}")})"
            }
            scopePath == "freshman/math-natural" || scopePath == "freshman/mathematics" ->
                "in.(${android.net.Uri.encode("freshman/math-natural")},${android.net.Uri.encode("freshman/mathematics")})"
            else -> "eq.${android.net.Uri.encode(scopePath)}"
        }
    }

    private fun parseItems(body: String): List<LockableItem>? = try {
        val arr = JSONArray(body)
        val out = ArrayList<LockableItem>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.text("id") ?: continue
            out.add(
                LockableItem(
                    id = id,
                    title = o.text("title") ?: "(no title)",
                    chapter = if (o.isNull("chapter")) null else o.optInt("chapter"),
                    published = o.optBoolean("published", false)
                )
            )
        }
        out
    } catch (e: Exception) {
        null
    }
}
