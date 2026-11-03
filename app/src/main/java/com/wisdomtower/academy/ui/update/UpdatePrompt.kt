package com.wisdomtower.academy.ui.update

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wisdomtower.academy.data.update.AppRelease

@Composable
fun UpdatePrompt(
    release: AppRelease,
    onUpdateNow: () -> Unit,
    onLater: () -> Unit
) {
    if (release.isForced) BackHandler(enabled = true) { /* forced dialog remains visible */ }

    AlertDialog(
        onDismissRequest = { if (!release.isForced) onLater() },
        title = { Text(if (release.isForced) "Update required" else "Update available") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Wisdom Tower Academy ${release.versionName}", style = MaterialTheme.typography.titleMedium)
                if (release.changelog.isNotEmpty()) {
                    Text("What's new", style = MaterialTheme.typography.titleSmall)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
