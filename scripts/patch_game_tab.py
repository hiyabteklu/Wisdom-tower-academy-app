#!/usr/bin/env python3
"""Replaces the bottom-navigation 'Packages' tab with a 'Game' tab (Tower Climb / Tower Defense).
Changes in MainActivity.kt only: tab title + icon, the tab's catalogue screen becomes GameTabScreen, one icon import.
The course page (PackageDetailScreen) behind the tab is unchanged. Safe to run twice."""
import sys
from pathlib import Path

FILE = Path("app/src/main/java/com/wisdomtower/academy/MainActivity.kt")
MARK = "ui.games.GameTabScreen("

EDITS = [
    ("icon import",
     "import androidx.compose.material.icons.filled.Settings\n",
     "import androidx.compose.material.icons.filled.Settings\nimport androidx.compose.material.icons.filled.SportsEsports\n"),
    ("nav item",
     'object Packages : BottomNavItem("Packages", Icons.AutoMirrored.Filled.ViewList, "https://www.wisdom-tower-academy.live/packages")',
     'object Packages : BottomNavItem("Game", Icons.Filled.SportsEsports, "https://www.wisdom-tower-academy.live/packages")'),
    ("tab screen",
     """                        PackagesScreen(
                            modifier = Modifier
                                .fillMaxSize()
                                .appBackdrop()
                                .zIndex(10f),
                            packageList = catalogPackages,
                            onSelectPackage = { pkg ->
                                selectedPackage = pkg
                            },
                            onNavigateToUrl = handleGlobalNav
                        )
""",
     """                        com.wisdomtower.academy.ui.games.GameTabScreen(
                            modifier = Modifier
                                .fillMaxSize()
                                .appBackdrop()
                                .zIndex(10f),
                            packageList = catalogPackages,
                            onSelectPackage = { pkg ->
                                selectedPackage = pkg
                            }
                        )
"""),
]

src = FILE.read_text(encoding="utf-8")
if MARK in src:
    print("already patched, nothing to do"); sys.exit(0)
for name, old, new in EDITS:
    n = src.count(old)
    if n != 1:
        print(f"ERROR: anchor '{name}' found {n} times (need exactly 1); file not changed"); sys.exit(1)
for name, old, new in EDITS:
    src = src.replace(old, new)
FILE.write_text(src, encoding="utf-8")
print("patched")
