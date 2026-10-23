#!/usr/bin/env python3
"""Copies the three bank logos (Telebirr, CBE, Abyssinia) from the read-only website snapshot into the app assets.

S2 of docs/PLAN_TELEGRAM_PAID_NATIVE_V2.md: the purchase screen loads app/src/main/assets/images/banks/<name>.png and shows
initials until these files exist. Binary files cannot be pushed by the chat tool, so this script copies them in the
"Apply pending patches" workflow. Safe to run twice (it overwrites with the same bytes). Marker: the files exist.
"""
import os
import shutil
import sys

SRC = "website-reference/public/images/banks"
DST = "app/src/main/assets/images/banks"
NAMES = ["telebirr.png", "cbe.png", "abyssinia.png"]

os.makedirs(DST, exist_ok=True)
missing = [n for n in NAMES if not os.path.isfile(os.path.join(SRC, n))]
if missing:
    print("Missing in snapshot:", missing)
    sys.exit(1)
for n in NAMES:
    shutil.copyfile(os.path.join(SRC, n), os.path.join(DST, n))
    print("copied", n, os.path.getsize(os.path.join(DST, n)), "bytes")
