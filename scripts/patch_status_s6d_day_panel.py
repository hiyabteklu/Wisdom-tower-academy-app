#!/usr/bin/env python3
"""My Status V2, S6d (docs/MY_STATUS_V2_S6_DAY_LOG_PLAN.md), safe to run twice:
NativeStatusScreen.kt: inserts ONE call `StatusDayPanel(dailyGoalMinutes = dailyGoalMinutes)` right after the
`StatusSubjectsPanel(...)` call (marker MYSTATUS-S6). The panel itself is ui/study/StatusDayPanel.kt (already on main)."""
import sys
from pathlib import Path

FILE = Path("app/src/main/java/com/wisdomtower/academy/ui/study/NativeStatusScreen.kt")
MARK = "MYSTATUS-S6"
ANCHOR = "                    StatusSubjectsPanel(rows = rows.orEmpty(), educationLevel = educationLevel)\n"
NEW = "\n                    // My Status V2 (S6d): today ring + 7-day bars from study_day_log\n                    StatusDayPanel(dailyGoalMinutes = dailyGoalMinutes) // MYSTATUS-S6\n"

src = FILE.read_text(encoding="utf-8")
if MARK in src:
    print("day panel already wired, nothing to do"); sys.exit(0)
n = src.count(ANCHOR)
if n != 1:
    print(f"ERROR: anchor found {n} times (need exactly 1); file not changed"); sys.exit(1)
FILE.write_text(src.replace(ANCHOR, ANCHOR + NEW), encoding="utf-8")
print("day panel wired after StatusSubjectsPanel")
