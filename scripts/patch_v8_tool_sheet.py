#!/usr/bin/env python3
"""V8 (docs/IOS_MOTION_V2_PLAN.md): the global native study tools get the sheet spring arrival and drag-down to close.

Needs the new file `ui/theme/WisdomToolSheet.kt` (already on main). Change in MainActivity.kt (2 edits, one block):
the `when (currentTool) { ... }` that draws AI Tutor, Calculator, Timer, Planner, Notebook, Targets and Status is wrapped in
`WisdomToolSheet(sheetKey = currentTool, onDismiss = { activeStudyTool = null }) { ... }`.
The BackHandler line, the screens and their parameters are not touched; the screens keep their own `.zIndex(120f)` inside.
"""
import subprocess
import sys
from pathlib import Path

FILE = Path("app/src/main/java/com/wisdomtower/academy/MainActivity.kt")
SHEET = Path("app/src/main/java/com/wisdomtower/academy/ui/theme/WisdomToolSheet.kt")
MARK = "V8 tool sheet"

HEAD_OLD = (
    "                if (currentTool != null) {\n"
    "                    BackHandler(enabled = true) { activeStudyTool = null }\n"
    "                    when (currentTool) {\n"
)
HEAD_NEW = (
    "                if (currentTool != null) {\n"
    "                    BackHandler(enabled = true) { activeStudyTool = null }\n"
    "                    // V8 tool sheet: spring arrival and drag-down to close (ui/theme/WisdomToolSheet.kt)\n"
    "                    com.wisdomtower.academy.ui.theme.WisdomToolSheet(\n"
    "                        sheetKey = currentTool,\n"
    "                        onDismiss = { activeStudyTool = null }\n"
    "                    ) {\n"
    "                    when (currentTool) {\n"
)
TAIL_OLD = (
    "                                .zIndex(120f)\n"
    "                        )\n"
    "                    }\n"
    "                }\n"
    "\n"
    "                // Restored & Fixed Custom Animated Loader Overlay:\n"
)
TAIL_NEW = (
    "                                .zIndex(120f)\n"
    "                        )\n"
    "                    }\n"
    "                    }\n"
    "                }\n"
    "\n"
    "                // Restored & Fixed Custom Animated Loader Overlay:\n"
)

if not SHEET.exists():
    print("ERROR: ui/theme/WisdomToolSheet.kt is missing; nothing changed")
    sys.exit(1)
src = FILE.read_text(encoding="utf-8")
if MARK in src:
    print("already patched, nothing to do")
    sys.exit(0)

out = src
for i, (old, new) in enumerate([(HEAD_OLD, HEAD_NEW), (TAIL_OLD, TAIL_NEW)]):
    n = out.count(old)
    if n != 1:
        print(f"ERROR: edit #{i + 1} anchor found {n} times (need exactly 1); file not changed")
        print(old[:200])
        sys.exit(1)
    out = out.replace(old, new)

FILE.write_text(out, encoding="utf-8")

# keep the packed copy of MainActivity.kt in step (HANDOFF rule 10); a failure here does not undo the patch
pack = Path("scripts/pack_mainactivity.py")
if pack.exists():
    r = subprocess.run([sys.executable, str(pack)], capture_output=True, text=True)
    print("pack_mainactivity.py:", "ok" if r.returncode == 0 else "FAILED (patch itself is applied): " + r.stderr[-200:])
print("patched (2 edits)")
