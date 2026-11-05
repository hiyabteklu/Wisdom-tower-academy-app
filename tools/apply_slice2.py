#!/usr/bin/env python3
"""Slice 2: silent update check behind the splash (wait up to 1.5 s, then fail open).
Run from the repo root:  python3 tools/apply_slice2.py
Safe to re-run: it refuses to patch twice and aborts without writing if any anchor is missing."""
import sys, pathlib

ROOT = pathlib.Path(__file__).resolve().parent.parent
MAIN = ROOT / "app/src/main/java/com/wisdomtower/academy/MainActivity.kt"
GATE = ROOT / "app/src/main/java/com/wisdomtower/academy/ui/update/UpdateGateScreens.kt"

def rep(text, old, new, label):
    n = text.count(old)
    if n != 1:
        sys.exit(f"ABORT: anchor '{label}' found {n} times (expected 1). Nothing written.")
    return text.replace(old, new)

m = MAIN.read_text(encoding="utf-8")
if "gateOpenEarly" in m:
    sys.exit("Already patched (gateOpenEarly present). Nothing to do.")

# 1. flag
m = rep(m,
"                var updateGateState by remember { mutableStateOf(UpdateGateState.Checking) }\n",
"                var updateGateState by remember { mutableStateOf(UpdateGateState.Checking) }\n"
"                var gateOpenEarly by remember { mutableStateOf(false) }\n", "flag")

# 2. fail open after the early release
m = rep(m,
"val wasAlreadyReady = updateGateState == UpdateGateState.Ready\n",
"val wasAlreadyReady = updateGateState == UpdateGateState.Ready || gateOpenEarly\n", "wasAlreadyReady")

# 3. startup effect
m = rep(m,
"""                LaunchedEffect(Unit) {
                    androidx.compose.runtime.withFrameNanos { }
                    keepSplash = false
                    checkUpdate(true)
                }
""",
"""                // Silent check: keep the system splash up to 1.5 s while the check runs. If it is slow,
                // open the app anyway (fail open); a later forced-update result still shows the prompt.
                LaunchedEffect(Unit) {
                    launch { checkUpdate(true) }
                    kotlinx.coroutines.withTimeoutOrNull(1500L) {
                        androidx.compose.runtime.snapshotFlow { updateGateState }
                            .first { it != UpdateGateState.Checking }
                    }
                    if (updateGateState == UpdateGateState.Checking) gateOpenEarly = true
                    androidx.compose.runtime.withFrameNanos { }
                    keepSplash = false
                }
""", "startup effect")

# 4. Checking branch
m = rep(m,
"                    UpdateGateState.Checking -> UpdateCheckingScreen()\n",
"""                    UpdateGateState.Checking -> if (gateOpenEarly) {
                        MainScreen(
                            onReady = { keepSplash = false },
                            initialNotificationUrl = pendingNotificationUrl.value,
                            onNotificationUrlConsumed = { pendingNotificationUrl.value = null }
                        )
                    } else {
                        Box(Modifier.fillMaxSize().background(Color(0xFF060B15)))
                    }
""", "checking branch")

# imports
m = rep(m, "import com.wisdomtower.academy.ui.update.UpdateCheckingScreen\n", "", "import UpdateCheckingScreen")
if "import kotlinx.coroutines.flow.first\n" not in m:
    m = rep(m, "import com.wisdomtower.academy.ui.update.UpdateCheckBlocked\n",
            "import com.wisdomtower.academy.ui.update.UpdateCheckBlocked\nimport kotlinx.coroutines.flow.first\n", "import first")

g = GATE.read_text(encoding="utf-8")
start = g.index("@Composable\nfun UpdateCheckingScreen()")
end = g.index("@Composable\nfun UpdateCheckBlocked")
g = g[:start] + g[end:]
for imp in ["import androidx.compose.foundation.layout.Arrangement\n",
            "import androidx.compose.foundation.layout.Box\n",
            "import androidx.compose.foundation.layout.Column\n",
            "import androidx.compose.foundation.layout.fillMaxSize\n",
            "import androidx.compose.material3.CircularProgressIndicator\n",
            "import androidx.compose.material3.MaterialTheme\n",
            "import androidx.compose.ui.Alignment\n",
            "import androidx.compose.ui.Modifier\n",
            "import androidx.compose.ui.unit.dp\n"]:
    g = g.replace(imp, "")

MAIN.write_text(m, encoding="utf-8")
GATE.write_text(g, encoding="utf-8")
print("Slice 2 applied. Build and test: cold start online, slow network, airplane mode, forced update.")
