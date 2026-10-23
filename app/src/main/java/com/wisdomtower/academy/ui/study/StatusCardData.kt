package com.wisdomtower.academy.ui.study

import android.content.Context
import com.wisdomtower.academy.data.auth.AuthStateManager
import com.wisdomtower.academy.data.content.ExamAttemptRow
import com.wisdomtower.academy.data.content.ExamAttemptsFetchResult
import com.wisdomtower.academy.data.content.ExamAttemptsReader
import com.wisdomtower.academy.data.content.MySubjectsStore
import com.wisdomtower.academy.data.content.ProgressRow
import com.wisdomtower.academy.data.content.ResourceCatalogIndex
import com.wisdomtower.academy.data.content.StudyDayLogFetchResult
import com.wisdomtower.academy.data.content.StudyDayLogReader
import com.wisdomtower.academy.data.content.StudyDayRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * STATUS CARD (docs/STATUS_CARD_PLAN.md, slice S1): DATA ONLY, no UI, no drawing. One plain data class ([StatusCardData])
 * that both the "Your week" block (S2) and the 9:16 picture (S3) read, so the two can never disagree.
 *
 * REAL DATA ONLY (same rule as Your status): a figure that cannot be known is null (the UI shows a dash and a grey ring),
 * never 0 and never invented. This includes the student ID: only the real ID number the profile already has
 * (`UserProfile.studentIdNumber`) is used. `StudentIdGenerator.computeStudentId` falls back to a made-up number when the
 * profile has none; that fallback is NOT used here. No ID = the line is left out.
 *
 * The pure functions (names starting `sc`, and [assembleStatusCardData]) take plain values and have no Android or network
 * calls, so they are easy to review. Only [buildStatusCardData] reads things (catalog, exam attempts, day log).
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */

/** One of the last 7 days, oldest first. [seconds] = null means this day cannot be told (never drawn as a made-up 0). */
data class StatusCardDay(
    /** one capital letter for the bar label, e.g. "M" */
    val letter: String,
    /** full weekday name, e.g. "Monday" */
    val dayName: String,
    val seconds: Int?,
    val isToday: Boolean
) {
    val minutes: Int? get() = seconds?.let { it / 60 }
}

data class StatusCardSubject(
    val key: String,
    val name: String,
    /** 0..100, null when the subject has no items */
    val completionPct: Int?,
    val itemsOpened: Int,
    val itemsTotal: Int,
    val studySeconds: Long,
    /** null when no quiz question was answered in this subject */
    val accuracyPct: Int?,
    val quizAttempted: Int,
    val quizCorrect: Int,
    /** true for the one subject shown with a rose dot (the weak subject) */
    val isWeak: Boolean
)

data class StatusCardWeak(
    val subject: StatusCardSubject,
    /** true = flagged because accuracy is low; false = flagged because the subject is barely opened */
    val byAccuracy: Boolean,
    /** points the LAST exam of this subject went down against the attempt before it; null when it did not go down or cannot be compared */
    val examDropPoints: Int?,
    /** first numbered chapter that still has unopened items; null when none is known */
    val startChapter: Int?
)

data class StatusCardData(
    /** never blank (fallback "Scholar") */
    val studentName: String,
    /** e.g. "WTA-01234"; null = the profile has no real ID, leave the line out */
    val studentId: String?,
    val trackName: String,
    /** plain wording ("Top performer", "Strong", "On track", "Needs work"); null when accuracy is unknown */
    val standing: String?,
    /** e.g. "17-23 Oct" (last 7 days ending today, from the phone dates) */
    val weekRangeLabel: String,
    /** minutes studied in the known days of the last 7; null when no day is known */
    val weekMinutes: Int?,
    val weekTargetHours: Double,
    /** 0..1 = min(done / target, 1); null when [weekMinutes] is null */
    val weekFill: Float?,
    /** true when all 7 days are known (not a partial saved copy) */
    val weekComplete: Boolean,
    val accuracyPct: Int?,
    val questionsCorrect: Int,
    val questionsAttempted: Int,
    /** null when progress could not be read */
    val streakDays: Int?,
    /** 0..1 = min(days / 7, 1); null when [streakDays] is null */
    val streakFill: Float?,
    val focusPct: Int?,
    /** always 7 entries, oldest first, today last */
    val days: List<StatusCardDay>,
    val bestDayName: String?,
    val bestDayMinutes: Int?,
    /** at most 5, most studied first, the weak subject always included */
    val subjects: List<StatusCardSubject>,
    val weak: StatusCardWeak?,
    /** shown instead of the weak box when [weak] is null and nothing is opened yet; null otherwise */
    val weakNote: String?,
    /** ONE plain sentence, always filled */
    val nextStep: String,
    /** "Saved copy from 23 Oct 2026" when any part comes from the offline copy; null when everything is live */
    val asOfLabel: String?,
    /** "Saved 23 Oct 2026" for the footer */
    val savedLabel: String
)

