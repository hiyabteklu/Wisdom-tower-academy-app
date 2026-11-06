package com.wisdomtower.academy.ui.study

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.content.StudyDayLogFetchResult
import com.wisdomtower.academy.data.content.StudyDayLogReader
import com.wisdomtower.academy.data.content.StudyDayRow
import com.wisdomtower.academy.ui.theme.AppThemeMode
import com.wisdomtower.academy.ui.theme.WisdomAccentAmber
import com.wisdomtower.academy.ui.theme.WisdomAccentEmerald
import com.wisdomtower.academy.ui.theme.WisdomAccentSky
import com.wisdomtower.academy.ui.theme.WisdomGlassBorder
import com.wisdomtower.academy.ui.theme.WisdomGlassFill
import com.wisdomtower.academy.ui.theme.WisdomGlassFillQuiet
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.wisdomTouchFeel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TreeMap

/**
 * U3d (docs/PLAN_2026-11-06_URGENT_FIVE_FIXES.md): one clear panel with the control Today | Week | Month | Total, the study
 * time of that range, how much of it was focus, and bars (a bar per day for Week and Month, a bar per month for Total).
 *
 * REAL DATA ONLY: the numbers come from the student's own `study_day_log` ([StudyDayLogReader], saved copy first, then the
 * server; the saved copy also works offline). A day without a row is shown as empty, never as a made-up value.
 * Each day counts at most [RG_DAY_CAP_SECONDS] (8 hours), so a few old runaway days (a phone left open) cannot dominate the
 * totals; the panel says when it limited a day. Nothing is deleted.
 * Focus: see ActiveStudyTimer (U3c). Days saved before U3c have no focus, so Focus shows a dash until focus exists in the range.
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
private const val RG_DAY_CAP_SECONDS = 8 * 3600
private val RG_CHART_HEIGHT = 120.dp

/** How far back the panel reads (a bit more than a year; the reader's row limit is raised to match by the U3d patch). */
private const val RG_FETCH_DAYS = 400

private enum class RgKind(val label: String) { TODAY("Today"), WEEK("Week"), MONTH("Month"), TOTAL("Total") }

private class RgBar(val label: String, val seconds: Int, val focusSeconds: Int)

private class RgResult(
    val studySeconds: Int,
    val focusSeconds: Int,
    val activeDays: Int,
    val bestSeconds: Int,
    val limitedDays: Int,
    val bars: List<RgBar>
)

private fun rgDayNight(night: Color, day: Color): Color = if (AppThemeMode.isDark) night else day

private val RgSky: Color get() = rgDayNight(Color(0xFF38BDF8), WisdomAccentSky)
private val RgEmerald: Color get() = rgDayNight(Color(0xFF34D399), WisdomAccentEmerald)
private val RgAmber: Color get() = rgDayNight(Color(0xFFFBBF24), WisdomAccentAmber)
private val RgMuted: Color get() = rgDayNight(Color(0xFF94A3B8), WisdomMuted)
private val RgPanelFill: Color get() = rgDayNight(Color.White.copy(alpha = 0.04f), WisdomGlassFill)
private val RgPanelBorder: Color get() = rgDayNight(Color.White.copy(alpha = 0.10f), WisdomGlassBorder)
private val RgInnerFill: Color get() = rgDayNight(Color.White.copy(alpha = 0.03f), WisdomGlassFillQuiet)
private val RgTrack: Color get() = rgDayNight(Color.White.copy(alpha = 0.10f), WisdomInk.copy(alpha = 0.10f))

/** Local date `n` days ago as `yyyy-MM-dd` (the same local date the app uses when it writes a day). */
private fun rgKeyAgo(n: Int): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -n)
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
}

private fun rgLabelAgo(n: Int, pattern: String): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -n)
    return SimpleDateFormat(pattern, Locale.getDefault()).format(cal.time)
}

/** "2025-10" -> "Oct" (empty text when it cannot be read). */
private fun rgMonthLabel(yearMonth: String): String = try {
    val date = SimpleDateFormat("yyyy-MM", Locale.US).parse(yearMonth)
    if (date == null) "" else SimpleDateFormat("MMM", Locale.getDefault()).format(date)
} catch (e: Exception) {
    ""
}

private fun rgDuration(totalSeconds: Int): String {
    val minutes = (totalSeconds + 30) / 60
    if (minutes < 60) return "$minutes min"
    val h = minutes / 60
    val m = minutes % 60
    return if (m == 0) "$h h" else "$h h $m min"
}

