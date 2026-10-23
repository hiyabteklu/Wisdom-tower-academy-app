# SESSION 2026-10-23: STATUS B4 ("By subject" rows open inline, no popup)

> Repo is PUBLIC: no secrets, no student data. Plan: `docs/MY_STATUS_V2_SUBJECT_BOARD_PLAN.md`. Continues `docs/SESSION_2026-10-22_STATUS_B3.md`.

OWNER ORDER (2026-10-23): the My status board already shows chapters and exam attempts in the page (no popup). The other places that still used the popup card must do the same.

DONE (one commit on `main`, NOT compiled, NOT run on a device):
- `ui/study/StatusSubjectBoard.kt`: the expanded part of the board (items opened, the 2-column stat tiles, chapters list, exam attempts list) moved into a public `StatusSubjectDetailInline(title, breakdown)`. The board calls it; nothing changes on the board itself.
- `ui/study/StatusSubjectsPanel.kt` ("By subject" on Your status): tapping a subject row (or its chevron, which replaces the old gauge icon) opens that subject's details right below the row, using `StatusSubjectDetailInline`. One subject open at a time, tap again to close. The `StatusDetailCard` popup is no longer called from this file.

CHECKED, NOTHING TO CONVERT:
- `StatusSubjectIcon` is on no screen since B1. Its only caller was `ui/packages/OpenCardWithStatus.kt`, which nothing called. So there is no "status icon" popup for the student to see.

CLEANUP (same day, follow-up chat, owner asked to finish what was left): DELETED `ui/packages/OpenCardWithStatus.kt` and `ui/study/StatusSubjectIcon.kt` (code search showed no other caller; only old docs and the old patch scripts under `scripts/` mention them, harmless). NOT touched on purpose: the popup function `StatusDetailCard(...)` inside `StatusDetailCard.kt`. It now has no caller at all, but the file also holds `StatusDetailInline` and the private helpers it needs, and cutting the popup out means rewriting a 22 KB file that has not been compiled. It is dead code with no effect on the student. Optional later slice: remove the popup function plus the imports only it used (`Dialog`, `DialogProperties`, `MaterialTheme`, `Color`, `fillMaxSize`, `heightIn`, `navigationBarsPadding`, `rememberScrollState`, `verticalScroll`, `MutableInteractionSource`, `Close`, `LaunchedEffect`) and `SdSubjectBody`.

TEST (owner, phone, after the green build): Learning > Your status > By subject. Tap a subject: tiles, Chapters and Exam attempts open right below it. Tap a chapter or an exam attempt: its details open in place with a Back arrow; the phone Back button returns to the list. Tap the subject again: it closes. Tap another subject: the first one closes. Also: Learning > any course > My status board still opens and shows chapters/exams inline.