private const val SC_MAX_SUBJECTS = 5
private const val SC_MIN_QUIZ_ANSWERS = 5
private const val SC_LOW_ACCURACY = 60
private const val SC_LOW_COMPLETION = 25

/** Plain standing words. Accepts the old tier names and the new ones, so renaming the tier later (slice 2b) changes nothing here. */
internal fun scPlainStanding(tier: String?): String? = when (tier) {
    null -> null
    "Elite Scholar Rank", "Top performer" -> "Top performer"
    "Honors Distinction", "Strong" -> "Strong"
    "Solid Foundational", "On track" -> "On track"
    "Critical Review Required", "Needs work" -> "Needs work"
    else -> tier
}

/** "WTA-01234" from the profile's own ID number (last 5 digits, same shape as the ID card), or null when there is none. */
internal fun scStudentId(raw: String?): String? {
    val digits = raw.orEmpty().filter { it.isDigit() }
    if (digits.isBlank()) return null
    val five = digits.takeLast(5).padStart(5, '0')
    return if (five == "00000") null else "WTA-$five"
}

/** "45 min", "5 h", "5 h 20 min". */
internal fun scDuration(minutes: Int): String {
    val m = maxOf(minutes, 0)
    val h = m / 60
    val r = m % 60
    return when {
        h == 0 -> "$r min"
        r == 0 -> "$h h"
        else -> "$h h $r min"
    }
}

/**
 * The last 7 local days, oldest first. A day without a log row is 0 seconds ONLY when the day is known: [dayLogKnown] false
 * (nothing read, nothing saved) makes every day unknown, and with a saved copy ([savedAtMs]) every day after the day the
 * copy was saved is unknown. Same rule as StatusDayPanel.
 */
internal fun scBuildDays(rows: List<StudyDayRow>, dayLogKnown: Boolean, savedAtMs: Long?, nowMs: Long): List<StatusCardDay> {
    val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val nameFmt = SimpleDateFormat("EEEE", Locale.getDefault())
    val bySeconds = rows.associate { it.day to it.seconds }
    val savedKey = savedAtMs?.let { keyFmt.format(Date(it)) }
    val cal = Calendar.getInstance()
    cal.timeInMillis = nowMs
    cal.add(Calendar.DAY_OF_YEAR, -6)
    val out = ArrayList<StatusCardDay>(7)
    for (i in 0 until 7) {
        val key = keyFmt.format(cal.time)
        val name = nameFmt.format(cal.time)
        val known = dayLogKnown && (savedKey == null || key <= savedKey)
        out.add(
            StatusCardDay(
                letter = name.take(1).uppercase(Locale.getDefault()),
                dayName = name,
                seconds = if (known) (bySeconds[key] ?: 0) else null,
                isToday = i == 6
            )
        )
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }
    return out
}

