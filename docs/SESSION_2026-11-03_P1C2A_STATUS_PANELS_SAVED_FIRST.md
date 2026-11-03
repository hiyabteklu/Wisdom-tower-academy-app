# SESSION 2026-11-03 P1c, part c2a: the course board, "By subject" and "Today & this week" draw the saved copy at once

Plan: `docs/READER_EXAM_FLASHCARD_FIX_PLAN.md` (slice P1c, part c2a, owner problem 8). Data layer from c1: `docs/SESSION_2026-11-03_P1C1_STATUS_READERS_DATA.md`.
Files changed (3, three commits "P1c-c2a 1/3, 2/3, 3/3"): `app/src/main/java/com/wisdomtower/academy/ui/study/StatusDayPanel.kt`, `StatusSubjectsPanel.kt`, `StatusSubjectBoard.kt`. NOT compiled, NOT tested on a device.

## What changed
The rule everywhere: draw the saved copy at once (no network), then read the server in the background and replace what is shown. The amber "No connection to the server..." line is NOT shown for the saved copy that is drawn first (the server has not been asked yet, so nothing is known about the connection); it only appears when the normal read really falls back to the copy, as before. The spinner / empty skeleton only show when nothing usable is saved. A failed refresh keeps what is already drawn.
- **"My status" board above the hubs of a course page** (`StatusSubjectBoard.kt`): new `loadSubjectBreakdownSaved(context, scopePath)` (saved catalog + saved progress + saved exam attempts, no network, null when any is missing or the subject is not in the saved catalog). The first time the board opens (nothing shown yet) it draws that, then runs the normal `loadSubjectBreakdown`. A later reload (coming back from a hub) behaves as before.
- **"By subject"** (`StatusSubjectsPanel.kt`): draws the rows from the saved catalog (`ResourceCatalogIndex.getSaved`) and `ExamAttemptsReader.fetchAllSaved()` when both exist and give at least one row, then reads normally. Failed refresh keeps the rows.
- **"Today & this week"** (`StatusDayPanel.kt`): draws `StudyDayLogReader.fetchRecentSaved(7)` at once. Honesty rule kept: a day after the day the copy was saved is still "unknown" (a dash), so today never shows a made-up 0. While the refresh runs, an unknown today says "Updating today… / Reading your latest study time." instead of "Connect to the internet". New state: `copyAtMs` (when the rows on screen were saved, null = live) and `refreshing`.

## Not changed
The "Your status" screen itself (`NativeStatusScreen.kt`: header, four HUD cards, Standing, recommendations, alerts, counted line) is c2b. `StatusWeekBlock` / `StatusCardData` were not touched. The catalog read inside the normal board/panel read is still `ResourceCatalogIndex.get` (an old copy still waits, that is P1d).

## Owner test (phone, new build)
1. Open a course you opened before, data ON: the "My status" board shows its four rings at once.
2. Same on a weak connection: rings at once, numbers may change a moment later.
3. Open Your status > scroll to "By subject" and "Today & this week": both show at once; no amber "No connection" line while online.
4. Airplane mode: the amber saved-copy lines still appear as before.

## Next
P1c c2b: `NativeStatusScreen.kt` ("Your status" header, HUD cards, week block, recommendations, alerts) draws `StudentProgressReader.fetchAllSaved()` at once, then replaces with `fetchAll()`. Note the screen's `rows` also feed `StatusSubjectsPanel` and `StatusWeekBlock`.
