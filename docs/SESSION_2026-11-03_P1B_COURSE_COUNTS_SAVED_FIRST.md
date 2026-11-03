# SESSION 2026-11-03 P1b: course page counts at once from the saved counts

Plan: `docs/READER_EXAM_FLASHCARD_FIX_PLAN.md` (slice P1b, owner problem 8). Measured in `docs/SESSION_2026-11-03_P1_MEASURE.md` (item 2).
Files changed (2): `app/src/main/java/com/wisdomtower/academy/data/content/LearningContentRepository.kt` and `app/src/main/java/com/wisdomtower/academy/ui/study/StudyWorkspaceScreen.kt`. NOT compiled, NOT tested on a device.

## What changed
- `LearningContentRepository.getHubCountsSaved(context, scopePath)`: reads the saved counts file of that course (the same `counts_<scope>.json` that `getHubCounts` writes) with NO network. Returns null when nothing was saved, the file cannot be read, or the saved map is empty. `getHubCounts` itself is unchanged (it already uses the short 4 s + 8 s client).
- `StudyWorkspaceScreen`: the effect reads the saved counts first and puts them on the hub cards at once; then it calls `getHubCounts` in the background and updates the cards only when the numbers really differ. An empty answer (failed read, nothing saved) never wipes counts that are already shown.
- Not touched: lock gates on the cards, the "My status" board above the hubs (that is P1c), the ECE second request (still up to about 24 s in the background when the first answer is empty, but the cards no longer wait for it).

## Owner test (on the phone, new build)
1. Open a course you opened before, with data ON: the cards show their counts ("12 notes" ...) at once.
2. Same on a weak connection: counts at once; if a number changed on the server it changes a moment later.
3. A course never opened: counts may appear a little later (nothing saved yet), as before.

## Next
P1c (status readers saved copy first): c1 data layer (`StudentProgressReader`, `ExamAttemptsReader`, `StudyDayLogReader`), then c2 screens.