/** "17-23 Oct", or "28 Oct-3 Nov" when the 7 days cross a month. */
internal fun scWeekRangeLabel(nowMs: Long): String {
    val end = Calendar.getInstance()
    end.timeInMillis = nowMs
    val start = Calendar.getInstance()
    start.timeInMillis = nowMs
    start.add(Calendar.DAY_OF_YEAR, -6)
    val d = SimpleDateFormat("d", Locale.US)
    val m = SimpleDateFormat("MMM", Locale.US)
    val sameMonth = start.get(Calendar.MONTH) == end.get(Calendar.MONTH) && start.get(Calendar.YEAR) == end.get(Calendar.YEAR)
    return if (sameMonth) {
        "${d.format(start.time)}-${d.format(end.time)} ${m.format(end.time)}"
    } else {
        "${d.format(start.time)} ${m.format(start.time)}-${d.format(end.time)} ${m.format(end.time)}"
    }
}

private fun scSubject(name: String, b: SubjectBreakdown): StatusCardSubject {
    val t = b.totals
    return StatusCardSubject(
        key = b.subjectKey,
        name = name,
        completionPct = t.completionPct?.roundToInt(),
        itemsOpened = t.itemsOpened,
        itemsTotal = t.itemsTotal,
        studySeconds = t.studySeconds,
        accuracyPct = t.accuracyPct,
        quizAttempted = t.quizAttempted,
        quizCorrect = t.quizCorrect,
        isWeak = false
    )
}

/**
 * WEAK SUBJECT RULE (plan, deterministic):
 * 1. Among subjects with at least 5 quiz answers: the lowest accuracy.
 * 2. Else among subjects with something opened: the lowest completion.
 * 3. Else none.
 * The chosen one is flagged only when the number is really low (accuracy under 60 or completion under 25). Otherwise
 * null (the "good week" step is shown). A rule that applies is not skipped for the next rule.
 */
internal fun scPickWeak(all: List<Pair<StatusCardSubject, SubjectBreakdown>>): StatusCardWeak? {
    val byAccuracy = all
        .filter { it.first.quizAttempted >= SC_MIN_QUIZ_ANSWERS && it.first.accuracyPct != null }
        .minWithOrNull(compareBy<Pair<StatusCardSubject, SubjectBreakdown>>({ it.first.accuracyPct ?: 101 }, { it.first.name }))
    val chosen: Pair<StatusCardSubject, SubjectBreakdown>
    val viaAccuracy: Boolean
    if (byAccuracy != null) {
        if ((byAccuracy.first.accuracyPct ?: 101) >= SC_LOW_ACCURACY) return null
        chosen = byAccuracy
        viaAccuracy = true
    } else {
        val byOpen = all
            .filter { it.first.itemsOpened > 0 && it.first.completionPct != null }
            .minWithOrNull(compareBy<Pair<StatusCardSubject, SubjectBreakdown>>({ it.first.completionPct ?: 101 }, { it.first.name }))
            ?: return null
        if ((byOpen.first.completionPct ?: 101) >= SC_LOW_COMPLETION) return null
        chosen = byOpen
        viaAccuracy = false
    }
    val b = chosen.second
    val delta = b.exams.attempts.firstOrNull()?.deltaVsPrevious
    val drop = if (delta != null && delta < 0) -delta else null
    val chapter = b.chapters.firstOrNull { it.chapter != null && it.totals.itemsOpened < it.totals.itemsTotal }?.chapter
    return StatusCardWeak(
        subject = chosen.first.copy(isWeak = true),
        byAccuracy = viaAccuracy,
        examDropPoints = drop,
        startChapter = chapter
    )
}

/** ONE next step, fixed templates, first match wins, no AI, no randomness. */
internal fun scNextStep(
    weak: StatusCardWeak?,
    weekMinutes: Int?,
    weekComplete: Boolean,
    targetHours: Double,
    hasAnyData: Boolean
): String {
    if (weak != null) {
        val name = weak.subject.name
        // 1. last exam of the weak subject went down
        val drop = weak.examDropPoints
        if (drop != null) {
            val pts = if (drop == 1) "1 point" else "$drop points"
            return "Your last $name exam dropped $pts. Redo the questions you missed."
        }
        // 2. accuracy low
        if (weak.byAccuracy) {
            return "In $name you got ${weak.subject.quizCorrect} of ${weak.subject.quizAttempted} right. Redo the missed ones before opening new chapters."
        }
        // 3. barely opened
        val k = weak.startChapter
        val start = if (k != null) "Start with Chapter $k short notes today." else "Start with the first chapter's short notes today."
        return "You opened ${weak.subject.itemsOpened} of ${weak.subject.itemsTotal} items in $name. $start"
    }
    // 4. under the weekly hours (only when all 7 days are known, a partial saved copy would be unfair)
    if (weekComplete && weekMinutes != null && targetHours > 0.0 && weekMinutes < targetHours * 60.0) {
        val done = ((weekMinutes / 60.0) * 10).roundToInt() / 10.0
        return "You studied ${formatStatusNumber(done)} of ${formatStatusNumber(targetHours)} hours this week. 20 more minutes today helps."
    }
    // nothing is known yet: do not say "good week"
    if (!hasAnyData) return "Open a subject today and study for 20 minutes. Your numbers will show here."
    // 5. nothing to flag
    return "Good week. Keep the streak alive, 20 minutes today is enough."
}

