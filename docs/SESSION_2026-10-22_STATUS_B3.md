# SESSION 2026-10-22 STATUS B3: wire the board (one patch run does B1 + B3)

> Repo is PUBLIC: no secrets, no student data. Plan: `docs/MY_STATUS_V2_SUBJECT_BOARD_PLAN.md`. Previous: `_STATUS_B1.md`, `_STATUS_B2.md`.

DONE (one commit on `main`, NOT run on the real repo):
- `scripts/patch_status_b3_board.py` (NEW, tested on a synthetic snippet, safe to run twice): first runs the B1 script (removes the gauge icons), then inserts the `MYSTATUS-B3` block as the first item of the course page `LazyColumn` in `StudyWorkspaceScreen.kt`, right before `val hubs = NATIVE_LEARNING_HUBS`. `loggedIn`, `contentScope`, `isExitExam` and `title` already exist there; `StatusSubjectBoard` is in the same package (no import). No `reloadKey` is passed: the course page leaves composition while a hub is open, so the board reads fresh numbers every time the student comes back.
- So the owner runs ONE script for B1 + B3: **`scripts/patch_status_b3_board.py`**. Do not run the B1 script separately and do not run the old S5 scripts.

PATCH (needs the owner): GitHub > Actions > **Apply pending patches** > Run workflow > script `scripts/patch_status_b3_board.py` + new branch name `feat/status-board` > wait for the green build > test > merge with a Pull request.

LEFT: the table in `docs/MY_STATUS_V2_SUBJECT_BOARD_PLAN.md` section 7 still says B1, B2, B3 open: tick them after the merge. Optional later: delete the unused `OpenCardWithStatus.kt`; if the dialog title overlap (plan section 2) still shows, fix it in `StatusDetailCard.kt`.

TEST (phone, after the merge): Learning > a course (for example Economics): "My status" board above the hub cards: 4 rings in one row; the chevron expands the details; "Chapters and exams" opens the card. Open Books: no gauge icon in the header. Packages > Freshman: no icon on the subject cards. Go into a hub, study a little, go back: the board shows the new numbers.
