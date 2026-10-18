# SESSION 2026-10-18 — ADMIN-native N2 (Inquiries, first fully native module)

> Repo is PUBLIC: no secrets, no student data. Plan: `docs/ADMIN_NATIVE_PLAN.md` (N2 row). Previous: `docs/SESSION_2026-10-18_ADMIN_NATIVE_N1.md`.

## Audit of the website panel (read, not guessed)
`website-reference/src/components/admin/InquiriesPanel.tsx` and `src/lib/admin-data.ts` (`listInquiries`, `updateInquiryStatus`):
- Pure Supabase, NO API route: `select *` from `inquiries` ordered by `created_at` descending, limit 200; `update inquiries set status = ? where id = ?`. Statuses: new, read, replied, archived. `admin_notes` exists in the table but the panel never edits it, so the app does not either.
- Shown per row: name, email, status, optional `service` as "Interest: ...", message, created date. Empty state texts copied exactly ("Inbox empty", "Messages from the Contact page land in the inquiries table."). Header "Contact inquiries" and "N new \u00B7 N total".

## What was pushed (one commit)
1. NEW `data/admin/AdminSupabase.kt`: ONE shared helper for all native admin modules (GET and PATCH on `/rest/v1/`, admin's own token, refresh and one retry on 401, honest error text). PATCH asks for the changed rows back (`Prefer: return=representation`) so an RLS refusal that returns HTTP 200 with zero rows is caught.
2. NEW `data/admin/InquiriesRepository.kt`: `list()` and `setStatus()`; zero changed rows is reported as a failure, never as success.
3. NEW `ui/admin/InquiriesScreen.kt`: header with refresh, five filter chips that share the width (All, New, Read, Replied, Archived; the filter is the only addition to the website panel), cards with name, email, status badge, interest, message (3 lines, full when opened), date in the phone's time zone; tap a card to show the four status buttons (2 by 2, 44 dp high); toast bar at the bottom; error card with Try again. Also defines `AdminStatusBadge` and `AdminToast`, which N3 onwards reuse.
4. EDIT `ui/admin/AdminModules.kt`: `NATIVE_ADMIN_MODULE_IDS = setOf("inquiries")`.
5. EDIT `ui/admin/AdminConsoleHost.kt`: Inquiries branch in the `when`; Back inside a native module returns to the grid.

## Evidence and limits
- `[~]` Written, NOT compiled, NOT run. N1 build result was NOT checked by me (no access to Actions from this chat): owner please confirm the run of commit `a09bf09` was green before trusting this one.
- Pitfall check by reading: all Modifier extensions imported (`background, border, clickable, clip, alpha, padding, size, height, width, fillMaxSize, fillMaxWidth`); `items` imported; no `@Composable` call in a LazyColumn builder body outside `item` or `items` lambdas; no trailing lambda on a function whose last parameter is not the lambda (every `modifier` parameter is last and always passed by name or position as a Modifier, lambdas are passed by name); no deprecated ripple; new icons `Check`, `Inbox`, `Refresh` (all already used or core).
- Smart casts: `current` is a local `val`, so `current is InquiriesResult.Failure` then `current.message` compiles.
- HYPOTHESIS: `created_at` is returned in UTC (+00:00), so the date shown is converted with that assumption; if a time looks off by hours on the phone, this is why.
- HYPOTHESIS: the admin's JWT is allowed to PATCH `inquiries` by RLS (`docs/SUPABASE_NOTES.md` says admin sees and changes all). If the toast says "Not changed: no permission...", RLS is refusing and the policy must be checked (SQL read first, owner approval before any change).
- Plan status line (the plan file was not resent, to save quota): 2026-10-18 | Claude | N2 written `[~]`: Inquiries native, shared helper `AdminSupabase`. NEXT: N3 Payments (audit first: approval may need an API route).

## What the owner should test
Admin Console > Inquiries opens natively. Check: the list matches the website; filter chips work and all five fit; tap a card, press Read, the toast says "Marked read" and the badge changes; go Back to the grid and the Inquiries badge number drops; airplane mode shows "Could not reach the server" with Try again.

## NEXT
N3 Payments. `PaymentsPanel` (20.7 KB) and `/admin/payments`: approve or reject likely touches `orders`, `enrollments` and maybe an API route (service role). Audit that before writing any code.
