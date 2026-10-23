#!/usr/bin/env python3
"""Drawer cleanup (owner order 2026-10-23): remove the "Refresh" row and the "Wisdom Tower Academy • Native Shell" footer.

Three edits in MainActivity.kt, nothing else:
 1. The drawer's "Refresh" row, together with the divider block above it (so only ONE divider is left between
    the Tools rows and "SUPPORT & ABOUT").
 2. The footer: the divider line and the "Wisdom Tower Academy • Native Shell" text at the bottom of the drawer.
 3. The "SUPPORT & ABOUT" rows' animation index 6 + linkIndex becomes 5 + linkIndex (index 5 was Refresh).
performHardRefresh() and the Refresh icon import are left alone (other code may still use them).
Safe to run twice (marker DRAWER-CLEAN). Changes nothing if an anchor is missing or not unique.
"""
import sys
from pathlib import Path

FILE = Path("app/src/main/java/com/wisdomtower/academy/MainActivity.kt")
MARK = "DRAWER-CLEAN"

# 1. divider block + Refresh row (the second divider block, before SUPPORT & ABOUT, stays)
A1 = (
    "                            Spacer(modifier = Modifier.height(8.dp))\n"
    "                            Box(\n"
    "                                modifier = Modifier\n"
    "                                    .fillMaxWidth()\n"
    "                                    .height(1.dp)\n"
    "                                    .background(CardBorderSubtle)\n"
    "                            )\n"
    "                            Spacer(modifier = Modifier.height(4.dp))\n"
    "\n"
    "                            DrawerCompactRow(\n"
    "                                icon = Icons.Filled.Refresh,\n"
    "                                label = \"Refresh\",\n"
    "                                index = 5,\n"
    "                                onClick = {\n"
    "                                    menuExpanded = false\n"
    "                                    performHardRefresh()\n"
    "                                }\n"
    "                            )\n"
    "\n"
)
R1 = f"                            // {MARK}: Refresh row removed from the drawer\n"

# 2. footer divider + Native Shell text
A2 = (
    "                        Box(\n"
    "                            modifier = Modifier\n"
    "                                .fillMaxWidth()\n"
    "                                .height(1.dp)\n"
    "                                .background(CardBorderSubtle)\n"
    "                        )\n"
    "                        Spacer(modifier = Modifier.height(8.dp))\n"
    "                        Text(\n"
    "                            text = \"Wisdom Tower Academy \u2022 Native Shell\",\n"
    "                            color = Muted.copy(alpha = if (AppThemeMode.isDark) 0.45f else 0.85f),\n"
    "                            fontSize = 12.sp,\n"
    "                            fontWeight = FontWeight.Normal,\n"
    "                            modifier = Modifier.fillMaxWidth(),\n"
    "                            textAlign = TextAlign.Center\n"
    "                        )\n"
)
R2 = ""

# 3. animation index of the support rows
A3 = "                                    index = 6 + linkIndex,\n"
R3 = "                                    index = 5 + linkIndex,\n"

src = FILE.read_text(encoding="utf-8")
if MARK in src:
    print("already patched, nothing to do")
    sys.exit(0)
for name, anchor in (("refresh row", A1), ("native shell footer", A2), ("support index", A3)):
    n = src.count(anchor)
    if n != 1:
        print(f"ERROR: anchor for {name} found {n} times (need exactly 1); file not changed")
        sys.exit(1)
src = src.replace(A1, R1).replace(A2, R2).replace(A3, R3)
FILE.write_text(src, encoding="utf-8")
print("patched")
