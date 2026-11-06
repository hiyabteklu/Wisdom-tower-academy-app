#!/usr/bin/env python3
"""U4 patch: the Scholarship guide becomes ONE card per article heading instead of one card per paragraph.

Edits ui/guides/GuidesScreen.kt (a big file, so a patch script, see docs/EDITING_LARGE_FILES.md):
  1. 4 imports (AnnotatedString, SpanStyle, buildAnnotatedString, withStyle) after the TextStyle import.
  2. Inserts the new article code (scripts/u4/ScholarshipArticle.kt.txt) before the "6. SCHOLARSHIPS" section,
     so it can use the file's private helpers (IconTile, Reveal, TipBodyStyle, GuideQuoteStyle ...).
  3. In ScholarshipsFullSection: parses the intro into sections and shows the article when the text has "## "
     headings; otherwise the old Tip landing is used exactly as before.
All-or-nothing: every anchor must match exactly once or NOTHING is written. Safe to run twice.
"""
import sys
from pathlib import Path

GUIDES = Path("app/src/main/java/com/wisdomtower/academy/ui/guides/GuidesScreen.kt")
FRAGMENT = Path("scripts/u4/ScholarshipArticle.kt.txt")
MARK = "SchArticleView"

IMPORT_ANCHOR = "import androidx.compose.ui.text.TextStyle\n"
IMPORT_ADD = (
    "import androidx.compose.ui.text.AnnotatedString\n"
    "import androidx.compose.ui.text.SpanStyle\n"
    "import androidx.compose.ui.text.buildAnnotatedString\n"
    "import androidx.compose.ui.text.withStyle\n"
)

SECTION_ANCHOR = "/* ============================== 6. SCHOLARSHIPS ============================== */\n"

PARSE_ANCHOR = "    val tipBlocks = remember(introText) { parseTipBlocks(introText, SCH_TIP_LABELS) }\n"
PARSE_ADD = "    val articleSections = remember(introText) { parseSchArticle(introText) }\n"

TIP_OLD = (
    "        TipLanding(\n"
    "            accent = WisdomAccentRose,\n"
    "            blocks = tipBlocks,\n"
    "            icons = SCH_TIP_ICONS,\n"
    "            accents = SCH_TIP_ACCENTS,\n"
    "            bulletIcon = Icons.Filled.CheckCircle\n"
    "        )\n"
)
TIP_NEW = (
    "        if (articleSections.any { it.title.isNotBlank() }) {\n"
    "            SchArticleView(sections = articleSections)\n"
    "        } else {\n"
    "            TipLanding(\n"
    "                accent = WisdomAccentRose,\n"
    "                blocks = tipBlocks,\n"
    "                icons = SCH_TIP_ICONS,\n"
    "                accents = SCH_TIP_ACCENTS,\n"
    "                bulletIcon = Icons.Filled.CheckCircle\n"
    "            )\n"
    "        }\n"
)


def main() -> int:
    if not GUIDES.exists():
        print(f"ERROR: {GUIDES} not found; nothing changed")
        return 1
    if not FRAGMENT.exists():
        print(f"ERROR: {FRAGMENT} not found; nothing changed")
        return 1
    src = GUIDES.read_text(encoding="utf-8")
    if MARK in src:
        print("already patched, nothing to do")
        return 0

    fragment = FRAGMENT.read_text(encoding="utf-8")
    if not fragment.endswith("\n"):
        fragment += "\n"

    edits = [
        (IMPORT_ANCHOR, IMPORT_ANCHOR + IMPORT_ADD),
        (SECTION_ANCHOR, fragment + SECTION_ANCHOR),
        (PARSE_ANCHOR, PARSE_ANCHOR + PARSE_ADD),
        (TIP_OLD, TIP_NEW),
    ]
    for anchor, _ in edits:
        n = src.count(anchor)
        if n != 1:
            first = anchor.strip().splitlines()[0]
            print(f"ERROR: anchor found {n} times (need exactly 1): {first!r}; file not changed")
            return 1

    for anchor, new in edits:
        src = src.replace(anchor, new, 1)
    GUIDES.write_text(src, encoding="utf-8")
    print("patched: scholarship guide is now one card per article heading")
    return 0


if __name__ == "__main__":
    sys.exit(main())
