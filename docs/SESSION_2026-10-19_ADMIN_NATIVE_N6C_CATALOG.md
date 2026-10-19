# SESSION 2026-10-19 — ADMIN-native N6c (Pricing and Catalog)

> Repo is PUBLIC: no secrets, no student data. Plan: `docs/ADMIN_NATIVE_PLAN.md` (N6 row, third and last panel). Previous: `docs/SESSION_2026-10-19_ADMIN_NATIVE_N6B_CONTENT_LOCKS.md`. Owner order: "Continue".

## Audit of the website panel (read, not guessed)
Files read: `CatalogPanel.tsx`, `lib/catalog.ts`.
- Table `catalog_items` (id, name, short_name, description, price_etb, href, image, includes, enrolled_label, group_key, active, sort_order, timestamps). List: every row, active or not, ordered by sort_order. Missing table message: run `docs/catalog-setup.sql`.
- Save = upsert on `id` (id lower-cased, other characters become a dash; id and name required). Delete = delete by id. Seed from static = upsert of every package of `academyPackages` (needs the website's full static texts).
- Plain Supabase REST with the admin's own login. No API route, no secret.
- **FINDING (from `applyStaticPrice` in `catalog.ts`)**: for a package id that exists in `data/packages.ts`, the price, includes and description students see come from the website CODE, not from the database row. Name, short name, href, image, group and active still come from the row. So editing the price of a built-in package here does not change what students pay or see. The website panel's own footnote ("DB rows override prices when present") says the opposite; the code wins. The app shows a note about this in the list and in the editor of a built-in id.

## What was pushed (one commit)
1. EDIT `ui/admin/AdminKitInputs.kt`: added `AdminTextArea` (several lines, grows with the text). `AdminTextField`, `AdminMessageCard`, `adminFormatDay` unchanged.
2. NEW `data/admin/CatalogRepository.kt`: `list()`, `save()`, `delete()`.
3. NEW `ui/admin/CatalogScreen.kt`: header and refresh, Add package, the note above, one card per row (name, id, price, Inactive badge, description, Edit, Delete), and the editor with every field of the website form (ID only for a new package, Price, Name, Short name, Group as four chips, About text, Preview href, Image URL, Includes one per line, Enrolled label, Sort order, Active or Inactive, Cancel, Save package).
4. EDIT `AdminModules.kt` (`catalog` native) and `AdminConsoleHost.kt` (Catalog branch).

## Deliberate differences from the website
- Seed from static is NOT in the app (it would need a second copy of all static package texts). Use the website button for it.
- Delete asks for confirmation in the app's dialog and reports an error when no row was deleted.
- The editor replaces the list (no inline form above the list); Back and the arrow return to the list without saving.
- The "Static fallback list has N packages" footnote counts this app's copy (`ADMIN_PACKAGES`), not the website's.
- A new package gets the id `custom-` plus the time in base 36, like the website.

## Evidence and limits
- `[~]` Written, NOT compiled, NOT run. Check the Actions runs of N1 to N6c.
- Pitfall check by reading: Modifier extensions imported (`defaultMinSize`, `alpha`, `clip`, `background`, `border`, `padding`); `items` imported; no composable call in a LazyColumn builder body outside `item` or `items`; no trailing lambda onto a non-last parameter (`AdminActionButton`, `AdminChoiceChip`, `AdminTextField`, `AdminTextArea` use named arguments); `KeyboardType.Decimal`, `Number`, `Uri` exist; `rememberSaveable` keys on the row id so a different row never inherits old text; the regex `[^a-z0-9_\-]` has the dash escaped; local `fun` helpers capture state like the Payments screen; keyboard: the main content area of the app already applies `imePadding()` (seen in `MainActivity.kt`), so no extra inset was added (if the keyboard still covers the lower fields on your phone, tell me).
- HYPOTHESIS: `id` is the primary key or unique in `catalog_items` (the website upsert relies on it). If not, the save fails with the server's message.
- HYPOTHESIS: the admin login may insert, update and delete in `catalog_items`. A refusal shows in the toast.
- HYPOTHESIS: `includes` comes back as a JSON array of text lines (the website reads it as `string[]`).
- HYPOTHESIS: the website running in production still has `applyStaticPrice` as in this repo snapshot.
- Plan status line (plan file not resent): 2026-10-19 | Claude | N6c written `[~]`: Pricing and Catalog native. N6 is complete. NEXT: N7 Course Content (ContentPanel, 18 KB; reuses `AdminContentTree.kt`).

## What the owner should test (use a TEST package, then delete it)
1. Admin Console > Pricing & Catalog opens natively and lists the same rows as the website.
2. Add package: fill name and price, Save: toast "Package created", the card appears.
3. Edit it: change the price, Save: "Package updated". Open Edit on a built-in id (for example grade-9): the amber note shows.
4. Delete the test package: confirm dialog, toast "Deleted".
5. Keyboard: in the editor, tap the Includes box and the Sort order field and check the keyboard does not hide what you type.
6. Airplane mode: "Could not reach the server" with Try again.

## NEXT
N7 Course Content (`ContentPanel.tsx`, 18 KB). Audit first; it may involve uploads (Appwrite), which need a separate decision.
