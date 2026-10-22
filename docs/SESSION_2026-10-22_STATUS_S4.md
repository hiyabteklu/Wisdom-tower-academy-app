# SESSION 2026-10-22 STATUS S4: chapter and exam cards inside the subject card

> Repo is PUBLIC: no secrets, no student data. Plan: `docs/MY_STATUS_V2_PLAN.md`. Rule: `docs/OWNER_RULE_PLAN_FIRST_PUSH_TO_MAIN.md`.

DONE (one commit on `main`, NOT compiled, NOT run on a device):
- `ui/study/StatusDetailCard.kt` rewritten whole (it is my own S2 file, not a forbidden file). One Dialog, three views: Subject (as before) + a CHAPTERS list (mini ring, "X of Y items opened", tap) + an EXAM ATTEMPTS list (title, date, score of total, %, tap; latest 8, says so when there are more); Chapter view (hero ring, 3 small rings, time rows, BY SECTION per hub: opened X of Y, time, % correct); Exam view (score ring, date, time taken, against previous attempt, best score on this exam, attempts of this exam). Back arrow / phone Back goes chapter or exam -> subject -> closes; the scrim and X close everything. The data functions of S1 are unchanged (`chapterStatus`, `examStatus` were already there).
- Labels: chapter = "Chapter N" or "General" (no chapter titles exist in the data, none invented); hub names are the permanent hub names (Books, Short Notes, Flashcards, Question Banks, Exams, Life Savers, plus Videos).
- `docs/MY_STATUS_V2_PLAN.md`: S3 and S4 ticked (S3 was done by the earlier chat but its table row was never ticked: correction here).

LEFT: S5 (same gauge icon on subject cards / hub rows in the course lists, patch protocol for big files). S6 only after the owner says yes (question B in the plan).

TEST (phone, after a green build): Learning > Your status > By subject > tap a subject: below the time rows you see CHAPTERS and (if you took exams) EXAM ATTEMPTS. Tap a chapter: its own card with rings and one line per section; back arrow returns. Tap an exam attempt: score ring, time taken, change against your previous try. Phone Back steps back once, then closes.

UNSURE (hypotheses, not verified):
1. `Icons.AutoMirrored.Filled.ArrowBack` / `KeyboardArrowRight` need the automirrored icon package (already used elsewhere in the app: `AutoMirrored.ArrowForward`).
2. `StatusRing` at 44.dp / stroke 4.dp / 11sp text may look tight with "100%"; if so, lower `textSize` in the chapter row.
3. Attempts with no `meta.total` show a dash for score and %; real, not estimated.
