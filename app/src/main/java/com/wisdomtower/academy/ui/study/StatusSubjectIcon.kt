package com.wisdomtower.academy.ui.study

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.wisdomtower.academy.data.content.ExamAttemptsFetchResult
import com.wisdomtower.academy.data.content.ExamAttemptsReader
import com.wisdomtower.academy.data.content.ProgressFetchResult
import com.wisdomtower.academy.data.content.ResourceCatalogIndex
import com.wisdomtower.academy.data.content.StudentProgressReader
import com.wisdomtower.academy.ui.theme.WisdomAccentSky
import com.wisdomtower.academy.ui.theme.WisdomCyan
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * My Status V2 (S5): the small gauge icon that can sit anywhere a course is shown. Tap: it reads the catalog, the student's
 * own progress and exam attempts, finds the subject of [scopePath] and opens [StatusDetailCard] in place (same card as in
 * "Your status"). Real numbers only (G4). Failure or an unknown subject = a short message, never a made-up card.
 * [title] = the course name shown on the card. Show it only for a signed-in student.
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
@Composable
fun StatusSubjectIcon(scopePath: String, title: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var breakdown by remember { mutableStateOf<SubjectBreakdown?>(null) }

    fun load() {
        if (loading) return
        loading = true
        scope.launch {
            try {
                val catalog = ResourceCatalogIndex.get(context)
                val progress = StudentProgressReader.fetchAll()
                val attempts = ExamAttemptsReader.fetchAll()
                if (catalog == null || progress !is ProgressFetchResult.Success || attempts !is ExamAttemptsFetchResult.Success) {
                    Toast.makeText(context, "Could not load your subject details. Check your internet connection and try again.", Toast.LENGTH_SHORT).show()
                } else {
                    val path = scopePath.trim().trim('/')
                    val key = catalog.items.firstOrNull { it.scopePath.trim().trim('/') == path }?.subjectKey
                    val b = key?.let {
                        StatusBreakdown.subjectStatus(it, catalog.items, StatusBreakdown.progressMap(progress.rows), attempts.rows)
                    }
                    if (b == null) {
                        Toast.makeText(context, "Nothing to show yet.", Toast.LENGTH_SHORT).show()
                    } else {
                        breakdown = b
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Toast.makeText(context, "Could not load your subject details. Check your internet connection and try again.", Toast.LENGTH_SHORT).show()
            } finally {
                loading = false
            }
        }
    }

    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(WisdomAccentSky.copy(alpha = 0.15f))
            .border(BorderStroke(1.dp, WisdomAccentSky.copy(alpha = 0.30f)), CircleShape)
            .clickable(enabled = !loading) { load() },
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(color = WisdomCyan, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
        } else {
            Icon(Icons.Filled.Speed, contentDescription = "Open $title status", tint = WisdomAccentSky, modifier = Modifier.size(18.dp))
        }
    }

    val open = breakdown
    if (open != null) {
        StatusDetailCard(title = title, breakdown = open, onDismiss = { breakdown = null })
    }
}
