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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * Update flow states (Slice 1). The button always reflects the real state, so there is
 * never a silent gap after the download finishes and a second tap cannot restart it.
 */
private enum class UpdatePhase { Idle, Downloading, Installing, ReadyToInstall }

@Composable
fun UpdatePrompt(
    release: AppRelease,
    onCheckAgain: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var progress by remember(release.versionCode) { mutableStateOf<Float?>(null) }
    var phase by remember(release.versionCode) { mutableStateOf(UpdatePhase.Idle) }
    // Downloaded + verified APK kept in memory so "Install" never downloads again.
    var readyApk by remember(release.versionCode) { mutableStateOf<File?>(null) }
    var sawPause by remember(release.versionCode) { mutableStateOf(false) }
    var waitingForInstallPermission by remember(release.versionCode) { mutableStateOf(false) }
    var waitingForTelegram by remember(release.versionCode) { mutableStateOf(false) }
    var status by remember(release.versionCode) { mutableStateOf<String?>(null) }

    val busy = phase == UpdatePhase.Downloading || phase == UpdatePhase.Installing

    fun hasInstallPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun launchInstaller(apk: File) {
        phase = UpdatePhase.Installing
        sawPause = false
        status = "Opening Android installer…"
        scope.launch {
            try {
                ApkInstaller.install(context, apk)
            } catch (error: Exception) {
                phase = UpdatePhase.ReadyToInstall
                status = error.message?.take(180) ?: "Could not open the installer. Tap Install to try again."
            }
        }
    }

    fun downloadAndInstall() {
        if (busy) return
        val existing = readyApk
        if (existing != null && existing.isFile) {
            launchInstaller(existing)
            return
        }
        phase = UpdatePhase.Downloading
        progress = null
        status = "Downloading update…"
        scope.launch {
            try {
                val apk = UpdateRepository.downloadApk(context, release) { value -> progress = value }
                readyApk = apk
                launchInstaller(apk)
            } catch (error: Exception) {
                phase = UpdatePhase.Idle
                status = error.message?.take(180) ?: "Check your internet connection and try again."
            }
        }
    }

    fun openTelegramFallback() {
        val url = release.telegramUrl ?: return
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            waitingForTelegram = true
        } catch (_: Exception) {
            status = "Telegram could not be opened. Try the in-app update instead."
        }
    }

    // If the installer never shows (or the user comes back without installing),
    // turn the button into "Install" so they can reopen it without downloading again.
    LaunchedEffect(phase) {
        if (phase == UpdatePhase.Installing) {
            delay(8_000)
            if (phase == UpdatePhase.Installing) {
                phase = UpdatePhase.ReadyToInstall
                status = "Tap Install to open the Android installer."
            }
        }
    }

    DisposableEffect(lifecycleOwner, waitingForInstallPermission, waitingForTelegram) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE && phase == UpdatePhase.Installing) {
                sawPause = true
            }
            if (event == Lifecycle.Event.ON_RESUME) {
                if (phase == UpdatePhase.Installing && sawPause) {
                    sawPause = false
                    phase = UpdatePhase.ReadyToInstall
                    status = "Installation not finished. Tap Install to try again."
                }
                if (waitingForInstallPermission && hasInstallPermission()) {
                    waitingForInstallPermission = false
                    downloadAndInstall()
                }
                if (waitingForTelegram) {
                    waitingForTelegram = false
                    onCheckAgain()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // A required update cannot be dismissed with Back or by tapping outside the dialog.
    BackHandler(enabled = true) { }

    AlertDialog(
        onDismissRequest = { },
        title = { Text("Update required") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Wisdom Tower Academy ${release.versionName}", style = MaterialTheme.typography.titleMedium)
                if (release.changelog.isNotEmpty()) {
                    Text("What's new", style = MaterialTheme.typography.titleSmall)
                    release.changelog.forEach { line -> Text("• $line", style = MaterialTheme.typography.bodyMedium) }
                }
                if (phase == UpdatePhase.Downloading) {
                    if (progress == null) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(
                            progress = { progress!!.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Text(progress?.let { "${(it * 100).toInt()}%" } ?: "Downloading…")
                } else if (phase == UpdatePhase.Installing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                status?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                if (release.telegramUrl != null) {
                    TextButton(enabled = !busy, onClick = { openTelegramFallback() }) {
                        Text("Get APK from Telegram")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy,
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
            ) {
                Text(
                    when (phase) {
                        UpdatePhase.Idle -> "Update now"
                        UpdatePhase.Downloading -> "Downloading…"
                        UpdatePhase.Installing -> "Opening installer…"
                        UpdatePhase.ReadyToInstall -> "Install"
                    }
                )
            }
        }
    )
}
