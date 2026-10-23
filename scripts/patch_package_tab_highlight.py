#!/usr/bin/env python3
"""Opening a package (from Home or My Learning) used to switch the bottom bar to the Game tab and Back landed on the
games list. The package page lives in tab layer 2 (the Game tab layer), so selectedIndex was set to 2.
Fix: remember the tab the student came from (packageOriginTab). The bottom bar highlights THAT tab while a package
page is open, and leaving the package (page Back, phone Back) returns to that tab. A package opened from the Game tab
itself behaves as before. Tapping the highlighted tab while a package is open leaves the package."""
import sys
from pathlib import Path

FILE = Path("app/src/main/java/com/wisdomtower/academy/MainActivity.kt")
MARK = "packageOriginTab"

src = FILE.read_text(encoding="utf-8")
if MARK in src:
    print("already patched, nothing to do"); sys.exit(0)

edits = [
    # 1) state + helper
    ("    var selectedPackage by remember { mutableStateOf<NativePackage?>(null) }\n",
     "    var selectedPackage by remember { mutableStateOf<NativePackage?>(null) }\n"
     "    // TAB-HIGHLIGHT-2026-10-23: the tab a package page was opened from (0 Home, 1 Learning, 2 Game)\n"
     "    var packageOriginTab by remember { mutableIntStateOf(2) }\n"
     "    fun markPackageOrigin() {\n"
     "        if (selectedPackage == null || selectedIndex != 2) packageOriginTab = selectedIndex\n"
     "    }\n"
     "    fun leavePackage() {\n"
     "        selectedPackage = null\n"
     "        selectedIndex = packageOriginTab\n"
     "        packageOriginTab = 2\n"
     "    }\n"),
    # 2) phone Back
    ("        if (selectedPackage != null) {\n            selectedPackage = null\n            return@BackHandler\n        }\n",
     "        if (selectedPackage != null) {\n            leavePackage()\n            return@BackHandler\n        }\n"),
    # 3) package link from Home / anywhere (handleGlobalNav)
    ("            if (matchedPkg != null) {\n                selectedPackage = matchedPkg\n                selectedIndex = 2\n",
     "            if (matchedPkg != null) {\n                markPackageOrigin()\n                selectedPackage = matchedPkg\n                selectedIndex = 2\n"),
    # 4) My Learning card
    ("                        onSelectCourse = { pkg ->\n                            selectedPackage = pkg\n                            selectedIndex = 2\n",
     "                        onSelectCourse = { pkg ->\n                            markPackageOrigin()\n                            selectedPackage = pkg\n                            selectedIndex = 2\n"),
    # 5) page Back button
    ("                            onBack = { selectedPackage = null },\n",
     "                            onBack = { leavePackage() },\n"),
    # 6) bottom bar highlight + tap on the highlighted tab
    ("                        selectedIndex = selectedIndex,\n                        onItemSelected = { index, _ ->\n                            resetOverlays()\n",
     "                        selectedIndex = if (selectedIndex == 2 && selectedPackage != null) packageOriginTab else selectedIndex,\n"
     "                        reselectable = selectedIndex == 2 && selectedPackage != null && packageOriginTab != 2,\n"
     "                        onItemSelected = { index, _ ->\n                            packageOriginTab = 2\n                            resetOverlays()\n"),
    ("    onItemSelected: (Int, BottomNavItem) -> Unit,\n    modifier: Modifier = Modifier,\n) {\n    val view = LocalView.current\n",
     "    onItemSelected: (Int, BottomNavItem) -> Unit,\n    modifier: Modifier = Modifier,\n    reselectable: Boolean = false,\n) {\n    val view = LocalView.current\n"),
    ("                                    if (index != selectedIndex) onItemSelected(index, item)\n",
     "                                    if (index != selectedIndex || reselectable) onItemSelected(index, item)\n"),
    # 7) package opened from the Game tab itself: origin = Game
    ("                            onSelectPackage = { pkg ->\n                                selectedPackage = pkg\n",
     "                            onSelectPackage = { pkg ->\n                                markPackageOrigin()\n                                selectedPackage = pkg\n"),
]
for old, new in edits:
    n = src.count(old)
    if n != 1:
        print(f"ERROR: anchor found {n} times (need exactly 1); file not changed:\n{old[:120]}"); sys.exit(1)
for old, new in edits:
    src = src.replace(old, new)
FILE.write_text(src, encoding="utf-8")
print("patched")
