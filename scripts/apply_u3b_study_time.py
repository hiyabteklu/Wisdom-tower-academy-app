#!/usr/bin/env python3
"""U3b (docs/PLAN_2026-11-06_URGENT_FIVE_FIXES.md): count study time only while the reader is really in front.

Before: NotesReaderScreen added (now - lastFlush) with no limit, PdfReaderScreen added wall-clock time since the last save
(max 10 minutes each), and neither stopped when the screen went off or the app went to the background. A phone left open
added hours. After: both readers take their seconds from ActiveStudyTimer (runs only between ON_RESUME and ON_PAUSE of the
screen's lifecycle), at most 600 seconds per save. All-or-nothing: if any anchor is missing or not unique, nothing changes.
Safe to run twice (marker: rememberActiveStudyTimer)."""
import sys
from pathlib import Path

BASE = Path("app/src/main/java/com/wisdomtower/academy/ui/study")
MARK = "rememberActiveStudyTimer"

EDITS = {
    BASE / "NotesReaderScreen.kt": [
        (
            "    @Volatile var lastFlush: Long = 0L\n}\n",
            "    @Volatile var lastFlush: Long = 0L\n    @Volatile var timer: ActiveStudyTimer? = null // U3b\n}\n",
        ),
        (
            "    val now = SystemClock.elapsedRealtime()\n"
            "    val seconds = ((now - progress.lastFlush) / 1000L).toInt()\n"
            "    if (seconds < 1) return\n"
            "    progress.lastFlush = now\n",
            "    // U3b: only the seconds the note was really in front (screen on, app open), at most 10 minutes per save\n"
            "    val seconds = progress.timer?.take(600) ?: 0\n"
            "    if (seconds < 1) return\n",
        ),
        (
            "    val progress = remember(resourceId) { NoteProgress() }\n",
            "    val progress = remember(resourceId) { NoteProgress() }\n"
            "    val studyTimer = rememberActiveStudyTimer(resourceId) // U3b\n"
            "    progress.timer = studyTimer\n",
        ),
    ],
    BASE / "PdfReaderScreen.kt": [
        (
            "    val sessionStart = remember(file) { longArrayOf(System.currentTimeMillis()) }\n",
            "    val studyTimer = rememberActiveStudyTimer(file) // U3b: counts only while the book is in front\n",
        ),
        (
            "        saveReading(id, currentPage, pageCount, sessionStart)\n",
            "        saveReading(id, currentPage, pageCount, studyTimer)\n",
        ),
        (
            "                    saveReading(id, page, pageCount, sessionStart)\n",
            "                    saveReading(id, page, pageCount, studyTimer)\n",
        ),
        (
            "private suspend fun saveReading(resourceId: String, page: Int, pageCount: Int, sessionStart: LongArray) {\n"
            "    val now = System.currentTimeMillis()\n"
            "    val seconds = ((now - sessionStart[0]) / 1000L).toInt().coerceIn(0, 600)\n"
            "    sessionStart[0] = now\n",
            "private suspend fun saveReading(resourceId: String, page: Int, pageCount: Int, timer: ActiveStudyTimer) {\n"
            "    val seconds = timer.take(600) // U3b: active seconds only, at most 10 minutes per save\n",
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
