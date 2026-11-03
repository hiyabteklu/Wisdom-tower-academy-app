#!/usr/bin/env python3
"""V7 (docs/IOS_MOTION_V2_PLAN.md): press feel on the four top-bar icon buttons (menu, back to Learning Suite, trophy, bell).

Needs `ui/theme/WisdomPressScale.kt` (already on main). Each IconButton gets `.wisdomPressScale()` on its modifier: a small
scale while the finger is down, springing back on release. Click actions and haptics are not touched. The bottom tabs already
had their own press scale and haptic (AliveBottomNav), the timer chip keeps its own clickable.
"""
import subprocess
import sys
from pathlib import Path

FILE = Path("app/src/main/java/com/wisdomtower/academy/MainActivity.kt")
HELPER = Path("app/src/main/java/com/wisdomtower/academy/ui/theme/WisdomPressScale.kt")
MARK = "V7 topbar press"
P = "com.wisdomtower.academy.ui.theme.wisdomPressScale"

A36 = " " * 36
A32 = " " * 32
EDITS = [
    (  # back to Learning Suite (46 dp)
        A36 + "activeStudyUrl = null\n" + A32 + "},\n" + A32 + "modifier = Modifier.size(46.dp)\n",
        A36 + "activeStudyUrl = null\n" + A32 + "},\n" + A32 + "modifier = Modifier.size(46.dp).wisdomPressScale() // V7 topbar press\n",
    ),
    (  # menu (46 dp)
        A36 + "menuExpanded = true\n" + A32 + "},\n" + A32 + "modifier = Modifier.size(46.dp)\n",
        A36 + "menuExpanded = true\n" + A32 + "},\n" + A32 + "modifier = Modifier.size(46.dp).wisdomPressScale() // V7 topbar press\n",
    ),
    (  # trophy (44 dp)
        A36 + "showCompetitions = true\n" + A32 + "},\n" + A32 + "modifier = Modifier.size(44.dp)\n",
        A36 + "showCompetitions = true\n" + A32 + "},\n" + A32 + "modifier = Modifier.size(44.dp).wisdomPressScale() // V7 topbar press\n",
    ),
    (  # bell (44 dp)
        A36 + "showNotifications = true\n" + A32 + "},\n" + A32 + "modifier = Modifier.size(44.dp)\n",
        A36 + "showNotifications = true\n" + A32 + "},\n" + A32 + "modifier = Modifier.size(44.dp).wisdomPressScale() // V7 topbar press\n",
    ),
]
IMPORT_OLD = "import com.wisdomtower.academy.ui.theme.wisdomStaggerIn\n"
IMPORT_NEW = IMPORT_OLD + "import com.wisdomtower.academy.ui.theme.wisdomPressScale\n"

if not HELPER.exists():
    print("ERROR: ui/theme/WisdomPressScale.kt is missing; nothing changed")
    sys.exit(1)
src = FILE.read_text(encoding="utf-8")
if MARK in src:
    print("already patched, nothing to do")
    sys.exit(0)

out = src
for i, (old, new) in enumerate(EDITS + [(IMPORT_OLD, IMPORT_NEW)]):
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
print(f"patched ({len(EDITS) + 1} edits)")
