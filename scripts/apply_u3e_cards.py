#!/usr/bin/env python3
"""U3e (docs/PLAN_2026-11-06_URGENT_FIVE_FIXES.md): the four cards at the top of My Status and the sentence
"Your cumulative study time is ..." use the same total as the Study time panel (study_day_log, each day capped at 8 h).

Does three things, all-or-nothing (if any anchor is missing or not unique, nothing changes and no file is written):
1. copies scripts/u3e/StatusCappedTotals.kt.txt to ui/study/StatusCappedTotals.kt (the small helper that reads the day log);
2. NativeStatusScreen.kt: the "Study Time" and "Focus" cards and the summary sentence read the helper instead of the old
   per-resource rows (Accuracy and Active Streak are NOT touched);
3. StudyDayLogReader.kt: the saved offline copy is MERGED by day instead of replaced, so the short 7-day read of the
   "Today & this week" panel can no longer cut the long history saved by the Study time panel and the cards.
Safe to run twice (marker: rememberCappedStudyTotals( in NativeStatusScreen.kt)."""
import sys
from pathlib import Path

SRC = Path("scripts/u3e/StatusCappedTotals.kt.txt")
STUDY = Path("app/src/main/java/com/wisdomtower/academy/ui/study")
DATA = Path("app/src/main/java/com/wisdomtower/academy/data/content")
MARK = "rememberCappedStudyTotals("

SAVE_OLD = (
    "    /** Saves the latest good read for this student. Never throws. */\n"
    "    private fun saveSnapshot(userId: String, rows: List<StudyDayRow>) {\n"
    "        runCatching {\n"
    "            val arr = JSONArray()\n"
    "            for (r in rows) {\n"
    "                arr.put(JSONObject().put(\"day\", r.day).put(\"s\", r.seconds).put(\"f\", r.focusSeconds))\n"
    "            }\n"
    "            StatusSnapshotStore.save(userId, KIND, arr)\n"
    "        }\n"
    "    }\n"
)
SAVE_NEW = (
    "    /**\n"
    "     * Saves the latest good read for this student. Never throws.\n"
    "     * U3e: MERGED by day with the copy already saved (the days of this read win), so a short read (the 7-day panel) cannot\n"
    "     * cut the long history that a longer read (Study time panel, cards: about a year) saved before.\n"
    "     */\n"
    "    private fun saveSnapshot(userId: String, rows: List<StudyDayRow>) {\n"
    "        runCatching {\n"
    "            val merged = java.util.TreeMap<String, StudyDayRow>()\n"
    "            val old = runCatching { StatusSnapshotStore.load(userId, KIND) }.getOrNull()\n"
    "            if (old != null) {\n"
    "                for (i in 0 until old.rows.length()) {\n"
    "                    val o = old.rows.getJSONObject(i)\n"
    "                    val day = o.optString(\"day\", \"\")\n"
    "                    if (day.length < 10) continue\n"
    "                    merged[day.substring(0, 10)] = StudyDayRow(day.substring(0, 10), o.optInt(\"s\", 0), o.optInt(\"f\", 0))\n"
    "                }\n"
    "            }\n"
    "            for (r in rows) merged[r.day] = r\n"
    "            val arr = JSONArray()\n"
    "            for (r in merged.values) {\n"
    "                arr.put(JSONObject().put(\"day\", r.day).put(\"s\", r.seconds).put(\"f\", r.focusSeconds))\n"
    "            }\n"
    "            StatusSnapshotStore.save(userId, KIND, arr)\n"
    "        }\n"
    "    }\n"
)

EDITS = {
    STUDY / "NativeStatusScreen.kt": [
        # 1. the capped totals, read once for the whole screen
        (
            "    val summary = remember(rows, track) { rows?.let { computeStatus(it, track) } }\n",
            "    val summary = remember(rows, track) { rows?.let { computeStatus(it, track) } }\n"
            "    // U3e: study time + focus of the four cards and of the summary sentence come from study_day_log (each day capped at 8 h)\n"
            "    val cappedTotals = rememberCappedStudyTotals(isLoggedIn)\n",
        ),
        # 2. the summary sentence
        (
            "                                \"Your cumulative study time is ${formatStatusNumber(summary.totalStudyHours)} hours (${summary.totalStudyMinutes} minutes). Your active streak is ${summary.streakDays} days.\",\n",
            "                                cumulativeStudySentence(cappedTotals, summary.streakDays), // U3e\n",
        ),
        # 3. the two places that draw the four cards
        (
            "StHudCards(summary = null, track = track, dailyGoalMinutes = dailyGoalMinutes)",
            "StHudCards(summary = null, track = track, dailyGoalMinutes = dailyGoalMinutes, capped = cappedTotals)",
        ),
        (
            "StHudCards(summary = summary, track = track, dailyGoalMinutes = dailyGoalMinutes)",
            "StHudCards(summary = summary, track = track, dailyGoalMinutes = dailyGoalMinutes, capped = cappedTotals)",
        ),
        # 4. the cards themselves
        (
            "private fun StHudCards(summary: StatusSummary?, track: StatusTrack, dailyGoalMinutes: Int) {",
            "private fun StHudCards(summary: StatusSummary?, track: StatusTrack, dailyGoalMinutes: Int, capped: CappedStudyTotals? = null) {",
        ),
        (
            "    val focus = summary?.focusRatioPct\n",
            "    val focus = capped?.focusPct // U3e: from study_day_log (each day capped); a dash until focus is recorded\n",
        ),
        (
            "        big = if (summary != null) formatStatusNumber(summary.totalStudyHours) else \"—\",\n"
            "        unit = if (summary != null) \"hrs\" else null,\n",
            "        big = if (capped != null) formatStatusNumber(capped.hours) else \"—\", // U3e: capped day-log total\n"
            "        unit = if (capped != null) \"hrs\" else null,\n",
        ),
    ],
    DATA / "StudyDayLogReader.kt": [
        (SAVE_OLD, SAVE_NEW),
    ],
}

if not SRC.exists():
    print(f"ERROR: {SRC} not found; nothing changed"); sys.exit(1)
dest = STUDY / "StatusCappedTotals.kt"

screen = STUDY / "NativeStatusScreen.kt"
if screen.exists() and MARK in screen.read_text(encoding="utf-8"):
    print("already patched"); sys.exit(0)

new_text = {}
for path, edits in EDITS.items():
    if not path.exists():
        print(f"ERROR: {path} not found; nothing changed"); sys.exit(1)
    src = path.read_text(encoding="utf-8")
    for old, new in edits:
        n = src.count(old)
        if n != 1:
            print(f"ERROR: anchor in {path.name} found {n} times (need exactly 1); nothing changed:\n{old}")
            sys.exit(1)
        src = src.replace(old, new)
    new_text[path] = src

dest.write_text(SRC.read_text(encoding="utf-8"), encoding="utf-8")
print(f"created {dest.name}")
for path, src in new_text.items():
    path.write_text(src, encoding="utf-8")
    print(f"patched {path.name}")
print("done")
