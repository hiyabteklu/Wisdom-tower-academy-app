#!/usr/bin/env python3
"""D3 (part 3) of docs/DAY_MODE_PLAN.md: Learning, Settings, My Subjects, GPA card and Study workspace follow day / night.

Same idea as D3 part 1 and 2 (Packages, Package detail, Home, done by hand): text, icons and tints that were written as
Color.White / Color.Black become the mode-aware tokens WisdomInk (white at night, deep navy 0B1B33 by day) and WisdomOnInk
(black at night, white by day). NIGHT IS UNCHANGED: the two tokens return exactly white and black at night.

Why a script: LearningScreen.kt (33 KB) and SettingsScreen.kt (63 KB) are too big to resend (docs/EDITING_LARGE_FILES.md);
the other three are small, but one script means one Action run, one branch and one merge for the whole slice.

Files (each was read in full on 2026-10-19; the first five have no literal dark surface, every Color.White / Color.Black in them is
text, an icon tint, a tint on glass, a border, the slider or the switch):
  ui/learning/LearningScreen.kt, ui/settings/SettingsScreen.kt, ui/settings/MySubjectsSection.kt,
  ui/packages/GpaCalculatorCard.kt, ui/study/StudyWorkspaceScreen.kt
  ui/settings/AccountDeletionSection.kt (EXACT ONLY: one line, the "Delete Account" title on the Settings card; its dark
  confirmation dialog keeps white text on purpose, the dialogs get their own day variant in a later slice)

Rules (single line, in this order; same rules as scripts/patch_l2_color_sweep.py, which was tested on a fixture):
  0. MySubjectsSection only, three exact replacements (selected pill fill / border / text, which the regexes do not cover)
  1. Color.White.copy(                      -> WisdomInk.copy(
  2. wisdomAnimatedColor(x, Color.White,    -> (x, WisdomInk,
     wisdomAnimatedColor(x, Color.Black,    -> (x, WisdomOnInk,
  3. ", Color.White)"                       -> ", WisdomInk)"
  4. name = Color.White                     -> name = WisdomInk   (names with color / tint / Thumb / Track, but NOT container or
                                               background names: those are handled by rule 1 when they are tints)
     xxxThumbxxx = Color.Black              -> WisdomOnInk
  5. BorderStroke(w, Color.White)           -> BorderStroke(w, WisdomInk)
Adds the imports for WisdomInk / WisdomOnInk when a file needs them. Safe to run twice (the second run changes nothing).
The balance of ( and ) in a file must stay the same, and every exact replacement must match once, or that file is left
unchanged. At the end it prints every Color.White / Color.Black / Color(0x...) still in the six files (for the next slice).
"""
import re
import sys
from pathlib import Path

BASE = Path("app/src/main/java/com/wisdomtower/academy")
FILES = [
    "ui/learning/LearningScreen.kt",
    "ui/settings/SettingsScreen.kt",
    "ui/settings/MySubjectsSection.kt",
    "ui/packages/GpaCalculatorCard.kt",
    "ui/study/StudyWorkspaceScreen.kt",
    "ui/settings/AccountDeletionSection.kt",
]
# Files where ONLY the exact replacements run (no regex rules): AccountDeletionSection has a dark confirmation dialog
# (containerColor 0xF20F1829) whose white text must STAY white, so only the one line that sits on the Settings card changes.
EXACT_ONLY = {"ui/settings/AccountDeletionSection.kt"}
EXACT = {
    "ui/settings/AccountDeletionSection.kt": [
        (
            'Text(text = "Delete Account", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)',
            'Text(text = "Delete Account", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = WisdomInk)',
        ),
    ],
    "ui/settings/MySubjectsSection.kt": [
        ("selected -> Color.White\n", "selected -> WisdomInk\n"),
        (
            "if (selected) Color.White else if (outlined) WisdomCyan.copy(alpha = 0.4f) else WisdomBorderWhite",
            "if (selected) WisdomInk else if (outlined) WisdomCyan.copy(alpha = 0.4f) else WisdomBorderWhite",
        ),
        (
            "color = if (selected) Color.Black else if (outlined) WisdomCyan else Color.White",
            "color = if (selected) WisdomOnInk else if (outlined) WisdomCyan else WisdomInk",
        ),
    ],
}
IMPORT_PREFIX = "import com.wisdomtower.academy.ui.theme."

