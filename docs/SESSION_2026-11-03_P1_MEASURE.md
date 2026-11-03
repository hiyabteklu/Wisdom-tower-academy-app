# SESSION 2026-11-03 P1 (measure first): which screens wait for the network, and the sub-slices

Plan: `docs/READER_EXAM_FLASHCARD_FIX_PLAN.md` (slice P1, owner problem 8). Read first: `OFFLINE_STATUS_PLAN.md` (the status screens already draw a saved copy when the server cannot be reached), `LearningContentRepository.kt`, `ResourceCatalogIndex.kt`, `StudentProgressReader.kt`, `HubItemListScreen.kt`, `StudyWorkspaceScreen.kt`.
No code changed in this commit: it is the measurement and the split, as the plan asked.

## The common cause (read from the code)
Every reader is NETWORK FIRST: it asks the server, and only when that FAILS (after its timeout) does it use the saved copy. Offline the failure is instant (no connection), so the saved copy shows at once. On a slow or weak connection the request hangs until the timeout, so the student waits with a spinner, or with missing numbers, even though a saved copy is already on the phone. That is exactly "better offline than online".

## Measured list (what waits, and for how long at worst)
1. **Hub list** (`HubItemListScreen` -> `LearningContentRepository.list`). The screen sets `result = null` (full spinner) and then waits for `list`. With a saved list file the client is `fastHttp` (4 s connect + 8 s read, about 12 s worst); with no saved file it is `http` (15 s + 30 s). The saved list is only used AFTER the failure. Notes, flashcards, question banks, exams, life savers and books all go through this.
2. **Hub counts on the course page** (`StudyWorkspaceScreen` -> `getHubCounts`). The hub cards draw at once (no spinner) but WITHOUT their counts (`hubCounts` starts empty) until the request ends: up to 12 s, and an ECE course with an empty first answer makes a second request (up to about 24 s). The saved counts are only used after the failure.
3. **Status readers** (`StudentProgressReader` 15 s + 20 s, plus `AuthStateManager.refreshIfNeeded()` first; `ExamAttemptsReader` and `StudyDayLogReader` were NOT read in this session, hypothesis: same pattern). Used by "Your status", "By subject", "Today & this week" and the "My status" board above the hubs of every course page. The saved copy (OFFLINE STATUS O1) is only used after the failure, so on a weak network these show their loading state for up to about 35 s.
4. **Catalog** (`ResourceCatalogIndex.get`). A copy younger than 24 h is returned at once. An OLDER copy is not: it waits for the full catalog read (paged, 15 s + 20 s per page) and only then falls back to the old copy. Used by the status panels and the board.
5. **Opening one item** (`getResource`): ALREADY cache-first (a saved item opens at once and refreshes quietly). A never-opened item needs the network (15 s + 30 s), which cannot be avoided; nothing to change.
6. **Not read yet (to measure in P1e):** `ContentAccess.resolveLocks` (locks and ownership, used by the course page and the hub list; the page starts with `staticLocks` so the cards draw at once, but how long the real answer takes and whether a hub tap waits for it was not read), the Learning tab, Home, the package screens, Notifications, the Games hubs (`ExamPickerScreen`, `TowerClimbScreen` also call `list`, so P1a helps them too).

## Design (same for every fix)
- Saved copy FIRST, drawn at once; ask the server in the background with a SHORT timeout when a copy exists (4 s + 8 s is already the `fastHttp` pair); replace the screen only when the new data DIFFERS from what is shown (no flicker, no jump).
- A full-screen spinner only when nothing is saved. When a saved copy is shown while the refresh runs, at most one small muted "Updating…" line, never blocking.
- Real data only: a saved copy is the server's last real answer; no invented numbers. Locked content is never shown because of a copy: the checked door (`open_resource`) and the lock rows still decide when an item is opened (SECURITY_PLAN_CONTENT_ACCESS.md).
- Online with a fast connection looks the same as today (the refresh just finishes before anyone notices).
- Sign-in changes: a copy is only used for the same signed-in state as today (hub list copies are per course and hub, not per student, as today).

## Sub-slices (also written in the plan)
- **P1a Hub list opens at once** (`LearningContentRepository.kt` new `listSaved`, `HubItemListScreen.kt`). Biggest felt win: every hub list.
- **P1b Course page counts at once** (`LearningContentRepository.kt` new `getHubCountsSaved`, `StudyWorkspaceScreen.kt`).
- **P1c Status readers: saved copy first** (c1: data layer, `StudentProgressReader`, `ExamAttemptsReader`, `StudyDayLogReader` get a saved-only read and a short timeout when a copy exists; c2: the four status screens and the board draw the copy at once and replace it when the refresh differs).
- **P1d Catalog: old copy first** (`ResourceCatalogIndex.get`: an older copy is returned at once and refreshed in the background; short timeout when a copy exists).
- **P1e Measure the rest** (locks, Learning tab, Home, packages, Notifications, Games hubs), write the findings here and split further only if something waits.

## Next
P1a.
