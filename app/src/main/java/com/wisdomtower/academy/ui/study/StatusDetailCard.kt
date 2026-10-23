package com.wisdomtower.academy.ui.study

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wisdomtower.academy.ui.theme.WisdomAccentSky
import com.wisdomtower.academy.ui.theme.WisdomGlassBorder
import com.wisdomtower.academy.ui.theme.WisdomGlassFillQuiet
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * My Status V2 (S2 + S4): the overlay card for ONE subject (bottom card over a dim scrim, same glass panels / tokens as
 * NativeStatusScreen). Real numbers only: unknown = dash (G4). Subject view: hero ring = completion, small rings = accuracy,
 * focus, flashcards known; reading time row; a Chapters list and an Exam attempts list. Tap a chapter -> chapter view (per hub),
 * tap an attempt -> exam view; back arrow / phone Back returns to the subject view, then closes.
 *
 * [title] = the subject name (the caller resolves it, e.g. from PackagesData.kt). Dismiss: tap the scrim, the X, or back.
 *
 * The course-page board does NOT use this popup any more: it uses [StatusDetailInline] (same lists, in the page flow).
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
@Composable
fun StatusDetailCard(
    title: String,
    breakdown: SubjectBreakdown,
    onDismiss: () -> Unit
) {
    var view by remember { mutableStateOf<SdView>(SdView.Subject) }
    val scroll = rememberScrollState()
    LaunchedEffect(view) { scroll.scrollTo(0) }
    val v = view
    val back: () -> Unit = { if (v is SdView.Subject) onDismiss() else view = SdView.Subject }
    val noRipple = remember { MutableInteractionSource() }
    val noRipple2 = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    val kicker = when (v) {
        is SdView.Subject -> "SUBJECT STATUS"
        is SdView.Chapter -> "CHAPTER STATUS"
        is SdView.Exam -> "EXAM ATTEMPT"
    }
    val heading = when (v) {
        is SdView.Subject -> title
        is SdView.Chapter -> "$title: ${sdChapterLabel(v.data.chapter)}"
        is SdView.Exam -> v.data.title ?: "Exam"
    }

    Dialog(onDismissRequest = back, properties = DialogProperties(usePlatformDefaultWidth = false)) {
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
                    .verticalScroll(scroll)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (v !is SdView.Subject) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable { back() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WisdomMuted, modifier = Modifier.size(20.dp))
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(kicker, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = WisdomAccentSky, letterSpacing = 0.8.sp)
                        Text(heading, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = WisdomInk, lineHeight = 23.sp)
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

                when (v) {
                    is SdView.Subject -> SdSubjectBody(
                        breakdown = breakdown,
                        onChapter = { view = SdView.Chapter(it) },
                        onExam = { view = SdView.Exam(it) }
                    )
                    is SdView.Chapter -> SdChapterBody(v.data)
                    is SdView.Exam -> SdExamBody(v.data, breakdown.exams.attempts)
                }
            }
        }
    }
}

private sealed interface SdView {
    object Subject : SdView
    class Chapter(val data: ChapterBreakdown) : SdView
    class Exam(val data: ExamAttemptBreakdown) : SdView
}

/** Attempts shown in the list (newest first); the card says so when there are more. */
private const val SD_MAX_ATTEMPTS = 8

@Composable
private fun SdSubjectBody(
    breakdown: SubjectBreakdown,
    onChapter: (ChapterBreakdown) -> Unit,
    onExam: (ExamAttemptBreakdown) -> Unit
) {
    val t = breakdown.totals
    SdTotalsBody(t, "${t.itemsOpened} of ${t.itemsTotal} items opened")

    SdPanel {
        SdRow("Reading time", sdDuration(t.readingSeconds))
        SdRow("Exams taken", "${breakdown.exams.attemptsTaken}")
        breakdown.exams.bestPct?.let { SdRow("Best exam score", "$it%") }
        breakdown.exams.latestPct?.let { SdRow("Latest exam score", "$it%") }
        breakdown.exams.averagePct?.let { SdRow("Average exam score", "$it%") }
        SdRow("Last active", sdDate(t.lastActiveIso))
        breakdown.lastPdfPage?.let { SdRow("Last PDF page", "$it") }
    }

    SdListsBody(breakdown, onChapter, onExam)
}

