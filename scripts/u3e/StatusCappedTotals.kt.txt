package com.wisdomtower.academy.ui.study

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.wisdomtower.academy.data.content.StudyDayLogFetchResult
import com.wisdomtower.academy.data.content.StudyDayLogReader
import com.wisdomtower.academy.data.content.StudyDayRow
import kotlin.math.roundToInt

/**
 * U3e (docs/PLAN_2026-11-06_URGENT_FIVE_FIXES.md): ONE study-time total for the four cards at the top of My Status and the
 * "Your cumulative study time is ..." sentence. It is the same total as the "Total" button of the Study time panel
 * (StatusRangePanel, U3d): the student's own `study_day_log` over the last [CAPPED_FETCH_DAYS] days, each day counted at most
 * [CAPPED_DAY_SECONDS] (8 hours). Nothing is deleted; the old per-resource rows are simply no longer added up for these numbers.
 *
 * REAL DATA ONLY: while nothing is read yet (or nothing is saved and the phone is offline) the cards show a dash, never a
 * made-up 0. A student with a day log but no rows yet gets a real 0.
 * Focus: the same rule as the panel (study_day_log focus, never more than the day's study time). A dash until focus exists.
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
private const val CAPPED_DAY_SECONDS = 8 * 3600

/** Same window as the Study time panel, so the card and the Total button always agree. */
private const val CAPPED_FETCH_DAYS = 400

class CappedStudyTotals(
    val studySeconds: Long,
    val focusSeconds: Long,
    /** how many days were over 8 hours and counted as 8 hours */
    val limitedDays: Int
) {
    val minutes: Int get() = (studySeconds / 60.0).roundToInt()
    val hours: Double get() = ((minutes / 60.0) * 10).roundToInt() / 10.0

    /** null until some focus time exists (the card then shows a dash) */
    val focusPct: Int?
        get() = if (studySeconds > 0L && focusSeconds > 0L) {
            minOf(100, (focusSeconds * 100.0 / studySeconds).roundToInt())
        } else {
            null
        }
}

/** Adds up the day log: one row per day, each day capped at 8 hours, focus never more than that day's study time. */
fun cappedStudyTotals(rows: List<StudyDayRow>): CappedStudyTotals {
    val byDay = HashMap<String, StudyDayRow>()
    for (r in rows) byDay[r.day] = r
    var study = 0L
    var focus = 0L
    var limited = 0
    for (r in byDay.values) {
        val s = r.seconds.coerceIn(0, CAPPED_DAY_SECONDS)
        if (r.seconds > CAPPED_DAY_SECONDS) limited++
        val f = r.focusSeconds.coerceIn(0, s)
        study += s
        focus += f
    }
    return CappedStudyTotals(study, focus, limited)
}

/** The sentence of the "Study progress summary" box. */
fun cumulativeStudySentence(totals: CappedStudyTotals?, streakDays: Int): String {
    val streak = "Your active streak is $streakDays days."
    if (totals == null) return "Your cumulative study time is loading. $streak"
    val limit = if (totals.limitedDays > 0) " Days over 8 hours count as 8 hours." else ""
    return "Your cumulative study time is ${formatStatusNumber(totals.hours)} hours (${totals.minutes} minutes).$limit $streak"
}

/**
 * Reads the day log (saved copy first, then the server, so it also works offline) and returns the capped totals, or null
 * while nothing is known yet. Reads again by itself when the internet comes back.
 */
@Composable
fun rememberCappedStudyTotals(isLoggedIn: Boolean): CappedStudyTotals? {
    var totals by remember { mutableStateOf<CappedStudyTotals?>(null) }
    val netBack = rememberNetworkReturnCount()

    LaunchedEffect(isLoggedIn, netBack) {
        if (!isLoggedIn) {
            totals = null
            return@LaunchedEffect
        }
        val saved = StudyDayLogReader.fetchRecentSaved(CAPPED_FETCH_DAYS)
        if (saved != null) totals = cappedStudyTotals(saved.rows)
        val fresh = StudyDayLogReader.fetchRecent(CAPPED_FETCH_DAYS)
        if (fresh is StudyDayLogFetchResult.Success) totals = cappedStudyTotals(fresh.rows)
    }
    return totals
}
