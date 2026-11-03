# SESSION 2026-11-03 P1c, part c2b: "Your status" draws the saved progress at once

Plan: `docs/READER_EXAM_FLASHCARD_FIX_PLAN.md` (slice P1c, part c2b, owner problem 8). Earlier parts: `docs/SESSION_2026-11-03_P1C1_STATUS_READERS_DATA.md` (data layer), `docs/SESSION_2026-11-03_P1C2A_STATUS_PANELS_SAVED_FIRST.md` (board and two panels).
File changed (1): `app/src/main/java/com/wisdomtower/academy/ui/study/NativeStatusScreen.kt`. NOT compiled, NOT tested on a device. Only the loading effect and the file's doc comment changed; the layout, texts, colours and every panel are untouched.

## What changed
- The loading effect first reads `StudentProgressReader.fetchAllSaved()` (no network) and, if there is a saved copy, draws the whole screen from it at once: header, Standing, the four HUD cards, week block, recommendations and alerts all come from the same `computeStatus` as a live read, and the rows go through the same T8 "My Subjects" filter. The "By subject" and "Today & this week" panels (c2a) get those rows too.
- No amber "No connection" line on that first draw (the server has not been asked yet). Then `fetchAll()` runs in the background and replaces the numbers; equal numbers change nothing on screen.
- The T8 filter step was the body of the old `Success` branch. It is now the local function `applyRows(result)` inside the effect, used by both the saved draw and the live draw (same code, no new logic). It returns false, changing nothing, when the subject list is needed but cannot be loaded. For the saved draw that means "nothing drawn yet, keep the spinner"; for the live read it is the old "Could not load your subject list" message, shown only when no saved copy was drawn.
- A failed refresh keeps the saved numbers on screen. The amber saved-copy line and the empty skeleton behave as before (the line only shows when the normal read really falls back to the copy; the skeleton only when nothing usable is saved).

## Not changed
`StatusWeekBlock`, `StatusCardData`, `StatusCompetitionPanel` (it keeps its own saved copy), `computeStatus`, the data layer. `ResourceScopeIndex.get` (needed by the T8 filter) is read as before; if its copy is missing the saved draw waits for the live read.

## Owner test (phone, new build)
1. Open Your status with data ON after you have opened it once before: the four cards, Standing, week sentences and the panels below show at once, no spinner.
2. Same on a weak connection: shows at once, a number may change a moment later.
3. Airplane mode: the amber "as last saved on ..." line still appears at the top as before.
4. First open ever with no connection: the notice with Retry and the empty skeleton, as before.

## Next
P1d (`ResourceCatalogIndex.get`: an old copy is returned at once and refreshed in the background, short timeout when a copy exists), then P1e (measure the rest).
