# SESSION 2026-10-23: STATUS B4 ("By subject" rows open inline, no popup)

> Repo is PUBLIC: no secrets, no student data. Plan: `docs/MY_STATUS_V2_SUBJECT_BOARD_PLAN.md`. Continues `docs/SESSION_2026-10-22_STATUS_B3.md`.

OWNER ORDER (2026-10-23): the My status board already shows chapters and exam attempts in the page (no popup). The other places that still used the popup card must do the same.

DONE (one commit on `main`, NOT compiled, NOT run on a device):
- `ui/study/StatusSubjectBoard.kt`: the expanded part of the board (items opened, the 2-column stat tiles, chapters list, exam attempts list) moved into a public `StatusSubjectDetailInline(title, breakdown)`. The board calls it; nothing changes on the board itself.
- `ui/study/StatusSubjectsPanel.kt` ("By subject" on Your status): tapping a subject row (or its chevron, which replaces the old gauge icon) opens that subject's details right below the row, using `StatusSubjectDetailInline`. One subject open at a time, tap again to close. The `StatusDetailCard` popup is no longer called from this file.

CHECKED, NOTHING TO CONVERT:
- `StatusSubjectIcon` is on no screen since B1. Its only caller is `ui/packages/OpenCardWithStatus.kt`, which nothing calls. So there is no "status icon" popup for the student to see.
- `StatusDetailCard` (the popup) now has only that unused icon as a caller. Owner decision, not done: delete `StatusSubjectIcon.kt`, `ui/packages/OpenCardWithStatus.kt` and the popup `StatusDetailCard` function together (keep the helper functions and `StatusDetailInline` that live in `StatusDetailCard.kt`).

TEST (owner, phone, after the green build): Learning > Your status > By subject. Tap a subject: tiles, Chapters and Exam attempts open right below it. Tap a chapter or an exam attempt: its details open in place with a Back arrow; the phone Back button returns to the list. Tap the subject again: it closes. Tap another subject: the first one closes.
