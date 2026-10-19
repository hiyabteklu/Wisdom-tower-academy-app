# SESSION 2026-10-18 — ADMIN-native N6a (Access Grants)

> Repo is PUBLIC: no secrets, no student data. Plan: `docs/ADMIN_NATIVE_PLAN.md` (N6 row, first of three panels). Previous: `docs/SESSION_2026-10-18_ADMIN_NATIVE_N5.md`. Owner order: "Continue".

## Audit of the website panel (read, not guessed)
Files read: `AccessGrantsPanel.tsx`, `lib/access-grants.ts`.
- List: `access_grants`, newest 500. Missing table message: run `docs/access-grants-setup.sql`.
- Grant: e-mail (no account needed), package or All packages, note. Source `beta`. All packages = the `__all__` sentinel plus every package. Each row mirrored into `enrollments`. Account id looked up in `profiles` by e-mail (empty if none).
- Revoke: delete the grant, then delete enrollment copies with source whitelist, admin, beta, comp or EMPTY. Revoke all: same for every grant of the e-mail.
- Plain Supabase REST with the admin's own login. No API route, no secret.

## What was pushed (one commit)
1. EDIT `data/admin/AdminSupabase.kt`: added `delete()` (asks for the deleted rows back, so a silent refusal shows as an empty array). All other functions unchanged.
2. NEW `data/admin/GrantsRepository.kt`: `list()`, `grant()`, `revoke()`, `revokeAllForEmail()`.
3. EDIT `ui/admin/AdminKitInputs.kt`: `AdminTextField` got an optional `keyboardType` (default Text; the N4 call sites are unchanged).
4. NEW `ui/admin/AccessGrantsScreen.kt`: header and refresh, grant form (e-mail, inline package list, note, Grant access), filter, count, grant cards (e-mail, package, note, granted by, linked account or pre-signup, Revoke, Revoke all), toast, confirm dialogs.
5. EDIT `AdminModules.kt` (`grants` native) and `AdminConsoleHost.kt` (Grants branch).

## Deliberate differences from the website
- Revoke, Revoke all and Grant all packages ask for confirmation in the app's own dialog (the website used a browser confirm for the revokes and none for grant-all).
- Revoke does NOT delete enrollment copies with an EMPTY source (the website does; that could also remove a PAID enrollment of the same e-mail and package). Grants made by the website or this app always carry a source, so their copies are removed. HYPOTHESIS: very old copies without a source would stay after a revoke; if you see that, tell me and we decide.
- Same write method as N4: existing rows are read once, existing ones updated, new ones inserted in one request (no ON CONFLICT rule needed). A refusal or zero-row delete is reported.
- Revoke all deletes all grants of the e-mail with one request (`id=in.(...)`) instead of one by one.
- The package list is the hand copy `ADMIN_PACKAGES` (12 packages), same as N4.
- The website's `clearOwnershipCache` is website memory only: not needed.

## Evidence and limits
- `[~]` Written, NOT compiled, NOT run. Check the Actions runs of N1 to N6a.
- Pitfall check by reading: Modifier extensions imported (`clickable`, `height`, `size`, `padding`, `alpha`, `clip`, `background`, `border`); `items` imported; no composable call in a LazyColumn builder body outside `item` or `items` (the package rows are inside the `form` item); `KeyboardOptions` imported from `androidx.compose.foundation.text`; arrow icons `KeyboardArrowDown` and `KeyboardArrowUp` are core Material icons; local `fun` helpers inside the composable capture state like the Payments screen; the dialogs use the same `AdminConfirmDialog` named arguments as N4.
- HYPOTHESIS: DELETE with `Prefer: return=representation` returns the deleted rows (PostgREST standard). HYPOTHESIS: `in.(id1,id2)` works for the id type of both tables (uuid or number).
- HYPOTHESIS: the admin login may insert, update and delete `access_grants` and `enrollments` (the website does the same). A refusal shows in the toast.
- Time shown in none of this screen (no dates on the cards, like the website).
- Plan status line (plan file not resent): 2026-10-18 | Claude | N6a written `[~]`: Access Grants native. NEXT: N6b Content Locks (LocksPanel, 14 KB), N6c Pricing and Catalog (CatalogPanel, 13.7 KB).

## What the owner should test (TEST e-mail only)
1. Admin Console > Access Grants opens natively and lists the same grants as the website.
2. Grant one package to a test e-mail that has NO account: toast "Granted ...", the card shows "email only (pre-signup)".
3. Grant to a test student e-mail that HAS an account: the card shows "linked account"; the student sees the package unlocked.
4. Grant All packages: confirm dialog, toast "Granted full access to ... (13 rows)".
5. Revoke one card: confirm, toast "Revoked", the student no longer has it (also check My Learning).
6. Revoke all on a test e-mail: confirm, toast "Removed N grant(s) for ...".
7. Airplane mode: "Could not reach the server" with Try again.

## NEXT
N6b Content Locks, then N6c Pricing and Catalog (which may also replace the hand copy of `ADMIN_PACKAGES`). Audit each panel first.
