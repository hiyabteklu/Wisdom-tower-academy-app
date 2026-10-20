# SESSION 2026-10-20: everything visible, access per material (second pass)

> Repo is PUBLIC: no secrets, no student data. Owner words: "No materials are visible at all, in paid or free mode. I want ALL materials visible to all signed-in users; what they can open and what they cannot is decided by the admins, down to material level (Chapter 1 notes, Chapter 3 flashcards...)."
> OWNER RULE (still valid): the website is NOT a priority. App only.

## What was found (read, not guessed)
- The previous agent DID push work (commits on 2026-10-19/20: `CourseLocks`, hub list lock icons, admin LocksScreen with chapters and materials, Paid mode card, DB migrations). It was written but never compiled or run.
- "No published materials in this hub yet" came from the database: the migration `content_guard_policy_swap` replaced the public read policy of `learning_resources` with one that needed a signed-in user token, but the app reads with the public key only. Later migrations `restore_open_visible_to_app_public_key` and `learning_resources_always_listable` (05:17 and 05:19 UTC) put the plain policy `lr_public_select_published` (published = true) back. Verified live: it is the only public policy now, and the rows exist (for example `freshman/economics` short-notes = 1 published row).
- Two REAL gaps were still in the app, so nothing could ever lock:
  1. `ContentAccess` had `IS_FREE_MODE = true` hardcoded, so every signed-in student counted as owner of everything; the admin PAID MODE switch (`system:enforce`) and the Owners-only mode were never read.
  2. The hub card was checked BEFORE the chapter / item rows, so "package owners only, Chapter 1 free" was impossible (a closed package closed the card and Chapter 1 was never reachable). Hub-level Coming soon also hid the whole hub behind a pop-up.

## Done (branch `fix/locks-paid-mode-ownership`, NOT compiled, NOT tested on a device)
- `data/content/ContentAccess.kt`:
  - hub cards ALWAYS open (`hubGate` is always Open): everything is always visible;
  - `CourseLocks.itemGate` now returns the gate of EVERY material: most specific row wins (item, chapter, hub, scope, package, static rule);
  - `resolveLocks` reads the PAID MODE row (`system:enforce`, mode `locked` = ON) in the same request as the other lock rows;
  - paid mode OFF: `require_purchase` is free for every signed-in student (same as the database function); paid mode ON: only owners;
  - owner = the student's own row in `enrollments` or `access_grants` (read with the student's login, RLS returns only their rows) for the package or `__all__`; any `ece-y3-sem-*` row opens both ECE semesters; cached 30 s;
  - network trouble: paid mode counts as OFF, unreadable ownership counts as owner (never a lock-out).
- No database change in this pass (the tables, rows and policies already exist).
- `HubItemListScreen` and `LocksScreen` unchanged: the list already draws a lock icon, "Coming soon" / "Package required", a pop-up on tap, no download for a locked row.

## Known gaps
- Locks are an interface gate: the rows of notes, decks and questions are readable with the public key, and Appwrite book files are served by the website proxy without a check. Real enforcement needs a server-side stage (the `content_guard` functions are in the database, unused).
- 93 materials have no chapter number (57 whole-book PDFs, 36 odd titles): lock them with the `item:` lock.
- `ece-y3-full` exists in the catalog but nothing maps it to the two semester packages.
- ECE chapter lock keys use the scope the admin screen shows; ECE rows are stored under `ece/sem-1/...` for both semesters, so check an ECE Semester 2 chapter lock on a phone.

## TEST (owner)
1. GitHub Action green, install the APK.
2. As a signed-in student, open Packages > any subject > Short Notes: ALL notes are listed (no lock yet, paid mode OFF).
3. Admin > Content locks: pick a subject, set its package to Owners only, set Chapter 1 of Short notes to Free for signed-in, turn Paid mode ON.
4. Student account with NO purchase / grant: Chapter 1 opens, other chapters show a lock and the "Package required" pop-up. A student with an enrollment or grant: everything opens.
5. Lock one single material (Chapter 3 flashcards): only that row shows a lock.
6. Paid mode OFF: everything opens at once (allow up to 30 seconds).
