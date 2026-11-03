package com.wisdomtower.academy.ui.update

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.wisdomtower.academy.data.update.AppRelease

/**
 * Mandatory update dialog when the published release raises min_version_code.
 * Forced mode has no Later/dismiss route; Android Back exits the Activity instead.
 */
@Composable
fun UpdatePrompt(
    release: AppRelease,
    onUpdateNow: () -> Unit,
    onLater: () -> Unit
) {
    val context = LocalContext.current
    if (release.isForced) {
        BackHandler(enabled = true) {
            (context as? Activity)?.finish()
        }
    }

    AlertDialog(
        onDismissRequest = { if (!release.isForced) onLater() },
        title = { Text(if (release.isForced) "Update required" else "Update available") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Wisdom Tower Academy ${release.versionName}", style = MaterialTheme.typography.titleMedium)
                if (release.changelog.isNotEmpty()) {
                    Text("What's new", style = MaterialTheme.typography.titleSmall)
                    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacingSmall)) {
                        release.changelog.forEach { line ->
                            Text("• $line", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onUpdateNow) { Text("Update now") }
        },
        dismissButton = {
            if (!release.isForced) {
                TextButton(onClick = onLater) { Text("Later") }
            }
        }
    )
}

private val MaterialTheme.spacingSmall: androidx.compose.ui.unit.Dp
    get() = androidx.compose.ui.unit.dp(8)
