#!/usr/bin/env python3
"""Install U2 update checking and the release prompt into MainActivity.kt.

Uses unique exact anchors and an idempotency marker. Stops without writing if an
anchor does not match exactly once, so unexpected source changes are preserved.
"""
import sys
from pathlib import Path

FILE = Path("app/src/main/java/com/wisdomtower/academy/MainActivity.kt")
MARK = "WTA_U2_UPDATE_CHECK_INTEGRATION"

src = FILE.read_text(encoding="utf-8")
if MARK in src:
    print("U2 update integration already applied")
    sys.exit(0)

replacements = [
    (
        "import androidx.compose.runtime.collectAsState\n",
        "import androidx.compose.runtime.collectAsState\n"
        "import androidx.lifecycle.lifecycleScope\n"
        "import kotlinx.coroutines.launch\n"
        "import com.wisdomtower.academy.data.update.UpdateRepository\n"
        "import com.wisdomtower.academy.data.update.AppRelease\n"
        "import com.wisdomtower.academy.ui.update.UpdatePrompt\n"
    ),
    (
        "            MyApplicationTheme {\n"
        "                MainScreen(\n"
        "                    onReady = { keepSplash = false },\n"
        "                    initialNotificationUrl = pendingNotificationUrl.value,\n"
        "                    onNotificationUrlConsumed = { pendingNotificationUrl.value = null }\n"
        "                )\n"
        "            }\n",
        "            MyApplicationTheme {\n"
        "                // " + MARK + ": update metadata is optional; network failure never blocks startup.\n"
        "                val updateContext = LocalContext.current\n"
        "                val updateLifecycleOwner = LocalLifecycleOwner.current\n"
        "                var updateRelease by remember { mutableStateOf<AppRelease?>(null) }\n"
        "                var lastUpdateCheckAt by remember { mutableStateOf(0L) }\n"
        "                val checkUpdate: suspend () -> Unit = {\n"
        "                    val now = System.currentTimeMillis()\n"
        "                    if (now - lastUpdateCheckAt >= 60L * 60L * 1000L) {\n"
        "                        lastUpdateCheckAt = now\n"
        "                        val latest = UpdateRepository.latest(updateContext)\n"
        "                        if (latest != null && latest.isUpdateAvailable) updateRelease = latest\n"
        "                    }\n"
        "                }\n"
        "                LaunchedEffect(Unit) { checkUpdate() }\n"
        "                androidx.compose.runtime.DisposableEffect(updateLifecycleOwner) {\n"
        "                    val observer = LifecycleEventObserver { _, event ->\n"
        "                        if (event == Lifecycle.Event.ON_RESUME) {\n"
        "                            androidx.lifecycle.lifecycleScope.launch { checkUpdate() }\n"
        "                        }\n"
        "                    }\n"
        "                    updateLifecycleOwner.lifecycle.addObserver(observer)\n"
        "                    onDispose { updateLifecycleOwner.lifecycle.removeObserver(observer) }\n"
        "                }\n"
        "                MainScreen(\n"
        "                    onReady = { keepSplash = false },\n"
        "                    initialNotificationUrl = pendingNotificationUrl.value,\n"
        "                    onNotificationUrlConsumed = { pendingNotificationUrl.value = null }\n"
        "                )\n"
        "                updateRelease?.let { release ->\n"
        "                    UpdatePrompt(\n"
        "                        release = release,\n"
        "                        onUpdateNow = {\n"
        "                            try {\n"
        "                                updateContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(release.apkUrl)))\n"
        "                            } catch (_: Exception) {\n"
        "                                Toast.makeText(updateContext, \"Check your internet connection and try again.\", Toast.LENGTH_LONG).show()\n"
        "                            }\n"
        "                        },\n"
        "                        onLater = { updateRelease = null }\n"
        "                    )\n"
        "                }\n"
        "            }\n"
    ),
]

# validate all anchors before modifying anything
for anchor, _ in replacements:
    count = src.count(anchor)
    if count != 1:
        print(f"ERROR: anchor found {count} times (need exactly 1); file not changed", file=sys.stderr)
        sys.exit(1)

for anchor, add in replacements:
    src = src.replace(anchor, add, 1)

FILE.write_text(src, encoding="utf-8")
print("U2 update integration patched")
