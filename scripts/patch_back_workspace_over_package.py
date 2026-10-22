#!/usr/bin/env python3
"""BACK FIX (hub item list -> course page -> subject list -> package), safe to run twice.
Cause: PackageDetailScreen keeps its own BackHandler enabled while an ECE semester or an Entrance stream is open, and it stays
composed UNDER the study workspace. When the workspace has nothing left to close (course page), that older handler answered
the back key first: it reset the semester/stream behind the workspace (nothing visible happened). The next back closed the
workspace and landed on the package's first level (two steps jumped, subject list never seen).
Fix: PackageDetailScreen gets `backEnabled` (default true); MainActivity passes `backEnabled = activeWorkspace == null`.
Marker BACKFIX-S7. Anchors must match exactly once; nothing is written if any is missing."""
import sys
from pathlib import Path

PD = Path("app/src/main/java/com/wisdomtower/academy/ui/packages/PackageDetailScreen.kt")
MA = Path("app/src/main/java/com/wisdomtower/academy/MainActivity.kt")
MARK = "BACKFIX-S7"

PD_A1 = "    isLoggedIn: Boolean = false,\n"
PD_N1 = PD_A1 + "    backEnabled: Boolean = true, // BACKFIX-S7\n"
PD_A2 = "    BackHandler(enabled = stream != null || eceSemester != null) { goBack() }\n"
PD_N2 = "    BackHandler(enabled = backEnabled && (stream != null || eceSemester != null)) { goBack() } // BACKFIX-S7\n"

MA_A = ("                            isLoggedIn = isLoggedIn,\n"
        "                            modifier = Modifier\n"
        "                                .fillMaxSize()\n"
        "                                .appBackdrop()\n"
        "                                .zIndex(10f)\n"
        "                        )\n"
        "                    } else {\n"
        "                        com.wisdomtower.academy.ui.games.GameTabScreen(\n")
MA_N = MA_A.replace("                            isLoggedIn = isLoggedIn,\n",
        "                            isLoggedIn = isLoggedIn,\n"
        "                            backEnabled = activeWorkspace == null, // BACKFIX-S7\n", 1)

pd, ma = PD.read_text(encoding="utf-8"), MA.read_text(encoding="utf-8")
if MARK in pd and MARK in ma:
    print("already patched, nothing to do"); sys.exit(0)
if (MARK in pd) != (MARK in ma):
    print("ERROR: only one file is patched; fix by hand, nothing changed"); sys.exit(1)
for name, text, a in (("pkg param", pd, PD_A1), ("pkg handler", pd, PD_A2), ("main call", ma, MA_A)):
    n = text.count(a)
    if n != 1:
        print(f"ERROR: {name} anchor found {n} times (need exactly 1); nothing changed"); sys.exit(1)
PD.write_text(pd.replace(PD_A1, PD_N1).replace(PD_A2, PD_N2), encoding="utf-8")
MA.write_text(ma.replace(MA_A, MA_N), encoding="utf-8")
print("back fix applied to PackageDetailScreen.kt and MainActivity.kt")
