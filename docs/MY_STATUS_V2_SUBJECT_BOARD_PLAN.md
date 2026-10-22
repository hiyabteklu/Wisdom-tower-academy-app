# MY STATUS V2: SUBJECT BOARD (replaces slice S5 of `docs/MY_STATUS_V2_PLAN.md`)

> Written 2026-10-22 by Claude (chat) from the owner's request and 3 phone screenshots. PLAN ONLY, nothing implemented. Repo is PUBLIC: no secrets, no student data.
> Rules for every slice: `docs/OWNER_RULE_PLAN_FIRST_PUSH_TO_MAIN.md`, `docs/AGENT_QUOTA_PROTOCOL.md`, `docs/EDITING_LARGE_FILES.md` (patch script for big files), `docs/UI_POLISH_TASKS.md` G1 to G9. Owner is not a coder and on the free plan: do every step yourself, one small slice per chat, push to `main`, tell the owner only the exact taps.
> **This file overrides the S5 row of `docs/MY_STATUS_V2_PLAN.md` section 6/8** (that file still says "S5 icon in course lists"; do NOT do that). Do not change the status design (rings, `StatusDetailCard`, colours, numbers): S1 to S4 stay as they are.

## 1. Owner words (2026-10-22)
"The subject level says there is nothing to show yet, since in all hubs it shows the same thing, please remove the status icon from inside the hubs and make it on the subject level. Since it tells us about all hubs. Don't change the status design or how it works. Just remove the status icon from inside hubs and keep it only on subject level and make the icon visible, don't overlay it on the subject card, make it above the hub list as 'My status', an amazing board showing the important gauges in one row, and it has an expand button to see the details."

## 2. What the screenshots showed (owner phone, 9:30, signed in, subject Economics)
- The gauge icon sits at the top right of the one-line hub header in Books / Short Notes / Flashcards / Exams lists; tapping it opens the SAME subject card (Completion 57%, 22 of 24 items opened, Accuracy 29% (28 of 95), Focus 0%, Flashcards known "-", Study 11h51m, Reading 11h48m, Exams 4, best 30%, latest 8%, average 17%, last active 22 Oct 2026). So the data layer and card WORK. It is the subject's total, identical in every hub, so repeating it inside each hub is wrong.
- The icon on the subject CARD (Packages > subject cards) says "Nothing to show yet.".
- In the third screenshot the card's top (title "Economics") overlaps the first list row: a layout glitch, see slice B3.

## 3. Root cause of "Nothing to show yet" on the subject card (verified in code)
`StatusSubjectIcon(scopePath, ...)` looks the subject up by `learning_resources.scope_path`. The hub screens pass `contentScope` (for example `freshman/economics`, built by `contentScopePath(basePath)` in `StudyWorkspaceScreen.kt`), which matches. The package cards (`OpenCardWithStatus`, patch `scripts/patch_status_s5_both.py`) passed `subject.path` (for example `/academy/freshman/economics`, a website URL path), which does NOT match. So the card icon can never find the subject. Fix = do not use `subject.path`; the new board lives inside `StudyWorkspaceScreen`, where `contentScope` already exists.

## 4. Target design
**Where:** `StudyWorkspaceScreen.kt` (the course page that lists the 6 hub cards: Books, Short Notes, Flashcards, Question Banks, Exams, Life Savers). The board is the FIRST item of that page's `LazyColumn`, above the hub grid. Shown only when signed in and `contentScope != null` and not Exit Exam.
**Collapsed (default):** ONE glass card in the existing panel style, title "My status" (the owner's words) on the left, an expand chevron button on the right, and under the title ONE row of 4 small rings (the existing `StatusRing`, same HUD colours: >= 75 emerald, >= 50 amber, below rose, no data = muted ring with a dash): Completion, Accuracy, Focus, Flashcards known, each with its small label. No new numbers, no new design.
**Expanded (chevron):** the detail rows that the card already has (study time, reading time, watch time, exams taken, best / latest / average exam score, last active), plus one button "Chapters and exams" that opens the existing `StatusDetailCard` dialog (chapter and exam views from S4). Default decision (owner may change): expand = inline rows; the dialog is only for chapters/exams.
**Data:** same sources as now (`ResourceCatalogIndex`, `StudentProgressReader`, `ExamAttemptsReader`, `StatusBreakdown.subjectStatus`). Real numbers only (G4). Reload when the student comes back from a hub (time and accuracy change), not on every recomposition.
**Empty and error states:** subject has published items but no progress = rings at 0% / dash (never "Nothing to show yet"); subject has no catalog items at all = board hidden; read failure = the existing `WisdomMessageCard` with Retry (as in `StatusSubjectsPanel`). Day / night colours: the existing `St...` getters (all three looks).

## 5. Slices (one per chat, tick the table in the same commit)
| Slice | What | Files |
|---|---|---|
| **B1 remove icons** | (a) Patch script that reverts the hub header to the original one `Text` (marker `MYSTATUS-S5`), idempotent. (b) Same script reverts the 4 `OpenCardWithStatus(...)` calls in `PackageDetailScreen.kt` back to `OpenCard(...)` (names/args as before: `OpenCard(subject.name, subject.assetImage) {` etc.), idempotent, does nothing if not applied. (c) Delete `ui/packages/OpenCardWithStatus.kt` and `StatusSubjectIcon` call sites; keep `StatusSubjectIcon.kt` only until B2 reuses its load logic. Owner runs the patch (Actions > Apply pending patches > script name > new branch). | `scripts/patch_status_b1_remove_icons.py` (NEW) |
| **B2 board** | NEW `ui/study/StatusSubjectBoard.kt`: `StatusSubjectBoard(scopePath, title)` with the collapsed row, chevron expand, detail rows, "Chapters and exams" button -> `StatusDetailCard`. Move the load logic of `StatusSubjectIcon` into one shared function (`loadSubjectBreakdown(context, scopePath)`) so the board and `StatusSubjectsPanel` use the same code. Reuse `StatusRing`. Compiles on its own. | `StatusSubjectBoard.kt` (NEW) |
| **B3 wire it** | Patch script (single anchor `val hubs = NATIVE_LEARNING_HUBS` in `StudyWorkspaceScreen.kt`): insert `if (loggedIn && contentScope != null && !isExitExam) item(key = "status_board") { StatusSubjectBoard(scopePath = contentScope, title = title) }` before the grid; reload key = `view == null`. Also fix the card top overlapping the first row seen in screenshot 3 (check `StatusDetailCard.kt` dialog header / sheet padding, only if it still happens on the board's dialog). B1 and B3 can share one patch run if the agent has quota. | `scripts/patch_status_b3_board.py` (NEW), small fix in `StatusDetailCard.kt` if needed |

## 6. Owner test (phone, after the green build)
Learning > any course (for example Economics): above the 6 hub cards there is a "My status" board with 4 rings in one row; tap the chevron: details open; tap "Chapters and exams": the card opens. Open Books: NO gauge icon in the hub header any more. Packages > Freshman: no gauge icon on subject cards. Open a hub, study a little, go back: the board numbers update.

## 7. Progress
| Slice | Status |
|---|---|
| Plan (this file) | done 2026-10-22 |
| B1 remove icons | open (next) |
| B2 board | open |
| B3 wire it | open |
| S6 day log (weekday chart, daily-goal ring) | still blocked on owner question B of `MY_STATUS_V2_PLAN.md` |
