# SESSION 2026-10-20: access control (database side) and admin Content locks

> Repo is PUBLIC: no secrets, no student data. Owner order: lock specific materials (example: Chapter 1 free, the rest locked) for signed-in students WITHOUT ownership; owners (purchase, grant, `__all__`) get the whole package; locked material stays VISIBLE in the lists with a lock (never hidden); everything controlled from the admin dashboard.

## How the rules work now (live Supabase project, read and verified)
- Table `public.content_locks`, one row per lock. Keys: `item:<id>`, `chapter:<scope>:<hub>:<n>`, `hub:<scope>:<hub>`, `scope:<scope>`, `package:<id>`. MOST SPECIFIC WINS, no row = open.
- Modes: `open` = free for any SIGNED-IN student (signed-out visitors cannot read content rows since 2026-10-20); `require_purchase` = owners only; `coming_soon` and `locked` = closed for everybody, owners too (shown as coming soon).
- Owner = a row in `enrollments` or `access_grants` (by user id or by email, case-insensitive) for the package, or `__all__`; any `ece-y3-sem-*` entitlement opens both ECE semesters.
- PAID MODE switch = row `system:enforce`: mode `locked` = ON (require_purchase is enforced), anything else = OFF (require_purchase is free for every signed-in student). It is OFF now, so NOTHING is locked yet.
- Enforcement is in the database: RLS policy `lr_public_select_unlocked` on `learning_resources` calls `content_guard.resource_visible(...)`, which uses `content_guard.effective_mode(...)`. `public.list_locked_resources(...)` returns the locked rows (metadata) of signed-in users so the app can show them with a lock.
- Example for a free trial: `package:freshman` = require_purchase, `chapter:freshman/physics:short-notes:1` = open, paid mode ON.

## Changes made this session
- Migration `content_locks_chapters_audit_signedin_open` (applied live):
  - `learning_resources.chapter` was NULL on all 400 rows, so chapter locks could not match. Filled from titles ("Chapter #3 notes", "Ch-4"): 307 rows set. 93 rows stay empty (57 whole-book PDFs, 36 odd titles such as "Ch 1-4 flashcards"); lock those with the `item:` lock. A trigger (`lr_autofill_chapter`) fills the chapter on new or edited rows when empty; a chapter typed by hand always wins.
  - Audit: table `public.content_lock_log` (admin read only) records set / change / clear with the admin email; `updated_by` and `updated_at` of `content_locks` now fill themselves.
  - `content_guard.resource_visible`: `open` now needs a signed-in user.
  - Label of `system:enforce` made explicit (behaviour unchanged).
- App: `ui/admin/LocksScreen.kt` (admin > Content locks): new PAID MODE ON/OFF card at the top, plain mode names (Free for signed-in / Owners only / Coming soon / Hard locked), a shortcut for every package (was 3). NOT compiled here: check the GitHub Action.

## Known gaps (not done)
- Book PDFs live in Appwrite and the website proxy `/api/content/pdf` serves them without any check (website repo, owner: not a priority). The database hides the row of a locked book, but a known file id still downloads. Notes, flashcards, question banks and exams are protected.
- The student app UI (`ContentAccess`, `HubItemListScreen`) was written before the database rules; it needs a phone test with a non-enrolled test account (package locked, Chapter 1 open, paid mode ON) to confirm locked rows show a lock and open rows open.
- Package id `ece-y3-full` exists in the catalog and in orders, but nothing in the database maps it to the two semester packages and no resources use it. Confirm how a Full purchase should unlock before selling it.
- The app still has a hardcoded `IS_FREE_MODE` in `ContentAccess.kt` (mirror of the website flag).

## TEST (owner)
1. GitHub Action green, install the new APK, sign in as admin: Content locks shows the PAID MODE card at the top.
2. Pick one subject: set its package to Owners only, Chapter 1 of Short notes to Free for signed-in, then turn Paid mode ON.
3. With a signed-in account that has no enrollment or grant: Chapter 1 opens, the other chapters show a lock. With an enrolled or granted account: everything opens.
4. Turn Paid mode OFF to open everything again at once.
