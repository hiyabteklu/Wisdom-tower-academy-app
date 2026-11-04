#!/usr/bin/env python3
"""BG-POPUP-3 (owner request 2026-11-04): pop-ups were see-through, so text behind them mixed with the pop-up text.
This patch makes four more pop-ups near-solid at night (96 percent navy 0F1829, the same navy as the Material pop-ups in
Theme.kt) and a little more solid by day (97 percent ivory):
  - NativeTimerScreen.kt  : TmDialogFill   (the "Already quitting?" pop-up, was white 4 percent)
  - NativePlannerScreen.kt: PlDialogFill   (was 30 percent)
  - NativeNotebookScreen.kt: NbDialogFill  (was 30 percent)
  - MainActivity.kt       : the Dialog(onDismissRequest = onDismiss) Surface (was CardSurface, 40 percent)
Each anchor must match exactly once, otherwise NOTHING is changed. Safe to run twice. Standard library only.
To go back: put the old values back (see the OLD strings below).
"""
import sys
from pathlib import Path

BASE = Path("app/src/main/java/com/wisdomtower/academy")
EDITS = [
    (
        BASE / "ui/study/NativeTimerScreen.kt",
        "private val TmDialogFill: Color get() = dayNight(Color.White.copy(alpha = 0.04f), Color(0xF2F8F5EE))",
        "private val TmDialogFill: Color get() = dayNight(Color(0xF50F1829), Color(0xF7F8F5EE)) // BG-POPUP-3: near-solid",
    ),
    (
        BASE / "ui/study/NativePlannerScreen.kt",
        "private val PlDialogFill: Color get() = dayNight(Color(0x4D0B1528), Color(0xF2F8F5EE))",
        "private val PlDialogFill: Color get() = dayNight(Color(0xF50F1829), Color(0xF7F8F5EE)) // BG-POPUP-3: near-solid",
    ),
    (
        BASE / "ui/study/NativeNotebookScreen.kt",
        "private val NbDialogFill: Color get() = dayNight(Color(0x4D0B1528), Color(0xF2F8F5EE))",
        "private val NbDialogFill: Color get() = dayNight(Color(0xF50F1829), Color(0xF7F8F5EE)) // BG-POPUP-3: near-solid",
    ),
    (
        BASE / "MainActivity.kt",
        "    Dialog(onDismissRequest = onDismiss) {\n        Surface(\n            color = dayNight(CardSurface, ShellOverlayIvory),",
        "    Dialog(onDismissRequest = onDismiss) {\n        Surface(\n            color = dayNight(Color(0xF50F1829), ShellOverlayIvory), // BG-POPUP-3: near-solid",
    ),
]

plan = []
for path, old, new in EDITS:
    if not path.exists():
        print(f"ERROR: {path} not found; nothing changed"); sys.exit(1)
    src = path.read_text(encoding="utf-8")
    if new in src and old not in src:
        print(f"already patched: {path.name}")
        continue
    n = src.count(old)
    if n != 1:
        print(f"ERROR: anchor in {path.name} found {n} times (need exactly 1); nothing changed"); sys.exit(1)
    plan.append((path, src.replace(old, new)))

for path, text in plan:
    path.write_text(text, encoding="utf-8")
    print(f"patched: {path.name}")
print("done")
