package com.wisdomtower.academy.ui.update

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

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
