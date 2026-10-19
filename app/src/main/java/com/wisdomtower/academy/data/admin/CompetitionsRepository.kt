package com.wisdomtower.academy.data.admin

import android.net.Uri
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** One row of the competitions table (the columns the website panel reads). */
data class CompetitionItem(
    val id: String,
    val title: String,
    val audience: String?,
    val packageId: String?,
    val requiresPackage: Boolean,
    val published: Boolean,
    val countdownState: String,
    val startsAt: String,
    val durationMin: Int,
    val endsAt: String?,
    val points: Int,
    val prizeTitle: String?,
    val prizeDetails: String?
)

/** One exam of the exam hub (learning_resources, hub exams, content_type exam) that a contest can be made from. */
data class ExamOption(
    val id: String,
    val title: String,
    val packageId: String?,
    val durationMin: String?
)

/** One integrity flag the student's app recorded during a ranked exam (table competition_flags). */
data class FlagItem(
    val kind: String,
    val awayMs: Long?,
    val createdAt: String
)

/** One ranked entry of a contest, with the student's name and the flags of that entry. */
data class EntryItem(
    val id: String,
    val userId: String,
    val name: String,
    val status: String,
    val score: Int?,
    val totalCount: Int?,
    val timeTakenMs: Long?,
    val disqualified: Boolean,
    val disqualifiedReason: String?,
    val flags: List<FlagItem>
)

sealed class CompetitionsResult {
    class Success(
        val items: List<CompetitionItem>,
        val exams: List<ExamOption>,
        val examsFailed: Boolean
    ) : CompetitionsResult()

    class Failure(val message: String) : CompetitionsResult()
}

sealed class EntriesResult {
    class Success(val entries: List<EntryItem>, val flagsError: String?) : EntriesResult()
    class Failure(val message: String) : EntriesResult()
}

/**
 * ADMIN-native N9 (docs/ADMIN_NATIVE_PLAN.md). Same calls as the website CompetitionsAdminPanel.tsx and
 * EntryFlagsInfo.tsx, with the admin's own login (no secret, no website route):
 *  - list: table competitions (newest 100), and the exams of the exam hub (table learning_resources)
 *  - create: database function admin_publish_competition_from_exam
 *  - show or hide: admin_set_competition_published; pause, resume, cancel: admin_set_countdown (paused, running, cancelled)
 *  - start now: admin_start_competition_now; reschedule: admin_reschedule_competition
 *  - entries: table competition_entries (ranked, best score first, 200), names from profiles, flags from competition_flags
 *  - disqualify: admin_disqualify_entry
 * Row security (private.is_admin) and the database functions decide what the account may do.
 * All times typed in the app are Ethiopia time (EAT, UTC+3), like the website.
 *
 * STATUS: `[~]` written, NOT compiled, NOT tested on a device.
 * Note: do not write a slash followed by a star inside any comment, Kotlin nests comments.
 */
object CompetitionsRepository {
    private const val COMPETITION_COLUMNS =
        "id,title,audience,package_id,requires_package,published,countdown_state,starts_at,duration_min,ends_at,points,prize_title,prize_details"
    private const val ENTRY_COLUMNS =
        "id,user_id,status,score,total_count,time_taken_ms,submitted_at,disqualified,disqualified_reason"

    private fun JSONObject.text(key: String): String? =
        if (isNull(key)) null else optString(key, "").takeIf { it.isNotEmpty() }

    private fun JSONObject.intOrNull(key: String): Int? = if (isNull(key)) null else optInt(key)

    private fun JSONObject.longOrNull(key: String): Long? = if (isNull(key)) null else optLong(key)

    /** Same wording as friendlyError on the website. */
    fun friendlyError(message: String): String {
        val m = message
        if (m.contains("admin_only")) return "Only an admin account can do this. Please sign in again as admin."
        if (m.contains("exam_not_found")) return "That exam was not found in the exam hub."
        if (m.contains("exam_has_no_questions")) return "That exam has no questions."
        if (m.contains("start_must_be_in_the_future")) return "The start time must be in the future."
        if (m.contains("duration_required")) return "This exam has no duration. Please type the duration in minutes."
        val bad = Regex("bad_question_at_position_(\\d+)").find(m)
        if (bad != null) {
            return "Question " + bad.groupValues[1] + " of the exam is incomplete (it needs a prompt, 2 to 8 choices, one correct choice and a solution). Fix it in the exam hub and try again."
        }
        if (m.contains("competition_not_found")) return "That competition no longer exists."
        if (m.contains("already_started")) return "The contest already started, so it cannot be paused."
        if (m.contains("reason_required")) return "Please type the reason."
        if (m.contains("entry_not_found")) return "That entry was not found."
        val lower = m.lowercase(Locale.US)
        if (lower.contains("lock") || lower.contains("started")) {
            return "The database refused this change (a contest that already started is locked): " + m
        }
        return if (m.isBlank()) "Something went wrong." else m
    }

