#!/usr/bin/env python3
"""V10 (docs/IOS_MOTION_V2_PLAN.md), header-only version: the top card or title of Home, Learning and Packages leaves the screen softly.

New file ui/theme/WisdomHeaderParallax.kt: `Modifier.wisdomHeaderParallax(listState, key)`. While the list scrolls and the item with that
key moves up past its resting place, it lags behind the scroll (about 35 percent), shrinks a little (up to 6 percent) and fades (up to
55 percent). At rest nothing is applied. `graphicsLayer` only, read in the draw phase: no recomposition, no layout change, touch is
unaffected. Alpha uses ModulateAlpha (no extra offscreen buffer), so the effect costs almost nothing.
Used on: Packages "Academy packages" title, Home welcome card, Learning guest banner and Study Tools card.
The "cards at the bottom edge settle" part of V10 is NOT written on purpose (it would cost frames on long lists).
No text, colour or layout changes. Every anchor must match exactly once or NOTHING is written. Safe to run twice.
"""
import sys
from pathlib import Path

BASE = Path("app/src/main/java/com/wisdomtower/academy/ui")
MARK = "V10 header"
LIST_IMPORT_OLD = "import androidx.compose.foundation.lazy.LazyColumn\n"
LIST_IMPORT_NEW = LIST_IMPORT_OLD + "import androidx.compose.foundation.lazy.rememberLazyListState\n"
PAR_IMPORT = "import com.wisdomtower.academy.ui.theme.wisdomHeaderParallax\n"

NEW_FILE = BASE / "theme/WisdomHeaderParallax.kt"
NEW_FILE_TEXT = '''package com.wisdomtower.academy.ui.theme

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer

/**
 * V10 header (docs/IOS_MOTION_V2_PLAN.md): the lazy item with [key] (a title or hero card at the top of a list) lags behind the scroll,
 * shrinks a little and fades while it leaves the top of the screen. At rest, and while the item is fully in place, nothing is applied.
 * Read in the draw phase through `graphicsLayer`, so scrolling does not recompose anything. [strength] scales all three effects.
 */
fun Modifier.wisdomHeaderParallax(listState: LazyListState, key: Any, strength: Float = 1f): Modifier =
    graphicsLayer {
        val layout = listState.layoutInfo
        val info = layout.visibleItemsInfo.firstOrNull { it.key == key }
        if (info != null && info.size > 0) {
            val scrolled = (layout.beforeContentPadding - info.offset).coerceAtLeast(0)
            if (scrolled > 0) {
                val gone = (scrolled.toFloat() / info.size.toFloat()).coerceIn(0f, 1f)
                translationY = scrolled * 0.35f * strength
                val s = 1f - 0.06f * gone * strength
                scaleX = s
                scaleY = s
                compositingStrategy = CompositingStrategy.ModulateAlpha
                alpha = (1f - 0.55f * gone * strength).coerceIn(0f, 1f)
            }
        }
    }
'''

