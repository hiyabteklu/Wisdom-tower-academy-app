# SESSION 2026-10-19 — ADMIN-native N7 (Course Content)

> Repo is PUBLIC: no secrets, no student data. Plan: `docs/ADMIN_NATIVE_PLAN.md` (the plan table there lists N7 as Push Broadcasts; the session logs N6a to N6c chose Course Content next, and the owner said "continue", so this slice follows the logs). Previous: `docs/SESSION_2026-10-19_ADMIN_NATIVE_N6C_CATALOG.md`. Owner order: "continue making such admin dashboard native".

## Audit of the website panel (read, not guessed)
Files read: `ContentPanel.tsx`, `lib/content.ts`, `data/admin-nav.ts`.
- Table `learning_resources` (id, package_id, scope_path, hub, title, chapter, sort_order, content_type, storage_path, body_md, meta jsonb, published, created_at, updated_at). Missing table message: run `docs/learning-content-setup.sql`.
- List: one scope plus one hub, ordered by sort_order. Scope rules: an ECE course also matches the same course in the other semester; freshman math has two scope names; the hub `short-notes` also matches the old name `references`.
- Save = upsert (insert when there is no id). Delete = delete by id. Plain Supabase REST with the admin's own login. **No API route, no secret.**
- The one API route in this panel is the file upload (`/api/storage/upload`, Appwrite, about 4 MB limit). The website offers a second way for big files: paste the Appwrite File ID. That second way is native in the app.

## What was pushed (one commit)
1. NEW `data/admin/AdminContentRepository.kt`: `list(scopePath, hub)`, `save(itemId, input)`, `delete(itemId)`, the hub rules (content type, hint, starting JSON for a new flashcard, question bank or exam), `prettyMeta()`, Appwrite id helpers.
2. NEW `ui/admin/CourseContentScreen.kt`: header, path line with an up arrow, the Academy tree as a 2-column grid of folder cards (reuses `ADMIN_CONTENT_TREE` from N6b), the hub view (Scope and hub line with the hint, Add item, item cards with Edit and Delete), and the editor with the website's fields: Title, Chapter, Appwrite File ID (books, life savers, videos), notes or video link (short notes, videos), Content JSON (flashcards, question banks, exams), Published or Draft, Cancel, Save.
3. EDIT `AdminModules.kt` (`content` is native) and `AdminConsoleHost.kt` (Content branch).

## Deliberate differences from the website
- **No file chooser / upload from the phone.** It needs the website's upload route called with the admin session and a file picker, and neither exists in the app. Attach a PDF by pasting its Appwrite File ID (upload it in the Appwrite Console first). Editing an item that already has a file keeps the file unless a new File ID is pasted.
- Folders are a 2-column grid of cards (owner: no thin long rows); the website shows a one-column list on phones.
- Delete asks for confirmation in the app's dialog; update and delete report an error when no row was changed (a silent refusal).
- Skipped: the website's background write that repairs old ECE rows while listing, and its same-origin API fallback (an admin reads the table directly). Saving an ECE item still writes the current scope and package, so it repairs that item.
- The toast after saving says "Saved & published" or "Saved (draft)" without the website's "(Appwrite)" suffix, which also appeared for JSON-only items.
- Notes and JSON text boxes are kept with plain `remember` (not saved across a screen rotation or process death) so a very large exam cannot overflow Android's saved-state limit. Title, chapter, File ID and Published are saved.

## Evidence and limits
- `[~]` Written, NOT compiled, NOT run. Check the Actions runs of N1 to N7.
- Pitfall check by reading: Modifier extensions imported (`clickable`, `defaultMinSize`, `fillMaxHeight`, `height`, `size`, `padding`, `alpha`, `clip`, `background`, `border`); `IntrinsicSize` imported; `items` imported; no composable call in a LazyColumn builder body outside `item` or `items`; no trailing lambda onto a non-last parameter (named arguments everywhere); smart casts use local vals (`current`, `node`, `editTarget`, `existing`); `KeyboardType.Number` exists; icons `Folder`, `Refresh` are core and `ArrowBack` is from the automirrored set; `AdminToast`, `AdminConfirmDialog`, `AdminMessageCard`, `AdminTextField`, `AdminTextArea`, `AdminChoiceChip`, `AdminActionButton`, `AdminRose` and `GrantOutcome` already exist; no slash-star inside any comment.
- HYPOTHESIS: `learning_resources` accepts an insert without `id` (the website relies on a default) and the admin login may insert, update and delete there. A refusal shows in the toast.
- HYPOTHESIS: `meta` comes back as a JSON object (jsonb). If it came back as text the editor would show `{}` and a save would overwrite it: check one existing exam on the phone before saving it.
- HYPOTHESIS: the Kotlin tree matches the website today (hand copy from N6b).
- Plan status line (plan file not resent): 2026-10-19 | Claude | N7 written `[~]`: Course Content native (no phone file upload, File ID paste only). NEXT: owner decides the order of the rest (Push Broadcasts, Competitions, Free Resources, Database and SQL Hub).

## What the owner should test (use a TEST item, then delete it)
1. Admin Console > Course Content opens natively; Grades, Entrance Questions, Freshman and Special packages show as folder cards, two per row.
2. Walk Freshman > a course > Short Notes: the list matches the website. Back goes up one step, then to the admin home.
3. Add item in Short Notes: type a title and a note, leave it as Draft, Save: toast "Saved (draft)". Edit it, switch to Published, Save: "Saved & published". Delete it.
4. Open Flashcards > Add item: the starting JSON shows; break it on purpose and Save: "Meta JSON is invalid". Fix it and Save.
5. Open an existing exam with Edit and check the JSON looks complete (links without backslashes) before you change anything.
6. Books: Edit an item that has a PDF, change only the title, Save: the PDF still opens in the student app.
7. Airplane mode: "Could not reach the server" with Try again.

## NEXT
Owner picks. Remaining modules still on the website: Push Broadcasts (needs the website API route for sending), Competitions, Free Resources (uploads), Database and SQL Hub (probably stays web).
