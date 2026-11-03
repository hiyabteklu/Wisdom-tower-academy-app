#!/usr/bin/env python3
"""V5c (docs/IOS_MOTION_V2_PLAN.md): the full-screen push layers grow out of the card you tapped.

Needs V5 and V5b (ui/theme/WisdomGrowFromCard.kt with `WisdomGrowLayer`, merged). Edits `ui/theme/WisdomPushLayer.kt` only:
  - when a pushed screen opens (subject / course workspace from a card, guides, competitions, drawer pages) and a card was
    tapped in the last 1.5 s (`WisdomCardOrigin`), the screen grows out of that card and shrinks back into it on Back
    (same shape of motion as the package screen). Predictive back follows the finger with the same shrink.
  - no tapped card = the old full-width slide, exactly as before.
  - while a screen grows out of a card, the tab underneath does not slide 30 percent to the left (it would carry the card
    away from under the growing screen); it still dims. `WisdomGrowFlag` is that one switch.
Back handling and the state that decides visibility are not touched.
"""
import sys
from pathlib import Path

BASE = Path("app/src/main/java/com/wisdomtower/academy/ui/theme")
PUSH = BASE / "WisdomPushLayer.kt"
HELPER = BASE / "WisdomGrowFromCard.kt"
MARK = "V5c grow"

HELPER_EDIT = (
    "private fun GraphicsLayerScope.applyGrow(",
    "internal fun GraphicsLayerScope.applyGrow(",
)

PUSH_EDITS = [
    (  # imports
        "import androidx.compose.ui.zIndex\n",
        "import androidx.compose.ui.zIndex\n"
        "import androidx.compose.runtime.SideEffect\n"
        "import androidx.compose.ui.geometry.Rect\n"
        "import androidx.compose.ui.layout.onGloballyPositioned\n"
        "import androidx.compose.ui.layout.positionInRoot\n",
    ),
    (  # the switch
        "private class LastItem<T>(var value: T?)\n",
        "private class LastItem<T>(var value: T?)\n"
        "\n"
        "/** V5c grow: true while a pushed screen grows out of a tapped card, so the tab below does not slide away from that card. */\n"
        "object WisdomGrowFlag {\n"
        "    var active by mutableStateOf(false)\n"
        "}\n",
    ),
    (  # origin taken once when the screen opens
        "    var shown by remember { mutableStateOf(wanted) }\n",
        "    var shown by remember { mutableStateOf(wanted) }\n"
        "    val originHolder = remember { LastItem<Rect>(null) } // V5c grow\n"
        "    val originTaken = remember { BooleanArray(1) }\n"
        "    val layerPos = remember { FloatArray(2) }\n"
        "    if (item != null) {\n"
        "        if (!originTaken[0]) {\n"
        "            originTaken[0] = true\n"
        "            originHolder.value = WisdomCardOrigin.take()\n"
        "        }\n"
        "    } else {\n"
        "        originTaken[0] = false\n"
        "    }\n"
        "    SideEffect {\n"
        "        if (originHolder.value != null && (wanted || shown)) WisdomGrowFlag.active = true\n"
        "    }\n",
    ),
    (  # at the end of the close, release the switch
        "            shown = false\n"
        "            last.value = null\n",
        "            shown = false\n"
        "            last.value = null\n"
        "            if (originHolder.value != null) {\n"
        "                WisdomGrowFlag.active = false\n"
        "                originHolder.value = null\n"
        "            }\n",
    ),
    (  # the transform
        "                .graphicsLayer {\n"
        "                    translationX = (1f - progress.value) * size.width\n"
        "                }\n",
        "                .onGloballyPositioned { coordinates ->\n"
        "                    val pos = coordinates.positionInRoot()\n"
        "                    layerPos[0] = pos.x\n"
        "                    layerPos[1] = pos.y\n"
        "                }\n"
        "                .graphicsLayer {\n"
        "                    val box = originHolder.value\n"
        "                    if (box != null) {\n"
        "                        applyGrow(progress.value, box, layerPos[0], layerPos[1])\n"
        "                    } else {\n"
        "                        translationX = (1f - progress.value) * size.width\n"
        "                    }\n"
        "                }\n",
    ),
    (  # the tab below stays put while a screen grows out of a card
        "        translationX = -size.width * 0.30f * p\n",
        "        translationX = -size.width * 0.30f * p * (if (WisdomGrowFlag.active) 0f else 1f) // V5c grow\n",
    ),
]

for f in (PUSH, HELPER):
    if not f.exists():
        print(f"ERROR: {f} is missing; nothing changed")
        sys.exit(1)

push_text = PUSH.read_text(encoding="utf-8")
helper_text = HELPER.read_text(encoding="utf-8")
if MARK in push_text:
    print("already patched, nothing to do")
    sys.exit(0)
if "WisdomGrowLayer" not in helper_text or helper_text.count(HELPER_EDIT[0]) != 1:
    print("ERROR: the V5b patch is not merged yet (WisdomGrowLayer / applyGrow not found); nothing changed")
    sys.exit(1)

new_push = push_text
for i, (old, new) in enumerate(PUSH_EDITS):
    n = new_push.count(old)
    if n != 1:
        print(f"ERROR: WisdomPushLayer.kt edit #{i + 1} anchor found {n} times (need exactly 1); nothing changed")
        print(old[:300])
        sys.exit(1)
    new_push = new_push.replace(old, new)
new_helper = helper_text.replace(*HELPER_EDIT)

PUSH.write_text(new_push, encoding="utf-8")
HELPER.write_text(new_helper, encoding="utf-8")
print(f"patched ({len(PUSH_EDITS)} edits in WisdomPushLayer.kt, 1 in WisdomGrowFromCard.kt)")