NAMED = re.compile(r"\b(\w*(?:[cC]olor|tint|Thumb|Track)\w*)(\s*=\s*)Color\.White\b(?![.\w])")


def named_white(m):
    name = m.group(1)
    if "ontainer" in name or "ackground" in name:
        return m.group(0)
    return f"{name}{m.group(2)}WisdomInk"


RULES = [
    (re.compile(r"Color\.White\.copy\("), lambda m: "WisdomInk.copy("),
    (re.compile(r"wisdomAnimatedColor\((\w+),\s*Color\.White,"), lambda m: f"wisdomAnimatedColor({m.group(1)}, WisdomInk,"),
    (re.compile(r"wisdomAnimatedColor\((\w+),\s*Color\.Black,"), lambda m: f"wisdomAnimatedColor({m.group(1)}, WisdomOnInk,"),
    (re.compile(r",\s*Color\.White\)"), lambda m: ", WisdomInk)"),
    (NAMED, named_white),
    (re.compile(r"\b(\w*Thumb\w*)(\s*=\s*)Color\.Black\b(?![.\w])"), lambda m: f"{m.group(1)}{m.group(2)}WisdomOnInk"),
    (re.compile(r"BorderStroke\(([^,()]+),\s*Color\.White\)"), lambda m: f"BorderStroke({m.group(1)}, WisdomInk)"),
]


def add_imports(src, names):
    need = [n for n in names if re.search(r"\b" + n + r"\b", src) and (IMPORT_PREFIX + n) not in src]
    if not need:
        return src
    lines = src.split("\n")
    for i, line in enumerate(lines):
        if line.startswith("import "):
            for n in reversed(need):
                lines.insert(i, IMPORT_PREFIX + n)
            return "\n".join(lines)
    return src


def sweep(rel):
    f = BASE / rel
    if not f.exists():
        print(f"ERROR: not found: {rel}")
        return False
    src = f.read_text(encoding="utf-8")
    out = src
    for old, new in EXACT.get(rel, []):
        n = out.count(old)
        if n == 1:
            out = out.replace(old, new)
        elif n == 0 and new in out:
            pass  # already done
        else:
            print(f"ERROR: exact text found {n} times in {rel} (need 1): {old[:70]!r}; file not written")
            return False
    if rel not in EXACT_ONLY:
        for pat, rep in RULES:
            out = pat.sub(rep, out)
    out = add_imports(out, ["WisdomInk", "WisdomOnInk"])
    if out == src:
        print(f"unchanged: {rel}")
        return True
    if src.count("(") - src.count(")") != out.count("(") - out.count(")"):
        print(f"ERROR: bracket balance changed in {rel}; file not written")
        return False
    f.write_text(out, encoding="utf-8")
    print(f"patched: {rel}")
    return True


def leftovers():
    lit = re.compile(r"Color\(0x[0-9A-Fa-f]+\)|Color\.White|Color\.Black")
    total = 0
    for rel in FILES:
        f = BASE / rel
        if not f.exists():
            continue
        for no, line in enumerate(f.read_text(encoding="utf-8").split("\n"), 1):
            s = line.strip()
            if lit.search(line) and not s.startswith(("*", "//", "/*")):
                print(f"LEFT {rel}:{no}: {s[:110]}")
                total += 1
    print(f"{total} colour literal(s) left in the six files (review them in the next slice)")


if not BASE.exists():
    print(f"ERROR: {BASE} not found (run from the repository root)")
    sys.exit(1)
ok = True
for rel in FILES:
    ok = sweep(rel) and ok
leftovers()
sys.exit(0 if ok else 1)
