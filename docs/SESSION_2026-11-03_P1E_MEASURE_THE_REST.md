# SESSION 2026-11-03 P1e: what else still waits for the network (measured by reading the code, no code changed)

Plan: `docs/READER_EXAM_FLASHCARD_FIX_PLAN.md` (slice P1e, owner problem 8). Earlier list: `docs/SESSION_2026-11-03_P1_MEASURE.md`.
No code was changed in this slice. Everything below was read from the files named; nothing was run on a phone.

## Result in one line
Only the two games hubs still show a spinner while the server answers. Everything else on the list already opens at once (or is not a network screen). Two new sub-slices are added: P1f (games hubs) and P1g (notifications, hypothesis).

## What was checked
1. **`ContentAccess.resolveLocks` (locks and ownership): does NOT make a hub tap wait.** `StudyWorkspaceScreen.kt` starts with `ContentAccess.staticLocks` (saved answers from memory, then from the phone, then the static rule), so the hub cards draw and can be tapped at once. The real answer is read in a background effect and replaces `locks` when it arrives. A hub tap checks the `locks` already in memory, then starts another background read (cached 30 s). The network client in `ContentAccess` is short (6 s connect, 8 s read, 10 s for the whole call) and a failure reuses the last saved answer, so it never holds the screen and never closes an open course. Side note: `AuthStateManager.refreshIfNeeded()` runs inside that background read only. Result: no change needed.
   - Known limit, on purpose (security rule): on the very first open of a paid course with NOTHING saved (new install, never online with this student), the static rule applies until the answer arrives, so a tap in the first second can show "Package required". It is not a speed problem and is not changed.
2. **Learning tab (`LearningScreen.kt`): no network read.** The only effect in the file reacts to a calculator request. It shows the packed tool and course cards. Result: nothing waits.
3. **Home (`HomeScreen.kt`): no network read.** The only effect is the greeting animation. Result: nothing waits (hypothesis: the greeting name comes from the sign-in state, which is already on the phone).
4. **Package screens (`ui/packages/*`): no effect that loads anything.** The course lists are packed data (`PackagesData.kt`, `EntranceData.kt`). Prices are read by `PackagePrices.refresh()` from the course page and only fill the price tag when they arrive (not read in detail, hypothesis: non-blocking, as on the course page). Result: nothing waits.
5. **Notifications (`NotificationsScreen.kt`, `NotificationRepository.kt`): partly.** The list is drawn from `NotificationRepository.notifications` (memory), refreshed on every open; the skeleton shows only while the list is empty. Once the list is in memory a reopen is instant. BUT the phone saves only the read ids (`read_ids` in SharedPreferences), not the list itself (hypothesis, from the imports and the fields; the whole repository file was not read). So after the app process was closed, the first open shows the skeleton until the server answers. Candidate P1g.
6. **Games hubs: YES, they wait.** `ExamPickerScreen.kt` (Tower Defense exam list) and `TowerClimbScreen.kt` (Tower Climb question banks) set `loading = true` and call `LearningContentRepository.list(...)` (network first). With a saved list the short client (4 s + 8 s) is used, so a weak connection still holds the spinner for up to about 12 s while a saved list sits on the phone. P1a added `listSaved` (no network) but did not touch these two screens on purpose. Candidate P1f.

## New sub-slices (added to the plan)
- **P1f Games hubs saved list first:** `ExamPickerScreen.kt` and `TowerClimbScreen.kt` draw `LearningContentRepository.listSaved` at once (same rule as P1a: an empty saved list counts as nothing saved), then call `list` in the background and replace only when the rows differ; a failed refresh keeps the saved list; the spinner only when nothing is saved. Tower Climb also reads `GameStore.getClimb` after the list: keep that step for both draws.
- **P1g Notifications saved list first (only if the owner wants it):** save the last notification list on the phone (per student) and draw it at once on a cold start. Needs a read of the whole `NotificationRepository.kt` first to confirm the hypothesis. Lower priority: a reopen with the app still alive is already instant.

## Owner test
Nothing to test for this slice (no code changed).

## Next
P1f (games hubs).
