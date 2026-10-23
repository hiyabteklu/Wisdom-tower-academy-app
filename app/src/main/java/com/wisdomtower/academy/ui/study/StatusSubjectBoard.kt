package com.wisdomtower.academy.ui.study

import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.content.ExamAttemptsFetchResult
import com.wisdomtower.academy.data.content.ExamAttemptsReader
import com.wisdomtower.academy.data.content.ProgressFetchResult
import com.wisdomtower.academy.data.content.ResourceCatalogIndex
import com.wisdomtower.academy.data.content.StudentProgressReader
import com.wisdomtower.academy.ui.theme.WisdomAccentAmber
import com.wisdomtower.academy.ui.theme.WisdomAccentSky
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomGlassBorder
import com.wisdomtower.academy.ui.theme.WisdomGlassFillQuiet
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import kotlinx.coroutines.CancellationException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** Result of reading one subject's real status (My Status V2, slice B2; offline copy added in O4). */
sealed interface SubjectBoardLoad {
    /**
     * OFFLINE STATUS (O4): [savedAtMs] is null when the numbers are live. When the progress or the exam attempts came from
     * the saved copy of the latest sync it is the OLDER of the two save times (the board then says so with the date).
     */
    data class Ready(val breakdown: SubjectBreakdown, val savedAtMs: Long? = null) : SubjectBoardLoad
    /** The catalog has no published item for this scope: nothing honest to show, the board hides itself. */
    object NoSubject : SubjectBoardLoad
    /**
     * No real numbers can be drawn (offline with nothing saved yet, catalog never saved, not signed in ...). OFFLINE STATUS (O4):
     * the board does NOT show an error card any more: it draws the four EMPTY rings and a connect notice. Never zeros.
     */
    object Failed : SubjectBoardLoad
}

/**
 * Same reads and the same rule as the S5 icon: catalog + own progress + own exam attempts, subject found by [scopePath].
 * OFFLINE STATUS (O4): the progress and exam readers return the saved copy of the same student when the server cannot be
 * reached (O1), so [SubjectBoardLoad.Ready] may carry [SubjectBoardLoad.Ready.savedAtMs].
 */
