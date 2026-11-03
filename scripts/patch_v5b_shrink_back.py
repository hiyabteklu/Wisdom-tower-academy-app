#!/usr/bin/env python3
"""V5b (docs/IOS_MOTION_V2_PLAN.md): the package screen shrinks back into the card on Back.

Needs the V5 patch (ui/theme/WisdomGrowFromCard.kt, merged). Adds `WisdomGrowLayer(item, zIndexValue) { ... }` to that file:
  - opening: same grow out of the tapped card as before, now owned by the layer;
  - closing: the last package stays composed while the screen shrinks, moves back onto the card it came from and fades out
    (push/pop spring, no bounce). If no card is known, a short fade with a small scale instead.
MainActivity.kt (Packages tab): the package screen is drawn inside the layer instead of `if (currentPkg != null)`, the old
`.wisdomGrowFromCard(...)` on it is removed (one clock only), and its back handler is only on while a package is selected.
Back handling, `selectedPackage`, `leavePackage()` and tab logic are not changed.
"""
import subprocess
import sys
from pathlib import Path

BASE = Path("app/src/main/java/com/wisdomtower/academy")
MAIN = BASE / "MainActivity.kt"
HELPER = BASE / "ui/theme/WisdomGrowFromCard.kt"
MARK = "V5b shrink"

HELPER_IMPORT_OLD = "import androidx.compose.ui.unit.dp\n"
HELPER_IMPORT_NEW = (
    "import androidx.compose.foundation.layout.Box\n"
    "import androidx.compose.foundation.layout.fillMaxSize\n"
    "import androidx.compose.runtime.getValue\n"
    "import androidx.compose.runtime.mutableStateOf\n"
    "import androidx.compose.runtime.setValue\n"
    "import androidx.compose.ui.graphics.GraphicsLayerScope\n"
    "import androidx.compose.ui.unit.dp\n"
    "import androidx.compose.ui.zIndex\n"
)

HELPER_APPEND = '''
private class GrowHolder<T> {
    var last: T? = null
    var origin: Rect? = null
    var active = false
}

private fun GraphicsLayerScope.applyGrow(p: Float, box: Rect?, layerX: Float, layerY: Float) {
    if (p >= 1f || size.width <= 0f) {
        scaleX = 1f
        scaleY = 1f
        translationX = 0f
        translationY = 0f
        alpha = 1f
        clip = false
    } else if (box != null) {
        val s = lerpFloat(box.width / size.width, 1f, p)
        scaleX = s
        scaleY = s
        translationX = lerpFloat(box.center.x - (layerX + size.width / 2f), 0f, p)
        translationY = lerpFloat(box.center.y - (layerY + size.height / 2f), 0f, p)
        alpha = (p * 3f).coerceIn(0f, 1f)
        shape = RoundedCornerShape(lerpFloat(16.dp.toPx(), 0f, p))
        clip = true
    } else {
        val s = lerpFloat(0.94f, 1f, p)
        scaleX = s
        scaleY = s
        translationX = 0f
        translationY = 0f
        alpha = p.coerceIn(0f, 1f)
        clip = false
    }
}

/**
 * V5b shrink (docs/IOS_MOTION_V2_PLAN.md): a full-screen layer that grows out of the tapped card when [item] appears and
 * shrinks back into the same card when [item] becomes null. The last item stays composed until the close finishes.
 * `graphicsLayer` only; the layer never changes the state that decides visibility, so back handling stays where it is.
 */
@Composable
fun <T : Any> WisdomGrowLayer(
    item: T?,
    zIndexValue: Float,
    content: @Composable (T) -> Unit
) {
    val holder = remember { GrowHolder<T>() }
    val wanted = item != null
    if (item != null) {
        holder.last = item
        if (!holder.active) {
            holder.active = true
            holder.origin = WisdomCardOrigin.take()
        }
    } else {
        holder.active = false
    }
    val progress = remember { Animatable(if (wanted) 1f else 0f) }
    var shown by remember { mutableStateOf(wanted) }
    val layerPos = remember { FloatArray(2) }

    LaunchedEffect(wanted) {
        if (wanted) {
            shown = true
            progress.animateTo(1f, spring(dampingRatio = 1f, stiffness = WisdomMotionV2.PushStiffness))
        } else {
            progress.animateTo(0f, spring(dampingRatio = 1f, stiffness = WisdomMotionV2.PopStiffness))
            shown = false
            holder.last = null
        }
    }

    val current = holder.last
    if (current != null && (wanted || shown)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(zIndexValue)
                .onGloballyPositioned { coordinates ->
                    val p = coordinates.positionInRoot()
                    layerPos[0] = p.x
                    layerPos[1] = p.y
                }
                .graphicsLayer {
                    applyGrow(progress.value, holder.origin, layerPos[0], layerPos[1])
                }
        ) {
            content(current)
        }
    }
}
'''

