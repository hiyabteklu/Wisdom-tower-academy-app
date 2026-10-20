# SESSION 2026-10-20 (admin B3f, Catalog: compact rows, editor in a panel)

> Repo is PUBLIC: no secrets, no student data.

## CI read first
The agent has no tool to read Actions. The owner's Actions screenshot (3:18 pm) showed: #856 (B3a), #857 (B3b/B3c), #858 (docs) and #859 (B3d, Inquiries, commit 64e215c) all GREEN (compiled). #860 (B3e, Payments, commit c485bd6) was In progress, so B3e is NOT confirmed compiled yet.
If #860 or the build of this commit is red, fix it before anything else and log it in `docs/BUILD_PITFALLS.md`. Both commits compile together, so a red build can come from either file (`PaymentsScreen.kt` or `CatalogScreen.kt`).

## What this chat did (pushed to `main`)
`ui/admin/CatalogScreen.kt` (whole file resent). LAYOUT CHANGE ONLY, save / delete logic untouched.
- Each catalog row is a compact row (`CatalogRow`: name, id, price, Inactive badge, arrow). A tap sets `editing` and opens the editor panel. The description is no longer shown on the row (it is in the editor).
- The editor (`CatalogEditor`) is an `AdminDetailPanel` (subtitle `Edit package` or `Catalog` for a new one, title = package name or `New package`) drawn AFTER the list inside the same Box, so the list stays composed and keeps its scroll position. Same 12 fields, same labels, same order, same `rememberSaveable` form state. New helper `CatalogField` keeps each label right above its field.
- `Cancel` and `Save package` (same 1 : 1.6 widths, same `Saving...` label) are the bar fixed at the bottom. `Delete package` is the last item inside the panel (existing packages only), still asks `Delete package <id>?` first.
- After a SUCCESSFUL save or delete the panel closes and the list reloads. After an ERROR the panel stays open and the toast shows the error; the toast sits 88 dp from the bottom while the panel is open.
- `RoseTone` (fixed pink) became the kit's `AdminRose`. The screen's own `BackHandler` was removed (the panel has its own, registered after the host's).
- UNCHANGED: `CatalogRepository.list / save / delete`, `newItem()`, `isBuiltIn()`, the built-in note, the header text, Refresh, Add package, loading / error / empty texts, the confirm dialog text.
- Removed (replaced): `CatalogCard`, the old full-screen `CatalogEditor` that replaced the list.
- New imports: `clickable`, `Spacer`, `width`, `KeyboardArrowRight` (automirrored). Removed imports: `BackHandler`, `ArrowBack`, `Color`. No database change. `[~]` Not compiled, not tested on a phone.
- Possible compile risks if the build is red: `AdminDetailPanel` called with named `title`, `subtitle`, `onBack`, `actions` and a trailing content lambda; `editTarget` smart cast inside the `onDelete` lambda (a local `val`); the new imports.

## Phone test for the owner (use a test package; do NOT delete a real package to test)
1. Admin > Catalog: each package is a short row (name, id, price, arrow). The list is much shorter than before.
2. Scroll down the list and tap a package = the editor panel opens at the top with all fields; `Save package` stays visible at the bottom while you scroll the fields.
3. Back (arrow, Cancel, or System Back) = the list at the same scroll position.
4. Add package = the same panel with `New package`; fill the name and save = the panel closes and the new row is in the list.
5. Open the test package, scroll to the end, tap `Delete package` = it asks first; Cancel = nothing happens.

## Next
Read the CI result of #860 (B3e) and of this commit first. Then B3g Users; also A4 on the phone, B2b, C1.
