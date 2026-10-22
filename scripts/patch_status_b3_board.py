#!/usr/bin/env python3
"""My Status V2, slices B1 + B3 in ONE run (docs/MY_STATUS_V2_SUBJECT_BOARD_PLAN.md), safe to run twice:
1) runs scripts/patch_status_b1_remove_icons.py (removes the gauge icons from the hub headers and the subject cards);
2) StudyWorkspaceScreen.kt: puts the "My status" board (ui/study/StatusSubjectBoard.kt) as the FIRST item above the hub grid
   of the course page, only for a signed-in student, when the course has a content scope and is not Exit Exam.
One anchor (the line `val hubs = NATIVE_LEARNING_HUBS`, marker MYSTATUS-B3)."""
import subprocess, sys
from pathlib import Path

b1 = Path("scripts/patch_status_b1_remove_icons.py")
if b1.exists():
    if subprocess.run([sys.executable, str(b1)]).returncode != 0:
        print("ERROR: B1 (remove icons) failed; stopping"); sys.exit(1)

FILE = Path("app/src/main/java/com/wisdomtower/academy/ui/study/StudyWorkspaceScreen.kt")
MARK = "MYSTATUS-B3"
ANCHOR = "                val hubs = NATIVE_LEARNING_HUBS\n"
NEW = '''                if (loggedIn && contentScope != null && !isExitExam) {
                    item(key = "status_board") {
                        StatusSubjectBoard(scopePath = contentScope, title = title) // MYSTATUS-B3
                    }
                }
'''
src = FILE.read_text(encoding="utf-8")
if MARK in src:
    print("board already wired, nothing to do"); sys.exit(0)
n = src.count(ANCHOR)
if n != 1:
    print(f"ERROR: anchor found {n} times (need exactly 1); file not changed"); sys.exit(1)
FILE.write_text(src.replace(ANCHOR, NEW + ANCHOR), encoding="utf-8")
print("board wired above the hub grid")
