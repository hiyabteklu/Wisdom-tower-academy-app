#!/usr/bin/env python3
"""D10 (docs/DAY_MODE_PLAN.md): day mode for the "Other resources" screens (ui/guides/GuidesScreen.kt, GuidesData.kt).

Problem: GuidesScreen.kt was never swept by D3 to D7. Titles and icons use Color.White, the reading text uses the light slate
E2E8F0 / CBD5E1, quotes use pale yellow FDE68A, and the white tints (borders, chip and search fills) are invisible on the
frosted ivory day cards. The text styles and colours were also top-level vals, read once at class load, so they could not
follow the day / night switch. GUIDE_TABS in GuidesData.kt has the same problem (accent colours read once at load).

Fix (night stays byte-for-byte the same colours):
  - Color.White (text, icon, tints)  -> WisdomInk   (white at night, deep navy by day)
  - GuideBodyColor / GuideSoftColor / new GuideQuoteColor -> getters that return the old night literal at night and
    WisdomTextPrimary / WisdomMuted / WisdomAccentAmber by day
  - GuideBodyStyle, TipBodyStyle, GuideQuoteStyle, GUIDE_TABS -> getters (read while composing)
Left on purpose: Color.White on solid accent fills (LinkPill "Apply" button), the dark placeholder behind story photos.
Safe to run twice. All anchors must match; otherwise nothing is written.
"""
import sys
from pathlib import Path

BASE = Path("app/src/main/java/com/wisdomtower/academy/ui/guides")
SCREEN = BASE / "GuidesScreen.kt"
DATA = BASE / "GuidesData.kt"
MARK = "GuideQuoteColor"

# (old, new, expected count)
SCREEN_EDITS = [
    ("import com.wisdomtower.academy.ui.theme.AtmosphereBackground\n",
     "import com.wisdomtower.academy.ui.theme.AppThemeMode\n"
     "import com.wisdomtower.academy.ui.theme.AtmosphereBackground\n", 1),
    ("import com.wisdomtower.academy.ui.theme.WisdomGlassFill\n",
     "import com.wisdomtower.academy.ui.theme.WisdomGlassFill\n"
     "import com.wisdomtower.academy.ui.theme.WisdomInk\n"
     "import com.wisdomtower.academy.ui.theme.WisdomMuted\n"
     "import com.wisdomtower.academy.ui.theme.WisdomTextPrimary\n", 1),
    ("private val GuideBodyColor = Color(0xFFE2E8F0)\n"
     "private val GuideSoftColor = Color(0xFFCBD5E1)\n",
     "private val GuideBodyColor: Color get() = if (AppThemeMode.isDark) Color(0xFFE2E8F0) else WisdomTextPrimary\n"
     "private val GuideSoftColor: Color get() = if (AppThemeMode.isDark) Color(0xFFCBD5E1) else WisdomMuted\n"
     "private val GuideQuoteColor: Color get() = if (AppThemeMode.isDark) Color(0xFFFDE68A) else WisdomAccentAmber\n", 1),
    ("private val GuideBodyStyle = TextStyle(", "private val GuideBodyStyle: TextStyle get() = TextStyle(", 1),
    ("private val TipBodyStyle = GuideBodyStyle.copy(", "private val TipBodyStyle: TextStyle get() = GuideBodyStyle.copy(", 1),
    ("private val GuideQuoteStyle = TextStyle(", "private val GuideQuoteStyle: TextStyle get() = TextStyle(", 1),
    ("    color = Color(0xFFFDE68A)\n", "    color = GuideQuoteColor\n", 1),
    ("color = Color.White", "color = WisdomInk", 14),
    ("tint = Color.White", "tint = WisdomInk", 1),
    # unfocused FIRST: 'focusedTextColor = ...' is also a substring of 'unfocusedTextColor = ...'
    ("unfocusedTextColor = Color.White", "unfocusedTextColor = WisdomInk", 1),
    ("focusedTextColor = Color.White", "focusedTextColor = WisdomInk", 1),
    ("Color.White.copy(", "WisdomInk.copy(", 7),
]
DATA_EDITS = [
    ("val GUIDE_TABS = listOf(", "val GUIDE_TABS: List<GuideTab> get() = listOf(", 1),
]


def apply(src, edits, name):
    for old, new, want in edits:
        n = src.count(old)
        if n != want:
            print(f"ERROR in {name}: {old!r} found {n} times, expected {want}; nothing written")
            sys.exit(1)
        src = src.replace(old, new)
    return src


screen_src = SCREEN.read_text(encoding="utf-8")
if MARK in screen_src:
    print("already patched, nothing to do")
    sys.exit(0)
data_src = DATA.read_text(encoding="utf-8")
new_screen = apply(screen_src, SCREEN_EDITS, SCREEN.name)
new_data = apply(data_src, DATA_EDITS, DATA.name)
SCREEN.write_text(new_screen, encoding="utf-8")
DATA.write_text(new_data, encoding="utf-8")
print("patched GuidesScreen.kt and GuidesData.kt")
