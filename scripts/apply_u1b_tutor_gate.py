#!/usr/bin/env python3
"""U1b (2026-11-06, docs/PLAN_2026-11-06_URGENT_FIVE_FIXES.md): AI tutor asks for sign-in first.

Edits ONE file: app/src/main/java/com/wisdomtower/academy/ui/study/NativeTutorScreen.kt
  1. adds two imports (collectAsState, AuthStateManager) after the ExamLockdown import
  2. right before `PauseShellWhileShown()` it adds: if the student is signed out, show TutorSignInGate and return

The new screen TutorSignInGate lives in ui/study/TutorSignInGate.kt (already on main).
Safe to run twice (marker check). Stops with an error if an anchor is not found, and changes nothing then.
"""
import sys
from pathlib import Path

PATH = Path("app/src/main/java/com/wisdomtower/academy/ui/study/NativeTutorScreen.kt")
MARKER = "U1b (2026-11-06"

IMPORT_ANCHOR = "import com.wisdomtower.academy.data.competition.ExamLockdown\n"
IMPORT_ADD = (
    "import androidx.compose.runtime.collectAsState\n"
    "import com.wisdomtower.academy.data.auth.AuthStateManager\n"
)

GATE_ANCHOR = (
    "    // Same touch fix + quiet background as the notes, flashcards and quiz screens (see ShellPause.kt).\n"
    "    PauseShellWhileShown()\n"
)
GATE_ADD = (
    "    // U1b (2026-11-06, docs/PLAN_2026-11-06_URGENT_FIVE_FIXES.md): the AI tutor is for signed-in students only.\n"
    "    val tutorSignedIn by AuthStateManager.isLoggedIn.collectAsState()\n"
    "    if (!tutorSignedIn) {\n"
    "        TutorSignInGate(onBack = onBack, modifier = modifier)\n"
    "        return\n"
    "    }\n"
    "\n"
)


def main() -> int:
    if not PATH.is_file():
        print(f"ERROR: {PATH} not found (run from the repository root)")
        return 1
    text = PATH.read_text(encoding="utf-8")
    if MARKER in text:
        print("Already applied, nothing to do.")
        return 0
    if text.count(IMPORT_ANCHOR) != 1:
        print("ERROR: import anchor not found exactly once; file changed since this patch was written.")
        return 1
    if text.count(GATE_ANCHOR) != 1:
        print("ERROR: gate anchor not found exactly once; file changed since this patch was written.")
        return 1
    text = text.replace(IMPORT_ANCHOR, IMPORT_ANCHOR + IMPORT_ADD, 1)
    text = text.replace(GATE_ANCHOR, GATE_ADD + GATE_ANCHOR, 1)
    PATH.write_text(text, encoding="utf-8")
    print("OK: NativeTutorScreen.kt now shows TutorSignInGate to signed-out students.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