private fun rgCompute(kind: RgKind, rows: List<StudyDayRow>): RgResult {
    val byDay = HashMap<String, StudyDayRow>()
    for (r in rows) byDay[r.day] = r
    var study = 0
    var focus = 0
    var active = 0
    var best = 0
    var limited = 0

    // Adds one day (capped) to the totals and returns [seconds, focus] after the cap.
    fun addDay(r: StudyDayRow?): IntArray {
        if (r == null) return intArrayOf(0, 0)
        val s = r.seconds.coerceIn(0, RG_DAY_CAP_SECONDS)
        if (r.seconds > RG_DAY_CAP_SECONDS) limited++
        val f = r.focusSeconds.coerceIn(0, s)
        study += s
        focus += f
        if (s > 0) active++
        if (s > best) best = s
        return intArrayOf(s, f)
    }

    val bars = ArrayList<RgBar>()
    when (kind) {
        RgKind.TODAY -> addDay(byDay[rgKeyAgo(0)])
        RgKind.WEEK -> for (n in 6 downTo 0) {
            val v = addDay(byDay[rgKeyAgo(n)])
            bars.add(RgBar(rgLabelAgo(n, "EEE").take(3), v[0], v[1]))
        }
        RgKind.MONTH -> for (n in 29 downTo 0) {
            val v = addDay(byDay[rgKeyAgo(n)])
            bars.add(RgBar(if (n % 5 == 0) rgLabelAgo(n, "d") else "", v[0], v[1]))
        }
        RgKind.TOTAL -> {
            val months = TreeMap<String, IntArray>()
            for ((day, r) in byDay) {
                if (day.length < 7) continue
                val v = addDay(r)
                val m = months.getOrPut(day.substring(0, 7)) { IntArray(2) }
                m[0] += v[0]
                m[1] += v[1]
            }
            for (e in months.entries.toList().takeLast(12)) {
                bars.add(RgBar(rgMonthLabel(e.key), e.value[0], e.value[1]))
            }
        }
    }
    return RgResult(study, focus, active, best, limited, bars)
}

