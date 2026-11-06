#!/usr/bin/env python3
"""U3c (docs/PLAN_2026-11-06_URGENT_FIVE_FIXES.md): the readers record FOCUS time.

Focus in plain words: a second is focus when the student touched, scrolled or turned a page in the last 3 minutes
(see ActiveStudyTimer.kt). The two readers tell the timer about every touch and save the focus seconds next to the study
seconds (`addFocusSeconds`, which LearningProgressRepository already writes to learning_progress.focus_seconds and, through
add_study_seconds, to study_day_log). Needs U3b (ActiveStudyTimer in both readers) and the U3c ActiveStudyTimer.kt on main.
All-or-nothing: if any anchor is missing or not unique, nothing changes. Safe to run twice (marker: takeSlice)."""
import sys
from pathlib import Path

BASE = Path("app/src/main/java/com/wisdomtower/academy/ui/study")
MARK = "takeSlice"

EDITS = {
    BASE / "NotesReaderScreen.kt": [
        (  # flush: study + focus seconds
            "    val seconds = progress.timer?.take(600) ?: 0\n"
            "    if (seconds < 1) return\n",
            "    val slice = progress.timer?.takeSlice(600) // U3c: study seconds and the focus part of them\n"
            "    val seconds = slice?.seconds ?: 0\n"
            "    val focusSeconds = slice?.focusSeconds ?: 0\n"
            "    if (seconds < 1) return\n",
        ),
        (
            "            progressPct = pct,\n"
            "            addSeconds = seconds\n"
            "        )\n",
            "            progressPct = pct,\n"
            "            addSeconds = seconds,\n"
            "            addFocusSeconds = focusSeconds // U3c\n"
            "        )\n",
        ),
        (  # page scrolled / progress reported = the student is reading
            "                    progress.pct = pct\n",
            "                    progress.pct = pct\n"
            "                    progress.timer?.touch() // U3c\n",
        ),
        (  # reading position reported on scroll
            "                    val frac = JSONObject(json).optDouble(\"frac\", 0.0).toFloat().coerceIn(0f, 1f)\n",
            "                    val frac = JSONObject(json).optDouble(\"frac\", 0.0).toFloat().coerceIn(0f, 1f)\n"
            "                    progress.timer?.touch() // U3c\n",
        ),
        (  # asking the tutor about the note = studying
            "                // Runs on the main thread: the answer is pushed back with evaluateJavascript.\n",
            "                // Runs on the main thread: the answer is pushed back with evaluateJavascript.\n"
            "                progress.timer?.touch() // U3c\n",
        ),
    ],
    BASE / "PdfReaderScreen.kt": [
        (  # finger down on the book
            "                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)\n",
            "                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)\n"
            "                    studyTimer.touch() // U3c\n",
        ),
        (  # finger moving (scrolling, pinching)
            "                        val event = awaitPointerEvent(PointerEventPass.Initial)\n",
            "                        val event = awaitPointerEvent(PointerEventPass.Initial)\n"
            "                        studyTimer.touch() // U3c\n",
        ),
        (
            "    val seconds = timer.take(600) // U3b: active seconds only, at most 10 minutes per save\n",
            "    val slice = timer.takeSlice(600) // U3b + U3c: active seconds only (at most 10 minutes per save) and the focus part\n"
            "    val seconds = slice.seconds\n",
        ),
        (
            "        addSeconds = seconds,\n"
            "        lastPage = page\n",
            "        addSeconds = seconds,\n"
            "        addFocusSeconds = slice.focusSeconds, // U3c\n"
            "        lastPage = page\n",
        ),
    ],
}

new_text = {}
for path, edits in EDITS.items():
    if not path.exists():
        print(f"ERROR: {path} not found; nothing changed"); sys.exit(1)
    src = path.read_text(encoding="utf-8")
    if MARK in src:
        print(f"{path.name}: already patched"); continue
    if "rememberActiveStudyTimer" not in src:
        print(f"ERROR: {path.name} has no U3b timer yet (merge U3b first); nothing changed"); sys.exit(1)
    for old, new in edits:
        n = src.count(old)
        if n != 1:
            print(f"ERROR: anchor in {path.name} found {n} times (need exactly 1); nothing changed:\n{old}")
            sys.exit(1)
        src = src.replace(old, new)
    new_text[path] = src

for path, src in new_text.items():
    path.write_text(src, encoding="utf-8")
    print(f"patched {path.name}")
print("done")
