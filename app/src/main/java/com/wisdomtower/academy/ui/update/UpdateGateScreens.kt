package com.wisdomtower.academy.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun UpdateCheckingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator()
            Text("Checking for required updates…", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun UpdateCheckBlocked(onRetry: () -> Unit) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text("Update check required") },
        text = {
            Text(
                "We couldn't verify the app's required version. Connect to the internet and try again. " +
                    "The app stays locked until its release status can be verified."
            )
        },
        confirmButton = {
            TextButton(onClick = onRetry) { Text("Try again") }
        }
    )
}
