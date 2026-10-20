# SESSION 2026-10-20: everything visible, access per material (second pass) + hardening stage 1

> Repo is PUBLIC: no secrets, no student data. Owner words: "No materials are visible at all, in paid or free mode. I want ALL materials visible to all signed-in users; what they can open and what they cannot is decided by the admins, down to material level (Chapter 1 notes, Chapter 3 flashcards...)."
> OWNER RULE (still valid): the website is NOT a priority. App only.

## What was found (read, not guessed)
- The previous agent DID push work (commits on 2026-10-19/20: `CourseLocks`, hub list lock icons, admin LocksScreen with chapters and materials, Paid mode card, DB migrations). It was written but never compiled or run.
- "No published materials in this hub yet" came from the database: the migration `content_guard_policy_swap` replaced the public read policy of `learning_resources` with one that needed a signed-in user token, but the app reads with the public key only. Later migrations `restore_open_visible_to_app_public_key` and `learning_resources_always_listable` (05:17 and 05:19 UTC) put the plain policy `lr_public_select_published` (published = true) back. Verified live: it is the only public policy now, and the rows exist (for example `freshman/economics` short-notes = 1 published row).
- Two REAL gaps were still in the app, so nothing could ever lock:
  1. `ContentAccess` had `IS_FREE_MODE = true` hardcoded, so every signed-in student counted as owner of everything; the admin PAID MODE switch (`system:enforce`) and the Owners-only mode were never read.
  2. The hub card was checked BEFORE the chapter / item rows, so "package owners only, Chapter 1 free" was impossible (a closed package closed the card and Chapter 1 was never reachable). Hub-level Coming soon also hid the whole hub behind a pop-up.

## Done, pass 2 (branch `fix/locks-paid-mode-ownership`, NOT compiled, NOT tested on a device)
- `data/content/ContentAccess.kt`:
  - hub cards ALWAYS open (`hubGate` is always Open): everything is always visible;
  - `CourseLocks.itemGate` returns the gate of EVERY material: most specific row wins (item, chapter, hub, scope, package, static rule);
  - `resolveLocks` reads the PAID MODE row (`system:enforce`, mode `locked` = ON) in the same request as the other lock rows;
  - paid mode OFF: `require_purchase` is free for every signed-in student (same as the database function); paid mode ON: only owners;
  - owner = the student's own row in `enrollments` or `access_grants` (read with the student's login, RLS returns only their rows) for the package or `__all__`; any `ece-y3-sem-*` row opens both ECE semesters; cached 30 s;
  - network trouble: paid mode counts as OFF, unreadable ownership counts as owner (never a lock-out).

## Done, hardening stage 1 (locks enforced by the DATABASE when a material is opened)
- Database (applied live, migration `open_resource_checked_door_and_item_count`, purely additive, nothing removed):
  - column `learning_resources.item_count` (generated from `meta`: number of cards or questions; 222 of 400 rows have one);
  - function `public.open_resource(p_id uuid)` returns jsonb: `{status: ok, row}`, `{status: locked, gate: package_required | coming_soon}`, `not_found` or `not_signed_in`. It uses `content_guard.resource_visible` (item > chapter > hub > scope > package, paid mode, the student's enrollments and grants). Admins always pass. Execute is granted to `authenticated` only (anon is refused).
  - Tested live inside a rolled-back transaction with a fake non-owner: no lock = ok; package owners only + paid ON = locked / package_required; the same item set to open = ok; anon = refused. Nothing was left in `content_locks`.
- App:
  - `LearningContentRepository`: lists read only light columns (+ `item_count`), never `body_md` or `meta`; `getResource` / downloads go through `open_resource` with the student's login; a refused material gives the message "This material is locked right now." and its saved copy is deleted; if the function cannot be used (missing, not signed in) the old direct read is the fallback;
  - `LearningResource.itemCount` (default null) and `HubItemListScreen` shows the card / question count from it.

## NOT done yet (hardening stage 2, do it AFTER the new app version is installed)
1. Take `body_md` and `meta` away from the public key (column-level revoke on `learning_resources` for `anon` and `authenticated`, keep the light columns). The direct-read fallback in the app then stops working for everybody, so students on the OLD app version would see empty notes: wait until the new APK is out. The admin screens that read note text (N7 course content, website ContentPanel) must read through an admin-only function first, or they break.
2. Book PDFs: the file id sits in `storage_path` and Appwrite serves files by id. Needs a private bucket plus a short-lived link function that runs the same check.
3. Align the "code default" of the app (`staticLockMode`: freshman, grades, UAT, COC... = owners only) with the database (no row = open). Today, with paid mode ON and NO lock rows, the app draws a lock but the database would still serve the material. The test below uses explicit rows, so it is not affected.

## Known gaps
- 93 materials have no chapter number (57 whole-book PDFs, 36 odd titles): lock them with the `item:` lock.
- `ece-y3-full` exists in the catalog but nothing maps it to the two semester packages.
- ECE chapter lock keys use the scope the admin screen shows; ECE rows are stored under `ece/sem-1/...` for both semesters. The database function checks both semesters, the app row check does not: test an ECE Semester 2 chapter lock on a phone.

## TEST (owner)
1. GitHub Action green, install the APK.
2. As a signed-in student, open Packages > any subject > Short Notes: ALL notes are listed (no lock yet, paid mode OFF). Flashcard and question-bank rows still show "N cards" / "N questions".
3. Admin > Content locks: pick a subject, set its package to Owners only, set Chapter 1 of Short notes to Free for signed-in, turn Paid mode ON.
4. Student account with NO purchase / grant: Chapter 1 opens, other chapters show a lock and the "Package required" pop-up. A student with an enrollment or grant: everything opens.
5. Lock one single material (Chapter 3 flashcards): only that row shows a lock.
6. Paid mode OFF: everything opens at once (allow up to 30 seconds).
7. Stage 1 check: open a note, a flashcard deck and a quiz as the student: they must still open (they now come through `open_resource`).
