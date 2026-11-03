#!/usr/bin/env python3
"""V6 (docs/IOS_MOTION_V2_PLAN.md) last coverage: soft return entrance for the two games pickers, row motion for hub lists.

  - games/LevelPickerScreen.kt and games/defense/ExamPickerScreen.kt: `wisdomScreenEntrance()` on the screen root (180 ms fade + 10 dp rise,
    replays every time the picker is opened, like Home and Settings).
  - study/HubItemListScreen.kt: the two row wrappers get `animateItem()` in front of `wisdomReveal`, so a row that appears, disappears or moves
    after the background refresh glides instead of jumping (same call the notifications list and the tutor chats list already use).
Nothing else changes. Every anchor must match exactly once or NOTHING is written. Safe to run twice.
"""
import sys
from pathlib import Path

BASE = Path("app/src/main/java/com/wisdomtower/academy/ui")
MARK = "V6 lists entrance"
ENTRANCE_IMPORT = "import com.wisdomtower.academy.ui.theme.wisdomScreenEntrance\n"

JOBS = [
    (BASE / "games/LevelPickerScreen.kt", [
        ("    Box(modifier = modifier.fillMaxSize()) {\n        AtmosphereBackground(modifier = Modifier.fillMaxSize())\n",
         "    Box(modifier = modifier.wisdomScreenEntrance().fillMaxSize()) { // V6 lists entrance\n        AtmosphereBackground(modifier = Modifier.fillMaxSize())\n"),
        ("import com.wisdomtower.academy.ui.theme.WisdomMuted\n",
         "import com.wisdomtower.academy.ui.theme.WisdomMuted\n" + ENTRANCE_IMPORT),
    ]),
    (BASE / "games/defense/ExamPickerScreen.kt", [
        ("    Column(modifier.fillMaxSize().background(Color(0xFF070C16))) {\n",
         "    Column(modifier.wisdomScreenEntrance().fillMaxSize().background(Color(0xFF070C16))) { // V6 lists entrance\n"),
        ("import com.wisdomtower.academy.ui.theme.WisdomTextPrimary\n",
         "import com.wisdomtower.academy.ui.theme.WisdomTextPrimary\n" + ENTRANCE_IMPORT),
    ]),
    (BASE / "study/HubItemListScreen.kt", [
        ('                                Box(modifier = Modifier.wisdomReveal("hub_${hubId}_soon_${entry.chapter}", rowIndex)) {\n',
         '                                Box(modifier = Modifier.animateItem().wisdomReveal("hub_${hubId}_soon_${entry.chapter}", rowIndex)) { // V6 lists entrance\n'),
        ('                                Box(modifier = Modifier.wisdomReveal("hub_${hubId}_${item.id}", rowIndex)) {\n',
         '                                Box(modifier = Modifier.animateItem().wisdomReveal("hub_${hubId}_${item.id}", rowIndex)) { // V6 lists entrance\n'),
    ]),
]

for path, _ in JOBS:
    if not path.exists():
        print(f"ERROR: {path} is missing; nothing changed")
        sys.exit(1)
texts = {p: p.read_text(encoding="utf-8") for p, _ in JOBS}
if all(MARK in t for t in texts.values()):
    print("already patched, nothing to do")
    sys.exit(0)

new_texts = {}
for path, edits in JOBS:
    text = texts[path]
    if MARK in text:
        print(f"ERROR: {path.name} already has the marker but the patch is incomplete; nothing changed")
        sys.exit(1)
    for i, (old, new) in enumerate(edits):
        n = text.count(old)
        if n != 1:
            print(f"ERROR: {path.name} edit #{i + 1} anchor found {n} times (need exactly 1); nothing changed")
            print(old[:240])
            sys.exit(1)
        text = text.replace(old, new)
    new_texts[path] = text

for path, text in new_texts.items():
    path.write_text(text, encoding="utf-8")
print(f"patched ({sum(len(e) for _, e in JOBS)} edits in {len(JOBS)} files)")
