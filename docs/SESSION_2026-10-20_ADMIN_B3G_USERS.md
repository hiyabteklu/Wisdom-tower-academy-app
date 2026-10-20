# SESSION 2026-10-20 (admin B3g, Users: compact rows, profile and grant panels)

> Repo is PUBLIC: no secrets, no student data.

## CI read first
The agent has no tool to read Actions. The owner's Actions screenshot (3:24 pm) showed #856 to #860 GREEN (#860 = B3e Payments, commit c485bd6, 5m 10s) and #861 (B3f Catalog, commit 564b342) In progress. So B3e is now confirmed compiled; B3f is NOT confirmed yet.
If #861 or the build of this commit is red, fix it before anything else and log it in `docs/BUILD_PITFALLS.md`. Both commits compile together, so a red build can come from either file (`CatalogScreen.kt` or `UsersScreen.kt`).

## What this chat did (pushed to `main`)
`ui/admin/UsersScreen.kt` (whole file resent). LAYOUT CHANGE ONLY. `UsersRepository.list / detail / grant`, search, both filter rows, the count line, loading / error / empty texts and Refresh are untouched.
- Each scholar is a compact row (`ScholarRow`: initials, name, e-mail, student id and level, arrow). A tap sets `inspectUser` and opens the profile panel.
- The profile (`ProfilePanel` = `AdminDetailPanel`, subtitle `Student Scholar Profile`, title = the scholar's name) holds: the student id (tap to copy, as on the old card), level, joined, e-mail, phone, school, stream, town / region, target exam, then the unlocked packages (`Enrolled` and `Manual grant`) and the recent scores. `Close` and `Grant New Package` are the bar fixed at the bottom.
- `Grant New Package` opens a SECOND panel over the profile (`GrantPanel`, subtitle `Direct Scholar Access Grant`): recipient, administrative note, package choices; `Cancel` and `Confirm & Unlock Package` are the bar fixed at the bottom (same 1 : 1.8 widths, same `Granting...` label). `All Packages` still asks `Grant full access?` first.
- After a SUCCESSFUL grant the grant panel closes, the profile stays open and reloads its package list (new `profileReload` counter), and the toast shows the result. After an ERROR the grant panel stays open and the toast shows the error. Back is ignored while a grant is running.
- The list stays composed under the panels, so search text, filters and scroll position survive. The toast sits 88 dp from the bottom while a panel is open.
- CHANGED ON PURPOSE: the quick `Grant Package` button on each list row is gone (open the scholar, then `Grant New Package`; one extra tap). The old screen-level `BackHandler`s were removed (each panel has its own). Removed (replaced): `ScholarCard`, `InspectView`, `GrantView`, `ViewHeader`.
- New imports: `KeyboardArrowRight` (automirrored). Removed imports: `BackHandler`, `ArrowBack`, `ClipData` stays (copy), `Color` stays.
- No database change. `[~]` Not compiled, not tested on a phone.
- Possible compile risks if the build is red: `AdminDetailPanel` called with named `title`, `subtitle`, `onBack`, `actions` and a trailing content lambda; `ProfileBody` and the content lambdas emitting several children into the panel's Column; smart casts of `inspectTarget` and `grantTarget` inside lambdas (local `val`s); `AdminTextField`, `AdminMessageCard`, `adminFormatDay` come from the kit files in the same package (unchanged).

## Phone test for the owner (use a test student; do NOT grant a real student a package to test)
1. Admin > Users: each scholar is a short row (name, e-mail, id and level, arrow). Type in the search box and pick a filter, then tap a scholar far down = the profile panel opens at the top.
2. In the profile: tap the student id = `Copied ...` toast. Scroll = packages and scores load below the details. `Close` stays at the bottom.
3. Back (arrow, Close or System Back) = the list with the same search, filters and scroll position.
4. `Grant New Package` = second panel on top. Pick one package, tap `Confirm & Unlock Package` = toast, grant panel closes, the profile shows the new `Manual grant` line. Back from the grant panel (before confirming) = the profile.
5. In the grant panel pick `All Packages` and confirm = it asks first; Cancel in the dialog = nothing is granted.

## Next
Read the CI result of #861 (B3f) and of this commit first. Then A4 on the phone, B2b (the 7 admin screens not audited), C1 (fixed colours audit). B3 is finished for the five audited modules.
