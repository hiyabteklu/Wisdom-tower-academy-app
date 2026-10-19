#!/usr/bin/env python3
"""D4 (docs/DAY_MODE_PLAN.md): day mode for the app shell in MainActivity.kt: top bar, bottom navigation pill, drawer,
tool overlay header and the study-timer dialog.

Why: the shell colours were private top-level vals read once at class load (so they never followed the day / night
switch) and several surfaces were literal dark colours or Color.White. This patch turns the vals into getters and moves
the literals to day / night pairs. NIGHT STAYS BYTE-FOR-BYTE THE SAME COLOURS: every pair keeps the old literal as the
night value. Day uses the Ivory & Navy tokens; shell overlays are 92 percent frosted ivory (plan section 4).

Safe to run twice (marker check). Every anchor must match exactly once, otherwise nothing is written.
"""
import sys
from pathlib import Path

FILE = Path("app/src/main/java/com/wisdomtower/academy/MainActivity.kt")
MARK = "D4: day mode shell"

EDITS = []


def edit(anchor, replacement):
    EDITS.append((anchor, replacement))


# 0. imports -----------------------------------------------------------------------------------------------------------
edit(
    "import com.wisdomtower.academy.ui.theme.WisdomNavy\n",
    "import com.wisdomtower.academy.ui.theme.WisdomNavy\n"
    "import com.wisdomtower.academy.ui.theme.AppThemeMode\n"
    "import com.wisdomtower.academy.ui.theme.WisdomAccentAmber\n"
    "import com.wisdomtower.academy.ui.theme.WisdomAccentRose\n"
    "import com.wisdomtower.academy.ui.theme.WisdomBorderWhite\n"
    "import com.wisdomtower.academy.ui.theme.WisdomCard\n"
    "import com.wisdomtower.academy.ui.theme.WisdomCardBorder\n"
    "import com.wisdomtower.academy.ui.theme.WisdomCardBorderSubtle\n"
    "import com.wisdomtower.academy.ui.theme.WisdomCyan\n"
    "import com.wisdomtower.academy.ui.theme.WisdomCyanDark\n"
    "import com.wisdomtower.academy.ui.theme.WisdomDarkOnCyan\n"
    "import com.wisdomtower.academy.ui.theme.WisdomInk\n"
    "import com.wisdomtower.academy.ui.theme.WisdomMuted\n"
    "import com.wisdomtower.academy.ui.theme.WisdomTextPrimary\n",
)

# 1. the private colour vals become getters ----------------------------------------------------------------------------
edit(
    "private val BarBg = Color(0xFF060B15) // Deep navy, exactly matching website page background\n"
    "private val Accent = Color(0xFF22E0FF) // Website source of truth cyan\n"
    "private val AccentDark = Color(0xFF00C4E6)\n"
    "private val CardSurface = Color(0x660C1424) // Glass-like card surface\n"
    "private val CardBorder = Color(0x3322E0FF) // Soft 1px cyan border\n"
    "private val CardBorderSubtle = Color(0x1F22E0FF) // Hairline quiet divider\n"
    "private val SurfaceColor = CardSurface\n"
    "private val Muted = Color(0xFF94A3B8)\n"
    "private val TextPrimary = Color(0xFFF8FAFC)\n"
    "private val DarkOnCyan = Color(0xFF070D17) // Dark text on solid cyan pills\n",
    "// D4: day mode shell. These are GETTERS now (they were read once at class load and never followed the day / night\n"
    "// switch). Night values are exactly the old literals; day values come from the Ivory & Navy tokens.\n"
    "private fun dayNight(night: Color, day: Color): Color = if (AppThemeMode.isDark) night else day\n"
    "private val ShellOverlayIvory = Color(0xEBF8F5EE) // day overlays (bars, nav, drawer, dialogs): 92 percent frosted ivory\n"
    "private val BarBg: Color get() = WisdomNavy // night 060B15, same as before\n"
    "private val Accent: Color get() = WisdomCyan // night 22E0FF, day deep teal-cyan\n"
    "private val AccentDark: Color get() = WisdomCyanDark // night 00C4E6\n"
    "private val CardSurface: Color get() = dayNight(Color(0x660C1424), WisdomCard) // night: the old glass-like card surface\n"
    "private val CardBorder: Color get() = WisdomCardBorder // night 3322E0FF, same as before\n"
    "private val CardBorderSubtle: Color get() = dayNight(Color(0x1F22E0FF), WisdomCardBorderSubtle) // hairline quiet divider\n"
    "private val SurfaceColor: Color get() = CardSurface\n"
    "private val Muted: Color get() = dayNight(Color(0xFF94A3B8), WisdomMuted)\n"
    "private val TextPrimary: Color get() = dayNight(Color(0xFFF8FAFC), WisdomTextPrimary)\n"
    "private val DarkOnCyan: Color get() = WisdomDarkOnCyan // night 070D17, day white on the teal pills\n"
    "private val BarGlass: Color get() = dayNight(Color.White.copy(alpha = 0.04f), ShellOverlayIvory)\n"
    "private val BarDivider: Color get() = dayNight(Color.White.copy(alpha = 0.08f), WisdomBorderWhite)\n",
)

