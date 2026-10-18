# SESSION 2026-10-18 — ADMIN-native N3 (Payments)

> Repo is PUBLIC: no secrets, no student data. Plan: `docs/ADMIN_NATIVE_PLAN.md` (N3 row). Previous: `docs/SESSION_2026-10-18_ADMIN_NATIVE_N2.md`.

## Audit of the website panel (read, not guessed)
Files read: `PaymentsPanel.tsx`, `lib/orders.ts` (`listOrdersFromDb`, `verifyOrder`, `rejectOrder`, `packageIdsFromOrderNote`, `statusLabel`), `lib/notify.ts`, `app/api/notify-approval/route.ts`, `data/packages.ts` (`formatEtb`).
- List: `orders` newest 200.
- Approve (website): read the order, set `status=verified`, `verified_at`, `verified_by` (admin e-mail); then for each package id (from the order note `packages:id1,id2`, or the order's package id unless it is `multi`) upsert an `enrollments` row (`order_id, package_id, package_name, email, user_id`), conflict key `user_id,package_id` with ignore-duplicates when the order has a user id; then POST `{orderId, type}` to `/api/notify-approval`, which sends e-mail (Resend) and SMS (Africa's Talking or Twilio) from server-side keys. The app needs NO key: it calls the route exactly like the website does.
- Reject: same status change with `rejected`, then the notify route.
- Not carried over: CSV export (needs a file save: later), the in-page receipt lightbox (the receipt opens in the phone's viewer instead, so images and PDFs both work).

## What was pushed (one commit)
1. EDIT `data/admin/AdminSupabase.kt`: added `post()` (insert or upsert with a Prefer header) and `postPublic()` (public website route). `get()` and `patch()` unchanged.
2. NEW `data/admin/OrdersRepository.kt`: `list()`, `verify()`, `reject()`, `notify()`, `packageIdsFor()` (copy of the website function), toast text built like `formatNotifyToast`.
3. NEW `ui/admin/AdminKit.kt`: shared search box, choice chip, tone badge, 44 dp action button, confirm dialog, date text. Reuse from now on.
4. NEW `ui/admin/PaymentsScreen.kt`: header and refresh, two total tiles (pending amount and count, verified revenue and count) plus the total order count, search (id, student, phone, e-mail, ref, package: same fields as the website), status chips (Pending with count, Verified, Rejected, All; Pending is the default like the website), method chips (Any method, Telebirr, CBE, Abyssinia), order cards (id, status badge, package, amount, method, student, phone, e-mail, ref, note, placed date), View Receipt, Approve & Unlock, Reject, confirm dialogs, toast.
5. EDIT `AdminModules.kt` (`payments` now native) and `AdminConsoleHost.kt` (Payments branch; the url path `/admin/payments` opens Payments directly).

## Deliberate differences from the website
- Approve asks for confirmation first. One mistaken tap would unlock paid content and send an SMS, and an approval cannot be undone from the app.
- The status change only applies while the order is still `pending_verification` (filter `status=eq.pending_verification` on the PATCH). A double tap, or a second admin acting first, gives "Not changed: the order is no longer pending, or no permission" instead of silently re-approving.
- A failed enrollment is shown in the toast (the website only logs it), so a verified order without access cannot go unnoticed.
- The order is not re-read before the change (the website did); the list row is used. Hypothesis: harmless because of the pending filter.

## Evidence and limits
- `[~]` Written, NOT compiled, NOT run. Check the Actions runs of N1, N2 and this commit.
- Pitfall check by reading: every Modifier extension imported; `items` imported; no composable call in a LazyColumn builder body outside `item` or `items`; `order.email` and `order.receiptUrl` and `order.note` are `val` properties of a data class in the same module, so the null checks smart-cast (if the compiler disagrees, copy to a local val); `sumOf` on a `Double` property; `AlertDialog` and `TextButton` from material3 with `containerColor`.
- HYPOTHESIS: RLS lets the admin JWT insert into `enrollments` and update `orders` (the website does both with the same login, so it should). A refusal shows as a toast, never as success.
- HYPOTHESIS: `/api/notify-approval` is reachable from the phone at the site origin taken from the admin url (default `https://www.wisdom-tower-academy.live`). If the toast says "Approved, but notify failed", the order IS approved; only the message failed.
- HYPOTHESIS: `enrollments` has a unique constraint on `(user_id, package_id)` (the website relies on it). If not, a retried approval could add a duplicate row.
- Time shown assumes `created_at` is UTC (same as N2).
- Plan status line (plan file not resent): 2026-10-18 | Claude | N3 written `[~]`: Payments native. NEXT: N4 Scholars and Users (audit `UsersPanel`, 28 KB, first).

## What the owner should test (use a TEST order, not a real student's)
1. Admin Console > Payments opens natively; totals and the Pending count match the website; chips and search work.
2. View Receipt opens the slip in the phone's viewer.
3. Approve a test order: confirm dialog, toast "Approved" with the e-mail and SMS result, the order moves to Verified, and the test student sees the package unlocked in My Learning.
4. Reject another test order: confirm dialog, toast "Rejected".
5. Tap Approve twice quickly on one order (or approve it on the website first): second attempt says the order is no longer pending.
6. Airplane mode: "Could not reach the server" with Try again.

## NEXT
N4 Scholars and Users. Audit `UsersPanel` first (28 KB): list, search, detail, edits.
