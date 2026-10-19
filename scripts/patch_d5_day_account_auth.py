#!/usr/bin/env python3
"""D5 (docs/DAY_MODE_PLAN.md): day mode for Account, native sign-in, forgot password and the Admin Console button.

Files: ui/account/AccountScreen.kt, ui/account/AdminEntryButton.kt, ui/auth/NativeSignInScreen.kt,
ui/auth/ForgotPasswordScreen.kt.

Why: these screens used Color.White for text, tints and glass, plus private colour vals read once at class load
(Slate200/300/400, CyanLight, field tokens), so by day they were white on ivory. Night stays the same colours:
WisdomInk is exactly white at night, and every literal that is not white keeps its old value as the night half of a
dayNight(night, day) pair. By day the card glass becomes the frosted ivory gradient, text is navy ink.

All-or-nothing: every anchor must match the expected number of times in every file, otherwise nothing is written.
Safe to run twice (marker check per file).
"""
import sys
from pathlib import Path

ROOT = Path("app/src/main/java/com/wisdomtower/academy/ui")
MARK = "D5: day mode"
HELPER = (
    "// D5: day mode. Night keeps the old value, day uses the Ivory & Navy tokens (docs/DAY_MODE_PLAN.md).\n"
    "private fun dayNight(night: Color, day: Color): Color = if (AppThemeMode.isDark) night else day\n"
)
SHEEN = "listOf(Color.White.copy(alpha = 0.30f), Color.Transparent)"
SHEEN_TOKEN = "@@D5_SHEEN@@"

# (anchor, replacement, expected_count)
FILES = {}

FILES["account/AccountScreen.kt"] = dict(
    edits=[
        ("import com.wisdomtower.academy.ui.theme.WisdomMuted\n",
         "import com.wisdomtower.academy.ui.theme.WisdomMuted\n"
         "import com.wisdomtower.academy.ui.theme.AppThemeMode\n"
         "import com.wisdomtower.academy.ui.theme.WisdomInk\n"
         "import com.wisdomtower.academy.ui.theme.WisdomPrimaryBtnGradient\n"
         "import com.wisdomtower.academy.ui.theme.WisdomTextPrimary\n", 1),
        ("private val PillOnAccent = Color(0xFF06101F)\n",
         HELPER + "private val PillOnAccent: Color get() = dayNight(Color(0xFF06101F), WisdomDarkOnCyan)\n", 1),
        ("    val accent = if (complete) GoldMid else WisdomCyan\n",
         "    val accent = if (complete) dayNight(GoldMid, Color(0xFF5C3F0A)) else WisdomCyan\n", 1),
        (".background(Brush.horizontalGradient(listOf(WisdomCyan, Color(0xFF38BDF8))))",
         ".background(if (AppThemeMode.isDark) Brush.horizontalGradient(listOf(WisdomCyan, Color(0xFF38BDF8))) else WisdomPrimaryBtnGradient)", 1),
        ("Color(0xFFE2E8F0)", "dayNight(Color(0xFFE2E8F0), WisdomTextPrimary)", 2),
    ],
    white=True,
)

FILES["account/AdminEntryButton.kt"] = dict(
    edits=[
        ("import com.wisdomtower.academy.data.admin.AdminAccess\n",
         "import com.wisdomtower.academy.data.admin.AdminAccess\n"
         "import com.wisdomtower.academy.ui.theme.AppThemeMode\n"
         "import com.wisdomtower.academy.ui.theme.WisdomAccentPurple\n", 1),
        ("    val accent = Color(0xFFC084FC)\n",
         "    val accent = if (AppThemeMode.isDark) Color(0xFFC084FC) else WisdomAccentPurple // D5: day mode\n", 1),
    ],
    white=False,
)