A20 = " " * 20
A24 = " " * 24
A28 = " " * 28
A32 = " " * 32
MAIN_EDITS = [
    (
        A20 + "if (currentPkg != null) {\n"
        + A24 + "PackageDetailScreen(\n"
        + A28 + "pkg = currentPkg,\n",
        A20 + "WisdomGrowLayer(item = currentPkg, zIndexValue = 10f) { shownPkg -> // V5b shrink\n"
        + A24 + "PackageDetailScreen(\n"
        + A28 + "pkg = shownPkg,\n",
    ),
    (
        'activeWorkspace = Triple(currentPkg.name, "Academy Package", studyPath)',
        'activeWorkspace = Triple(shownPkg.name, "Academy Package", studyPath)',
    ),
    (
        "backEnabled = activeWorkspace == null, // BACKFIX-S7\n"
        + A28 + "modifier = Modifier\n"
        + A32 + ".fillMaxSize()\n"
        + A32 + ".wisdomGrowFromCard(currentPkg.name) // V5 grow\n",
        "backEnabled = activeWorkspace == null && currentPkg != null, // BACKFIX-S7\n"
        + A28 + "modifier = Modifier\n"
        + A32 + ".fillMaxSize()\n",
    ),
    (
        "import com.wisdomtower.academy.ui.theme.wisdomGrowFromCard\n",
        "import com.wisdomtower.academy.ui.theme.WisdomGrowLayer\n",
    ),
]

for f in (MAIN, HELPER):
    if not f.exists():
        print(f"ERROR: {f} is missing (is the V5 patch merged?); nothing changed")
        sys.exit(1)

main_text = MAIN.read_text(encoding="utf-8")
helper_text = HELPER.read_text(encoding="utf-8")
if MARK in main_text and MARK in helper_text:
    print("already patched, nothing to do")
    sys.exit(0)
if MARK in main_text or MARK in helper_text:
    print("ERROR: the marker is in only one file; the patch is incomplete; nothing changed")
    sys.exit(1)

if helper_text.count(HELPER_IMPORT_OLD) != 1:
    print("ERROR: helper import anchor not found exactly once; nothing changed")
    sys.exit(1)
new_helper = helper_text.replace(HELPER_IMPORT_OLD, HELPER_IMPORT_NEW) + HELPER_APPEND

new_main = main_text
for i, (old, new) in enumerate(MAIN_EDITS):
    n = new_main.count(old)
    if n != 1:
        print(f"ERROR: MainActivity.kt edit #{i + 1} anchor found {n} times (need exactly 1); nothing changed")
        print(old[:300])
        sys.exit(1)
    new_main = new_main.replace(old, new)

# no reference to the outer, nullable `currentPkg` may be left inside the layer block
start = new_main.index("WisdomGrowLayer(item = currentPkg")
end = new_main.index("// UX-CONSISTENCY-2026-10-17", start)
leftover = new_main[start:end].count("currentPkg.")
if leftover:
    print(f"ERROR: {leftover} use(s) of currentPkg. left inside the layer block; nothing changed")
    sys.exit(1)

HELPER.write_text(new_helper, encoding="utf-8")
MAIN.write_text(new_main, encoding="utf-8")

# keep the packed copy of MainActivity.kt in step (HANDOFF rule 10); a failure here does not undo the patch
pack = Path("scripts/pack_mainactivity.py")
if pack.exists():
    r = subprocess.run([sys.executable, str(pack)], capture_output=True, text=True)
    print("pack_mainactivity.py:", "ok" if r.returncode == 0 else "FAILED (patch itself is applied): " + r.stderr[-200:])
print(f"patched ({len(MAIN_EDITS)} edits in MainActivity.kt, plus the layer added to {HELPER.name})")