@Composable
fun StatusRangePanel(dailyGoalMinutes: Int, modifier: Modifier = Modifier) {
    var rows by remember { mutableStateOf<List<StudyDayRow>?>(null) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var kind by remember { mutableStateOf(RgKind.WEEK) }
    val netBack = rememberNetworkReturnCount()

    LaunchedEffect(netBack) {
        // Saved copy first (no network), then the server; a failed refresh keeps what is already drawn.
        val saved = StudyDayLogReader.fetchRecentSaved(RG_FETCH_DAYS)
        if (saved != null) {
            rows = saved.rows
            loading = false
        }
        when (val r = StudyDayLogReader.fetchRecent(RG_FETCH_DAYS)) {
            is StudyDayLogFetchResult.Success -> {
                rows = r.rows
                failed = false
            }
            else -> if (rows == null) failed = true
        }
        loading = false
    }

    val current = rows
    val result = remember(current, kind) { current?.let { rgCompute(kind, it) } }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(RgPanelFill)
            .border(BorderStroke(1.dp, RgSky.copy(alpha = 0.20f)), RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("STUDY TIME", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RgSky, letterSpacing = 0.8.sp)

        // Today | Week | Month | Total
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(RgInnerFill)
                .border(BorderStroke(1.dp, RgPanelBorder), RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (k in RgKind.values()) {
                val selected = k == kind
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .wisdomTouchFeel()
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (selected) RgSky.copy(alpha = 0.22f) else Color.Transparent)
                        .clickable { kind = k }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        k.label,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) WisdomInk else RgMuted
                    )
                }
            }
        }

        if (result == null) {
            if (loading) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RgSky, modifier = Modifier.size(24.dp))
                }
            } else {
                Text("—", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = RgMuted)
                Text(
                    if (failed) "Could not reach the server. Connect to the internet once to load your study time." else "No study time recorded yet.",
                    fontSize = 11.sp,
                    color = RgMuted,
                    lineHeight = 16.sp
                )
            }
        } else {
            val rangeLine = when (kind) {
                RgKind.TODAY -> "Today"
                RgKind.WEEK -> "Last 7 days"
                RgKind.MONTH -> "Last 30 days"
                RgKind.TOTAL -> "All time"
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(rgDuration(result.studySeconds), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = RgSky)
                Text(rangeLine, fontSize = 11.sp, color = RgMuted)
            }

            if (kind == RgKind.TODAY) {
                // Today against the student's own daily goal
                val goalSeconds = dailyGoalMinutes.coerceAtLeast(1) * 60
                val frac = (result.studySeconds.toFloat() / goalSeconds).coerceIn(0f, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(RgTrack)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(frac)
                                .height(10.dp)
                                .clip(RoundedCornerShape(50))
                                .background(RgEmerald)
                        )
                    }
                    Text(
                        "Daily goal: ${rgDuration(result.studySeconds)} of ${rgDuration(goalSeconds)}",
                        fontSize = 11.sp,
                        color = RgMuted
                    )
                }
            } else {
                RgBars(kind = kind, bars = result.bars, goalSeconds = dailyGoalMinutes.coerceAtLeast(1) * 60)
            }

            // Study vs focus
            val focusKnown = result.focusSeconds > 0 && result.studySeconds > 0
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(RgInnerFill)
                    .border(BorderStroke(1.dp, RgPanelBorder), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("FOCUS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WisdomInk, letterSpacing = 0.6.sp)
                    Text(
                        if (focusKnown) "${(result.focusSeconds * 100L / result.studySeconds).toInt()}% of your study time" else "—",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (focusKnown) RgEmerald else RgMuted
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(RgTrack)
                ) {
                    if (focusKnown) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((result.focusSeconds.toFloat() / result.studySeconds).coerceIn(0f, 1f))
                                .height(10.dp)
                                .clip(RoundedCornerShape(50))
                                .background(RgEmerald)
                        )
                    }
                }
                Text(
                    if (focusKnown) {
                        "Focus = you touched, scrolled or turned a page in the last 3 minutes."
                    } else {
                        "Focus is recorded by the newest version of the app while you read. Nothing is recorded for this range yet."
                    },
                    fontSize = 11.sp,
                    color = RgMuted,
                    lineHeight = 16.sp
                )
            }

            // Numbers behind the bars
            if (kind != RgKind.TODAY) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RgStat("Active days", "${result.activeDays}", Modifier.weight(1f))
                    RgStat(
                        "Average per active day",
                        if (result.activeDays > 0) rgDuration(result.studySeconds / result.activeDays) else "—",
                        Modifier.weight(1f)
                    )
                    RgStat("Best day", if (result.bestSeconds > 0) rgDuration(result.bestSeconds) else "—", Modifier.weight(1f))
                }
            }

            if (result.limitedDays > 0) {
                Text(
                    "${result.limitedDays} day(s) over 8 hours are counted as 8 hours, so a phone left open cannot inflate your totals.",
                    fontSize = 11.sp,
                    color = RgMuted,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun RgStat(label: String, value: String, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(RgInnerFill)
            .border(BorderStroke(1.dp, RgPanelBorder), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WisdomInk)
        Text(label, fontSize = 10.sp, color = RgMuted, lineHeight = 13.sp)
    }
}

/** Bars of the range: lower part = focus (green), upper part = the rest of the study time (blue). Amber line = daily goal. */
@Composable
private fun RgBars(kind: RgKind, bars: List<RgBar>, goalSeconds: Int) {
    val showGoal = kind != RgKind.TOTAL
    var scaleMax = 1
    for (b in bars) if (b.seconds > scaleMax) scaleMax = b.seconds
    if (showGoal && goalSeconds > scaleMax) scaleMax = goalSeconds
    val gap = when (kind) {
        RgKind.MONTH -> 2.dp
        RgKind.TOTAL -> 6.dp
        else -> 8.dp
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(RG_CHART_HEIGHT)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(RG_CHART_HEIGHT),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment = Alignment.Bottom
            ) {
                for (b in bars) {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        if (b.seconds <= 0) {
                            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(RgTrack))
                        } else {
                            val barHeight = RG_CHART_HEIGHT * (b.seconds.toFloat() / scaleMax)
                            val focusShare = (b.focusSeconds.toFloat() / b.seconds).coerceIn(0f, 1f)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(barHeight * (1f - focusShare))
                                        .background(RgSky.copy(alpha = 0.60f))
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(barHeight * focusShare)
                                        .background(RgEmerald)
                                )
                            }
                        }
                    }
                }
            }
            if (showGoal) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(bottom = (RG_CHART_HEIGHT * (goalSeconds.toFloat() / scaleMax)).coerceAtMost(RG_CHART_HEIGHT - 1.dp))
                        .height(1.dp)
                        .background(RgAmber.copy(alpha = 0.70f))
                )
            }
        }
        // Labels under the bars
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
            for (b in bars) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        b.label,
                        fontSize = 9.sp,
                        color = RgMuted,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.wrapContentWidth(unbounded = true)
                    )
                }
            }
        }
        // Legend
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RgLegend("Focus", RgEmerald)
            RgLegend("Other study time", RgSky.copy(alpha = 0.60f))
            if (showGoal) RgLegend("Daily goal", RgAmber)
        }
    }
}

@Composable
private fun RgLegend(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(text, fontSize = 10.sp, color = RgMuted)
    }
}