FILES["auth/NativeSignInScreen.kt"] = dict(
    edits=[
        ("import com.wisdomtower.academy.ui.theme.WisdomDarkOnCyan\n",
         "import com.wisdomtower.academy.ui.theme.WisdomDarkOnCyan\n"
         "import com.wisdomtower.academy.ui.theme.AppThemeMode\n"
         "import com.wisdomtower.academy.ui.theme.WisdomCardSurfaceGradient\n"
         "import com.wisdomtower.academy.ui.theme.WisdomInk\n"
         "import com.wisdomtower.academy.ui.theme.WisdomMuted\n"
         "import com.wisdomtower.academy.ui.theme.WisdomTextPrimary\n", 1),
        ("private val Slate200 = Color(0xFFE2E8F0)\n"
         "private val Slate300 = Color(0xFFCBD5E1)\n"
         "private val Slate400 = Color(0xFF94A3B8)\n"
         "private val CyanLight = Color(0xFF67E8F9)\n",
         HELPER +
         "private val Slate200: Color get() = dayNight(Color(0xFFE2E8F0), WisdomTextPrimary)\n"
         "private val Slate300: Color get() = dayNight(Color(0xFFCBD5E1), WisdomTextPrimary)\n"
         "private val Slate400: Color get() = dayNight(Color(0xFF94A3B8), WisdomMuted)\n"
         "private val CyanLight: Color get() = dayNight(Color(0xFF67E8F9), WisdomCyan)\n", 1),
        ("private val FieldBackground = Color.White.copy(alpha = 0.06f)\n"
         "private val FieldBorder = Color.White.copy(alpha = 0.12f)\n"
         "private val GlassRowFill = Color.White.copy(alpha = 0.04f)\n"
         "private val GlassRowBorder = Color.White.copy(alpha = 0.10f)\n",
         "private val FieldBackground: Color get() = Color.White.copy(alpha = 0.06f)\n"
         "private val FieldBorder: Color get() = Color.White.copy(alpha = 0.12f)\n"
         "private val GlassRowFill: Color get() = Color.White.copy(alpha = 0.04f)\n"
         "private val GlassRowBorder: Color get() = Color.White.copy(alpha = 0.10f)\n", 1),
        ("                    .background(\n"
         "                        Brush.verticalGradient(\n"
         "                            listOf(\n"
         "                                Color.White.copy(alpha = 0.09f),\n"
         "                                Color.White.copy(alpha = 0.03f)\n"
         "                            )\n"
         "                        )\n"
         "                    )\n",
         "                    .background(\n"
         "                        if (AppThemeMode.isDark) {\n"
         "                            Brush.verticalGradient(\n"
         "                                listOf(\n"
         "                                    Color.White.copy(alpha = 0.09f),\n"
         "                                    Color.White.copy(alpha = 0.03f)\n"
         "                                )\n"
         "                            )\n"
         "                        } else {\n"
         "                            WisdomCardSurfaceGradient\n"
         "                        }\n"
         "                    )\n", 1),
        ("                                    Color.White.copy(alpha = 0.28f),\n"
         "                                    Color.White.copy(alpha = 0.06f)\n",
         "                                    Color.White.copy(alpha = if (AppThemeMode.isDark) 0.28f else 0.14f),\n"
         "                                    Color.White.copy(alpha = if (AppThemeMode.isDark) 0.06f else 0.05f)\n", 1),
        (".background(Color(0x66101A2E))",
         ".background(dayNight(Color(0x66101A2E), Color(0xEBF8F5EE)))", 1),
        ("if (isSelected) Color(0x3306B6D4) else Color.Transparent",
         "if (isSelected) dayNight(Color(0x3306B6D4), WisdomCyan.copy(alpha = 0.14f)) else Color.Transparent", 1),
        (".background(Color.Black.copy(alpha = 0.22f))",
         ".background(dayNight(Color.Black.copy(alpha = 0.22f), Color.White.copy(alpha = 0.08f)))", 1),
    ],
    white=True,
)

