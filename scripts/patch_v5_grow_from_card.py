#!/usr/bin/env python3
"""V5 (docs/IOS_MOTION_V2_PLAN.md): the package screen grows out of the card you tapped.

Creates `ui/theme/WisdomGrowFromCard.kt`:
  - `WisdomCardOrigin`: remembers the on-screen box of the last tapped card for 1.5 seconds.
  - `Modifier.wisdomGrowFromCard(key)`: on the first frame of a screen the layer starts at the size and place of that card
    (uniform scale, rounded corners, fading in) and springs to full screen with the push spring. No remembered card, or a card
    older than 1.5 s = no effect (the screen just appears as before).
Edits:
  - ui/packages/OpenCard.kt: every OpenCard records its box when tapped (card body or Open button). Click actions unchanged.
  - MainActivity.kt (Packages tab): the package list stays composed under the package screen (so it is visible while the
    screen grows, keeps its scroll position on Back, and is hidden from TalkBack while the package screen is open), and the
    package screen gets `.wisdomGrowFromCard(name)`.
Not in this slice: shrinking back into the card on Back (Back is as before), hub cards and the Home program card.
"""
import subprocess
import sys
from pathlib import Path

BASE = Path("app/src/main/java/com/wisdomtower/academy")
MAIN = BASE / "MainActivity.kt"
CARD = BASE / "ui/packages/OpenCard.kt"
HELPER = BASE / "ui/theme/WisdomGrowFromCard.kt"
MARK = "V5 grow"

HELPER_SRC = '''package com.wisdomtower.academy.ui.theme

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

/**
 * V5 (docs/IOS_MOTION_V2_PLAN.md): the box of the card that was just tapped, so the next screen can grow out of it.
 * Only a tap in the last 1.5 seconds counts, and it is used once.
 */
object WisdomCardOrigin {
    private var bounds: Rect? = null
    private var at = 0L

    fun record(box: Rect?) {
        bounds = box
        at = SystemClock.uptimeMillis()
    }

    fun take(): Rect? {
        val box = bounds
        val fresh = box != null && SystemClock.uptimeMillis() - at < 1500L
        bounds = null
        return if (fresh) box else null
    }
}

/** Per-card holder of the card's current box (plain field, no recomposition). */
class WisdomCardSource {
    var bounds: Rect? = null
}

@Composable
fun rememberWisdomCardSource(): WisdomCardSource = remember { WisdomCardSource() }

fun Modifier.wisdomCardSource(source: WisdomCardSource): Modifier =
    onGloballyPositioned { coordinates -> source.bounds = coordinates.boundsInRoot() }

fun WisdomCardSource.markOrigin() {
    WisdomCardOrigin.record(bounds)
}

private fun lerpFloat(a: Float, b: Float, t: Float): Float = a + (b - a) * t

/**
 * The screen starts as a small copy of itself on top of the tapped card and springs to full screen (push spring, no bounce).
 * Scale, move, fade and corner radius only, read in the draw phase through `graphicsLayer`; touch works from the first frame.
 * Without a fresh tapped card ([WisdomCardOrigin]) nothing is animated.
 */
fun Modifier.wisdomGrowFromCard(key: Any?): Modifier = composed {
    val origin = remember(key) { WisdomCardOrigin.take() }
    val progress = remember(key) { Animatable(if (origin == null) 1f else 0f) }
    val layerPos = remember { FloatArray(2) }
    LaunchedEffect(key) {
        if (origin != null) {
            progress.animateTo(1f, spring(dampingRatio = 1f, stiffness = WisdomMotionV2.PushStiffness))
        }
    }
    this
        .onGloballyPositioned { coordinates ->
            val p = coordinates.positionInRoot()
            layerPos[0] = p.x
            layerPos[1] = p.y
        }
        .graphicsLayer {
            val p = progress.value
            val box = origin
            if (box != null && p < 1f && size.width > 0f) {
                val s = lerpFloat(box.width / size.width, 1f, p)
                scaleX = s
                scaleY = s
                translationX = lerpFloat(box.center.x - (layerPos[0] + size.width / 2f), 0f, p)
                translationY = lerpFloat(box.center.y - (layerPos[1] + size.height / 2f), 0f, p)
                alpha = (p * 3f).coerceIn(0f, 1f)
                shape = RoundedCornerShape(lerpFloat(16.dp.toPx(), 0f, p))
                clip = true
            } else {
                scaleX = 1f
                scaleY = 1f
                translationX = 0f
                translationY = 0f
                alpha = 1f
                clip = false
            }
        }
}
'''.replace("import androidx.compose.ui.layout.positionInRoot\n",
            "import androidx.compose.ui.layout.positionInRoot\nimport androidx.compose.ui.unit.dp\n")