# 2. top bar -------------------------------------------------------------------------------------------------------------
edit(
    "                        .background(Color.White.copy(alpha = 0.04f))\n"
    "                ) {\n"
    "                    // Top App Bar Row\n",
    "                        .background(BarGlass)\n"
    "                ) {\n"
    "                    // Top App Bar Row\n",
)
edit(
    '                                    text = if (inStudy) "Study Workspace" else "Wisdom Tower Academy",\n'
    "                                    color = Color.White,\n",
    '                                    text = if (inStudy) "Study Workspace" else "Wisdom Tower Academy",\n'
    "                                    color = WisdomInk,\n",
)
edit(
    "                                    color = if (isTimerRunning) Color(0x2422E0FF) else Color(0x24F59E0B),\n"
    "                                    shape = RoundedCornerShape(999.dp),\n"
    "                                    border = BorderStroke(1.dp, if (isTimerRunning) Accent.copy(alpha = 0.6f) else Color(0x66F59E0B)),\n",
    "                                    color = if (isTimerRunning) dayNight(Color(0x2422E0FF), Accent.copy(alpha = 0.14f)) else dayNight(Color(0x24F59E0B), WisdomAccentAmber.copy(alpha = 0.14f)),\n"
    "                                    shape = RoundedCornerShape(999.dp),\n"
    "                                    border = BorderStroke(1.dp, if (isTimerRunning) Accent.copy(alpha = 0.6f) else dayNight(Color(0x66F59E0B), WisdomAccentAmber.copy(alpha = 0.5f))),\n",
)
edit(
    "                                            tint = if (isTimerRunning) Accent else Color(0xFFF59E0B),\n",
    "                                            tint = if (isTimerRunning) Accent else WisdomAccentAmber,\n",
)
edit(
    "                                            text = formatted,\n"
    "                                            color = Color.White,\n"
    "                                            fontSize = 11.5.sp,\n",
    "                                            text = formatted,\n"
    "                                            color = WisdomInk,\n"
    "                                            fontSize = 11.5.sp,\n",
)
edit(
    "                            .background(Color.White.copy(alpha = 0.08f))\n"
    "                    )\n"
    "                }\n"
    "            },\n"
    "            bottomBar = {\n",
    "                            .background(BarDivider)\n"
    "                    )\n"
    "                }\n"
    "            },\n"
    "            bottomBar = {\n",
)

# 3. tool overlay header -----------------------------------------------------------------------------------------------
edit(
    "                    color = Color(0xFF070D18),\n"
    "                    tonalElevation = 6.dp,\n",
    "                    color = dayNight(Color(0xFF070D18), ShellOverlayIvory),\n"
    "                    tonalElevation = 6.dp,\n",
)
edit(
    "                                text = activeToolOverlayTitle,\n"
    "                                color = Color.White,\n",
    "                                text = activeToolOverlayTitle,\n"
    "                                color = WisdomInk,\n",
)
edit(
    "                                .background(Color(0x26EF4444))\n"
    "                                .border(BorderStroke(1.dp, Color(0x66EF4444)), RoundedCornerShape(8.dp))\n",
    "                                .background(dayNight(Color(0x26EF4444), WisdomAccentRose.copy(alpha = 0.10f)))\n"
    "                                .border(BorderStroke(1.dp, dayNight(Color(0x66EF4444), WisdomAccentRose.copy(alpha = 0.40f))), RoundedCornerShape(8.dp))\n",
)
edit(
    "                                color = Color(0xFFF87171),\n",
    "                                color = dayNight(Color(0xFFF87171), WisdomAccentRose),\n",
)

