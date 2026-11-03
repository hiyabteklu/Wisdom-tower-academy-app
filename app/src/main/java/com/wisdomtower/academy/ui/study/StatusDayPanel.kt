package com.wisdomtower.academy.ui.study

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMessageCard
import com.wisdomtower.academy.ui.theme.WisdomMuted
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Day / night colours, same rule as NativeStatusScreen (night = old literal, day = Ivory & Navy token).
private fun sdPick(night: Color, day: Color): Color = if (AppThemeMode.isDark) night else day

private val SdSky: Color get() = sdPick(Color(0xFF38BDF8), WisdomAccentSky)
private val SdEmerald: Color get() = sdPick(Color(0xFF34D399), WisdomAccentEmerald)
private val SdAmber: Color get() = sdPick(Color(0xFFFBBF24), WisdomAccentAmber)
private val SdMuted: Color get() = sdPick(Color(0xFF94A3B8), WisdomMuted)
private val SdPanelFill: Color get() = sdPick(Color.White.copy(alpha = 0.04f), WisdomGlassFill)
private val SdPanelBorder: Color get() = sdPick(Color.White.copy(alpha = 0.10f), WisdomGlassBorder)
private val SdTrack: Color get() = sdPick(Color.White.copy(alpha = 0.10f), WisdomInk.copy(alpha = 0.10f))

/** [known] = false: this day cannot be told (nothing read, or it is after the saved copy was made). Never drawn as a made-up 0. */
private class SdDay(val label: String, val seconds: Int, val isToday: Boolean, val known: Boolean)

/**
 * The last 7 local days, oldest first. A day without a log row is 0 seconds (an empty bar) ONLY when the day is known:
 * [noData] (nothing read and nothing saved) makes every day unknown, and with a saved copy ([savedAtMs]) every day after
 * the day the copy was saved is unknown (study done since then is not in the copy).
 */
private fun sdLastSevenDays(rows: List<StudyDayRow>, noData: Boolean, savedAtMs: Long?): List<SdDay> {
    val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val labelFmt = SimpleDateFormat("EEE", Locale.getDefault())
    val bySeconds = rows.associate { it.day to it.seconds }
    val savedKey = savedAtMs?.let { keyFmt.format(Date(it)) }
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -6)
    val out = ArrayList<SdDay>(7)
    for (i in 0 until 7) {
        val key = keyFmt.format(cal.time)
        val known = !noData && (savedKey == null || key <= savedKey)
        out.add(SdDay(labelFmt.format(cal.time), if (known) (bySeconds[key] ?: 0) else 0, i == 6, known))
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }
    return out
}

/** "23 Oct 2026, 14:05" in the phone's own time. */
private fun sdFormatSavedAt(ms: Long): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", Locale.US).format(Date(ms))

