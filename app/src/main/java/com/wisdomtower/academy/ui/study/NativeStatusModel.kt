package com.wisdomtower.academy.ui.study

import com.wisdomtower.academy.data.content.ProgressRow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

/**
 * Logic of the native "Your status" screen (F7 part 5). Port of `computeStudentAnalytics` and
 * `resolveStudentTrackBenchmark` in `website-reference/src/lib/student-knowledge-base.ts`, with ONE owner-rule change:
 * REAL DATA ONLY (owner, 2026-10-11: no fabricated numbers). The website invents values when the student has no
 * records (42 questions with 34 correct, 91% focus, a 4-day streak, 35 minutes of study per day since sign-up, a focus
 * ratio never below 65%, a fixed split of the total over the weekdays, a reading speed computed from the track benchmark,
 * a solve pace of benchmark + 0.1 minutes). None of that is copied: a figure that cannot be measured is `null` and the
 * screen shows a dash. Not ported because they cannot be measured from `learning_progress`: reading speed (WPM) and the
 * reading-method diagnostic, solve pace, the weekly percent of the target (the table has lifetime seconds per item, no
 * per-day log), the weekday chart.
 *
 * 2026-10-23 (docs/STATUS_CARD_PLAN.md, slice 2b): WORDS ONLY. The standing names, the alert texts, the retention texts and
 * the tips were rewritten in plain, human language (owner: the old words sounded like a machine). No number, rule or
 * threshold changed.
 */
data class StatusTrack(
    val id: String,
    val name: String,
    val category: String,
    val weeklyTargetHours: Double,
    val targetMinutesPerQuestion: Double,
    val eliteAccuracy: Int,
    val honorsAccuracy: Int,
    val passAccuracy: Int
)

/** Same tracks, names, categories, targets and thresholds as `ACADEMIC_KNOWLEDGE_BASE`, same order. */
val STATUS_TRACKS: List<StatusTrack> = listOf(
    StatusTrack("freshman-natural", "Freshman (Natural Science Stream)", "University Freshman", 12.0, 1.3, 88, 78, 60),
    StatusTrack("freshman-social", "Freshman (Social Science Stream)", "University Freshman", 10.5, 1.2, 88, 78, 60),
    StatusTrack("freshman-all", "Freshman (Complete 21 Subjects Hub)", "University Freshman", 14.0, 1.25, 88, 78, 60),
    StatusTrack("grade-12", "Grade 12 National Matriculation", "Secondary Education", 14.0, 1.2, 85, 75, 55),
    StatusTrack("grade-11", "Grade 11 Secondary Curriculum", "Secondary Education", 12.0, 1.2, 85, 75, 55),
    StatusTrack("grade-10", "Grade 10 Secondary Curriculum", "Secondary Education", 11.0, 1.2, 82, 72, 55),
    StatusTrack("grade-9", "Grade 9 Secondary Curriculum", "Secondary Education", 10.0, 1.2, 82, 72, 55),
    StatusTrack("remedial", "Remedial University Program", "Remedial Program", 11.0, 1.25, 82, 72, 50),
    StatusTrack("ece-engineering", "ECE Year 3 Semester 1 (Engineering)", "Department Special Track", 15.0, 2.0, 85, 75, 60)
)

/** Prints 12.0 as "12" and 10.5 as "10.5", like JavaScript. */
fun formatStatusNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

/**
 * Steps 1 and 4 of `resolveStudentTrackBenchmark` (education level text first, else the Freshman default by stream).
 * Steps 2 and 3 are skipped on purpose: step 2 (enrolled package ids) gives the same answer as step 4 for the website's
 * default course lists, and step 3 compares resource ids with paths like "grade/12/" while the real ids are UUIDs, so it
 * can never match.
 */
fun resolveStatusTrack(educationLevel: String?, stream: String?): StatusTrack {
    val edu = (educationLevel ?: "").lowercase().trim()
    val st = (stream ?: "").lowercase().trim()
    fun track(id: String): StatusTrack = STATUS_TRACKS.first { it.id == id }
    return when {
        edu.contains("12") || edu.contains("matric") -> track("grade-12")
        edu.contains("11") -> track("grade-11")
        edu.contains("10") -> track("grade-10")
        edu.contains("9") -> track("grade-9")
        edu.contains("remedial") -> track("remedial")
        edu.contains("ece") || edu.contains("electrical") || edu.contains("engineering") -> track("ece-engineering")
        edu.contains("freshman") -> if (edu.contains("social") || st.contains("social")) track("freshman-social") else track("freshman-natural")
        st.contains("social") -> track("freshman-social")
        else -> track("freshman-natural")
    }
}

