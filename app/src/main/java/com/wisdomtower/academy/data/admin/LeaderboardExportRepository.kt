package com.wisdomtower.academy.data.admin

import android.content.Context
import org.json.JSONObject

data class LeaderboardExportWinner(
    val rank: Int,
    val name: String,
    val score: Int,
    val total: Int,
    val timeTakenMs: Long?,
    val submittedAt: String?
)

data class LeaderboardExportData(
    val competitionTitle: String,
    val examTitle: String,
    val subjectTitle: String,
    val eventDate: String,
    val winners: List<LeaderboardExportWinner>
)

sealed class LeaderboardExportResult {
    data class Success(val data: LeaderboardExportData) : LeaderboardExportResult()
    data class Failure(val message: String) : LeaderboardExportResult()
}

/**
 * The export data is fetched through an admin-only SECURITY DEFINER RPC.
 * The function checks private.is_admin() on every request; no UI-only admin flag is trusted.
 * U5 (2026-11-06, owner): NO minimum number of participants. The server now returns 0, 1, 2 or 3 winners
 * (migration u5_admin_competition_export_no_minimum) and the image is drawn with whoever exists.
 */
object LeaderboardExportRepository {
    suspend fun load(context: Context, competitionId: String): LeaderboardExportResult {
        val body = JSONObject().put("p_competition_id", competitionId).toString()
        val reply = AdminSupabase.rpc("admin_competition_export", body)
        if (!reply.ok) {
            val raw = reply.errorText()
            val message = when {
                raw.contains("admin_only", ignoreCase = true) -> "This export is available to verified admins only."
                raw.contains("competition_not_found", ignoreCase = true) -> "This competition could not be found."
                else -> raw
            }
            return LeaderboardExportResult.Failure(message)
        }

        return try {
            val json = JSONObject(reply.body)
            when (json.optString("status")) {
                "not_closed" -> LeaderboardExportResult.Failure("The leaderboard export opens after the competition closes.")
                "ok" -> {
                    val top = json.optJSONArray("top")
                    val winners = ArrayList<LeaderboardExportWinner>()
                    if (top != null) {
                        for (index in 0 until top.length()) {
                            val row = top.getJSONObject(index)
                            val rank = row.optInt("rank", 0)
                            if (rank !in 1..3) continue
                            winners.add(
                                LeaderboardExportWinner(
                                    rank = rank,
                                    name = row.optString("name", "Student"),
                                    score = row.optInt("score", 0),
                                    total = row.optInt("total", 0),
                                    timeTakenMs = if (row.isNull("time_taken_ms")) null else row.optLong("time_taken_ms"),
                                    submittedAt = row.optString("submitted_at").takeIf { it.isNotBlank() && it != "null" }
                                )
                            )
                        }
                    }
                    val scope = json.optString("subject_scope")
                    LeaderboardExportResult.Success(
                        LeaderboardExportData(
                            competitionTitle = json.optString("competition_title"),
                            examTitle = json.optString("exam_title"),
                            subjectTitle = resolveSubjectTitle(context, scope),
                            eventDate = json.optString("event_date"),
                            winners = winners.sortedBy { it.rank }
                        )
                    )
                }
                else -> LeaderboardExportResult.Failure("The server returned an unexpected export response.")
            }
        } catch (_: Exception) {
            LeaderboardExportResult.Failure("The leaderboard export data could not be read.")
        }
    }

    private fun resolveSubjectTitle(context: Context, scopePath: String): String {
        if (scopePath.isBlank()) return "Subject title unavailable"
        return runCatching {
            val root = context.assets.open("data/catalog_snapshot.json").bufferedReader().use { JSONObject(it.readText()) }
            val courses = root.optJSONArray("courses") ?: return@runCatching scopePath.substringAfterLast('/').uppercase()
            val parts = scopePath.split('/')
            val slug = parts.last()
            for (index in 0 until courses.length()) {
                val course = courses.getJSONObject(index)
                val path = course.optString("path")
                val matches = when (parts.firstOrNull()) {
                    "grade" -> parts.size >= 3 && path.endsWith("/grades/${parts[1]}/$slug")
                    "freshman" -> path.endsWith("/freshman/$slug")
                    "remedial" -> path.endsWith("/remedial/$slug")
                    "ece" -> path.endsWith("/$slug")
                    else -> path.endsWith("/$slug")
                }
                if (matches) {
                    val name = course.optString("name").trim()
                    if (name.isNotEmpty()) return@runCatching name
                }
            }
            slug.replace('-', ' ').replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }.getOrDefault(scopePath.substringAfterLast('/').uppercase())
    }
}