suspend fun loadSubjectBreakdown(context: Context, scopePath: String): SubjectBoardLoad {
    return try {
        val catalog = ResourceCatalogIndex.get(context)
        val progress = StudentProgressReader.fetchAll()
        val attempts = ExamAttemptsReader.fetchAll()
        if (catalog == null || progress !is ProgressFetchResult.Success || attempts !is ExamAttemptsFetchResult.Success) {
            SubjectBoardLoad.Failed
        } else {
            val path = scopePath.trim().trim('/')
            val key = catalog.items.firstOrNull { it.scopePath.trim().trim('/') == path }?.subjectKey
            val b = key?.let {
                StatusBreakdown.subjectStatus(it, catalog.items, StatusBreakdown.progressMap(progress.rows), attempts.rows)
            }
            if (b == null) {
                SubjectBoardLoad.NoSubject
            } else {
                val savedAt = listOfNotNull(
                    if (progress.fromCache) progress.savedAtMs else null,
                    if (attempts.fromCache) attempts.savedAtMs else null
                ).minOrNull()
                SubjectBoardLoad.Ready(b, savedAt)
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        SubjectBoardLoad.Failed
    }
}

/** OFFLINE STATUS (O4): shown with the empty rings when nothing real can be drawn. */
private const val BOARD_CONNECT_NOTICE = "Could not reach the server. Connect to the internet to see your status."

/** "23 Oct 2026, 14:05" in the phone's own time. */
private fun sbFormatSavedAt(ms: Long): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", Locale.US).format(Date(ms))

/**
 * My Status V2 (B2): the "My status" board that goes ABOVE the hub list of a course page (wired in B3). Collapsed = one
 * glass card: title, an expand chevron and ONE row of four rings (Completion, Accuracy, Focus, Flashcards known) with the
 * existing [StatusRing] and HUD colours. Expanded = the detail rows of the subject card (times, exams, last active) and a
 * "Chapters and exams" button that opens the existing [StatusDetailCard]. Real numbers only: unknown = dash (G4).
 * [reloadKey]: change it (for example when the student returns from a hub) to read the numbers again; a failed reload keeps
 * the numbers already shown.
 *
 * OFFLINE STATUS (2026-10-23, docs/OFFLINE_STATUS_PLAN.md slice O4): the board always draws, it never shows an error card.
 * (1) Numbers available (live or the saved copy of the same student): the four rings as before; when they come from the
 * saved copy ONE amber line says "No connection to the server. Showing your status as last saved on <date, time>.", and the
 * expanded rows and the "Chapters and exams" card use the same copy. (2) Nothing can be read and nothing is saved (or the
 * catalog was never saved): the title, a connect notice with Retry and the four EMPTY rings (dashes). No chevron, no detail
 * rows and no "Chapters and exams" button, because there is nothing real to open. Online behaviour is unchanged.
 * Slice O5a: the board also reads again by itself when the internet comes back ([rememberNetworkReturnCount]).
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
@Composable
fun StatusSubjectBoard(
    scopePath: String,
    title: String,
    modifier: Modifier = Modifier,
    reloadKey: Any? = null
) {
    val context = LocalContext.current
    var state by remember(scopePath) { mutableStateOf<SubjectBoardLoad?>(null) }
    var retry by remember { mutableStateOf(0) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var showCard by remember { mutableStateOf(false) }
    // OFFLINE STATUS (O5a): goes up when the internet comes back, which reads the numbers again
    val netBack = rememberNetworkReturnCount()

    LaunchedEffect(scopePath, reloadKey, retry, netBack) {
        val r = loadSubjectBreakdown(context, scopePath)
        state = if (r is SubjectBoardLoad.Failed && state is SubjectBoardLoad.Ready) state else r
    }

    when (val s = state) {
        null -> SbPanel(modifier) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(color = WisdomCyan, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                Text("My status", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WisdomInk)
            }
        }

        SubjectBoardLoad.NoSubject -> Unit

        // OFFLINE STATUS (O4): the empty skeleton. Same card, same four rings, all empty (dashes), one notice, Retry.
        SubjectBoardLoad.Failed -> SbPanel(modifier) {
            Text("My status", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = WisdomInk)
            Text(BOARD_CONNECT_NOTICE, fontSize = 12.sp, color = WisdomMuted, lineHeight = 17.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SbRing("Completion", null)
                SbRing("Accuracy", null)
                SbRing("Focus", null)
                SbRing("Flashcards known", null)
            }
            Text(
                "Retry",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = WisdomAccentSky,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { retry++ }
                    .padding(vertical = 6.dp, horizontal = 4.dp)
            )
        }

        is SubjectBoardLoad.Ready -> {
            val b = s.breakdown
            val t = b.totals
            val completion = t.completionPct?.roundToInt()
            val savedAt = s.savedAtMs
            SbPanel(modifier) {
                // header: title + expand chevron (the whole row toggles)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { expanded = !expanded },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "My status",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = WisdomInk,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(WisdomAccentSky.copy(alpha = 0.15f))
                            .border(BorderStroke(1.dp, WisdomAccentSky.copy(alpha = 0.30f)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (expanded) "Hide status details" else "Show status details",
                            tint = WisdomAccentSky,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // OFFLINE STATUS (O4): the numbers below are the saved copy of the latest sync
                if (savedAt != null) {
                    Text(
                        "No connection to the server. Showing your status as last saved on ${sbFormatSavedAt(savedAt)}. Connect to the internet to update.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = WisdomAccentAmber,
                        lineHeight = 16.sp
                    )
                }

                // ONE row of the four key gauges
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    SbRing("Completion", completion)
                    SbRing("Accuracy", t.accuracyPct)
                    SbRing("Focus", t.focusPct)
                    SbRing("Flashcards known", t.flashKnownPct)
                }

                if (expanded) {
                    Text("${t.itemsOpened} of ${t.itemsTotal} items opened", fontSize = 12.sp, color = WisdomMuted)
                    SbRow("Study time", sbDuration(t.studySeconds))
                    SbRow("Reading time", sbDuration(t.readingSeconds))
                    SbRow("Watch time", sbDuration(t.watchSeconds))
                    SbRow("Exams taken", "${b.exams.attemptsTaken}")
                    b.exams.bestPct?.let { SbRow("Best exam score", "$it%") }
                    b.exams.latestPct?.let { SbRow("Latest exam score", "$it%") }
                    b.exams.averagePct?.let { SbRow("Average exam score", "$it%") }
                    SbRow("Last active", sbDate(t.lastActiveIso))
                    if (b.chapters.isNotEmpty() || b.exams.attempts.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(WisdomAccentSky.copy(alpha = 0.15f))
                                .border(BorderStroke(1.dp, WisdomAccentSky.copy(alpha = 0.30f)), RoundedCornerShape(12.dp))
                                .clickable { showCard = true }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Chapters and exams", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = WisdomAccentSky)
                        }
                    }
                }
            }
            if (showCard) {
                StatusDetailCard(title = title, breakdown = b, onDismiss = { showCard = false })
            }
        }
    }
}

@Composable
private fun SbPanel(modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(WisdomGlassFillQuiet)
            .border(BorderStroke(1.dp, WisdomGlassBorder), RoundedCornerShape(20.dp))
            .animateContentSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) { content() }
}

@Composable
private fun SbRing(label: String, pct: Int?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        StatusRing(
            value = pct?.let { it / 100f },
            color = statusRingColor(pct),
            diameter = 56.dp,
            strokeWidth = 5.dp,
            textSize = 13.sp
        )
        Text(label, fontSize = 10.sp, color = WisdomMuted, textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
private fun SbRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = WisdomMuted, modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = WisdomInk)
    }
}

/** 0 seconds = nothing recorded = dash (G4). Same rule as the subject card. */
private fun sbDuration(seconds: Long): String {
    if (seconds <= 0L) return "\u2014"
    val m = seconds / 60L
    if (m < 1L) return "<1m"
    val h = m / 60L
    return if (h > 0L) "${h}h ${m % 60L}m" else "${m}m"
}

private val SB_MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/** ISO text (yyyy-MM-dd...) to "22 Oct 2026"; unknown = dash. */
private fun sbDate(iso: String?): String {
    if (iso == null || iso.length < 10) return "\u2014"
    val p = iso.substring(0, 10).split("-")
    if (p.size != 3) return "\u2014"
    val y = p[0].toIntOrNull()
    val m = p[1].toIntOrNull()
    val d = p[2].toIntOrNull()
    if (y == null || m == null || d == null || m !in 1..12) return "\u2014"
    return "$d ${SB_MONTHS[m - 1]} $y"
}