/** Chapters list + exam attempts list (shared by the popup card and the inline board). */
@Composable
private fun SdListsBody(
    breakdown: SubjectBreakdown,
    onChapter: (ChapterBreakdown) -> Unit,
    onExam: (ExamAttemptBreakdown) -> Unit
) {
    if (breakdown.chapters.isNotEmpty()) {
        SdPanel {
            SdKicker("CHAPTERS")
            for (c in breakdown.chapters) {
                val pct = c.totals.completionPct?.roundToInt()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onChapter(c) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatusRing(
                        value = pct?.let { it / 100f },
                        color = statusRingColor(pct),
                        diameter = 44.dp,
                        strokeWidth = 4.dp,
                        textSize = 11.sp
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(sdChapterLabel(c.chapter), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = WisdomInk)
                        Text("${c.totals.itemsOpened} of ${c.totals.itemsTotal} items opened", fontSize = 12.sp, color = WisdomMuted)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = WisdomMuted, modifier = Modifier.size(20.dp))
                }
            }
        }
    }

    val attempts = breakdown.exams.attempts
    if (attempts.isNotEmpty()) {
        SdPanel {
            SdKicker("EXAM ATTEMPTS")
            for (a in attempts.take(SD_MAX_ATTEMPTS)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onExam(a) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(a.title ?: "Exam", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = WisdomInk)
                        Text("${sdDate(a.startedAtIso)} · ${sdScore(a.score, a.total)}", fontSize = 12.sp, color = WisdomMuted)
                    }
                    Text(
                        a.percent?.let { "$it%" } ?: "—",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusRingColor(a.percent)
                    )
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = WisdomMuted, modifier = Modifier.size(20.dp))
                }
            }
            if (attempts.size > SD_MAX_ATTEMPTS) {
                Text("Showing the latest $SD_MAX_ATTEMPTS of ${attempts.size} attempts", fontSize = 12.sp, color = WisdomMuted)
            }
        }
    }
}

@Composable
private fun SdChapterBody(c: ChapterBreakdown) {
    val t = c.totals
    SdTotalsBody(t, "${t.itemsOpened} of ${t.itemsTotal} items opened")

    SdPanel {
        SdRow("Reading time", sdDuration(t.readingSeconds))
        SdRow("Last active", sdDate(t.lastActiveIso))
    }

    if (c.hubs.isNotEmpty()) {
        SdPanel {
            SdKicker("BY SECTION")
            for (h in c.hubs) {
                val parts = ArrayList<String>()
                parts.add("${h.itemsOpened} of ${h.itemsTotal} opened")
                if (h.studySeconds > 0L) parts.add(sdDuration(h.studySeconds))
                h.accuracyPct?.let { parts.add("$it% correct") }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(sdHubLabel(h.hub), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = WisdomInk)
                    Text(parts.joinToString(" · "), fontSize = 12.sp, color = WisdomMuted)
                }
            }
        }
    }
}

