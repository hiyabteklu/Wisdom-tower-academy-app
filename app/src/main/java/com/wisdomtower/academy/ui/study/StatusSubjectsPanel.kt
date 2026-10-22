package com.wisdomtower.academy.ui.study

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
import androidx.compose.material.icons.filled.Speed
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
import kotlin.math.roundToInt

private data class SubjectEntry(val key: String, val name: String, val breakdown: SubjectBreakdown)

/**
 * My Status V2 (S3): the "By subject" panel inside "Your status". One row per picked subject (T8 picks; with no picks, every
 * subject that has activity): mini completion ring, subject name, items opened X of Y, and a small gauge icon. Tapping the row
 * or the icon opens [StatusDetailCard]. Real numbers only (G4). [rows] = the progress rows the status screen already loaded
 * (already filtered to the picks). Catalog = [ResourceCatalogIndex], exams = [ExamAttemptsReader]; a failed read shows the
 * existing message card with Retry.
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
@Composable
fun StatusSubjectsPanel(rows: List<ProgressRow>, educationLevel: String?) {
    val context = LocalContext.current
    var reloadKey by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var entries by remember { mutableStateOf<List<SubjectEntry>>(emptyList()) }
    var openKey by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(rows, reloadKey) {
        loading = true
        failed = false
        val userId = AuthStateManager.currentUser.value?.id
        val picks = if (userId != null) MySubjectsStore.load(context, userId, educationLevel).keys else emptySet()
        val catalog = ResourceCatalogIndex.get(context)
        val attempts = ExamAttemptsReader.fetchAll()
        if (catalog == null || attempts !is ExamAttemptsFetchResult.Success) {
            failed = true
            loading = false
            return@LaunchedEffect
        }
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
            failed -> WisdomMessageCard(
                message = "Could not load your subject details. Check your internet connection and try again.",
                isError = true,
                onRetry = { reloadKey += 1 }
            )
            entries.isEmpty() -> Text("Nothing to show yet.", fontSize = 12.sp, color = WisdomMuted)
            else -> entries.forEach { e -> SubjectRow(e) { openKey = e.key } }
        }
    }

    val open = entries.firstOrNull { it.key == openKey }
    if (open != null) {
        StatusDetailCard(title = open.name, breakdown = open.breakdown, onDismiss = { openKey = null })
    }
}

@Composable
private fun SubjectRow(entry: SubjectEntry, onClick: () -> Unit) {
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
            Icon(Icons.Filled.Speed, contentDescription = "Open ${entry.name} status", tint = WisdomAccentSky, modifier = Modifier.size(18.dp))
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