    private fun parseInstant(iso: String): Date? {
        return try {
            var s = iso.trim().replace(Regex("\\.\\d+"), "")
            if (s.endsWith("Z")) s = s.dropLast(1) + "+00:00"
            if (s.length == 19) s += "+00:00"
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(s)
        } catch (e: Exception) {
            null
        }
    }

    /** Milliseconds since 1970 of a timestamp text from the database, or null if unreadable. */
    fun instantMs(iso: String): Long? = parseInstant(iso)?.time

    /**
     * Date text yyyy-MM-dd and time text HH:mm, both typed as Ethiopia time, to a UTC timestamp text for the database.
     * Null when either text is not a real date or time.
     */
    fun eatToIso(date: String, time: String): String? {
        val d = date.trim()
        val t = time.trim()
        if (!Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(d)) return null
        if (!Regex("^\\d{2}:\\d{2}$").matches(t)) return null
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            parser.timeZone = TimeZone.getTimeZone("Africa/Addis_Ababa")
            parser.isLenient = false
            val parsed = parser.parse(d + " " + t)
            if (parsed == null) {
                null
            } else {
                val out = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                out.timeZone = TimeZone.getTimeZone("UTC")
                out.format(parsed)
            }
        } catch (e: Exception) {
            null
        }
    }

    /** upcoming, live or ended, same rule as phaseOf on the website. */
    fun phaseOf(c: CompetitionItem, nowMs: Long): String {
        val start = instantMs(c.startsAt) ?: return "upcoming"
        val endFromColumn = c.endsAt?.let { instantMs(it) }
        val end = endFromColumn ?: (start + c.durationMin * 60_000L)
        if (nowMs < start) return "upcoming"
        if (nowMs < end) return "live"
        return "ended"
    }

    /** 12m 05s style text of a number of milliseconds. */
    fun formatTime(ms: Long): String {
        val s = Math.round(ms / 1000.0)
        val minutes = s / 60
        val seconds = (s % 60).toString().padStart(2, '0')
        return "${minutes}m ${seconds}s"
    }

    private fun formatAway(ms: Long): String = formatTime(if (ms < 0L) 0L else ms)

    /** Clock time with seconds in Ethiopia time, for the flag details. */
    fun flagClock(iso: String): String {
        val date = parseInstant(iso) ?: return iso
        val out = SimpleDateFormat("HH:mm:ss", Locale.US)
        out.timeZone = TimeZone.getTimeZone("Africa/Addis_Ababa")
        return out.format(date) + " EAT"
    }

    fun flagLabel(kind: String): String = when (kind) {
        "app_background" -> "Left the app"
        "app_foreground" -> "Came back"
        "overlay_open" -> "Opened an overlay"
        "copy_attempt" -> "Tried to copy"
        else -> "Other"
    }

    /** One line about the flags of an entry, same rule as EntryFlagsInfo on the website. Null when there is nothing to say. */
    fun flagSummary(flags: List<FlagItem>): String? {
        if (flags.isEmpty()) return null
        val left = flags.count { it.kind == "app_background" }
        val back = flags.count { it.kind == "app_foreground" }
        val leaves = if (left > back) left else back
        var awayMs = 0L
        for (f in flags) {
            if (f.kind == "app_foreground") {
                val v = f.awayMs
                if (v != null && v > 0L) awayMs += v
            }
        }
        val others = flags.count { it.kind != "app_background" && it.kind != "app_foreground" }
        val parts = ArrayList<String>()
        if (leaves > 0) {
            var text = "Left the app $leaves time" + (if (leaves == 1) "" else "s")
            if (awayMs > 0L) text += " (" + formatAway(awayMs) + " away in total)"
            parts.add(text)
        }
        if (others > 0) parts.add("$others other flag" + (if (others == 1) "" else "s"))
        return if (parts.isEmpty()) null else parts.joinToString(", ")
    }

    /** One flag as a line for the details list. */
    fun flagLine(f: FlagItem): String {
        var line = flagClock(f.createdAt) + " - " + flagLabel(f.kind)
        val away = f.awayMs
        if (f.kind == "app_foreground" && away != null && away > 0L) line += " after " + formatAway(away)
        return line
    }

    suspend fun load(): CompetitionsResult = coroutineScope {
        val competitionsJob = async {
            AdminSupabase.get("competitions?select=$COMPETITION_COLUMNS&order=starts_at.desc&limit=100")
        }
        val examsJob = async {
            AdminSupabase.get(
                "learning_resources?select=id,title,package_id,durationMin:meta->>durationMin" +
                    "&hub=eq.exams&content_type=eq.exam&order=title.asc"
            )
        }
        val competitions = competitionsJob.await()
        val examsReply = examsJob.await()
        if (!competitions.ok) {
            CompetitionsResult.Failure(friendlyError(competitions.errorText()))
        } else {
            try {
                val arr = JSONArray(competitions.body)
                val items = ArrayList<CompetitionItem>(arr.length())
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    items.add(
                        CompetitionItem(
                            id = o.text("id") ?: continue,
                            title = o.text("title") ?: "",
                            audience = o.text("audience"),
                            packageId = o.text("package_id"),
                            requiresPackage = o.optBoolean("requires_package", false),
                            published = o.optBoolean("published", false),
                            countdownState = o.text("countdown_state") ?: "running",
                            startsAt = o.text("starts_at") ?: "",
                            durationMin = o.optInt("duration_min", 0),
                            endsAt = o.text("ends_at"),
                            points = o.optInt("points", 0),
                            prizeTitle = o.text("prize_title"),
                            prizeDetails = o.text("prize_details")
                        )
                    )
                }
                val exams = ArrayList<ExamOption>()
                var examsFailed = false
                if (examsReply.ok) {
                    try {
                        val examArr = JSONArray(examsReply.body)
                        for (i in 0 until examArr.length()) {
                            val o = examArr.getJSONObject(i)
                            exams.add(
                                ExamOption(
                                    id = o.text("id") ?: continue,
                                    title = o.text("title") ?: "",
                                    packageId = o.text("package_id"),
                                    durationMin = o.text("durationMin")
                                )
                            )
                        }
                    } catch (e: Exception) {
                        examsFailed = true
                    }
                } else {
                    examsFailed = true
                }
                CompetitionsResult.Success(items, exams, examsFailed)
            } catch (e: Exception) {
                CompetitionsResult.Failure("Unexpected reply from the server")
            }
        }
    }

    private fun inList(ids: List<String>): String = ids.joinToString(",") { Uri.encode(it) }

    suspend fun loadEntries(competitionId: String): EntriesResult {
        val reply = AdminSupabase.get(
            "competition_entries?select=$ENTRY_COLUMNS&competition_id=eq." + Uri.encode(competitionId) +
                "&mode=eq.ranked&order=score.desc.nullslast&limit=200"
        )
        if (!reply.ok) return EntriesResult.Failure(friendlyError(reply.errorText()))
        return try {
            val arr = JSONArray(reply.body)
            val ids = ArrayList<String>()
            val userIds = ArrayList<String>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val id = o.text("id") ?: continue
                ids.add(id)
                val userId = o.text("user_id")
                if (userId != null && !userIds.contains(userId)) userIds.add(userId)
            }
            val names = HashMap<String, String>()
            val flagsByEntry = HashMap<String, ArrayList<FlagItem>>()
            var flagsError: String? = null
            if (ids.isNotEmpty()) {
                coroutineScope {
                    val namesJob = async {
                        AdminSupabase.get("profiles?select=id,full_name&id=in.(" + inList(userIds) + ")")
                    }
                    val flagsJob = async {
                        AdminSupabase.get(
                            "competition_flags?select=entry_id,kind,detail,created_at&entry_id=in.(" + inList(ids) +
                                ")&order=created_at.asc&limit=5000"
                        )
                    }
                    val namesReply = namesJob.await()
                    val flagsReply = flagsJob.await()
                    if (namesReply.ok) {
                        try {
                            val nameArr = JSONArray(namesReply.body)
                            for (i in 0 until nameArr.length()) {
                                val p = nameArr.getJSONObject(i)
                                val pid = p.text("id") ?: continue
                                names[pid] = p.text("full_name")?.trim() ?: "(no name)"
                            }
                        } catch (e: Exception) {
                            // names stay unknown, the rows show (unknown)
                        }
                    }
                    if (flagsReply.ok) {
                        try {
                            val flagArr = JSONArray(flagsReply.body)
                            for (i in 0 until flagArr.length()) {
                                val f = flagArr.getJSONObject(i)
                                val entryId = f.text("entry_id") ?: continue
                                val detail = f.optJSONObject("detail")
                                val away = if (detail == null) null else detail.longOrNull("away_ms")
                                val list = flagsByEntry.getOrPut(entryId) { ArrayList<FlagItem>() }
                                list.add(
                                    FlagItem(
                                        kind = f.text("kind") ?: "",
                                        awayMs = away,
                                        createdAt = f.text("created_at") ?: ""
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            flagsError = "Could not load flags."
                        }
                    } else {
                        flagsError = friendlyError(flagsReply.errorText())
                    }
                }
            }
            val out = ArrayList<EntryItem>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val id = o.text("id") ?: continue
                val userId = o.text("user_id") ?: ""
                out.add(
                    EntryItem(
                        id = id,
                        userId = userId,
                        name = names[userId] ?: "(unknown)",
                        status = o.text("status") ?: "",
                        score = o.intOrNull("score"),
                        totalCount = o.intOrNull("total_count"),
                        timeTakenMs = o.longOrNull("time_taken_ms"),
                        disqualified = o.optBoolean("disqualified", false),
                        disqualifiedReason = o.text("disqualified_reason"),
                        flags = flagsByEntry[id] ?: ArrayList<FlagItem>()
                    )
                )
            }
            EntriesResult.Success(out, flagsError)
        } catch (e: Exception) {
            EntriesResult.Failure("Unexpected reply from the server")
        }
    }

    /** Calls one admin database function. Returns null on success, otherwise a short message. */
    private suspend fun callRpc(name: String, body: JSONObject): String? {
        val reply = AdminSupabase.post("rpc/$name", body.toString(), "count=none")
        if (!reply.ok) return friendlyError(reply.errorText())
        return null
    }

    private fun textOrNull(value: String): Any = if (value.isBlank()) JSONObject.NULL else value.trim()

    /** Create a contest from an exam. startsIso comes from eatToIso. Returns null on success, otherwise a short message. */
    suspend fun create(
        resourceId: String,
        startsIso: String,
        title: String,
        audience: String,
        durationMin: Int?,
        points: Int,
        prizeTitle: String,
        prizeDetails: String,
        requiresPackage: Boolean,
        publish: Boolean
    ): String? {
        val duration: Any = if (durationMin == null) JSONObject.NULL else durationMin
        val body = JSONObject()
            .put("p_resource_id", resourceId)
            .put("p_starts_at", startsIso)
            .put("p_title", textOrNull(title))
            .put("p_audience", textOrNull(audience))
            .put("p_duration_min", duration)
            .put("p_points", points)
            .put("p_prize_title", textOrNull(prizeTitle))
            .put("p_prize_details", textOrNull(prizeDetails))
            .put("p_requires_package", requiresPackage)
            .put("p_publish", publish)
        return callRpc("admin_publish_competition_from_exam", body)
    }

    suspend fun setPublished(id: String, published: Boolean): String? =
        callRpc("admin_set_competition_published", JSONObject().put("p_id", id).put("p_published", published))

    /** state is paused, running or cancelled. */
    suspend fun setCountdown(id: String, state: String): String? =
        callRpc("admin_set_countdown", JSONObject().put("p_id", id).put("p_state", state))

    suspend fun startNow(id: String): String? =
        callRpc("admin_start_competition_now", JSONObject().put("p_id", id))

    suspend fun reschedule(id: String, startsIso: String, durationMin: Int?): String? {
        val duration: Any = if (durationMin == null) JSONObject.NULL else durationMin
        val body = JSONObject()
            .put("p_id", id)
            .put("p_starts_at", startsIso)
            .put("p_duration_min", duration)
        return callRpc("admin_reschedule_competition", body)
    }

    suspend fun disqualify(entryId: String, reason: String): String? =
        callRpc("admin_disqualify_entry", JSONObject().put("p_entry_id", entryId).put("p_reason", reason))
}
