package com.wisdomtower.academy.ui.study

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.auth.AuthStateManager
import com.wisdomtower.academy.data.content.CatalogItem
import com.wisdomtower.academy.data.content.ExamAttemptRow
import com.wisdomtower.academy.data.content.ExamAttemptsFetchResult
import com.wisdomtower.academy.data.content.ExamAttemptsReader
import com.wisdomtower.academy.data.content.MySubjectsStore
import com.wisdomtower.academy.data.content.ProgressRow
import com.wisdomtower.academy.data.content.ResourceCatalogIndex
import com.wisdomtower.academy.ui.theme.WisdomAccentAmber
import com.wisdomtower.academy.ui.theme.WisdomAccentSky
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomGlassBorder
import com.wisdomtower.academy.ui.theme.WisdomGlassFill
import com.wisdomtower.academy.ui.theme.WisdomGlassFillQuiet
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMessageCard
import com.wisdomtower.academy.ui.theme.WisdomMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private data class SubjectEntry(val key: String, val name: String, val breakdown: SubjectBreakdown)

/** OFFLINE STATUS (O3): shown when the subject details cannot be read and nothing is saved yet. */
private const val SUBJECTS_CONNECT_NOTICE = "Could not reach the server. Connect to the internet once to load your subject details."

/** "23 Oct 2026, 14:05" in the phone's own time. */
private fun ssFormatSavedAt(ms: Long): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", Locale.US).format(Date(ms))

/**
 * My Status V2 (S3): the "By subject" panel inside "Your status". One row per picked subject (T8 picks; with no picks, every
 * subject that has activity): mini completion ring, subject name, items opened X of Y, and a small expand chevron. Tapping the
 * row or the chevron opens that subject's details RIGHT BELOW the row, in the page (no popup, slice B4): the stat tiles, the
 * chapters list and the exam attempts list ([StatusSubjectDetailInline], shared with the My status board). One subject is open
 * at a time; tapping it again closes it. Real numbers only (G4). [rows] = the progress rows the status screen already loaded
 * (already filtered to the picks). Catalog = [ResourceCatalogIndex], exams = [ExamAttemptsReader].
 *
 * OFFLINE STATUS (2026-10-23, docs/OFFLINE_STATUS_PLAN.md slice O3): the panel always draws. (1) Catalog and exam attempts
 * available (the exam read may be the saved copy, then one amber line says when it was saved): the rows as before.
 * (2) Either is missing, or [progressKnown] is false (the status screen could not read the progress, so its empty skeleton
 * is shown): a notice with Retry, then one EMPTY row per picked subject (name from the packed subject list, empty ring,
 * dashes instead of "x of y items opened", nothing to open). Nothing is counted as zero. With no picks there are no names to
 * draw: "Nothing to show yet." plus the notice. Online behaviour is unchanged. Slice O5a: the panel also reads again by
 * itself when the internet comes back.
 *
 * SAVED COPY FIRST (2026-11-03, docs/READER_EXAM_FLASHCARD_FIX_PLAN.md slice P1c, part c2a): when the saved catalog
 * ([ResourceCatalogIndex.getSaved]) and the saved exam attempts ([ExamAttemptsReader.fetchAllSaved]) both exist, the rows are
 * drawn from them at once (no network, no "No connection" line, because the server has not been asked yet); the normal read
 * then runs in the background and replaces the rows. A failed refresh keeps the rows already drawn; the spinner and the
 * empty skeleton only show when nothing usable is saved.
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
@Composable
fun StatusSubjectsPanel(rows: List<ProgressRow>, educationLevel: String?, progressKnown: Boolean = true) {
    val context = LocalContext.current
    var reloadKey by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    // true = no data to compute from: draw the empty skeleton rows (see skeletonNames)
    var noData by remember { mutableStateOf(false) }
    var entries by remember { mutableStateOf<List<SubjectEntry>>(emptyList()) }
    var skeletonNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var savedAtMs by remember { mutableStateOf<Long?>(null) }
    // the subject whose details are open below its row (null = all closed)
    var openKey by remember { mutableStateOf<String?>(null) }
    // OFFLINE STATUS (O5a): goes up when the internet comes back, which reads the exam attempts again
    val netBack = rememberNetworkReturnCount()

    LaunchedEffect(rows, reloadKey, progressKnown, netBack) {
        noData = false
        savedAtMs = null
        val userId = AuthStateManager.currentUser.value?.id
        val picks = if (userId != null) MySubjectsStore.load(context, userId, educationLevel).keys else emptySet()
        val names = MySubjectsStore.packages.flatMap { it.subjects }.associate { it.key to it.name }
        if (!progressKnown) {
            entries = emptyList()
            skeletonNames = picks.map { names[it] ?: it.substringAfter('/') }.sorted()
            noData = true
            loading = false
            return@LaunchedEffect
        }
        // P1c: the saved copy first (no network), drawn at once when it gives at least one row
        var drewSaved = false
        val catalogSaved = ResourceCatalogIndex.getSaved(context)
        val attemptsSaved = ExamAttemptsReader.fetchAllSaved()
        if (catalogSaved != null && attemptsSaved != null) {
            val savedEntries = withContext(Dispatchers.Default) { buildEntries(catalogSaved.items, rows, attemptsSaved.rows, picks) }
            if (savedEntries.isNotEmpty()) {
                entries = savedEntries
                loading = false
                drewSaved = true
            }
        }
        if (!drewSaved) loading = true
        val catalog = ResourceCatalogIndex.get(context)
        val attempts = ExamAttemptsReader.fetchAll()
        if (catalog == null || attempts !is ExamAttemptsFetchResult.Success) {
            // a failed refresh keeps the saved rows that are already drawn
            if (!drewSaved) {
                entries = emptyList()
                skeletonNames = picks.map { names[it] ?: it.substringAfter('/') }.sorted()
                noData = true
            }
            loading = false
            return@LaunchedEffect
        }
        savedAtMs = if (attempts.fromCache) attempts.savedAtMs else null
        entries = withContext(Dispatchers.Default) { buildEntries(catalog.items, rows, attempts.rows, picks) }
        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(WisdomGlassFill)
            .border(BorderStroke(1.dp, WisdomAccentSky.copy(alpha = 0.20f)), RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(WisdomAccentSky.copy(alpha = 0.15f))
                .border(BorderStroke(1.dp, WisdomAccentSky.copy(alpha = 0.35f)), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text("By subject", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WisdomAccentSky)
        }

        when {
            loading -> Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = WisdomCyan, modifier = Modifier.size(24.dp))
            }
            noData -> {
                WisdomMessageCard(
                    message = SUBJECTS_CONNECT_NOTICE,
                    isError = false,
                    onRetry = { reloadKey += 1 }
                )
                if (skeletonNames.isEmpty()) {
                    Text("Nothing to show yet.", fontSize = 12.sp, color = WisdomMuted)
                } else {
                    skeletonNames.forEach { n -> SkeletonSubjectRow(n) }
                }
            }
            entries.isEmpty() -> Text("Nothing to show yet.", fontSize = 12.sp, color = WisdomMuted)
            else -> {
                val savedAt = savedAtMs
                if (savedAt != null) {
                    Text(
                        "No connection to the server. Exam details as last saved on ${ssFormatSavedAt(savedAt)}.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = WisdomAccentAmber,
                        lineHeight = 16.sp
                    )
                }
                entries.forEach { e ->
                    val isOpen = openKey == e.key
                    // the row and, when open, its details right below it (no popup)
                    Column(
                        modifier = Modifier.fillMaxWidth().animateContentSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SubjectRow(e, isOpen) { openKey = if (isOpen) null else e.key }
                        if (isOpen) StatusSubjectDetailInline(title = e.name, breakdown = e.breakdown)
                    }
                }
            }
        }
    }
}

/** OFFLINE STATUS (O3): one empty row (name, empty ring, dashes). Not clickable: there is nothing to open. */
@Composable
private fun SkeletonSubjectRow(name: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WisdomGlassFillQuiet)
            .border(BorderStroke(1.dp, WisdomGlassBorder), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatusRing(
            value = null,
            color = statusRingColor(null),
            diameter = 48.dp,
            strokeWidth = 5.dp,
            textSize = 12.sp
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = WisdomInk, lineHeight = 19.sp)
            Text("— of — items opened", fontSize = 11.sp, color = WisdomMuted)
        }
    }
}

