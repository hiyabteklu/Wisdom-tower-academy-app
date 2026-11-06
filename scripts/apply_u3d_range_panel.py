#!/usr/bin/env python3
"""U3d (docs/PLAN_2026-11-06_URGENT_FIVE_FIXES.md): the My Status panel "Study time" with Today | Week | Month | Total.

Does three things, all-or-nothing (if any anchor is missing or not unique, nothing changes and no file is written):
1. copies scripts/u3d/StatusRangePanel.kt.txt to ui/study/StatusRangePanel.kt (the new panel);
2. NativeStatusScreen.kt: shows the panel in the loaded screen (before "Your week") and in the empty offline skeleton (before
   the "Today & this week" panel);
3. StudyDayLogReader.kt: the read limit goes from 60 to 500 rows (the panel reads about a year; with 60 and oldest-first it
   would miss the newest days).
Safe to run twice (marker: StatusRangePanel in NativeStatusScreen.kt)."""
import sys
from pathlib import Path

SRC = Path("scripts/u3d/StatusRangePanel.kt.txt")
STUDY = Path("app/src/main/java/com/wisdomtower/academy/ui/study")
DATA = Path("app/src/main/java/com/wisdomtower/academy/data/content")
MARK = "StatusRangePanel("

EDITS = {
    STUDY / "NativeStatusScreen.kt": [
        (
            "                    // STATUS CARD (S2): plain \"Your week\" sentences",
            "                    // U3d: Today | Week | Month | Total (study and focus from study_day_log, each day capped at 8 h)\n"
            "                    StatusRangePanel(dailyGoalMinutes = dailyGoalMinutes)\n\n"
            "                    // STATUS CARD (S2): plain \"Your week\" sentences",
        ),
        (
            "                    StatusDayPanel(dailyGoalMinutes = dailyGoalMinutes)\n"
            "                    // Competition points (own saved copy, own offline states)\n",
            "                    StatusRangePanel(dailyGoalMinutes = dailyGoalMinutes) // U3d\n"
            "                    StatusDayPanel(dailyGoalMinutes = dailyGoalMinutes)\n"
            "                    // Competition points (own saved copy, own offline states)\n",
        ),
    ],
    DATA / "StudyDayLogReader.kt": [
        (
            "            .addQueryParameter(\"limit\", \"60\")\n",
            "            .addQueryParameter(\"limit\", \"500\") // U3d: was 60; the status panel reads about a year\n",
        ),
    ],
}

if not SRC.exists():
    print(f"ERROR: {SRC} not found; nothing changed"); sys.exit(1)
dest = STUDY / "StatusRangePanel.kt"

screen = STUDY / "NativeStatusScreen.kt"
if screen.exists() and MARK in screen.read_text(encoding="utf-8"):
    print("already patched"); sys.exit(0)

new_text = {}
for path, edits in EDITS.items():
    if not path.exists():
        print(f"ERROR: {path} not found; nothing changed"); sys.exit(1)
    src = path.read_text(encoding="utf-8")
    for old, new in edits:
        n = src.count(old)
        if n != 1:
            print(f"ERROR: anchor in {path.name} found {n} times (need exactly 1); nothing changed:\n{old}")
            sys.exit(1)
        src = src.replace(old, new)
    new_text[path] = src

dest.write_text(SRC.read_text(encoding="utf-8"), encoding="utf-8")
print(f"created {dest.name}")
for path, src in new_text.items():
    path.write_text(src, encoding="utf-8")
    print(f"patched {path.name}")
print("done")