/**
 * Pure assembly of [StatusCardData] from values that are already read. [subjects] = (name, breakdown) per subject, the same
 * list the By subject panel shows. [dayLogKnown] false = the day log could not be read and nothing is saved.
 * [dayLogSavedAtMs] / [examsSavedAtMs] are set only when that part comes from the offline copy.
 */
internal fun assembleStatusCardData(
    studentName: String?,
    studentIdNumber: String?,
    track: StatusTrack,
    summary: StatusSummary,
    progressKnown: Boolean,
    dayRows: List<StudyDayRow>,
    dayLogKnown: Boolean,
    dayLogSavedAtMs: Long?,
    examsSavedAtMs: Long?,
    subjects: List<Pair<String, SubjectBreakdown>>,
    nowMs: Long
): StatusCardData {
    val days = scBuildDays(dayRows, dayLogKnown, dayLogSavedAtMs, nowMs)
    val knownSeconds = days.mapNotNull { it.seconds }
    val weekMinutes: Int? = if (knownSeconds.isEmpty()) null else (knownSeconds.sum() / 60.0).roundToInt()
    val weekComplete = knownSeconds.size == 7
    val weekFill: Float? = weekMinutes?.let {
        if (track.weeklyTargetHours > 0.0) (it / 60.0 / track.weeklyTargetHours).coerceIn(0.0, 1.0).toFloat() else 0f
    }
    val best = days.filter { (it.seconds ?: 0) > 0 }.maxByOrNull { it.seconds ?: 0 }

    val hasProgress = progressKnown && summary.itemCount > 0
    val streak: Int? = if (hasProgress) summary.streakDays else null

    val all = subjects.map { (name, b) -> scSubject(name, b) to b }
    val weak = scPickWeak(all)
    val ordered = all.map { it.first }.sortedWith(compareByDescending<StatusCardSubject> { it.studySeconds }.thenBy { it.name })
    var shown = ordered.take(SC_MAX_SUBJECTS)
    if (weak != null && shown.none { it.key == weak.subject.key }) {
        shown = shown.take(SC_MAX_SUBJECTS - 1) + ordered.first { it.key == weak.subject.key }
    }
    shown = shown.map { it.copy(isWeak = weak != null && it.key == weak.subject.key) }

    val weakNote = if (weak == null && all.none { it.first.itemsOpened > 0 }) "Open a subject this week to see where you stand." else null
    val hasAnyData = hasProgress || (weekMinutes ?: 0) > 0

    val savedTimes = listOfNotNull(dayLogSavedAtMs, examsSavedAtMs)
    val dateFmt = SimpleDateFormat("d MMM yyyy", Locale.US)
    val asOf = savedTimes.minOrNull()?.let { "Saved copy from ${dateFmt.format(Date(it))}" }

    val accuracy: Int? = if (progressKnown) summary.accuracyPct?.roundToInt() else null
    val name = studentName?.trim().orEmpty().ifBlank { "Scholar" }

    return StatusCardData(
        studentName = name,
        studentId = scStudentId(studentIdNumber),
        trackName = track.name,
        standing = if (progressKnown) scPlainStanding(summary.tier) else null,
        weekRangeLabel = scWeekRangeLabel(nowMs),
        weekMinutes = weekMinutes,
        weekTargetHours = track.weeklyTargetHours,
        weekFill = weekFill,
        weekComplete = weekComplete,
        accuracyPct = accuracy,
        questionsCorrect = if (progressKnown) summary.questionsCorrect else 0,
        questionsAttempted = if (progressKnown) summary.questionsAttempted else 0,
        streakDays = streak,
        streakFill = streak?.let { (it / 7f).coerceIn(0f, 1f) },
        focusPct = if (progressKnown) summary.focusRatioPct else null,
        days = days,
        bestDayName = best?.dayName,
        bestDayMinutes = best?.minutes,
        subjects = shown,
        weak = weak,
        weakNote = weakNote,
        nextStep = scNextStep(weak, weekMinutes, weekComplete, track.weeklyTargetHours, hasAnyData),
        asOfLabel = asOf,
        savedLabel = "Saved ${dateFmt.format(Date(nowMs))}"
    )
}