@Composable
private fun SubjectRow(entry: SubjectEntry, expanded: Boolean, onClick: () -> Unit) {
    val t = entry.breakdown.totals
    val pct = t.completionPct?.roundToInt()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WisdomGlassFillQuiet)
            .border(BorderStroke(1.dp, WisdomGlassBorder), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatusRing(
            value = pct?.let { it / 100f },
            color = statusRingColor(pct),
            diameter = 48.dp,
            strokeWidth = 5.dp,
            textSize = 12.sp
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(entry.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = WisdomInk, lineHeight = 19.sp)
            Text("${t.itemsOpened} of ${t.itemsTotal} items opened", fontSize = 11.sp, color = WisdomMuted)
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(WisdomAccentSky.copy(alpha = 0.15f))
                .border(BorderStroke(1.dp, WisdomAccentSky.copy(alpha = 0.30f)), CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expanded) "Hide ${entry.name} status details" else "Show ${entry.name} status details",
                tint = WisdomAccentSky,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

private fun buildEntries(
    catalog: List<CatalogItem>,
    rows: List<ProgressRow>,
    attempts: List<ExamAttemptRow>,
    picks: Set<String>
): List<SubjectEntry> {
    val progress = StatusBreakdown.progressMap(rows)
    val names = MySubjectsStore.packages.flatMap { it.subjects }.associate { it.key to it.name }
    val keys = if (picks.isNotEmpty()) picks.toList() else StatusBreakdown.subjectKeys(catalog)
    return keys.mapNotNull { key ->
        val b = StatusBreakdown.subjectStatus(key, catalog, progress, attempts) ?: return@mapNotNull null
        // no picks: only subjects that have any activity
        if (picks.isEmpty() && b.totals.itemsOpened == 0 && b.exams.attemptsTaken == 0) return@mapNotNull null
        SubjectEntry(key, names[key] ?: key.substringAfter('/'), b)
    }.sortedBy { it.name }
}
