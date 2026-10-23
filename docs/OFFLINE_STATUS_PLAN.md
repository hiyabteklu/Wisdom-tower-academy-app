# OFFLINE STATUS PLAN (owner order 2026-10-23)

> Repo is PUBLIC: no secrets, no student data. App only (website repo is not touched).
> Rules in force: `HANDOFF.md` section 0, `docs/OWNER_RULE_PLAN_FIRST_PUSH_TO_MAIN.md`, G1 to G9 of `docs/UI_POLISH_TASKS.md`, FORBIDDEN FILES (HANDOFF rule 12). One slice per chat, push each slice to `main`, tick it below in the same commit. The owner says "Continue" for the next slice.

## 1. Owner words (2026-10-23)
"Look at my status on the learning section, also the status gauges and details on every hub (inside each subject). Can't we make the skeleton offline, because currently it doesn't show while offline. The skeleton and all the screens should be accessible offline. Save the latest sync and show empty on first open (or a notice to connect to the internet to draw his accurate status)."

## 2. What was read (code read, NOT run on a phone)
The three status screens and where their numbers come from:

| Screen (file) | Reads | Offline today |
|---|---|---|
| "Your status" (`ui/study/NativeStatusScreen.kt`) | `StudentProgressReader.fetchAll()` (table `learning_progress`), `ResourceScopeIndex` | Read fails, `failed = true`, only an error card with Retry. No rings, no cards. |
| "By subject" panel (`StatusSubjectsPanel.kt`) and "today + 7 days" (`StatusDayPanel.kt`) | `ResourceCatalogIndex` (cached), `ExamAttemptsReader.fetchAll()`, `StudyDayLogReader.fetchRecent()` | `failed` as soon as the exam read fails, even when the catalog is cached. Error card. |
| "My status" board above the hubs of a course (`StatusSubjectBoard.kt`, `loadSubjectBreakdown`, wired in `StudyWorkspaceScreen.kt`) | catalog + `StudentProgressReader` + `ExamAttemptsReader` | `SubjectBoardLoad.Failed` shows "Could not load your status" with Retry. No gauges. |

ROOT CAUSE: `StudentProgressReader`, `ExamAttemptsReader` and `StudyDayLogReader` keep NO copy on the phone. Only the catalog (`ResourceCatalogIndex`, 24 h, older copy used offline) and the scope map (`ResourceScopeIndex`) are saved. So every status screen needs the network for its first frame. Each screen also treats a failed read as "show an error instead of the layout".
Already fine offline: the learning screen, course cards, hub cards (packed), hub counts (`getHubCounts` saves a copy), items the student downloaded or opened before (`LearningContentRepository.list` / `getResource` keep copies), the sign-in session (a network failure does not sign the student out, see `AuthStateManager.refreshIfNeeded`).

## 3. Design (owner decisions applied)
1. The screens and their skeleton ALWAYS draw: rings, HUD cards, subject rows, board. A failed read never replaces the layout with an error card.
2. Every successful read is saved on the phone for that student (the "latest sync"), with the time it was saved.
3. Offline or read failed, with a saved copy of the SAME student: draw the layout from the saved copy and say so in one line: "Offline. Showing your status from 23 Oct 2026, 14:05. Connect to the internet to update."
4. Offline, nothing saved yet (first open): draw the same layout EMPTY (dashes, empty rings) and one notice: "Connect to the internet once to load your real status." Never zeros that look real.
5. REAL DATA ONLY (owner rule 2026-10-11, G4): a saved number is the server's last real answer and is labelled with its date. An unmeasured figure stays a dash. Do NOT run `computeStatus` on an empty list to fill the skeleton: it would print "0 hours", "0-day streak" and the alert "Zero Study Sessions Logged", which is a claim, not a fact. Empty skeleton = summary is null, cards show dashes, the alerts panel is hidden, recommendations (fixed texts) may stay.
6. A different student never sees another student's copy: files are named with the user id and are used only when the id matches; saving for one student deletes saved copies of other ids.
7. Online behaves exactly as today (same texts, same layout, same colours, day and night mode).