/**
 * Reads what the card needs (subject catalog, exam attempts, the 7-day log) and returns [StatusCardData]. A part that cannot
 * be read becomes unknown (dash), never 0. The reads switch dispatchers themselves. [rows] / [summary] / [track] are what
 * Your status already has; [progressKnown] false = the screen could not read the progress (everything taken from [summary]
 * is then ignored).
 */
suspend fun buildStatusCardData(
    context: Context,
    rows: List<ProgressRow>,
    summary: StatusSummary,
    track: StatusTrack,
    studentName: String?,
    studentIdNumber: String?,
    educationLevel: String?,
    progressKnown: Boolean = true,
    nowMs: Long = System.currentTimeMillis()
): StatusCardData {
    val userId = AuthStateManager.currentUser.value?.id
    val picks = if (userId != null) MySubjectsStore.load(context, userId, educationLevel).keys else emptySet()
    val names = MySubjectsStore.packages.flatMap { it.subjects }.associate { it.key to it.name }

    val catalog = if (progressKnown) ResourceCatalogIndex.get(context) else null
    val attempts = if (catalog != null) ExamAttemptsReader.fetchAll() else null
    val attemptsOk = attempts as? ExamAttemptsFetchResult.Success
    val attemptRows: List<ExamAttemptRow> = attemptsOk?.rows ?: emptyList()
    val examsSavedAtMs: Long? = if (attemptsOk != null && attemptsOk.fromCache) attemptsOk.savedAtMs else null

    // same subject list as the By subject panel (buildEntries there is private, these are the same few lines)
    val subjects: List<Pair<String, SubjectBreakdown>> = if (catalog == null) {
        emptyList()
    } else {
        withContext(Dispatchers.Default) {
            val progress = StatusBreakdown.progressMap(rows)
            val keys = if (picks.isNotEmpty()) picks.toList() else StatusBreakdown.subjectKeys(catalog.items)
            keys.mapNotNull { key ->
                val b = StatusBreakdown.subjectStatus(key, catalog.items, progress, attemptRows) ?: return@mapNotNull null
                if (picks.isEmpty() && b.totals.itemsOpened == 0 && b.exams.attemptsTaken == 0) return@mapNotNull null
                (names[key] ?: key.substringAfter('/')) to b
            }
        }
    }

    var dayRows: List<StudyDayRow> = emptyList()
    var dayLogKnown = false
    var dayLogSavedAtMs: Long? = null
    val dayLog = StudyDayLogReader.fetchRecent(7)
    if (dayLog is StudyDayLogFetchResult.Success) {
        dayRows = dayLog.rows
        dayLogKnown = true
        dayLogSavedAtMs = if (dayLog.fromCache) dayLog.savedAtMs else null
    }

    return assembleStatusCardData(
        studentName = studentName,
        studentIdNumber = studentIdNumber,
        track = track,
        summary = summary,
        progressKnown = progressKnown,
        dayRows = dayRows,
        dayLogKnown = dayLogKnown,
        dayLogSavedAtMs = dayLogSavedAtMs,
        examsSavedAtMs = examsSavedAtMs,
        subjects = subjects,
        nowMs = nowMs
    )
}
