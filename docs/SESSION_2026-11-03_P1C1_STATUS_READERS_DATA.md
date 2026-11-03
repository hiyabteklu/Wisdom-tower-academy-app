# SESSION 2026-11-03 P1c, part c1: the three status readers get a saved-only read and a short timeout

Plan: `docs/READER_EXAM_FLASHCARD_FIX_PLAN.md` (slice P1c, part c1, owner problem 8). Measured in `docs/SESSION_2026-11-03_P1_MEASURE.md` (item 3).
Files changed (3, data layer only, no screen touched): `app/src/main/java/com/wisdomtower/academy/data/content/StudentProgressReader.kt`, `ExamAttemptsReader.kt`, `StudyDayLogReader.kt`. NOT compiled, NOT tested on a device.

## What changed (same two changes in each reader)
- **Saved-only read, NO network:** `StudentProgressReader.fetchAllSaved()`, `ExamAttemptsReader.fetchAllSaved()`, `StudyDayLogReader.fetchRecentSaved(days = 14)`. Each reads the saved copy (OFFLINE STATUS O1, `StatusSnapshotStore`) of the signed-in student and returns `Success(fromCache = true, savedAtMs = ...)`, or `null`. It never calls the server and never refreshes the sign-in. It runs on the IO dispatcher, so a screen can call it from a normal coroutine. The returned rows are exactly what the normal fallback already returned, so nothing new is shown that the screens could not show before.
- **`null` means "nothing to draw yet":** nobody signed in, no saved file, file unreadable, or the saved list is EMPTY (for the day log: no saved day inside the asked window). An empty copy is treated as "nothing saved" on purpose (same rule as P1a and P1b), so a screen keeps its loading state until the server answers instead of showing "no activity" from an old empty answer.
- **Short timeout when a copy exists:** the normal read (`fetchAll` / `fetchRecent`) now uses `fastHttp` (4 s connect + 8 s read, built from the same client) when a saved copy of the signed-in student exists, and the old 15 s + 20 s client when nothing is saved. When the short read fails, the saved copy is returned as before (`fromCache = true`). The worst wait on a weak connection with a copy drops from about 35 s to about 12 s per reader.

## Not changed
- Result types, the saved file format, `NotSignedIn` (never uses a copy), the 401 retry, the select/query of each read, who may read whose copy (only the SAME student id).
- `AuthStateManager.refreshIfNeeded()` still runs first in `fetchAll` / `fetchRecent`. Hypothesis: it only goes to the network when the sign-in token is close to expiry, so it is usually instant; P1e will measure it if it turns out to wait.
- No screen calls the new functions yet, so nothing is visible on the phone from this commit (that is c2).

## Owner test
Nothing to test yet (no visible change). Everything status related must look and behave as before; c2 makes it visible.

## Next
P1c c2 (screens): "Your status", "By subject", "Today & this week" and the "My status" board above the hubs call `fetchAllSaved()` / `fetchRecentSaved()` first and draw it at once (with the existing dated "from cache" line), then call `fetchAll()` / `fetchRecent()` in the background and swap in the numbers only when they differ; the loading state only shows when the saved read returns `null`. The callers of the readers were NOT read in this session: find them first (search the repo for `StudentProgressReader.fetchAll`, `ExamAttemptsReader.fetchAll`, `StudyDayLogReader.fetchRecent`).
