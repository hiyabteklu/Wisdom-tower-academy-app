# SESSION 2026-10-20 (admin B3d, Inquiries: compact rows, search, inquiry panel)

> Repo is PUBLIC: no secrets, no student data.

## CI read first
The agent has no tool to read Actions. The owner's Actions screenshot (3:06 pm) showed run #857 (B3b/B3c, commit 8496f70) and #858 (docs commit 29e2d96) both In progress, so B3b/B3c is NOT confirmed compiled. #856 (B3a) and everything before it is green.
This chat re-read `CompetitionsScreen.kt` for the three risk spots named in the B3b/B3c session file. Code read only, not a build:
- `EntryItem.name` is passed as `Text(text = entry.name)` in `EntryRow`, so it is a plain String and `it.name.contains(...)` is fine.
- `EntriesBody` takes `result` as a parameter, so the smart casts on `EntriesResult.Failure` and `.Success` are stable.
- `CompetitionDetailBody` and `EntriesBody` are plain composables called inside the panel's composable Column lambda; their `Row` and `Column` calls give `Modifier.weight` its receiver.
If the next Actions list shows #857 red, fix it before anything else and log it in `docs/BUILD_PITFALLS.md`.

## What this chat did (pushed to `main`)
`ui/admin/InquiriesScreen.kt` (whole file resent). Layout change only: `InquiriesRepository` calls, the status logic, the toast texts and the empty-state texts are unchanged.
- Each inquiry is a compact row (`InquiryRow`: name, e-mail, status badge, two lines of the message, date, arrow). A tap sets `detailId` and opens the inquiry panel (`AdminDetailPanel`, subtitle "Inquiry").
- The panel (`InquiryDetailBody`): status badge and date, e-mail, the Interest line when there is one, and the whole message. The four status buttons (`StatusButtons`, two to a row) are the bar fixed at the bottom. The panel stays open after a status change; the badge and the highlighted button change after the list reloads.
- New search box under the filter chips (`AdminSearchBox`, placeholder "Search by name, e-mail or message"). It narrows the list by name, e-mail, interest or message, on top of the status filter. With no match the text says "No message matches your search" (or "No messages with this status" when the box is empty).
- Removed: `InquiryCard` and the `expandedId` state (replaced by `InquiryRow`, `InquiryDetailBody`, `StatusButtons`, `detailId`). Added import `Icons.AutoMirrored.Filled.KeyboardArrowRight`.
- `AdminToast` and `AdminStatusBadge` stay in this file, untouched, because other screens use them. Not moved to `AdminKit.kt` (a move would add compile risk for no visible gain).
- The toast sits 128 dp from the bottom while the panel is open, so the status bar does not cover it.
- No database changes. `[~]` Not compiled, not tested on a phone.
- Possible compile risks if the build is red: `InquiriesRepository.STATUSES` is used with `.chunked(2)` as before (now inside `StatusButtons`); the new KeyboardArrowRight import; `it.service` read into a local `val service` for the search (the old code already checked `item.service != null`).

## Phone test for the owner
1. Admin > Inquiries: each message is a short row with an arrow; the list is much shorter than before.
2. Type a name or part of an e-mail in the search box = the list narrows. Clear it = the full list is back. Use it together with a status chip (for example New).
3. Tap a message = the panel opens at the top with the whole message and the four status buttons fixed at the bottom. Long messages scroll; the buttons stay visible.
4. Tap a status (for example Read) = the toast shows "Marked read" above the buttons, the badge in the panel changes, and the current status button is highlighted and cannot be tapped again.
5. Back (arrow or System Back) = the list at the same scroll position, with the search text still there; the row shows the new status.

## Next
Read the CI result of #857 and of this commit first. Then B3e Payments (Approve / Reject fixed at the bottom, logic unchanged), B3f Catalog, B3g Users; also A4 on the phone, B2b, C1.
