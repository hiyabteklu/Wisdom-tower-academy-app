package com.wisdomtower.academy.data.admin

import org.json.JSONArray

// S3 (docs/PLAN_TELEGRAM_PAID_NATIVE_V2.md, 2026-10-23). Two small helpers next to LocksRepository, used only by the
// admin Locks tree. Setting a PACKAGE action first removes every chapter / material lock of that package ("apply to all"),
// so the admin then makes the trial exceptions one by one.
//  - clearMany: delete several content_locks rows by key (in chunks).
//  - itemIdsInPrefix: of the given learning_resources ids, which ones live under a scope prefix (for example grade/9/).
// Plain Supabase REST with the admin's own login, same as LocksRepository. STATUS: [~] written, NOT compiled.
// Note: do not write a slash followed by a star inside any comment, Kotlin nests comments.
object LocksBulk {
    private const val CHUNK = 40

    private fun quoted(values: List<String>): String =
        values.joinToString(",") { android.net.Uri.encode("\"" + it + "\"") }

    suspend fun clearMany(keys: List<String>): GrantOutcome {
        if (keys.isEmpty()) return GrantOutcome(true, "Nothing to clear")
        var removed = 0
        for (chunk in keys.chunked(CHUNK)) {
            val reply = AdminSupabase.delete("content_locks?lock_key=in.(${quoted(chunk)})")
            if (!reply.ok) return GrantOutcome(false, reply.errorText())
            removed += try {
                JSONArray(reply.body).length()
            } catch (e: Exception) {
                0
            }
        }
        return GrantOutcome(true, "Cleared $removed")
    }

    /** Null when the server could not be asked (the caller must stop, never guess). */
    suspend fun itemIdsInPrefix(ids: List<String>, prefix: String): List<String>? {
        val out = ArrayList<String>()
        for (chunk in ids.chunked(CHUNK)) {
            val reply = AdminSupabase.get(
                "learning_resources?select=id&id=in.(${quoted(chunk)})&scope_path=like.${android.net.Uri.encode(prefix)}*"
            )
            if (!reply.ok) return null
            try {
                val arr = JSONArray(reply.body)
                for (i in 0 until arr.length()) {
                    val id = arr.getJSONObject(i).optString("id", "")
                    if (id.isNotEmpty()) out.add(id)
                }
            } catch (e: Exception) {
                return null
            }
        }
        return out
    }
}
