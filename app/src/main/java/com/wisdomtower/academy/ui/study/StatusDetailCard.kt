package com.wisdomtower.academy.ui.study

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wisdomtower.academy.ui.theme.WisdomAccentSky
import com.wisdomtower.academy.ui.theme.WisdomGlassBorder
import com.wisdomtower.academy.ui.theme.WisdomGlassFillQuiet
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import kotlin.math.roundToInt

/**
 * My Status V2 (S2): the overlay card for ONE subject (bottom card over a dim scrim, same glass panels / tokens as
 * NativeStatusScreen). Real numbers only: unknown = dash (G4). Hero ring = completion, small rings = accuracy, focus,
 * flashcards known; rows = study / reading / watch time, exams, last active, last PDF page. Chapters and exam lists come in S4.
 * Nothing opens it yet (S3 adds the icon / row that does).
 *
 * [title] = the subject name (the caller resolves it, e.g. from PackagesData.kt). Dismiss: tap the scrim, the X, or back.
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
@Composable
fun StatusDetailCard(
    title: String,
    breakdown: SubjectBreakdown,
    onDismiss: () -> Unit
) {
    val t = breakdown.totals
    val completion = t.completionPct?.roundToInt()
    val noRipple = remember { MutableInteractionSource() }
    val noRipple2 = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(interactionSource = noRipple, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 640.dp)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(BorderStroke(1.dp, WisdomGlassBorder), shape)
                    .clickable(interactionSource = noRipple2, indication = null) { }
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("SUBJECT STATUS", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = WisdomAccentSky, letterSpacing = 0.8.sp)
                        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = WisdomInk, lineHeight = 23.sp)
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = WisdomMuted, modifier = Modifier.size(20.dp))
                    }
                }

                // hero ring: completion
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(WisdomGlassFillQuiet)
                        .border(BorderStroke(1.dp, WisdomGlassBorder), RoundedCornerShape(20.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatusRing(
                        value = completion?.let { it / 100f },
                        color = statusRingColor(completion),
                        diameter = 96.dp,
                        strokeWidth = 9.dp,
                        textSize = 22.sp
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("COMPLETION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WisdomMuted, letterSpacing = 0.8.sp)
                        Text("${t.itemsOpened} of ${t.itemsTotal} items opened", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = WisdomInk)
                    }
                }

                // small rings
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    SdMiniRing(
                        label = "Accuracy",
                        pct = t.accuracyPct,
                        caption = if (t.quizAttempted > 0) "${t.quizCorrect} of ${t.quizAttempted}" else null
                    )
                    SdMiniRing(label = "Focus", pct = t.focusPct, caption = null)
                    SdMiniRing(
                        label = "Flashcards known",
                        pct = t.flashKnownPct,
                        caption = if (t.flashSeen > 0) "${t.flashKnow} of ${t.flashSeen}" else null
                    )
                }

                // rows
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(WisdomGlassFillQuiet)
                        .border(BorderStroke(1.dp, WisdomGlassBorder), RoundedCornerShape(20.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SdRow("Study time", sdDuration(t.studySeconds))
                    SdRow("Reading time", sdDuration(t.readingSeconds))
                    SdRow("Watch time", sdDuration(t.watchSeconds))
                    SdRow("Exams taken", "${breakdown.exams.attemptsTaken}")
                    breakdown.exams.bestPct?.let { SdRow("Best exam score", "$it%") }
                    breakdown.exams.latestPct?.let { SdRow("Latest exam score", "$it%") }
                    breakdown.exams.averagePct?.let { SdRow("Average exam score", "$it%") }
                    SdRow("Last active", sdDate(t.lastActiveIso))
                    breakdown.lastPdfPage?.let { SdRow("Last PDF page", "$it") }
                }
            }
        }
    }
}

@Composable
private fun SdMiniRing(label: String, pct: Int?, caption: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        StatusRing(
            value = pct?.let { it / 100f },
            color = statusRingColor(pct),
            diameter = 64.dp,
            strokeWidth = 6.dp,
            textSize = 14.sp
        )
        Text(label, fontSize = 11.sp, color = WisdomMuted, textAlign = TextAlign.Center)
        if (caption != null) Text(caption, fontSize = 11.sp, color = WisdomMuted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SdRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = WisdomMuted, modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = WisdomInk)
    }
}

/** 0 seconds = nothing recorded = dash (G4). */
private fun sdDuration(seconds: Long): String {
    if (seconds <= 0L) return "—"
    val m = seconds / 60L
    if (m < 1L) return "<1m"
    val h = m / 60L
    return if (h > 0L) "${h}h ${m % 60L}m" else "${m}m"
}

private val SD_MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/** ISO text (yyyy-MM-dd...) to "22 Oct 2026"; unknown = dash. */
private fun sdDate(iso: String?): String {
    if (iso == null || iso.length < 10) return "—"
    val p = iso.substring(0, 10).split("-")
    if (p.size != 3) return "—"
    val y = p[0].toIntOrNull()
    val m = p[1].toIntOrNull()
    val d = p[2].toIntOrNull()
    if (y == null || m == null || d == null || m !in 1..12) return "—"
    return "$d ${SD_MONTHS[m - 1]} $y"
}