data class StatusRecommendation(
    val id: String,
    val category: String,
    val title: String,
    val description: String,
    val actionableStep: String
)

data class StatusAlert(
    val id: String,
    val severity: String,
    val signal: String,
    val observedData: String,
    val immediateAction: String
)

data class StatusSummary(
    val itemCount: Int,
    val totalStudyMinutes: Int,
    val totalStudyHours: Double,
    /** null when no focus time was recorded */
    val focusRatioPct: Int?,
    val questionsAttempted: Int,
    val questionsCorrect: Int,
    /** null when no question was answered */
    val accuracyPct: Double?,
    val streakDays: Int,
    /** null when accuracy is unknown */
    val tier: String?,
    /** null unless both accuracy and focus are known */
    val retentionIndexPct: Int?,
    val retentionRating: String?,
    val retentionExplanation: String?,
    val recommendations: List<StatusRecommendation>,
    val alerts: List<StatusAlert>
)

private fun utcDay(daysAgo: Int): String {
    val zone = TimeZone.getTimeZone("UTC")
    val cal = Calendar.getInstance(zone)
    cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    fmt.timeZone = zone
    return fmt.format(cal.time)
}

fun computeStatus(rows: List<ProgressRow>, track: StatusTrack): StatusSummary {
    var totalSec = 0L
    var focusSec = 0L
    var attempted = 0
    var correct = 0
    val days = HashSet<String>()
    for (r in rows) {
        totalSec += r.totalSeconds
        focusSec += r.focusSeconds
        attempted += r.quizAttempted
        correct += r.quizCorrect
        val opened = r.lastOpenedAt
        if (opened != null && opened.length >= 10) days.add(opened.substring(0, 10))
    }

    val minutes = (totalSec / 60.0).roundToInt()
    val hours = ((minutes / 60.0) * 10).roundToInt() / 10.0
    val focus: Int? =
        if (totalSec > 0L && focusSec > 0L) minOf(100, (focusSec * 100.0 / totalSec).roundToInt()) else null
    val accuracy: Double? =
        if (attempted > 0) (correct * 1000.0 / attempted).roundToInt() / 10.0 else null

    // Streak: consecutive days (UTC dates, like the website) ending today or yesterday
    var streak = 0
    val startAgo = when {
        days.contains(utcDay(0)) -> 0
        days.contains(utcDay(1)) -> 1
        else -> -1
    }
    if (startAgo >= 0) {
        var i = startAgo
        while (days.contains(utcDay(i))) {
            streak++
            i++
        }
    }

    // Plain standing words (2026-10-23). Only displayed, never compared anywhere else.
    val tier: String? = accuracy?.let { a ->
        when {
            a >= track.eliteAccuracy -> "Top performer"
            a >= track.honorsAccuracy -> "Strong"
            a < track.passAccuracy -> "Needs work"
            else -> "On track"
        }
    }

    var retentionIndex: Int? = null
    var retentionRating: String? = null
    var retentionExplanation: String? = null
    if (accuracy != null && focus != null) {
        val idx = (accuracy * 0.75 + focus * 0.25).roundToInt()
        retentionIndex = idx
        when {
            idx >= 80 -> {
                retentionRating = "You remember well"
                retentionExplanation = "You answer most questions right and you stay focused while you study. That is what makes things stick. What you practised recently should come back to you in the exam."
            }
            idx < 62 -> {
                retentionRating = "You may be forgetting"
                retentionExplanation = "Your answers get worse when you come back to a topic after a few days. Try questions on it today, without looking at the notes first."
            }
            else -> {
                retentionRating = "You remember some"
                retentionExplanation = "You know the basics when you are tested soon after studying. If you go back to a topic after two days, it will stay with you much longer."
            }
        }
    }

    val recommendations = listOf(
        StatusRecommendation(
            id = "rec-active-recall",
            category = "Test yourself",
            title = "Test yourself instead of re-reading",
            description = "Reading the same notes again feels like learning, but it is not. Answering questions without looking is what really sticks.",
            actionableStep = "After every 20 minutes of reading, close the chapter and answer at least 8 practice questions."
        ),
        StatusRecommendation(
            id = "rec-pomodoro-focus",
            category = "Stay focused",
            title = "Study in 25-minute rounds",
            description = (if (focus != null) "You stayed focused for $focus% of your study time. " else "") +
                "Long sessions with no break make you tired and your mind starts to wander.",
            actionableStep = "Use the Timer: 25 minutes on one thing only, then a real 5-minute break."
        ),
        StatusRecommendation(
            id = "rec-spaced-repetition",
            category = "Daily habit",
            title = "Review yesterday before you start new things",
            description = "Most people forget a large part of new material within two days unless they go over it once the next day.",
            actionableStep = "Start each study day with 10 minutes on yesterday's questions, then open the new chapter."
        ),
        StatusRecommendation(
            id = "rec-exam-pacing",
            category = "Exam speed",
            title = "Practise with a countdown",
            description = "For ${track.name}, aim for about ${formatStatusNumber(track.targetMinutesPerQuestion)} minutes per question.",
            actionableStep = "In multiple-choice questions, cross out two wrong choices in the first 20 seconds."
        )
    )

    val alerts = ArrayList<StatusAlert>()
    if (minutes == 0) {
        alerts.add(
            StatusAlert(
                id = "stop-inactivity-gap",
                severity = "Warning",
                signal = "No study time yet",
                observedData = "0 hours so far toward your ${formatStatusNumber(track.weeklyTargetHours)}-hour weekly goal for ${track.name}.",
                immediateAction = "Open any chapter today and study for 20 minutes."
            )
        )
    }
    if (accuracy != null && accuracy < 60.0) {
        alerts.add(
            StatusAlert(
                id = "stop-unreviewed-misses",
                severity = "Critical",
                signal = "Many wrong answers (under 60% right)",
                observedData = "You got ${formatStatusNumber(accuracy)}% right ($correct of $attempted). Moving on without fixing mistakes keeps the mistakes.",
                immediateAction = "Stop new questions for now. Open the ones you missed, read the solution step by step, then try again."
            )
        )
    }
    if (minutes > 10 && focus != null && focus < 60) {
        alerts.add(
            StatusAlert(
                id = "stop-idle-distraction",
                severity = "Warning",
                signal = "Too much time off focus",
                observedData = "You stayed focused for $focus% of your study time. More than 40% went to waiting, or to the app being in the background.",
                immediateAction = "Try 25-minute rounds in full-screen reading mode, with your phone notifications off."
            )
        )
    }
    if (minutes > 0 && streak == 0) {
        alerts.add(
            StatusAlert(
                id = "stop-broken-streak",
                severity = "Warning",
                signal = "Your streak stopped (0 days)",
                observedData = "You have not studied for a few days. Long gaps make you forget more.",
                immediateAction = "Do one short 15-minute round of flashcards or questions today to start your streak again."
            )
        )
    }
    if (alerts.isEmpty()) {
        val focusPart = if (focus != null) "$focus% focus and a " else "a "
        alerts.add(
            StatusAlert(
                id = "signal-healthy-habits",
                severity = "Warning",
                signal = "Nothing to worry about",
                observedData = "So far: ${formatStatusNumber(hours)} hours studied, $focusPart$streak-day streak. You are studying steadily.",
                immediateAction = "Keep testing yourself, and take one full mock exam each weekend."
            )
        )
    }

    return StatusSummary(
        itemCount = rows.size,
        totalStudyMinutes = minutes,
        totalStudyHours = hours,
        focusRatioPct = focus,
        questionsAttempted = attempted,
        questionsCorrect = correct,
        accuracyPct = accuracy,
        streakDays = streak,
        tier = tier,
        retentionIndexPct = retentionIndex,
        retentionRating = retentionRating,
        retentionExplanation = retentionExplanation,
        recommendations = recommendations,
        alerts = alerts
    )
}