# 4. drawer ------------------------------------------------------------------------------------------------------------
edit(
    "                    .background(Color(0x8C030812)) // BG-READ: slightly stronger dim behind the drawer\n",
    "                    .background(dayNight(Color(0x8C030812), Color(0x660B1B33))) // BG-READ: slightly stronger dim behind the drawer; D4: day = navy 40 percent\n",
)
edit(
    "                                listOf(Color(0xD9152238), Color(0xD90D1829)) // BG-READ: 85 percent so text does not mix with what is behind\n",
    "                                listOf(dayNight(Color(0xD9152238), ShellOverlayIvory), dayNight(Color(0xD90D1829), Color(0xEBF4F0E6))) // BG-READ: 85 percent so text does not mix with what is behind; D4: day = frosted ivory 92 percent\n",
)
edit(
    "                            BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),\n"
    "                            RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)\n",
    "                            BorderStroke(1.dp, dayNight(Color.White.copy(alpha = 0.10f), WisdomBorderWhite)),\n"
    "                            RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)\n",
)
edit(
    '                                    text = "Wisdom Tower",\n'
    "                                    color = Color.White,\n",
    '                                    text = "Wisdom Tower",\n'
    "                                    color = WisdomInk,\n",
)
edit(
    "                            color = Muted.copy(alpha = 0.45f),\n",
    "                            color = Muted.copy(alpha = if (AppThemeMode.isDark) 0.45f else 0.85f),\n",
)

# 5. bottom navigation pill ----------------------------------------------------------------------------------------------
edit(
    "private val NavTrackBottom = CardSurface\n",
    "private val NavTrackBottom: Color get() = CardSurface\n",
)
edit(
    "                .background(Color.White.copy(alpha = 0.09f)) // BG-NAV: glass track, no outline, no glow (BG-READ: 9 percent)\n",
    "                .background(dayNight(Color.White.copy(alpha = 0.09f), ShellOverlayIvory)) // BG-NAV: glass track, no outline, no glow (BG-READ: 9 percent); D4: day = ivory track\n"
    "                .then(if (AppThemeMode.isDark) Modifier else Modifier.border(1.dp, WisdomBorderWhite, trackShape)) // D4: day gets a hairline, night stays without outline\n",
)

# 6. drawer rows -------------------------------------------------------------------------------------------------------
edit(
    "            .background(if (isPressed) Color(0x2222E0FF) else Color.Transparent)\n",
    "            .background(if (isPressed) dayNight(Color(0x2222E0FF), Accent.copy(alpha = 0.14f)) else Color.Transparent)\n",
)
edit(
    "                    .background(Color(0x1F22E0FF)),\n"
    "                contentAlignment = Alignment.Center\n",
    "                    .background(dayNight(Color(0x1F22E0FF), Accent.copy(alpha = 0.12f))),\n"
    "                contentAlignment = Alignment.Center\n",
)
edit(
    "                color = Color(0xFFF1F5F9),\n",
    "                color = dayNight(Color(0xFFF1F5F9), WisdomTextPrimary),\n",
)

# 7. study-timer dialog --------------------------------------------------------------------------------------------------
edit(
    "            color = CardSurface,\n"
    "            shape = RoundedCornerShape(20.dp),\n",
    "            color = dayNight(CardSurface, ShellOverlayIvory),\n"
    "            shape = RoundedCornerShape(20.dp),\n",
)
edit(
    '                        text = "Focus Session",\n'
    "                        color = Color.White,\n",
    '                        text = "Focus Session",\n'
    "                        color = WisdomInk,\n",
)
edit(
    "containerColor = if (isRunning) Color(0x3322E0FF) else Accent),\n",
    "containerColor = if (isRunning) dayNight(Color(0x3322E0FF), Accent.copy(alpha = 0.20f)) else Accent),\n",
)
edit(
    "                            tint = if (isRunning) Accent else Color(0xFF04101A),\n",
    "                            tint = if (isRunning) Accent else dayNight(Color(0xFF04101A), WisdomDarkOnCyan),\n",
)
edit(
    "                            color = if (isRunning) Accent else Color(0xFF04101A),\n",
    "                            color = if (isRunning) Accent else dayNight(Color(0xFF04101A), WisdomDarkOnCyan),\n",
)
edit(
    "                            color = Color(0xFFEF4444),\n"
    "                            fontWeight = FontWeight.Bold,\n"
    "                            fontSize = 13.sp\n",
    "                            color = dayNight(Color(0xFFEF4444), WisdomAccentRose),\n"
    "                            fontWeight = FontWeight.Bold,\n"
    "                            fontSize = 13.sp\n",
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

FILE.write_text(out, encoding="utf-8")
print(f"patched ({len(EDITS)} edits)")