@Composable
private fun SdExamBody(a: ExamAttemptBreakdown, all: List<ExamAttemptBreakdown>) {
    val sameExam = all.filter { it.resourceId == a.resourceId }
    val best = sameExam.mapNotNull { it.percent }.maxOrNull()
    SdPanelRow {
        StatusRing(
            value = a.percent?.let { it / 100f },
            color = statusRingColor(a.percent),
            diameter = 96.dp,
            strokeWidth = 9.dp,
            textSize = 22.sp
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("SCORE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WisdomMuted, letterSpacing = 0.8.sp)
            Text(sdScore(a.score, a.total), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = WisdomInk)
        }
    }
    SdPanel {
        SdRow("Date", sdDate(a.startedAtIso))
        SdRow("Time taken", a.durationSeconds?.let { sdDuration(it) } ?: "—")
        SdRow("Against previous attempt", sdDelta(a.deltaVsPrevious))
        SdRow("Best score on this exam", best?.let { "$it%" } ?: "—")
        SdRow("Attempts of this exam", "${sameExam.size}")
    }
}

/** Hero ring (completion) + the three small rings, shared by the subject and chapter views. */
@Composable
private fun SdTotalsBody(t: BreakdownTotals, itemsLabel: String) {
    val completion = t.completionPct?.roundToInt()
    SdPanelRow {
        StatusRing(
            value = completion?.let { it / 100f },
            color = statusRingColor(completion),
            diameter = 96.dp,
            strokeWidth = 9.dp,
            textSize = 22.sp
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("COMPLETION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WisdomMuted, letterSpacing = 0.8.sp)
            Text(itemsLabel, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = WisdomInk)
        }
    }
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
}

@Composable
private fun SdPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(WisdomGlassFillQuiet)
            .border(BorderStroke(1.dp, WisdomGlassBorder), RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
private fun SdPanelRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(WisdomGlassFillQuiet)
            .border(BorderStroke(1.dp, WisdomGlassBorder), RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) { content() }
}

@Composable
private fun SdKicker(text: String) {
    Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WisdomMuted, letterSpacing = 0.8.sp)
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

private fun sdChapterLabel(chapter: Int?): String = if (chapter == null) "General" else "Chapter $chapter"

/** The six Learning Hub names (permanent, owner rule 2026-10-11) plus videos; unknown ids are shown as stored. */
private fun sdHubLabel(hub: String): String = when (hub) {
    "books" -> "Books"
    "short-notes" -> "Short Notes"
    "videos" -> "Videos"
    "flashcards" -> "Flashcards"
    "question-banks" -> "Question Banks"
    "exams" -> "Exams"
    "life-savers" -> "Life Savers"
    else -> hub
}

/** "17 of 20" (whole scores without decimals); unknown = dash. */
private fun sdScore(score: Double?, total: Int?): String {
    if (score == null || total == null) return "—"
    val s = if (score % 1.0 == 0.0) score.toInt().toString() else String.format(Locale.US, "%.1f", score)
    return "$s of $total"
}

/** Percentage points against the previous attempt; unknown = dash. */
private fun sdDelta(d: Int?): String = when {
    d == null -> "—"
    d > 0 -> "+$d pts"
    d < 0 -> "−${abs(d)} pts"
    else -> "No change"
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

/**
 * INLINE VERSION (no popup, no scrim): the chapters list and the exam attempts list sit right in the page flow, above the
 * hubs. Tap a chapter or an attempt -> its details replace the lists in place, with a Back arrow (phone Back works too).
 */
@Composable
fun StatusDetailInline(title: String, breakdown: SubjectBreakdown) {
    var view by remember { mutableStateOf<SdView>(SdView.Subject) }
    val v = view
    BackHandler(enabled = v !is SdView.Subject) { view = SdView.Subject }
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (v) {
            is SdView.Subject -> SdListsBody(
                breakdown = breakdown,
                onChapter = { view = SdView.Chapter(it) },
                onExam = { view = SdView.Exam(it) }
            )
            else -> {
                val kicker = if (v is SdView.Chapter) "CHAPTER STATUS" else "EXAM ATTEMPT"
                val heading = when (v) {
                    is SdView.Chapter -> sdChapterLabel(v.data.chapter)
                    is SdView.Exam -> v.data.title ?: "Exam"
                    else -> title
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable { view = SdView.Subject },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WisdomMuted, modifier = Modifier.size(20.dp))
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(kicker, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = WisdomAccentSky, letterSpacing = 0.8.sp)
                        Text(heading, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WisdomInk)
                    }
                }
                when (v) {
                    is SdView.Chapter -> SdChapterBody(v.data)
                    is SdView.Exam -> SdExamBody(v.data, breakdown.exams.attempts)
                    else -> Unit
                }
            }
        }
    }
}
