package com.wisdomtower.academy.ui.update

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.wisdomtower.academy.data.update.ApkInstaller
import com.wisdomtower.academy.data.update.AppRelease
import com.wisdomtower.academy.data.update.UpdateRepository
import kotlinx.coroutines.launch

@Composable
fun UpdatePrompt(
    release: AppRelease,
    onUpdateNow: () -> Unit,
    onLater: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var progress by remember(release.versionCode) { mutableStateOf<Float?>(null) }
    var downloading by remember(release.versionCode) { mutableStateOf(false) }
    var waitingForInstallPermission by remember(release.versionCode) { mutableStateOf(false) }
    var status by remember(release.versionCode) { mutableStateOf<String?>(null) }

    fun hasInstallPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun downloadAndInstall() {
        if (downloading) return
        downloading = true
        status = "Downloading update…"
        scope.launch {
            try {
                val apk = UpdateRepository.downloadApk(context, release) { value -> progress = value }
                status = "Opening Android installer…"
                ApkInstaller.install(context, apk)
            } catch (error: Exception) {
                status = error.message?.take(180) ?: "Check your internet connection and try again."
            } finally {
                downloading = false
            }
        }
    }

    DisposableEffect(lifecycleOwner, waitingForInstallPermission) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && waitingForInstallPermission && hasInstallPermission()) {
                waitingForInstallPermission = false
                downloadAndInstall()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (release.isForced) {
        BackHandler(enabled = true) { (context as? android.app.Activity)?.finish() }
    }

    AlertDialog(
        onDismissRequest = { if (!release.isForced && !downloading) onLater() },
        title = { Text(if (release.isForced) "Update required" else "Update available") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Wisdom Tower Academy ${release.versionName}", style = MaterialTheme.typography.titleMedium)
                if (release.changelog.isNotEmpty()) {
                    Text("What's new", style = MaterialTheme.typography.titleSmall)
                    release.changelog.forEach { line -> Text("• $line", style = MaterialTheme.typography.bodyMedium) }
                }
                if (downloading) {
                    LinearProgressIndicator(
                        progress = { (progress ?: 0f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(progress?.let { "${(it * 100).toInt()}%" } ?: "Downloading…")
                }
                status?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !downloading,
                onClick = {
                    if (hasInstallPermission()) {
                        downloadAndInstall()
                    } else {
                        waitingForInstallPermission = true
                        status = "Allow installs for Wisdom Tower Academy, then return here."
                        try {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                    Uri.parse("package:${context.packageName}")
                                )
                            )
                        } catch (_: Exception) {
                            waitingForInstallPermission = false
                            status = "Open Android Settings and allow Install unknown apps for Wisdom Tower Academy."
                        }
                    }
                }
            ) { Text(if (downloading) "Downloading…" else "Update now") }
        },
        dismissButton = {
            if (!release.isForced) {
                TextButton(enabled = !downloading, onClick = onLater) { Text("Later") }
            }
        }
    )
}
