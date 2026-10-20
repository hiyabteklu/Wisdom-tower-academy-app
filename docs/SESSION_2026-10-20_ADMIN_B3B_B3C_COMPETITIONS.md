# SESSION 2026-10-20 (admin B3b / B3c, Competitions: contest panel, reschedule panel, entries panel)

> Repo is PUBLIC: no secrets, no student data.

## CI read first
Owner's Actions screenshot (3:02 pm): run #856 "Admin B3a (Competitions)" is GREEN (5m 19s), so B3a is compiled. All runs #847 to #856 green.

## What this chat did (pushed to `main`)
`ui/admin/CompetitionsScreen.kt` (whole file resent). Layout change only; every repository call, confirm dialog and text is unchanged.
- B3b: each contest in the list is now a compact row (`CompetitionRow`: state badge, visible/hidden badge, title, start and minutes, arrow). A tap sets `detailId` and opens the contest panel (`AdminDetailPanel`, subtitle "Competition") with the badges, when / ends / points, audience, prize, and the actions two to a row: Show/Hide, Pause/Resume, Start now, Reschedule, Cancel contest, Entries. The panel looks the contest up by id in the loaded list, so after an action (list reloads) it shows the new state.
- B3c reschedule: `Reschedule` opens a small panel over the contest panel (subtitle "Reschedule"): new date, time, duration; `Save new time` fixed at the bottom. `submitReschedule()` unchanged except it sets `rescheduleId = null`.
- B3c entries: `Entries` opens a panel over the contest panel (title "Entries", subtitle = the contest): count "Entries (N)", search box by name, then the ranked entries. Flag details stay inside each entry; Disqualify still opens the reason dialog.
- Removed: `CompetitionCard`, `RescheduleEditor`, `EntriesPanel` (replaced by `CompetitionRow`, `CompetitionDetailBody`, `EntriesBody`). Added import `Icons.AutoMirrored.Filled.KeyboardArrowRight`.
- Draw order in the Box: list, New competition panel, exam picker, contest panel, reschedule panel, entries panel, toast. Later panels win System Back.
- Toast lifted to 88 dp while a panel with a bottom bar is open (New competition without the picker, or Reschedule).
- No database changes. `[~]` Not compiled, not tested on a phone.
- Possible compile risks if the build is red: `CompetitionDetailBody` / `EntriesBody` emit several children into the panel's Column (plain composables, no receiver); `it.name.contains(...)` in `EntriesBody` assumes `EntryItem.name` is a non-null String (it is used as `Text(text = entry.name)`); the new KeyboardArrowRight import.

## Phone test for the owner
1. Admin > Competitions: each contest is a short row with an arrow; the list is much shorter than before.
2. Tap a contest = the contest panel opens at the top with the details and the buttons. Back = list at the same scroll position.
3. In the panel tap Hide or Show = the toast shows and the badge in the panel changes. Pause / Resume / Start now / Cancel contest still ask or act as before.
4. Tap Reschedule = small panel; type a date (2026-10-25) and time (14:30); `Save new time` is always visible at the bottom; after saving, the panel closes and the contest panel shows the new time.
5. Tap Entries = panel with the count and a search box; type a name = the list narrows; tap a flag line = details open inside the entry; Disqualify asks for a reason.
6. System Back closes the top panel only (entries or reschedule first, then the contest panel, then the list).

## Next
Read the CI result of this commit first. Then B3d Inquiries (detail panel + search), B3e Payments (Approve / Reject fixed at the bottom, logic unchanged), B3f Catalog, B3g Users; also A4 on the phone, B2b, C1.
