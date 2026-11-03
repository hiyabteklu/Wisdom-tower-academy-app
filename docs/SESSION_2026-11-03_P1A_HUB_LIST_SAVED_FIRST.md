# SESSION 2026-11-03 P1a: hub lists open at once from the saved list

Plan: `docs/READER_EXAM_FLASHCARD_FIX_PLAN.md` (slice P1a, owner problem 8). Measured in `docs/SESSION_2026-11-03_P1_MEASURE.md`.
Files changed (2): `app/src/main/java/com/wisdomtower/academy/data/content/LearningContentRepository.kt` and `app/src/main/java/com/wisdomtower/academy/ui/study/HubItemListScreen.kt`. NOT compiled, NOT tested on a device.

## What changed
- `LearningContentRepository.listSaved(context, scopePath, hub)`: reads the saved list file of that course and hub (the same `list_<scope>_<hub>_light.json` that `list` writes) with NO network. Returns null when nothing was ever saved, the file cannot be read, or the saved list is empty (an old empty answer is not shown as "nothing published" before the server is asked). `list` itself is unchanged (it already uses the short 4 s + 8 s client when a saved list exists, and the saved-catalog fallback of OFFLINE O5b).
- `HubItemListScreen`: the effect now (1) reads `listSaved` and, if there is one, shows it at once as a normal list (no spinner); (2) calls `list` in the background; (3) replaces the shown list ONLY when the new rows differ (`LearningResource` is a data class, so every field is compared: ids, titles, chapters, counts, file address); (4) if the refresh takes longer than 1.5 s the hub line at the top reads `<Hub name> · Updating…` (muted, same line, nothing moves, nothing blocks); (5) a failed refresh never removes a list already on screen; (6) the full-screen spinner now shows only when nothing was ever saved for this hub; a Retry on a failure card shows the spinner again only when there is no saved list.
- Not touched: the lock rows and lock icons, the checked door (`open_resource`) when an item is opened, sign-in gate, downloads, the Books file-size probe (it re-runs only when the list really changed). A saved list is the server's last real answer; no invented rows.
- Not touched on purpose: the games hubs that also call `list` (`ExamPickerScreen`, `TowerClimbScreen`); they still wait. Sign-in changes: hub list copies are per course and hub, not per student, as before.

## Owner test (on the phone, new build)
1. Open a hub you opened before (for example Notes of any course), with data ON: the list is there at once, no spinner.
2. Same with a weak connection: list appears at once; after a moment the top line may say "Updating…" and then goes back to the hub name; the list does not jump.
3. Open a hub you NEVER opened: a spinner is expected (nothing saved yet).
4. Locked rows still show their lock and still refuse to open.

## Next
P1b (course page counts at once): `LearningContentRepository.kt` new `getHubCountsSaved`, `StudyWorkspaceScreen.kt`.