## 4. Slices (one per chat)
- [x] **O0** This plan.
- [ ] **O1 Data layer.** New file `data/content/StatusSnapshotStore.kt`: `save(userId, kind, json)` and `load(userId, kind)` returning the text and the saved time, files in `filesDir/status_snapshots/`, kinds `progress`, `exams`, `daylog`. Use `WisdomTowerApplication.instance` for the context (it exists), so no caller changes signature. In `StudentProgressReader.kt`, `ExamAttemptsReader.kt`, `StudyDayLogReader.kt`: after a good read, save the rows; on a failed read (not `NotSignedIn`) with a saved copy of the same user, return `Success` with two new fields that have defaults, `fromCache: Boolean = false` and `savedAtMs: Long? = null`, so every existing `Success(rows)` call and `is Success` check still compiles. No screen changes in this slice. Never log bodies or tokens.
- [ ] **O2 "Your status" screen** (`NativeStatusScreen.kt`, about 35 KB: resend whole, or a patch script if the tool cannot carry it, `docs/EDITING_LARGE_FILES.md`). States: online (unchanged), offline with copy (cards from the copy plus the "Offline. Showing ..." line), offline without copy (empty skeleton plus the notice, section 3 rule 5). The error card stays only for a real server error with nothing saved. Retry stays.
- [ ] **O3 "By subject" and day panels** (`StatusSubjectsPanel.kt`, `StatusDayPanel.kt`). Use the saved exam attempts and day log. With no catalog saved: rows from the packed subject list (`MySubjectsStore.packages`, names are in the app) with empty rings and dashes instead of "x of y items opened". Day panel: empty ring and empty bars plus the same notice.
- [ ] **O4 Course board** (`StatusSubjectBoard.kt`, `loadSubjectBreakdown`). `Failed` no longer shows the error card: with a copy, the four rings from the copy plus the "from <date>" line; without a copy, the four EMPTY rings (Completion, Accuracy, Focus, Flashcards known) plus "Connect to the internet to see your status". When the catalog was never saved the board still draws (empty) instead of hiding. The expanded rows and the "Chapters and exams" card use the copy; with no copy the button is hidden.
- [ ] **O5 Polish (optional, only if the owner asks).** (a) Redraw by itself when the internet comes back (connectivity callback, then reload once). (b) Hub item list for a hub never opened before: build the rows from `ResourceCatalogIndex` (title, chapter, count) marked "Connect to open" unless the item is saved. Today such a hub shows the "Check your internet connection" card. (c) "Last synced" line in Settings.

## 5. Not decided / not checked (hypothesis, say it plainly)
- Whether study time and quiz results done OFFLINE are queued and sent later (`LearningProgressRepository.kt` was not read). If they are not, offline study is not counted until the student studies again online. Check before O2 and write the answer here.
- `StatusDayPanel.kt`, `StatusBreakdown.kt`, `StatusDetailCard.kt` were not read in full while planning; read them in the slice that touches them.
- A saved copy can be old: progress made on another device shows only after the next online read. Said in the notice line (date shown).
- Size: 1000 progress rows is about 200 KB of JSON; fine for app storage.

## 6. Phone test (after each slice)
1. Online: open Learning, Your status, a course page. Everything looks and reads as before.
2. Turn on airplane mode, force-stop the app, reopen, open the same three screens: layout is there with the saved numbers and the "Offline. Showing ..." line.
3. Clear the app data (or install fresh), sign in online, open nothing, go to airplane mode, open Your status and a course page: empty skeleton with the connect notice, no error card, no invented zeros.
4. Sign in as another student: the first student's numbers do not appear.
5. Turn the internet back on and tap Retry: real numbers replace the saved ones.

## 7. Status log (append only)
- 2026-10-23 O0: plan written from the code (files above). Nothing compiled, nothing run. Next: O1.
