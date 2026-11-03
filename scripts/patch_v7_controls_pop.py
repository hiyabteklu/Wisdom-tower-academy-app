#!/usr/bin/env python3
"""V7 (docs/IOS_MOTION_V2_PLAN.md): press feel for the hub list pills and back button, selection pop for the switch and the checkbox.

Creates `ui/theme/WisdomSelectionPop.kt` (`Modifier.wisdomSelectionPop(selected)`: when `selected` changes, a short spring pop;
nothing on the first draw). Then, in four existing files:
  - HubItemListScreen.kt: `ToolPill` (Download all, Downloading x of y) and `ScreenBackButton` (back arrow of every study tool)
    get the shared `wisdomPress` (scale 0.97 + light release haptic). Same interaction source as their clickable, ripple unchanged.
  - SettingsScreen.kt: the Switch of `ToggleRow` pops when it flips (the row already had the press feel).
  - NativeSignInScreen.kt: the terms Checkbox pops when it is ticked.
Click actions, text and colours are not touched.
"""
import sys
from pathlib import Path

BASE = Path("app/src/main/java/com/wisdomtower/academy")
HUB = BASE / "ui/study/HubItemListScreen.kt"
SETTINGS = BASE / "ui/settings/SettingsScreen.kt"
SIGNIN = BASE / "ui/auth/NativeSignInScreen.kt"
HELPER = BASE / "ui/theme/WisdomSelectionPop.kt"
MARK = "V7 controls pop"

HELPER_SRC = '''package com.wisdomtower.academy.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * V7 (docs/IOS_MOTION_V2_PLAN.md, "Selection"): a small spring pop when a switch or checkbox changes.
 * Nothing happens on the first draw. When [selected] becomes true the control starts at [from] and springs to full size
 * (damping 0.55, a small bounce on a small thing); when it becomes false it just rests at full size.
 * Scale only, read in the draw phase through `graphicsLayer`; the control's own click and state are not touched.
 */
fun Modifier.wisdomSelectionPop(selected: Boolean, from: Float = 0.8f): Modifier = composed {
    val scale = remember { Animatable(1f) }
    val firstDraw = remember { mutableStateOf(true) }
    LaunchedEffect(selected) {
        if (firstDraw.value) {
            firstDraw.value = false
        } else if (selected) {
            scale.snapTo(from)
            scale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 500f))
        } else {
            scale.snapTo(1f)
        }
    }
    graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}
'''

PRESS_IMPORT = "import com.wisdomtower.academy.ui.theme.wisdomPress\n"

# (file, [(old, new), ...]); every old must be found exactly once
JOBS = [
    (HUB, [
        (  # ToolPill
            "    Box(\n"
            "        modifier = Modifier\n"
            "            .clip(RoundedCornerShape(10.dp))\n"
            "            .background(color.copy(alpha = 0.12f))\n"
            "            .border(BorderStroke(1.dp, color.copy(alpha = 0.45f)), RoundedCornerShape(10.dp))\n"
            "            .clickable(\n"
            "                enabled = enabled,\n"
            "                interactionSource = remember { MutableInteractionSource() },\n",
            "    val pillSource = remember { MutableInteractionSource() } // V7 controls pop\n"
            "    Box(\n"
            "        modifier = Modifier\n"
            "            .wisdomPress(pillSource)\n"
            "            .clip(RoundedCornerShape(10.dp))\n"
            "            .background(color.copy(alpha = 0.12f))\n"
            "            .border(BorderStroke(1.dp, color.copy(alpha = 0.45f)), RoundedCornerShape(10.dp))\n"
            "            .clickable(\n"
            "                enabled = enabled,\n"
            "                interactionSource = pillSource,\n",
        ),
        (  # ScreenBackButton
            "    Box(\n"
            "        modifier = modifier\n"
            "            .size(48.dp)\n"
            "            .clip(CircleShape)\n"
            "            .background(WisdomGlassFillStrong)\n"
            "            .border(BorderStroke(1.dp, WisdomGlassBorder), CircleShape)\n"
            "            .clickable(\n"
            "                interactionSource = remember { MutableInteractionSource() },\n",
            "    val backSource = remember { MutableInteractionSource() } // V7 controls pop\n"
            "    Box(\n"
            "        modifier = modifier\n"
            "            .wisdomPress(backSource)\n"
            "            .size(48.dp)\n"
            "            .clip(CircleShape)\n"
            "            .background(WisdomGlassFillStrong)\n"
            "            .border(BorderStroke(1.dp, WisdomGlassBorder), CircleShape)\n"
            "            .clickable(\n"
            "                interactionSource = backSource,\n",
        ),
        (  # import (this file did not import wisdomPress before)
            "import com.wisdomtower.academy.ui.theme.wisdomReveal\n",
            "import com.wisdomtower.academy.ui.theme.wisdomReveal\n" + PRESS_IMPORT,
        ),
    ]),
    (SETTINGS, [
        (  # Switch of ToggleRow
            "        Switch(\n"
            "            checked = checked,\n"
            "            onCheckedChange = null,\n"
            "            colors = SwitchDefaults.colors(\n",
            "        Switch(\n"
            "            checked = checked,\n"
            "            onCheckedChange = null,\n"
            "            modifier = Modifier.wisdomSelectionPop(checked, from = 0.88f), // V7 controls pop\n"
            "            colors = SwitchDefaults.colors(\n",
        ),
        (
            PRESS_IMPORT,
            PRESS_IMPORT + "import com.wisdomtower.academy.ui.theme.wisdomSelectionPop\n",
        ),
    ]),
    (SIGNIN, [
        (  # terms Checkbox
            "                                    modifier = Modifier.semantics {\n"
            "                                        contentDescription = \"Accept Terms of Service and Privacy Policy\"\n",
            "                                    modifier = Modifier.wisdomSelectionPop(agreedToTerms).semantics { // V7 controls pop\n"
            "                                        contentDescription = \"Accept Terms of Service and Privacy Policy\"\n",
        ),
        (
            "import com.wisdomtower.academy.ui.theme.WisdomCyan\n",
            "import com.wisdomtower.academy.ui.theme.WisdomCyan\n"
            "import com.wisdomtower.academy.ui.theme.wisdomSelectionPop\n",
        ),
    ]),
]

for f in (HUB, SETTINGS, SIGNIN):
    if not f.exists():
        print(f"ERROR: {f} is missing; nothing changed")
        sys.exit(1)

if HELPER.exists() and MARK in "".join(p.read_text(encoding="utf-8") for p in (HUB, SETTINGS, SIGNIN)):
    print("already patched, nothing to do")
    sys.exit(0)

# plan everything in memory first, write only if every anchor is found exactly once
new_texts = {}
for path, edits in JOBS:
    text = path.read_text(encoding="utf-8")
    if MARK in text:
        print(f"ERROR: {path} already has the marker but the patch is incomplete; nothing changed")
        sys.exit(1)
    for i, (old, new) in enumerate(edits):
        n = text.count(old)
        if n != 1:
            print(f"ERROR: {path.name} edit #{i + 1} anchor found {n} times (need exactly 1); nothing changed")
            print(old[:240])
            sys.exit(1)
        text = text.replace(old, new)
    new_texts[path] = text

HELPER.write_text(HELPER_SRC, encoding="utf-8")
for path, text in new_texts.items():
    path.write_text(text, encoding="utf-8")
print(f"patched ({sum(len(e) for _, e in JOBS)} edits in {len(JOBS)} files, plus {HELPER.name})")
