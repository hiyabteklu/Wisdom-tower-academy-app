# SESSION 2026-10-22 STATUS B2: the "My status" subject board (composable only)

> Repo is PUBLIC: no secrets, no student data. Plan: `docs/MY_STATUS_V2_SUBJECT_BOARD_PLAN.md` (slice B2). Previous: `docs/SESSION_2026-10-22_STATUS_B1.md`.

DONE (one commit on `main`, NOT compiled, NOT run on a device, NOT wired into any screen):
- `ui/study/StatusSubjectBoard.kt` (NEW): `StatusSubjectBoard(scopePath, title, modifier, reloadKey)`. One glass card "My status": expand chevron on the right (the whole header row toggles), ONE row of 4 rings (Completion, Accuracy, Focus, Flashcards known) using the existing `StatusRing` + `statusRingColor`; expanded: items opened, study / reading / watch time, exams taken, best / latest / average, last active, and a "Chapters and exams" button that opens the existing `StatusDetailCard`. Loading = small spinner card; no catalog item for the scope = board hides itself (never "nothing to show"); read failure = message + Retry, and a failed RELOAD keeps the numbers already on screen.
- Also in the file: `loadSubjectBreakdown(context, scopePath): SubjectBoardLoad` (same reads and lookup as `StatusSubjectIcon`). `StatusSubjectsPanel` and `StatusSubjectIcon` were NOT touched (no refactor, to save quota); merge them later only if wanted.
- Because it is a new file nothing changes on the phone until B3 wires it in.

LEFT: B3 (patch `StudyWorkspaceScreen.kt`: one anchor `val hubs = NATIVE_LEARNING_HUBS`, insert `if (loggedIn && contentScope != null && !isExitExam) item(key = "status_board") { StatusSubjectBoard(scopePath = contentScope, title = title, reloadKey = view == null) }` before `openGrid("hubs", hubs)`; the file already has `loggedIn`, `contentScope`, `isExitExam`, `title`, `view` in scope; no new import needed because the board is in the same package `ui.study`). Then the owner runs it from Actions. B1 may be run in the same patch run if the agent has quota.

UNSURE (hypotheses, not verified):
1. Field names copied from `StatusDetailCard.kt`: `totals.{completionPct, accuracyPct, focusPct, flashKnownPct, itemsOpened, itemsTotal, studySeconds, readingSeconds, watchSeconds, lastActiveIso}`, `exams.{attemptsTaken, bestPct, latestPct, averagePct, attempts}`, `chapters`; the first CI run is the proof.
2. `Icons.Filled.KeyboardArrowUp/Down` are in the core icon set (the GPA card already uses them).
3. `animateContentSize()` on the panel gives a smooth expand; if the build complains, remove that one modifier.

TEST (after B3 only): Learning > a course: "My status" board above the hub cards, 4 rings in one row; chevron expands the details; "Chapters and exams" opens the card.
