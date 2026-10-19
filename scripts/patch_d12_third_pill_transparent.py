#!/usr/bin/env python3
"""Third pill "Transparent" (owner order 2026-10-19, answer to the open question in docs/DAY_MODE_PLAN.md section 8:
Day sits NEXT to the picture background, it does not replace it).

The Settings pill (ui/settings/DisplaySection.kt) draws one slot per entry of AppBackground.options, so the only change is
the list in ui/theme/AppBackground.kt: Night, Day, Transparent (the picture look, enum Photo2, file
assets/backgrounds/transparent.jpg, see-through glass over the picture, night colours). A phone that saved Transparent
before keeps it, because the saved name is now in the list again. When DAY_MODE_OFFERED is false the list stays Night,
Transparent as before. Safe to run twice; if the anchor is missing nothing is written.
"""
import sys
from pathlib import Path

FILE = Path("app/src/main/java/com/wisdomtower/academy/ui/theme/AppBackground.kt")
OLD = "            listOf(AppBackgroundOption.Black, AppBackgroundOption.Day)\n"
NEW = "            listOf(AppBackgroundOption.Black, AppBackgroundOption.Day, AppBackgroundOption.Photo2)\n"

src = FILE.read_text(encoding="utf-8")
if NEW in src:
    print("already patched, nothing to do")
    sys.exit(0)
n = src.count(OLD)
if n != 1:
    print(f"ERROR: anchor found {n} times (need exactly 1); file not changed")
    sys.exit(1)
FILE.write_text(src.replace(OLD, NEW), encoding="utf-8")
print("patched AppBackground.kt: options are Night, Day, Transparent")
