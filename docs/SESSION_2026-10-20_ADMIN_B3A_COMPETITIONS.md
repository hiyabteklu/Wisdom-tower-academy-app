# SESSION 2026-10-20 (admin B3a, Competitions: create form and exam picker as panels)

> Repo is PUBLIC: no secrets, no student data.

## What this chat did (pushed to `main`)
- Read the status first. CI result for B1b was read from the owner's Actions screenshot: run #851 ("Admin B1b (Grants)") is GREEN, so B1a (#849, #850) and B1b (#851) are all compiled. Runs #854 and #855 (docs) green; #853 (docs, B2 audit) was still in progress when the screenshot was taken. No red build, nothing to fix first.
- B3a: `ui/admin/CompetitionsScreen.kt` rewritten (whole file resent). Layout change only.
  - The "Create a Competition" card keeps its intro text and the `New competition` button.
  - A tap sets `createOpen = true` and opens the `AdminDetailPanel` "New competition" (subtitle "Competition") with the 12 fields; `Create competition` is the bar fixed at the bottom.
  - The "Exam from the exam hub" field is a button; a tap opens a SECOND panel "Choose an exam" (search box + exam rows, still the first 30). Tap an exam = it is selected and only the picker closes.
  - The picker panel is drawn after the create panel, so System Back closes the picker first, then the form.
  - `createOpen` and every form field stay `rememberSaveable`; the list and the typed form stay composed underneath.
  - `submitCreate()`, `createError()`, `doCreate()`, the `confirmCreate` dialog and every `CompetitionsRepository` call are unchanged (only `pickerOpen = false` added on success). The old `Close` button became the panel's Back arrow.
  - New small helper `CompFieldBlock` (label + field close together); import `ColumnScope` added.
  - The toast is lifted to `bottom = 88.dp` while the create panel with its bottom bar is open (not while the picker is open).
  - NOT touched: contest actions, reschedule, entries, flags (B3b, B3c); `CompOrange` literal (C1 finding).
- No database changes.
- `[~]` Not compiled, not tested on a phone. Commit message ends with `not compiled`.

## Phone test for the owner
1. Admin > Competitions > tap `New competition` = a full panel opens at the top with the form; `Create competition` is always visible at the bottom.
2. Tap the exam field = a second panel opens with a search box; type a few letters = the list narrows; tap one exam = back to the form, that exam shown, the other fields still filled.
3. Back (arrow or phone Back) closes the exam picker first, the second Back closes the form and shows the list. Reopen the form = typed text is still there.
4. Leave the exam empty and tap `Create competition` = the message "Please choose an exam from the exam hub." shows above the button.
5. Create a real (hidden) competition = the panel closes, the toast says it was created as hidden, it appears in the list.

## Next
B3b / B3c (Competitions rest: compact contest row + detail panel with the actions; reschedule as a small panel with Save fixed; entries as a panel with a search box and a count). First read the CI result of this B3a commit.