OPENCARD_EDITS = [
    (
        "import com.wisdomtower.academy.ui.theme.wisdomPressClickable\n",
        "import com.wisdomtower.academy.ui.theme.wisdomPressClickable\n"
        "import com.wisdomtower.academy.ui.theme.markOrigin\n"
        "import com.wisdomtower.academy.ui.theme.rememberWisdomCardSource\n"
        "import com.wisdomtower.academy.ui.theme.wisdomCardSource\n",
    ),
    (
        "    val openText = if (night) WisdomCyan else WisdomDarkOnCyan\n",
        "    val openText = if (night) WisdomCyan else WisdomDarkOnCyan\n"
        "    val cardSource = rememberWisdomCardSource() // V5 grow\n"
        "    val markedClick: () -> Unit = {\n"
        "        cardSource.markOrigin()\n"
        "        onClick()\n"
        "    }\n",
    ),
    (
        "    WisdomGlassCard(\n"
        "        modifier = modifier.fillMaxWidth(),\n"
        "        cornerRadius = 16.dp,\n"
        "        onClick = onClick\n"
        "    ) {\n",
        "    WisdomGlassCard(\n"
        "        modifier = modifier.fillMaxWidth().wisdomCardSource(cardSource),\n"
        "        cornerRadius = 16.dp,\n"
        "        onClick = markedClick\n"
        "    ) {\n",
    ),
    (
        "                        .wisdomPressClickable(onClick = onClick)\n",
        "                        .wisdomPressClickable(onClick = markedClick)\n",
    ),
]

A20 = " " * 20
A24 = " " * 24
A28 = " " * 28
MAIN_EDITS = [
    (  # keep the list composed under the package screen
        A20 + "val currentPkg = selectedPackage\n"
        + A20 + "if (currentPkg != null) {\n"
        + A24 + "PackageDetailScreen(\n",
        A20 + "val currentPkg = selectedPackage\n"
        + A20 + "com.wisdomtower.academy.ui.games.GameTabScreen( // V5 grow: the list stays under the package screen\n"
        + A24 + "modifier = Modifier\n"
        + A28 + ".fillMaxSize()\n"
        + A28 + ".appBackdrop()\n"
        + A28 + ".zIndex(10f)\n"
        + A28 + ".then(if (currentPkg != null) Modifier.clearAndSetSemantics { } else Modifier),\n"
        + A24 + "packageList = catalogPackages,\n"
        + A24 + "onSelectPackage = { pkg ->\n"
        + A28 + "markPackageOrigin()\n"
        + A28 + "selectedPackage = pkg\n"
        + A24 + "}\n"
        + A20 + ")\n"
        + A20 + "if (currentPkg != null) {\n"
        + A24 + "PackageDetailScreen(\n",
    ),
    (  # the package screen grows from the card; the old else branch (list) is gone
        " " * 28 + "modifier = Modifier\n"
        + " " * 32 + ".fillMaxSize()\n"
        + " " * 32 + ".appBackdrop()\n"
        + " " * 32 + ".zIndex(10f)\n"
        + " " * 24 + ")\n"
        + " " * 20 + "} else {\n"
        + " " * 24 + "com.wisdomtower.academy.ui.games.GameTabScreen(\n"
        + " " * 28 + "modifier = Modifier\n"
        + " " * 32 + ".fillMaxSize()\n"
        + " " * 32 + ".appBackdrop()\n"
        + " " * 32 + ".zIndex(10f),\n"
        + " " * 28 + "packageList = catalogPackages,\n"
        + " " * 28 + "onSelectPackage = { pkg ->\n"
        + " " * 32 + "markPackageOrigin()\n"
        + " " * 32 + "selectedPackage = pkg\n"
        + " " * 28 + "}\n"
        + " " * 24 + ")\n"
        + " " * 20 + "}\n",
        " " * 28 + "modifier = Modifier\n"
        + " " * 32 + ".fillMaxSize()\n"
        + " " * 32 + ".wisdomGrowFromCard(currentPkg.name) // V5 grow\n"
        + " " * 32 + ".appBackdrop()\n"
        + " " * 32 + ".zIndex(10f)\n"
        + " " * 24 + ")\n"
        + " " * 20 + "}\n",
    ),
    (
        "import com.wisdomtower.academy.ui.theme.WisdomPushLayer\n",
        "import com.wisdomtower.academy.ui.theme.WisdomPushLayer\n"
        "import com.wisdomtower.academy.ui.theme.wisdomGrowFromCard\n",
    ),
]

for f in (MAIN, CARD):
    if not f.exists():
        print(f"ERROR: {f} is missing; nothing changed")
        sys.exit(1)

main_text = MAIN.read_text(encoding="utf-8")
card_text = CARD.read_text(encoding="utf-8")
if MARK in main_text and MARK in card_text and HELPER.exists():
    print("already patched, nothing to do")
    sys.exit(0)
if MARK in main_text or MARK in card_text:
    print("ERROR: the marker is in only one file; the patch is incomplete; nothing changed")
    sys.exit(1)


def apply(text, edits, name):
    for i, (old, new) in enumerate(edits):
        n = text.count(old)
        if n != 1:
            print(f"ERROR: {name} edit #{i + 1} anchor found {n} times (need exactly 1); nothing changed")
            print(old[:300])
            sys.exit(1)
        text = text.replace(old, new)
    return text


new_main = apply(main_text, MAIN_EDITS, "MainActivity.kt")
new_card = apply(card_text, OPENCARD_EDITS, "OpenCard.kt")

HELPER.write_text(HELPER_SRC, encoding="utf-8")
MAIN.write_text(new_main, encoding="utf-8")
CARD.write_text(new_card, encoding="utf-8")

# keep the packed copy of MainActivity.kt in step (HANDOFF rule 10); a failure here does not undo the patch
pack = Path("scripts/pack_mainactivity.py")
if pack.exists():
    r = subprocess.run([sys.executable, str(pack)], capture_output=True, text=True)
    print("pack_mainactivity.py:", "ok" if r.returncode == 0 else "FAILED (patch itself is applied): " + r.stderr[-200:])
print(f"patched ({len(MAIN_EDITS) + len(OPENCARD_EDITS)} edits in 2 files, plus {HELPER.name})")
