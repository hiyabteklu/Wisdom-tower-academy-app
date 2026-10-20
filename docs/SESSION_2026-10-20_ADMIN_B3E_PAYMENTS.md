# SESSION 2026-10-20 (admin B3e, Payments: compact rows, order panel, Approve / Reject fixed)

> Repo is PUBLIC: no secrets, no student data.

## CI read first
The agent has no tool to read Actions. The owner's Actions screenshot (3:12 pm) showed: #856 (B3a), #857 (B3b/B3c), #858 (docs) all GREEN (compiled). #859 (B3d, Inquiries, commit 64e215c) was In progress, so B3d is NOT confirmed compiled yet.
A code read of `InquiriesScreen.kt` (the B3d result) in this chat found nothing wrong in the imports (`Spacer`, `width`, `size`, `height`, `clickable`, `KeyboardArrowRight`, `Check` are all imported). Code read only, not a build.
If #859 or the build of this commit is red, fix it before anything else and log it in `docs/BUILD_PITFALLS.md`. Both commits compile together, so a red build can come from either file.

## What this chat did (pushed to `main`)
`ui/admin/PaymentsScreen.kt` (whole file resent). LAYOUT CHANGE ONLY, money logic untouched.
- Each order is a compact row (`OrderRow`): package, student, amount, date, status badge, arrow. Pending rows keep the amber border. A tap sets `detailId` and opens the order panel (`AdminDetailPanel`, subtitle "Order", title = student name).
- The panel (`OrderDetailBody`) has the same facts and wording as the old card, in the same order: status badge, placed date, order id, package, amount and method, student name, phone, e-mail, Ref and note, `View Receipt` (when there is a receipt).
- For a PENDING order, `Approve & Unlock` and `Reject` are the bar fixed at the bottom (`OrderActionButtons`, same labels, same 1.6 : 1 widths, same "Working..." label). Other orders (verified, rejected) have no bottom bar.
- UNCHANGED: `doApprove`, `doReject`, both confirm dialogs (still ask first, same texts), every `OrdersRepository` call, totals, search, status and method chips, loading / error / empty texts.
- One behaviour detail: after a SUCCESSFUL approve or reject the panel closes (the order leaves the Pending list) and the toast shows the result (for example the e-mail / SMS notice). After an ERROR the panel stays open and the toast shows the error above the buttons.
- The toast sits 88 dp from the bottom while a pending order's panel is open, so the buttons do not cover it.
- Removed (replaced): `OrderCard`. `PaymentsMessage` kept as it was (duplicate of the kit message card, tidy in C2).
- New import: `KeyboardArrowRight` (automirrored), plus `clickable`, `Spacer`, `width`. No database change. `[~]` Not compiled, not tested on a phone.
- Possible compile risks if the build is red: the two separate `AdminDetailPanel` call sites (with and without `actions`); smart cast of `detailOrder` (a local `val`) inside the lambdas; the new imports.

## Phone test for the owner (one test order is enough; do NOT approve a real student's payment to test)
1. Admin > Payments: each order is a short row with an arrow (package, student, amount, date, badge). The list is much shorter than before.
2. Search a student name or a transaction ref = the list narrows. Use it with a status chip and a method chip.
3. Tap a pending order far down the list = the panel opens at the top with all the facts; Approve & Unlock and Reject stay visible at the bottom while you scroll. `View Receipt` opens the receipt in the phone's viewer.
4. Tap Reject (or Approve) = the confirm dialog still appears with the same text. Cancel = nothing happens.
5. Back (arrow or System Back) = the list at the same scroll position, search text still there. Tap a Verified order (chip Verified) = panel with the facts and no bottom buttons.

## Next
Read the CI result of #859 (B3d) and of this commit first. Then B3f Catalog (editor in the panel, `RoseTone` to `AdminRose`, list scroll position kept), B3g Users; also A4 on the phone, B2b, C1.
