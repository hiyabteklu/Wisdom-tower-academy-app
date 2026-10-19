# SESSION 2026-10-19 — ADMIN-native N10 (Free Resources)

> Repo is PUBLIC: no secrets, no student data. Plan: `docs/ADMIN_NATIVE_PLAN.md` (slice numbers shifted after N6, see the N9 log: Competitions was N9, so Free Resources is N10). Previous: `docs/SESSION_2026-10-19_ADMIN_NATIVE_N9_COMPETITIONS.md` (commit `70c06f6`). Owner order: "continue making such admin dashboard native", then "Continue, from the status" (the status ended with: Free Resources next?). Operating rules read: `docs/ADMIN_NATIVE_PLAN.md`, `docs/BUILD_PITFALLS.md`.

## Audit of the website panel (read, not guessed)
Files read: `website-reference/src/components/admin/FreeResourcesPanel.tsx`, `lib/free-resources.ts`, `docs/free-resources-setup.sql`, `docs/free-resources-rls-fix.sql`. Everything is PLAIN Supabase with the admin's own login. **No website API route, no secret.** The plan said uploads "need a file chooser": the website uploads straight to the public storage bucket `free-resources` (no Appwrite, no API route), so the app can do the same with its own token.
- **Pages**: table `free_resource_pages` (id, slug, title, subtitle, body_md, meta, cover_path, published, sort_order, updated_by, created_at, updated_at). Save = upsert on `slug`; sort_order = (tab position + 1) times 10.
- **Items**: table `free_resource_items` (id, page_slug, kind, title, subtitle, body_md, image_path, gallery, meta, featured, published, sort_order, deadline date, external_url, created_at, updated_at). Save = upsert (insert when there is no id). Delete = delete by id. List = one slug, ordered by sort_order then newest first.
- **Photo**: bucket `free-resources` (public read, signed-in write), path `<slug>/<time>-<safe name>`, upsert, limit 8 MB.
- **Tabs**: Success Stories and Scholarships are full cards (page settings plus items). University, Department, Campus Life and Study Technique tabs are additive notes only (no page settings). Meta fields: story = studentName, result, program, year, quote; scholarship = organization, amount, eligibility (status defaults to open); universities note = universityId.
- The student app (`FreeResourcesLive.kt`) reads meta keys the editor does not show (provider, level, country). The native editor keeps every existing meta key when it saves.

## What was pushed (one commit)
1. NEW `data/admin/AdminStorage.kt`: `uploadFreeResourcePhoto(context, uri, folder)` (reads the chosen file, 8 MB limit, one raw POST to the storage address with the admin token, `x-upsert: true`, one retry after a refresh on HTTP 401) and `publicUrl(path)`.
2. NEW `data/admin/FreeResourcesRepository.kt`: `load(slug)` (pages and items in parallel), `savePage`, `saveItem`, `deleteItem`, meta helpers (`emptyMeta`, `metaText`, `buildMeta`), slug lists and labels copied from `free-resources.ts`.
3. NEW `ui/admin/FreeResourcesScreen.kt`: header with Refresh, the six tabs as two chips per row (a check mark on a published full page), the page settings card (title, subtitle, Markdown text, Published or Draft, Save page, Open live page), the amber info card on the note-only tabs, the item list (photo, title, line, Published or Draft badge, Featured badge, Edit, Delete), Add button, and the item editor with every field of the website form plus the photo chooser with a preview.
4. EDIT `AdminModules.kt` (`free-resources` is native) and `AdminConsoleHost.kt` (Free Resources branch; the deep link `/admin?tab=free-resources` already maps to this module).

## Deliberate differences from the website
- The scholarship deadline is typed as `2026-11-30` (no date picker in the app) and checked as a real date before sending.
- The university for a University note is chosen from a search list. The list is read from the app's own university guide (`GuidesRepository.loadUniversities`, assets `data/universities.json`), so nothing was copied or invented.
- The photo is chosen with the phone's file chooser (images only) and uploaded when the item is saved. A new photo replaces the old one in the row; the old file stays in the bucket (same as the website).
- The page save does NOT send `meta`, `cover_path` or `updated_by` (the website sends empty values, which would wipe them; neither panel edits them).
- Every write asks for the saved rows back, so a silent refusal (HTTP 200 with zero rows) shows as an error.
- Delete asks in the app's dialog. Tapping a tab while data loads shows Loading, never the previous tab's items.
- Open live page leaves the admin to the student app page `/academy/<slug>` (same mechanism as the Live Academy pill).
- Not carried over: department meta (`departmentId`): the website has no field for it either.

## Evidence and limits
- `[~]` Written, NOT compiled, NOT run. Check the Actions run of this commit (and of N9, `70c06f6`, which was also not checked).
- Pitfall check by reading: every Modifier extension used is imported (`background`, `border`, `clickable`, `padding`, `fillMaxSize`, `fillMaxWidth`, `height`, `size`, `width`, `alpha`, `clip`); `items` imported; no composable call in a LazyColumn builder body outside `item` or `items`; named arguments for every lambda that is not last; smart casts use local vals (`current`, `editTarget`, `uni`, `toDelete`, `picked`, `uploadError`, `uri`); icons used: core `Refresh` and automirrored `ArrowBack` only; `AsyncImage` from Coil (already used in `GuidesScreen.kt`); no slash-star inside any comment; no secret.
- HYPOTHESIS: the storage address accepts a raw request body with `x-upsert: true` and the file's content type the same as the website client (which sends form data). If the upload fails, the toast shows the server's own sentence.
- HYPOTHESIS: the six tables and policies exist as in `free-resources-setup.sql` (the student app already reads them with the anon key).
- HYPOTHESIS: `AuthStateManager.refreshIfNeeded()` and `refreshIfNeeded(force = true)` can be called from a coroutine the same way `AdminSupabase.kt` does.
- HYPOTHESIS: `rememberSaveable { mutableStateOf<Uri?>(null) }` saves a chosen photo address across a screen rotation.
- Plan status line (plan file not resent): 2026-10-19 | Claude | N10 written `[~]`: Free Resources native (pages, stories, scholarships, notes, photo upload). NEXT: Database and SQL Hub (probably stays web), then the regression pass.

## What the owner should test (use a TEST item, then delete it)
1. Admin Console > Free Resources opens natively; Back returns to the grid. `/admin?tab=free-resources` opens it directly.
2. Success Stories tab: the list matches the website (title, line, Published or Draft). Page settings show the saved title and subtitle.
3. Add story: type a title and a student name, choose a photo, leave it as Draft, Save item: toast `Item saved as draft`. The card shows the photo. Edit it, set Published, Save: `Item published`. Open the student app Success Stories page and check it appears with the photo.
4. Scholarships tab: Add opportunity with a deadline `2026-11-30`; try `2026-13-40` and expect `Deadline must be a real date like 2026-11-30`. Edit an existing scholarship and check Organization and Amount are filled.
5. University notes tab: Add note, search a university, pick one, Save as Draft. The card line shows the university's short name and name.
6. Page settings: change the subtitle on Success Stories, Save page: `Page saved (draft)` or `Page saved and published`.
7. Choose a photo larger than 8 MB: `Max file size 8 MB`.
8. Delete the test item: it asks first, then `Deleted`.
9. Airplane mode: a card with `Could not reach the server` and Try again.
10. Afterwards remove the test photo files from the `free-resources` bucket (the app leaves them there, like the website).

## NEXT
Owner picks. Remaining on the website: Database and SQL Hub (probably stays web: arbitrary SQL needs a server route; decide with the owner). Then the regression pass (plan slice N11) and the owner's phone test.
