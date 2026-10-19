#!/usr/bin/env python3
"""D7b (docs/DAY_MODE_PLAN.md): day colours for the app-start loader in MainActivity.kt.

Why: the start-up loader (private `CustomCenteredLoader` / `FuturisticGearRings`, also used for the tool overlay
loading) draws its rings, gear ticks and arcs in neon cyan 00E5FF / sky 38BDF8 / indigo 818CF8. It is drawn on the chosen
app background, so by day it sits on ivory where these colours are almost invisible. D7a already gave the public copy
`BrandSplashLoader.kt` the same fix; this patch does the same for the private original.

Only the colours inside `FuturisticGearRings` change. NIGHT KEEPS THE OLD LITERALS (every pair has the old colour as the
night value). Day values are the deep Ivory & Navy ones: teal 0B7285, sky 0369A1, indigo 4338CA (same as D7a).
`AppThemeMode` is already imported in this file (D4). The value `isDark` is read while composing, so the loader follows
the Day / Night switch.

Safe to run twice (marker check). Every anchor must match exactly once, otherwise nothing is written.
"""
import sys
from pathlib import Path

FILE = Path("app/src/main/java/com/wisdomtower/academy/MainActivity.kt")
MARK = "D7b: day colours for the loader"

EDITS = []


def edit(anchor, replacement):
    EDITS.append((anchor, replacement))


# 1. choose once per composition ---------------------------------------------------------------------------------------
edit(
    '        val transition = rememberInfiniteTransition(label = "futuristicGear")\n',
    '        // D7b: day colours for the loader. Night keeps the old neon literals.\n'
    '        val isDark = AppThemeMode.isDark\n'
    '        val transition = rememberInfiniteTransition(label = "futuristicGear")\n',
)

# 2. base ring ---------------------------------------------------------------------------------------------------------
edit(
    "                color = Color(0x3300E5FF),\n"
    "                radius = rGearInner,\n",
    "                color = if (isDark) Color(0x3300E5FF) else Color(0x330B7285),\n"
    "                radius = rGearInner,\n",
)

# 3. gear ticks --------------------------------------------------------------------------------------------------------
edit(
    "                val tickColor = if (i % 2 == 0) Color(0xFF00E5FF) else Color(0xFF38BDF8)\n",
    "                val tickColor = if (i % 2 == 0) {\n"
    "                    if (isDark) Color(0xFF00E5FF) else Color(0xFF0B7285)\n"
    "                } else {\n"
    "                    if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1)\n"
    "                }\n",
)

# 4. outer arc ---------------------------------------------------------------------------------------------------------
edit(
    "                brush = Brush.sweepGradient(\n"
    "                    listOf(\n"
    "                        Color(0x0000E5FF),\n"
    "                        Color(0x5500E5FF),\n"
    "                        Color(0xFF00E5FF),\n"
    "                        Color(0xFF38BDF8)\n"
    "                    )\n"
    "                ),\n",
    "                brush = Brush.sweepGradient(\n"
    "                    if (isDark) {\n"
    "                        listOf(\n"
    "                            Color(0x0000E5FF),\n"
    "                            Color(0x5500E5FF),\n"
    "                            Color(0xFF00E5FF),\n"
    "                            Color(0xFF38BDF8)\n"
    "                        )\n"
    "                    } else {\n"
    "                        listOf(\n"
    "                            Color(0x000B7285),\n"
    "                            Color(0x550B7285),\n"
    "                            Color(0xFF0B7285),\n"
    "                            Color(0xFF0369A1)\n"
    "                        )\n"
    "                    }\n"
    "                ),\n",
)

# 5. inner arc ---------------------------------------------------------------------------------------------------------
edit(
    "                brush = Brush.sweepGradient(\n"
    "                    listOf(\n"
    "                        Color(0x00818CF8),\n"
    "                        Color(0x55818CF8),\n"
    "                        Color(0xFF818CF8),\n"
    "                        Color(0xFF00E5FF)\n"
    "                    )\n"
    "                ),\n",
    "                brush = Brush.sweepGradient(\n"
    "                    if (isDark) {\n"
    "                        listOf(\n"
    "                            Color(0x00818CF8),\n"
    "                            Color(0x55818CF8),\n"
    "                            Color(0xFF818CF8),\n"
    "                            Color(0xFF00E5FF)\n"
    "                        )\n"
    "                    } else {\n"
    "                        listOf(\n"
    "                            Color(0x004338CA),\n"
    "                            Color(0x554338CA),\n"
    "                            Color(0xFF4338CA),\n"
    "                            Color(0xFF0B7285)\n"
    "                        )\n"
    "                    }\n"
    "                ),\n",
)

# ---------------------------------------------------------------------------------------------------------------------
src = FILE.read_text(encoding="utf-8")
if MARK in src:
    print("already patched, nothing to do")
    sys.exit(0)

out = src
for i, (anchor, replacement) in enumerate(EDITS):
    n = out.count(anchor)
    if n != 1:
        print(f"ERROR: edit #{i} anchor found {n} times (need exactly 1); file not changed")
        print(anchor[:200])
        sys.exit(1)
    out = out.replace(anchor, replacement)

# marker goes in a comment so the second run does nothing
out = out.replace(
    "        // D7b: day colours for the loader. Night keeps the old neon literals.\n",
    "        // D7b: day colours for the loader (marker: " + MARK + "). Night keeps the old neon literals.\n",
    1,
)
FILE.write_text(out, encoding="utf-8")
print(f"patched ({len(EDITS)} edits)")
