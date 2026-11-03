# SESSION 2026-11-03 P1f: the two games hubs draw the saved list at once

Plan: `docs/READER_EXAM_FLASHCARD_FIX_PLAN.md` (slice P1f, owner problem 8). Cause list: `docs/SESSION_2026-11-03_P1E_MEASURE_THE_REST.md`.
Files changed (2): `app/src/main/java/com/wisdomtower/academy/ui/games/defense/ExamPickerScreen.kt` (Tower Defense exam list) and `app/src/main/java/com/wisdomtower/academy/ui/games/climb/TowerClimbScreen.kt` (Tower Climb tower). NOT compiled, NOT tested on a device.

## What changed
The same rule as the course hub lists (P1a), applied to both games hubs:
- On open, `LearningContentRepository.listSaved` (no network) is read first. If a saved list exists the screen draws from it at once: the exam list (Tower Defense) or the tower built from the saved question banks (Tower Climb). No spinner.
- Then `list` runs as before (short client when a saved list exists) and the screen is replaced with the answer. Tower Defense only assigns the list when it differs.
- A failed refresh keeps what is drawn (no error screen over a working list). The spinner ("Lighting the lantern" in Tower Climb) and the error with Retry only show when nothing was saved. An empty saved list counts as nothing saved.
- Retry still reads the server directly (no saved draw), as before.
- Tower Climb: the tower build was the body of the old `Success` branch; it is now the local function `applyItems`, used by the saved draw and the live draw (same code: reads `GameStore.getClimb`, builds with `ClimbTowerBuilder.build`). Coming back from a floor (`onExitToTower`) now also rebuilds from the saved list at once with the new stars, then refreshes, instead of showing the spinner again.

## Not changed
Game rules, scoring, `GameStore`, `ClimbTowerBuilder`, the lock rules (the `locks` passed in are used exactly as before), every other game screen.

## Owner test (phone, new build)
1. Open a course's Tower Defense and Tower Climb after you have opened them once before, data on: the list / tower shows at once, no spinner.
2. Same on a weak connection: shows at once, may change a moment later.
3. Tower Climb: finish a floor and go back to the tower: the new stars show at once, no spinner.
4. A course you never opened, airplane mode: the message with Retry, as before.

## Next
P1g (notifications saved list) only if the owner wants it. Otherwise P1 is finished once the owner has tested the new build.
