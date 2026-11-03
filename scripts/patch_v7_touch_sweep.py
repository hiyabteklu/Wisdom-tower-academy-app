#!/usr/bin/env python3
"""V7 sweep (docs/IOS_MOTION_V2_PLAN.md): every plain `.clickable` gets the shared press feel (scale 0.97 + light tick).

Adds `.wisdomTouchFeel()` (new file ui/theme/WisdomTouchFeel.kt, already in main) right after the ROOT of the modifier chain that
contains the `.clickable`, so the background, border and content scale together. Click actions are not touched.
A call is left alone when: it uses interactionSource/indication, the chain is full-screen (fillMaxSize, matchParentSize = scrims),
already has a press or pointer modifier, or the chain root cannot be found with certainty.
Skipped on purpose: admin, theme, competition, exam and in-play game screens.
Safe to run twice (files that already contain wisdomTouchFeel are skipped). Usage: python scripts/patch_v7_touch_sweep.py [--dry]
"""
import re
import sys
from pathlib import Path

UI = Path("app/src/main/java/com/wisdomtower/academy/ui")
SKIP_DIRS = {"admin", "theme", "competition"}
SKIP_FILES = {"QuizExamScreen.kt"}
GAME_KEEP = {"LevelPickerScreen.kt", "GameHubScreen.kt", "GameHubBanner.kt", "ExamPickerScreen.kt", "GameButtons.kt"}
BAD = ("fillMaxSize", "matchParentSize", "wisdomPress", "wisdomTouchFeel", "pointerInput", "combinedClickable",
       "draggable", "scrollable", "verticalScroll", "horizontalScroll", "wisdomSelectionPop", "toggleable", "selectable")
IMPORT = "import com.wisdomtower.academy.ui.theme.wisdomTouchFeel\n"
CALL = re.compile(r"\.clickable\b\s*([({])")


def match_back(s, k):
    close = s[k]
    opn = "(" if close == ")" else "{"
    depth = 0
    while k >= 0:
        if s[k] == close:
            depth += 1
        elif s[k] == opn:
            depth -= 1
            if depth == 0:
                return k
        k -= 1
    return None


def match_fwd(s, k):
    opn = s[k]
    close = ")" if opn == "(" else "}"
    depth = 0
    while k < len(s):
        if s[k] == opn:
            depth += 1
        elif s[k] == close:
            depth -= 1
            if depth == 0:
                return k
        k += 1
    return None


def ident_back(s, k):
    e = k
    while k >= 0 and (s[k].isalnum() or s[k] == "_"):
        k -= 1
    return s[k + 1:e + 1], k


def skip_ws(s, k):
    while k >= 0 and s[k] in " \t\r\n":
        k -= 1
    return k


def chain_root_end(s, dot):
    """Index right after the chain root identifier (Modifier / *modifier), or None."""
    j = dot
    for _ in range(80):
        k = skip_ws(s, j - 1)
        if k < 0:
            return None
        c = s[k]
        if c in ")}":
            o = match_back(s, k)
            if o is None:
                return None
            k2 = o - 1
            while k2 >= 0 and s[k2] in " \t":
                k2 -= 1
            if c == "}" and k2 >= 0 and s[k2] == ")":  # name(args) { lambda }
                o2 = match_back(s, k2)
                if o2 is None:
                    return None
                k2 = o2 - 1
            name, k3 = ident_back(s, k2)
            if not name:
                return None
            m = skip_ws(s, k3)
            if m >= 0 and s[m] == ".":
                j = m
                continue
            return None
        if c.isalnum() or c == "_":
            ident, k3 = ident_back(s, k)
            m = skip_ws(s, k3)
            if m >= 0 and s[m] == ".":
                j = m
                continue
            if ident == "Modifier" or ident.lower().endswith("modifier"):
                return k + 1
            return None
        return None
    return None


def patch(text):
    edits, skipped = [], 0
    for m in CALL.finditer(text):
        dot = m.start()
        op = m.end() - 1
        cl = match_fwd(text, op)
        if cl is None:
            skipped += 1
            continue
        args = text[op:cl + 1] if text[op] == "(" else ""
        if "interactionSource" in args or "indication" in args:
            skipped += 1
            continue
        root = chain_root_end(text, dot)
        if root is None:
            skipped += 1
            continue
        chain = text[root:dot]
        if any(b in chain for b in BAD):
            skipped += 1
            continue
        edits.append(root)
    for root in sorted(set(edits), reverse=True):
        text = text[:root] + ".wisdomTouchFeel()" + text[root:]
    return text, len(set(edits)), skipped


def main():
    dry = "--dry" in sys.argv
    if not (UI / "theme/WisdomTouchFeel.kt").exists():
        print("ERROR: ui/theme/WisdomTouchFeel.kt is missing; nothing changed")
        return 1
    total = files = 0
    for f in sorted(UI.rglob("*.kt")):
        rel = f.relative_to(UI).parts
        if rel[0] in SKIP_DIRS or f.name in SKIP_FILES:
            continue
        if rel[0] == "games" and f.name not in GAME_KEEP:
            continue
        text = f.read_text(encoding="utf-8")
        if "wisdomTouchFeel" in text or ".clickable" not in text:
            continue
        new, n, skipped = patch(text)
        if n == 0:
            continue
        new = re.sub(r"^(import [^\n]+\n)", lambda mo: mo.group(1) + IMPORT, new, count=1, flags=re.M)
        if IMPORT not in new:
            print(f"skip {f.name}: no import line found")
            continue
        print(f"{f.name}: {n} patched, {skipped} left alone")
        total += n
        files += 1
        if not dry:
            f.write_text(new, encoding="utf-8")
    print(("DRY RUN: " if dry else "") + f"patched {total} clickables in {files} files")
    return 0


sys.exit(main())
