#!/usr/bin/env python3
"""V5d (docs/IOS_MOTION_V2_PLAN.md): the hub list grows out of the tapped hub tile and shrinks back into it on Back.

File: ui/study/StudyWorkspaceScreen.kt (the course page with the six hub tiles: Books, Short Notes, Flashcards ...).
Before: tapping a tile REMOVED the tile grid and drew the hub list at once (nothing to grow from, nothing to shrink to).
After:
  - the tile grid stays composed (scroll position kept, tile visible behind the growing list);
  - the hub list, and the book / note / cards / quiz opened from it, are drawn inside `WisdomGrowLayer` (the same layer the
    package screen uses since V5b): grows out of the tapped tile, shrinks back into it when Back returns to the grid;
  - the grid is hidden from TalkBack while the layer is open, and the layer swallows touches so nothing underneath is tapped;
  - opened straight into a hub from a website link (entrance questions): no grid, no grow, exactly as before.
Not changed: `view` (the state that decides what is shown), the BackHandler, every screen that is shown, locks, counts, dialogs.
Every anchor must match exactly once or NOTHING is written. Safe to run twice.
"""
import sys
from pathlib import Path

FILE = Path("app/src/main/java/com/wisdomtower/academy/ui/study/StudyWorkspaceScreen.kt")
MARK = "V5d hub grow"

EDITS = [
    # 1. imports
    ("import androidx.compose.ui.Modifier\n",
     "import androidx.compose.ui.Modifier\n"
     "import androidx.compose.ui.input.pointer.pointerInput\n"
     "import androidx.compose.ui.semantics.clearAndSetSemantics\n"),
    ("import com.wisdomtower.academy.ui.theme.AtmosphereBackground\n",
     "import com.wisdomtower.academy.ui.theme.AtmosphereBackground\n"
     "import com.wisdomtower.academy.ui.theme.WisdomGrowLayer\n"),
    # 2. the sub-screens become a lambda (same code, no early return)
    ("    val shown = view\n"
     "    if (shown != null && contentScope != null) {\n"
     "        PauseShellWhileShown()\n"
     "        when (shown) {\n",
     "    val shown = view\n"
     "    if (shown != null && contentScope != null) PauseShellWhileShown() // V5d hub grow\n"
     "\n"
     "    // V5d hub grow: the sub-screens (hub list, book, note, cards, quiz) are drawn by this lambda, either inside the grow layer\n"
     "    // or directly (website entrance link). The two parameters and `modifier` deliberately keep the old names, so the code\n"
     "    // of every branch below is the same as before; the layer or the Box around it already applies the caller's modifier.\n"
     "    val subView: @Composable (WorkspaceView, String) -> Unit = { shown, contentScope ->\n"
     "        val modifier = Modifier\n"
     "        when (shown) {\n"),
    # 3. end of the sub-screens, start of the grid
    ("        }\n"
     "        return\n"
     "    }\n"
     "\n"
     "    Box(modifier = modifier.fillMaxSize()) {\n"
     "        AtmosphereBackground(modifier = Modifier.fillMaxSize())\n"
     "        Column(Modifier.fillMaxSize()) {\n",
     "        }\n"
     "    }\n"
     "\n"
     "    val layerItem: WorkspaceView? = if (contentScope != null) shown else null\n"
     "    if (entranceUrl != null && layerItem != null && contentScope != null) {\n"
     "        // V5d hub grow: opened straight into a hub from a website link. No grid behind it, no grow (as before).\n"
     "        Box(modifier = modifier.fillMaxSize()) { subView(layerItem, contentScope) }\n"
     "        return\n"
     "    }\n"
     "\n"
     "    Box(modifier = modifier.fillMaxSize()) {\n"
     "        AtmosphereBackground(modifier = Modifier.fillMaxSize())\n"
     "        Column(Modifier.fillMaxSize().then(if (layerItem != null) Modifier.clearAndSetSemantics { } else Modifier)) { // V5d hub grow\n"),
    # 4. the grow layer on top of the grid (before the gate dialogs)
    ("        val message = gateMessage\n"
     "        if (message != null) {\n",
     "        // V5d hub grow: grows out of the tapped hub tile, shrinks back into it. Touches stop here (nothing under it is tapped).\n"
     "        if (contentScope != null) {\n"
     "            WisdomGrowLayer(item = layerItem, zIndexValue = 1f) { layerView ->\n"
     "                Box(\n"
     "                    Modifier\n"
     "                        .fillMaxSize()\n"
     "                        .pointerInput(Unit) {\n"
     "                            awaitPointerEventScope {\n"
     "                                while (true) {\n"
     "                                    awaitPointerEvent()\n"
     "                                }\n"
     "                            }\n"
     "                        }\n"
     "                ) { subView(layerView, contentScope) }\n"
     "            }\n"
     "        }\n"
     "\n"
     "        val message = gateMessage\n"
     "        if (message != null) {\n"),
]

if not FILE.exists():
    print(f"ERROR: {FILE} is missing; nothing changed")
    sys.exit(1)
text = FILE.read_text(encoding="utf-8")
if MARK in text:
    print("already patched, nothing to do")
    sys.exit(0)
if "WisdomGrowLayer" not in Path("app/src/main/java/com/wisdomtower/academy/ui/theme/WisdomGrowFromCard.kt").read_text(encoding="utf-8"):
    print("ERROR: WisdomGrowLayer is missing (is V5b merged?); nothing changed")
    sys.exit(1)

for i, (old, new) in enumerate(EDITS):
    n = text.count(old)
    if n != 1:
        print(f"ERROR: edit #{i + 1} anchor found {n} times (need exactly 1); nothing changed")
        print(old[:300])
        sys.exit(1)
    text = text.replace(old, new)

if text.count("{") != text.count("}"):
    print("ERROR: braces would not balance after the patch; nothing changed")
    sys.exit(1)

FILE.write_text(text, encoding="utf-8")
print(f"patched ({len(EDITS)} edits in {FILE.name})")