/**
 * My Status V2 (S6d, docs/MY_STATUS_V2_S6_DAY_LOG_PLAN.md): "Today" ring (today's minutes against the daily goal) and the
 * 7-day weekday bar chart, both from the student's own `study_day_log` rows ([StudyDayLogReader]). Real data only: days
 * without a row are empty bars. Wired once in NativeStatusScreen (marker MYSTATUS-S6).
 *
 * OFFLINE STATUS (2026-10-23, docs/OFFLINE_STATUS_PLAN.md slice O3): the panel always draws. With a saved copy of the
 * latest sync ([StudyDayLogFetchResult.Success.fromCache]) it is drawn from the copy and one amber line says when it was
 * saved; days after that day are unknown (a dash above an empty bar), so "today" shows a dash instead of a made-up 0 min.
 * With nothing read and nothing saved: a notice with Retry, then the empty ring and seven empty bars, all days unknown.
 * Online behaviour is unchanged. Slice O5a: the panel also reads again by itself when the internet comes back.
 *
 * SAVED COPY FIRST (2026-11-03, docs/READER_EXAM_FLASHCARD_FIX_PLAN.md slice P1c, part c2a): the panel first draws the saved
 * copy ([StudyDayLogReader.fetchRecentSaved], no network) at once, WITHOUT the amber "No connection" line (the server has not
 * been asked yet, so nothing is known about the connection), then reads the server in the background and swaps in the fresh
 * rows. While that read runs, a day that the copy cannot tell shows "Updating" instead of "Connect to the internet". The
 * spinner only shows when nothing is saved; a failed refresh keeps the saved copy on screen.
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
@Composable
fun StatusDayPanel(dailyGoalMinutes: Int, modifier: Modifier = Modifier) {
    var reloadKey by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var fromCache by remember { mutableStateOf(false) }
    var savedAtMs by remember { mutableStateOf<Long?>(null) }
    // P1c: when the rows on screen were saved (they may be a copy or live); days after it are unknown. Null = live rows.
    var copyAtMs by remember { mutableStateOf<Long?>(null) }
    // P1c: true while the saved copy is drawn and the server read is still running
    var refreshing by remember { mutableStateOf(false) }
    var rows by remember { mutableStateOf<List<StudyDayRow>>(emptyList()) }
    // OFFLINE STATUS (O5a): goes up when the internet comes back, which reads the log again
    val netBack = rememberNetworkReturnCount()

    LaunchedEffect(reloadKey, netBack) {
        fromCache = false
        savedAtMs = null
        // P1c: the saved copy first (no network), drawn at once
        val saved = StudyDayLogReader.fetchRecentSaved(7)
        if (saved != null) {
            rows = saved.rows
            copyAtMs = saved.savedAtMs
            failed = false
            loading = false
            refreshing = true
        } else {
            loading = true
            failed = false
            copyAtMs = null
        }
        when (val r = StudyDayLogReader.fetchRecent(7)) {
            is StudyDayLogFetchResult.Success -> {
                rows = r.rows
                fromCache = r.fromCache
                savedAtMs = r.savedAtMs
                copyAtMs = if (r.fromCache) r.savedAtMs else null
                failed = false
            }
            else -> {
                // a failed refresh keeps the saved copy that is already drawn
                if (saved == null) {
                    rows = emptyList()
                    copyAtMs = null
                    failed = true
                }
            }
        }
        refreshing = false
        loading = false
    }

    if (failed && !loading) {
        WisdomMessageCard(
            message = "Could not reach the server. Connect to the internet once to load your daily study log.",
            isError = false,
            onRetry = { reloadKey += 1 }
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SdPanelFill)
            .border(BorderStroke(1.dp, SdSky.copy(alpha = 0.20f)), RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("TODAY & THIS WEEK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SdSky, letterSpacing = 0.8.sp)
        if (loading) {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SdSky, modifier = Modifier.size(28.dp))
            }
            return@Column
        }

        val savedAt = savedAtMs
        if (fromCache && savedAt != null) {
            Text(
                "No connection to the server. Showing your daily log as last saved on ${sdFormatSavedAt(savedAt)}.",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = SdAmber,
                lineHeight = 16.sp
            )
        }

        val days = sdLastSevenDays(rows, noData = failed, savedAtMs = copyAtMs)
        val today = days.last()
        val todaySeconds = today.seconds
        val todayMin = todaySeconds / 60
        val goal = dailyGoalMinutes
        val fraction = if (today.known && goal > 0) (todaySeconds / 60f / goal).coerceIn(0f, 1f) else 0f
        val ringColor = when {
            !today.known -> SdSky
            goal > 0 && fraction >= 1f -> SdEmerald
            fraction >= 0.5f -> SdAmber
            else -> SdSky
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.size(104.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 10.dp.toPx()
                    val topLeft = Offset(stroke / 2f, stroke / 2f)
                    val arc = Size(size.width - stroke, size.height - stroke)
                    drawArc(SdTrack, 0f, 360f, false, topLeft, arc, style = Stroke(stroke))
                    if (fraction > 0f) {
                        drawArc(ringColor, -90f, 360f * fraction, false, topLeft, arc, style = Stroke(stroke, cap = StrokeCap.Round))
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (today.known) "$todayMin" else "\u2014", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = ringColor)
                    Text("min today", fontSize = 10.sp, color = SdMuted)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    when {
                        !today.known && refreshing -> "Updating today\u2026"
                        !today.known -> "Today is not available yet"
                        goal > 0 -> "${(fraction * 100).toInt()}% of your daily goal"
                        else -> "No daily goal set"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = WisdomInk
                )
                Text(
                    when {
                        !today.known && refreshing -> "Reading your latest study time."
                        !today.known -> "Connect to the internet to see today's study time."
                        goal > 0 -> "$todayMin of $goal min studied today"
                        else -> "Set one in Settings to see your ring fill."
                    },
                    fontSize = 12.sp,
                    color = SdMuted,
                    lineHeight = 17.sp
                )
            }
        }

        // 7-day bars (minutes per day)
        val maxSeconds = maxOf(days.maxOf { it.seconds }, if (goal > 0) goal * 60 else 0, 60)
        Row(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            days.forEach { d ->
                val m = d.seconds / 60
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Bottom)
                ) {
                    Text(
                        if (!d.known) "\u2013" else if (d.seconds <= 0) "" else if (m == 0) "<1" else "$m",
                        fontSize = 10.sp,
                        color = SdMuted,
                        textAlign = TextAlign.Center
                    )
                    val barColor = if (d.isToday) ringColor else SdSky
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (d.seconds <= 0) 3.dp else (80f * d.seconds / maxSeconds).coerceAtLeast(4f).dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (d.seconds <= 0) SdTrack else barColor)
                    )
                    Text(
                        d.label,
                        fontSize = 10.sp,
                        fontWeight = if (d.isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (d.isToday) WisdomInk else SdMuted
                    )
                }
            }
        }

        if (!failed && rows.size < 7) {
            Text(
                "Daily log starts the day this version is installed. Empty days are days with no recorded study.",
                fontSize = 11.sp,
                color = SdMuted,
                lineHeight = 16.sp
            )
        }
    }
}