FILES["auth/ForgotPasswordScreen.kt"] = dict(
    edits=[
        ("import com.wisdomtower.academy.ui.theme.WisdomDarkOnCyan\n",
         "import com.wisdomtower.academy.ui.theme.WisdomDarkOnCyan\n"
         "import com.wisdomtower.academy.ui.theme.AppThemeMode\n"
         "import com.wisdomtower.academy.ui.theme.WisdomAccentEmerald\n"
         "import com.wisdomtower.academy.ui.theme.WisdomAccentRose\n"
         "import com.wisdomtower.academy.ui.theme.WisdomBorderWhite\n"
         "import com.wisdomtower.academy.ui.theme.WisdomCardSurfaceGradient\n"
         "import com.wisdomtower.academy.ui.theme.WisdomInk\n"
         "import com.wisdomtower.academy.ui.theme.WisdomMuted\n"
         "import com.wisdomtower.academy.ui.theme.WisdomTextPrimary\n", 1),
        ("private val ResetSlate300 = Color(0xFFCBD5E1)\n"
         "private val ResetSlate400 = Color(0xFF94A3B8)\n"
         "private val ResetCyanLight = Color(0xFF67E8F9)\n",
         HELPER +
         "private val ResetSlate300: Color get() = dayNight(Color(0xFFCBD5E1), WisdomTextPrimary)\n"
         "private val ResetSlate400: Color get() = dayNight(Color(0xFF94A3B8), WisdomMuted)\n"
         "private val ResetCyanLight: Color get() = dayNight(Color(0xFF67E8F9), WisdomCyan)\n", 1),
        ("            .background(\n"
         "                Brush.verticalGradient(\n"
         "                    listOf(Color.White.copy(alpha = 0.07f), Color.White.copy(alpha = 0.04f), Color.White.copy(alpha = 0.03f))\n"
         "                )\n"
         "            )\n",
         "            .background(\n"
         "                if (AppThemeMode.isDark) {\n"
         "                    Brush.verticalGradient(\n"
         "                        listOf(Color.White.copy(alpha = 0.07f), Color.White.copy(alpha = 0.04f), Color.White.copy(alpha = 0.03f))\n"
         "                    )\n"
         "                } else {\n"
         "                    WisdomCardSurfaceGradient\n"
         "                }\n"
         "            )\n", 1),
        (".border(BorderStroke(1.dp, Color(0x33FFFFFF)), cardShape)",
         ".border(BorderStroke(1.dp, dayNight(Color(0x33FFFFFF), WisdomBorderWhite)), cardShape)", 1),
        (".border(BorderStroke(1.dp, Color(0x1AFFFFFF)), shape)",
         ".border(BorderStroke(1.dp, dayNight(Color(0x1AFFFFFF), WisdomBorderWhite)), shape)", 1),
        (".background(Color(0x0DFFFFFF))",
         ".background(dayNight(Color(0x0DFFFFFF), Color.White.copy(alpha = 0.05f)))", 1),
        ("tint = Color(0xFF4ADE80),",
         "tint = dayNight(Color(0xFF4ADE80), WisdomAccentEmerald),", 1),
        ("Text(text = text, color = Color(0xFFF87171), fontSize = 13.sp)",
         "Text(text = text, color = dayNight(Color(0xFFF87171), WisdomAccentRose), fontSize = 13.sp)", 1),
    ],
    white=True,
)


def process(rel, spec, text):
    for i, (anchor, repl, want) in enumerate(spec["edits"]):
        n = text.count(anchor)
        if n != want:
            raise SystemExit(f"ERROR: {rel}: edit #{i} anchor found {n} times (need {want}); nothing written\n{anchor[:160]}")
        text = text.replace(anchor, repl)
    if spec["white"]:
        # night-identical: WisdomInk is exactly white at night. The glossy sheen on the primary button stays white.
        if SHEEN in text:
            if text.count(SHEEN) != 1:
                raise SystemExit(f"ERROR: {rel}: sheen anchor found {text.count(SHEEN)} times; nothing written")
            text = text.replace(SHEEN, SHEEN_TOKEN)
        text = text.replace("Color.White.copy(", "WisdomInk.copy(")
        text = text.replace("Color.White", "WisdomInk")
        text = text.replace(SHEEN_TOKEN, SHEEN)
    return text


def main():
    outputs = {}
    for rel, spec in FILES.items():
        path = ROOT / rel
        src = path.read_text(encoding="utf-8")
        if MARK in src:
            print(f"{rel}: already patched, skipped")
            continue
        outputs[path] = process(rel, spec, src)
    for path, text in outputs.items():
        path.write_text(text, encoding="utf-8")
        print(f"patched {path.name}")
    if not outputs:
        print("nothing to do")


if __name__ == "__main__":
    main()
