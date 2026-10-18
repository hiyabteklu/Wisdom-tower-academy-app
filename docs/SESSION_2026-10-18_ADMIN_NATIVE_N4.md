# SESSION 2026-10-18 — ADMIN-native N4 (Scholars and Users)

> Repo is PUBLIC: no secrets, no student data. Plan: `docs/ADMIN_NATIVE_PLAN.md` (N4 row). Previous: `docs/SESSION_2026-10-18_ADMIN_NATIVE_N3.md`. Owner order of this chat: "continue making such admin dashboard native".

## Audit of the website panel (read, not guessed)
Files read: `UsersPanel.tsx`, `lib/admin-data.ts` (`listProfiles`, `getStudentDetail`), `lib/access-grants.ts` (`grantAccess`, `mirrorEnrollment`, `grantAllPackages`), `data/packages.ts` (`academyPackages`).
- List: `profiles`, newest 500 (rich columns; the website falls back to a short column list, newest 300, if the first query fails). Search fields: name, e-mail, student ID, school, town. Filters: stream (natural, social: substring `nat` and `soc`) and education level (exact, lower case: grade-12, grade-11, freshman, remedial).
- Inspect: `academic_results` of the user (newest 25), `enrollments` of the user, `access_grants` and `orders` of the e-mail. The website fetches `orders` but never shows them (not carried over, not fetched).
- Grant: single package = one `access_grants` row (source `whitelist`); all packages = the `__all__` sentinel plus every package (source `beta`). Each grant is mirrored into `enrollments` (columns `email, package_id, package_name, user_id, source, granted_by, note`). Note defaults to "Direct Admin Grant". Everything is plain Supabase REST with the admin's own login: NO API route and NO secret key is involved.
- Not carried over: CSV export (needs a file save: later), the photo (initials only), `clearOwnershipCache` (website memory only).

## What was pushed (one commit)
1. NEW `data/admin/AdminPackages.kt`: `ALL_PACKAGES_ID`, `ADMIN_PACKAGES` (ids and names copied from `academyPackages`, 12 packages, no prices).
2. NEW `data/admin/UsersRepository.kt`: `list()`, `detail()`, `grant()`.
3. NEW `ui/admin/AdminKitInputs.kt`: `AdminTextField`, `AdminMessageCard`, `adminFormatDay` (shared; merge with `AdminKit.kt` in N11).
4. NEW `ui/admin/UsersScreen.kt`: header and refresh, search, stream chips, level chips, "Showing X of Y", scholar cards (initials, name, e-mail, tap-to-copy student ID, school, region, stream, level, joined date, History & Scores, Grant Package), History & Scores view, Grant Package view (recipient, note, 13 choices, Cancel, Confirm & Unlock Package), toast.
5. EDIT `AdminModules.kt` (`users` added to `NATIVE_ADMIN_MODULE_IDS`) and `AdminConsoleHost.kt` (Users branch). The website panel stays reachable only through the WebView fallback code path (no tile points to it any more).

## Deliberate differences from the website
- Grant of ALL packages asks for confirmation first (the website pre-selects All packages, so one stray tap on Confirm would unlock everything).
- The student number placeholder (`WTA-01042`) and the name placeholder (`Scholar Scholar`) of the website are NOT shown: a missing value shows "Not assigned", "Not set" or "Scholar" (fake-content rule, HANDOFF section 7).
- Enrollments and manual grants are listed under separate tags (the website labels every enrollment "Verified Order", but a manual grant is also copied into `enrollments`, so it would appear twice with a wrong label).
- A failed read of the history shows an error with Try again (the website shows an empty history).
- The user id for a grant comes from the profile row (the website looks it up by e-mail).
- The grant does not rely on an ON CONFLICT rule: it reads the existing rows once, updates those and inserts the rest in one request. A refusal or a zero-row update is reported, never shown as success.
- Search also matches the phone number (phone sign-ups have a made-up e-mail).
- Grant and Inspect replace the list inside the screen (no pop-up windows); system Back and the arrow go back to the list; the list keeps its scroll position.

## Evidence and limits
- `[~]` Written, NOT compiled, NOT run. Check the Actions runs of N1 to N4.
- Pitfall check by reading: every Modifier extension imported (`clickable`, `size`, `height`, `width`, `padding`, `alpha`, `clip`, `background`, `border`); `items` imported; no composable call in a LazyColumn builder body outside `item` or `items`; no trailing lambda onto a non-last parameter (all calls to `AdminActionButton`, `AdminChoiceChip`, `AdminMessageCard`, `ViewHeader` use named arguments); icons used exist already in the repo (`Check`, `Refresh`, `AutoMirrored.Filled.ArrowBack` is the standard core one); clipboard uses the Android `ClipboardManager` (no deprecated Compose clipboard API); `current` is a local `val` so the smart casts compile.
- HYPOTHESIS: RLS lets the admin login read other users' `academic_results` and `enrollments` and write `access_grants` and `enrollments` (the website does the same with the same login). A refusal shows in the toast.
- HYPOTHESIS: `access_grants` has `updated_at` and `enrollments` has `source, granted_by, note` (the website writes them). If a column is missing the server answers HTTP 400 and the toast shows the server's message.
- HYPOTHESIS: `ilike` with backslash-escaped `_` and `%` works in PostgREST. Even if it did not, the code only treats a row as the same grant when its e-mail equals the target e-mail, ignoring case.
- HYPOTHESIS: the profiles column names of the short fallback list exist (the website uses them).
- The `ADMIN_PACKAGES` list is a hand copy of the website list: update it when the website adds or renames a package.
- Time shown assumes `created_at` is UTC (same as N2 and N3).
- Plan status line (plan file not resent): 2026-10-18 | Claude | N4 written `[~]`: Scholars and Users native. NEXT: N5 Overview (audit `AnalyticsPanel`, 23 KB, first), or the owner's order.

## What the owner should test (use a TEST student, not a real one)
1. Admin Console > Scholars & Users opens natively; the count under the header matches the Scholars count on the home grid (up to 500).
2. Search by name, e-mail, student ID, school; the stream and level chips narrow the list; "Showing X of Y" follows.
3. Tap a student ID: "Copied ..." toast, paste somewhere to check.
4. History & Scores: summary, enrollments and manual grants, and the latest scores of a student who has taken tests; Back returns to the list at the same scroll position.
5. Grant Package to the test student: pick one package, Confirm: toast "Granted ..."; open History & Scores again and see it under Manual grant; check the student sees the package unlocked in My Learning.
6. Grant All Packages to the test student: confirm dialog, toast with the number of packages (13 including the all-access marker).
7. Airplane mode: "Could not reach the server" with Try again, in the list and in History & Scores.

## NEXT
N5 Overview (`AnalyticsPanel`, 23 KB) unless the owner picks another module. Audit the panel first.
