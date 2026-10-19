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

/**
 * ADMIN-native N6b (Content Locks). Same data and steps as the website LocksPanel and lib/content-locks.ts
 * (listAllLocks, setLock, clearLock), with the admin's own login. Plain Supabase REST, no API route, no secret.
 *  - list: every row of content_locks, ordered by lock_key
 *  - set: upsert on lock_key with mode and label; note and updated_by are written as null, exactly like the website
 *  - clear: delete the row (the code default applies again)
 * Difference, on purpose: clear reports an error when no row was deleted (a silent refusal), the website does not.
 *
 * STATUS: `[~]` written, NOT compiled, NOT tested on a device.
 */
object LocksRepository {
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
                if (missingTable) "Table missing: run docs/content-locks-setup.sql in Supabase SQL Editor." else msg
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
}