JOBS = [
    (BASE / "packages/PackagesScreen.kt", [
        (LIST_IMPORT_OLD, LIST_IMPORT_NEW),
        ("import com.wisdomtower.academy.ui.theme.WisdomInk\n",
         "import com.wisdomtower.academy.ui.theme.WisdomInk\n" + PAR_IMPORT),
        ('    val specials = remember(packageList) { packageList.filter { it.group == "special" } }\n',
         '    val specials = remember(packageList) { packageList.filter { it.group == "special" } }\n'
         '    val listState = rememberLazyListState() // V10 header\n'),
        ("        LazyColumn(\n            modifier = Modifier.fillMaxSize(),\n            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),\n",
         "        LazyColumn(\n            state = listState,\n            modifier = Modifier.fillMaxSize(),\n            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),\n"),
        ("                Column(\n                    modifier = Modifier\n                        .fillMaxWidth()\n                        .padding(vertical = 12.dp),\n",
         "                Column(\n                    modifier = Modifier\n                        .wisdomHeaderParallax(listState, \"packages_title_header\")\n                        .fillMaxWidth()\n                        .padding(vertical = 12.dp),\n"),
    ]),
    (BASE / "home/HomeScreen.kt", [
        (LIST_IMPORT_OLD, LIST_IMPORT_NEW),
        ("import com.wisdomtower.academy.ui.theme.wisdomReveal\n",
         "import com.wisdomtower.academy.ui.theme.wisdomReveal\n" + PAR_IMPORT),
        ("    val programs = HOME_PROGRAMS\n",
         "    val programs = HOME_PROGRAMS\n    val listState = rememberLazyListState() // V10 header\n"),
        ("        LazyColumn(\n            modifier = Modifier.fillMaxSize().wisdomScreenEntrance(),\n",
         "        LazyColumn(\n            state = listState,\n            modifier = Modifier.fillMaxSize().wisdomScreenEntrance(),\n"),
        ('                Box(modifier = Modifier.wisdomReveal("home_welcome_card", 0)) {\n',
         '                Box(modifier = Modifier.wisdomHeaderParallax(listState, "welcome_card").wisdomReveal("home_welcome_card", 0)) {\n'),
    ]),
    (BASE / "learning/LearningScreen.kt", [
        (LIST_IMPORT_OLD, LIST_IMPORT_NEW),
        ("import com.wisdomtower.academy.ui.theme.wisdomReveal\n",
         "import com.wisdomtower.academy.ui.theme.wisdomReveal\n" + PAR_IMPORT),
        ("    // Courses for the Curriculum section: the website's default list for the student's academic level\n",
         "    val listState = rememberLazyListState() // V10 header\n"
         "    // Courses for the Curriculum section: the website's default list for the student's academic level\n"),
        ("        LazyColumn(\n            modifier = Modifier.fillMaxSize(),\n            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),\n",
         "        LazyColumn(\n            state = listState,\n            modifier = Modifier.fillMaxSize(),\n            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),\n"),
        ('                            .wisdomReveal("learning_guest_banner", 1),\n',
         '                            .wisdomHeaderParallax(listState, "guest_banner")\n                            .wisdomReveal("learning_guest_banner", 1),\n'),
        ('                        .wisdomReveal("learning_tools_dock", 2),\n',
         '                        .wisdomHeaderParallax(listState, "study_tools_dock")\n                        .wisdomReveal("learning_tools_dock", 2),\n'),
    ]),
]

for path, _ in JOBS:
    if not path.exists():
        print(f"ERROR: {path} is missing; nothing changed")
        sys.exit(1)
texts = {p: p.read_text(encoding="utf-8") for p, _ in JOBS}
have_file = NEW_FILE.exists()
marked = [MARK in t for t in texts.values()]
if have_file and all(marked):
    print("already patched, nothing to do")
    sys.exit(0)
if have_file or any(marked):
    print("ERROR: the patch is half applied (new file or marker found in some files only); nothing changed")
    sys.exit(1)

new_texts = {}
for path, edits in JOBS:
    text = texts[path]
    for i, (old, new) in enumerate(edits):
        n = text.count(old)
        if n != 1:
            print(f"ERROR: {path.name} edit #{i + 1} anchor found {n} times (need exactly 1); nothing changed")
            print(old[:240])
            sys.exit(1)
        text = text.replace(old, new)
    if text.count("{") != text.count("}") or text.count("(") != texts[path].count("(") + text.count(")") - texts[path].count(")"):
        print(f"ERROR: brackets would not balance in {path.name}; nothing changed")
        sys.exit(1)
    new_texts[path] = text

NEW_FILE.write_text(NEW_FILE_TEXT, encoding="utf-8")
for path, text in new_texts.items():
    path.write_text(text, encoding="utf-8")
print(f"patched ({sum(len(e) for _, e in JOBS)} edits in {len(JOBS)} files, plus {NEW_FILE.name})")
