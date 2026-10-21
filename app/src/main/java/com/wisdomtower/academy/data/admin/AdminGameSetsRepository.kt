package com.wisdomtower.academy.data.admin

import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

/** One row of public.game_sets (docs/GAMES_HUB_PLAN.md). */
data class AdminGameSetRow(
    val id: String,
    val scopePath: String,
    val game: String,
    val chapter: Int,
    val chapterTitle: String,
    val title: String,
    val published: Boolean,
    val itemCount: Int
)

sealed class AdminGameSetsListResult {
    data class Success(val rows: List<AdminGameSetRow>) : AdminGameSetsListResult()
    data class Failure(val message: String) : AdminGameSetsListResult()
}

/**
 * Admin write path for game_sets (plan step 7). Uses the admin session via [AdminSupabase].
 * Bulk paste: items is a JSON array (defense: q/choices/correct; match: a/b).
 * `package_id` (NOT NULL) is filled by the DB trigger `game_sets_fill_package_id`.
 * Editing an existing set with the items box left as `[]` keeps the stored items (title/publish-only edit).
 */
object AdminGameSetsRepository {

    suspend fun list(scopePath: String): AdminGameSetsListResult {
        val path = "game_sets?select=id,scope_path,game,chapter,chapter_title,title,published,items" +
            "&scope_path=eq.${Uri.encode(scopePath)}&order=chapter.asc,game.asc"
        val reply = AdminSupabase.get(path)
        if (!reply.ok) {
            return AdminGameSetsListResult.Failure(reply.errorText().ifBlank { "Could not load game sets." })
        }
        return try {
            val arr = JSONArray(reply.body)
            val rows = ArrayList<AdminGameSetRow>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val items = o.optJSONArray("items")
                rows.add(
                    AdminGameSetRow(
                        id = o.optString("id"),
                        scopePath = o.optString("scope_path"),
                        game = o.optString("game"),
                        chapter = o.optInt("chapter"),
                        chapterTitle = o.optString("chapter_title", ""),
                        title = o.optString("title", ""),
                        published = o.optBoolean("published", false),
                        itemCount = items?.length() ?: 0
                    )
                )
            }
            AdminGameSetsListResult.Success(rows)
        } catch (_: Exception) {
            AdminGameSetsListResult.Failure("Could not read game sets.")
        }
    }

    suspend fun save(
        existingId: String?,
        scopePath: String,
        game: String,
        chapter: Int,
        chapterTitle: String,
        title: String,
        itemsText: String,
        published: Boolean
    ): Pair<Boolean, String> {
        if (game != "match" && game != "defense") {
            return false to "Game must be match or defense."
        }
        if (chapter < 1) return false to "Chapter must be 1 or higher."
        if (scopePath.isBlank()) return false to "Pick a subject first."
        val keepItems = !existingId.isNullOrBlank() &&
            itemsText.trim().let { it.isEmpty() || it == "[]" }
        val items: JSONArray? = if (keepItems) null else try {
            val parsed = JSONTokener(itemsText.trim()).nextValue()
            if (parsed !is JSONArray) return false to "Items must be a JSON array."
            if (parsed.length() == 0) return false to "Items array is empty."
            for (i in 0 until parsed.length()) {
                val o = parsed.optJSONObject(i) ?: return false to "Item $i is not an object."
                if (game == "match") {
                    if (o.optString("a").isBlank() || o.optString("b").isBlank()) {
                        return false to "Match item $i needs non-empty a and b."
                    }
                } else {
                    val choices = o.optJSONArray("choices")
                    if (o.optString("q").isBlank()) return false to "Defense item $i needs q."
                    if (choices == null || choices.length() != 4) {
                        return false to "Defense item $i needs choices[4]."
                    }
                    val c = o.optInt("correct", -1)
                    if (c !in 0..3) return false to "Defense item $i correct must be 0..3."
                }
            }
            parsed
        } catch (_: Exception) {
            return false to "Items JSON is not valid."
        }

        val body = JSONObject()
            .put("scope_path", scopePath)
            .put("game", game)
            .put("chapter", chapter)
            .put("chapter_title", chapterTitle)
            .put("title", title.ifBlank { chapterTitle.ifBlank { "Chapter $chapter" } })
            .put("published", published)
            .put("sort_order", chapter)
        if (items != null) body.put("items", items)

        if (existingId.isNullOrBlank()) {
            val reply = AdminSupabase.post("game_sets", body.toString(), "return=minimal")
            return if (reply.ok) {
                true to if (published) "Saved & published" else "Saved (draft)"
            } else {
                val err = reply.errorText()
                false to when {
                    err.contains("duplicate", true) || err.contains("unique", true) ->
                        "This chapter already has a $game set for this subject. Open it from the list to edit."
                    else -> err.ifBlank { "Save failed." }
                }
            }
        }
        val reply = AdminSupabase.patch(
            "game_sets?id=eq.${Uri.encode(existingId)}&select=id",
            body.toString()
        )
        return if (reply.ok) {
            true to if (published) "Updated & published" else "Updated (draft)"
        } else {
            false to reply.errorText().ifBlank { "Update failed." }
        }
    }

    suspend fun delete(id: String): Pair<Boolean, String> {
        val reply = AdminSupabase.delete("game_sets?id=eq.${Uri.encode(id)}")
        return if (reply.ok) true to "Deleted" else false to reply.errorText().ifBlank { "Delete failed." }
    }
}
