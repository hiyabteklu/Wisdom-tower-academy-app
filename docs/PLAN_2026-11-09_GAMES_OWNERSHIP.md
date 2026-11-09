# PLAN 2026-11-09: Games follow package ownership

Written so another agent can continue without the chat. Owner: Hiyab. Supabase project: "Wisdom tower academy" (id `amieczmpqrvavzjikbdi`, eu-central-1).

## 1. Goal (owner order)
Games (Match, Wisdom Defense) must be related to package ownership, with the same rule as the learning content:
- Chapter 1 and 2 of every game: free for any signed-in student.
- Chapter 3 and up: only for students who own the package. Others see "Package required".
- Applies to every package that has game sets. Today that is `freshman` (12 sets) and `ece-y3-sem-1` (70 sets). Grades 9-12 and Remedial have no game sets yet.

The owner said "make it related to package ownership too". The "chapter 1 and 2 free" part is the assumed rule (same as the content rule below). Confirm with the owner if in doubt.

## 2. Findings (what games do today)
Read from the repo (main at c6ba67b, 2026-11-08) and the database.

App, `app/src/main/java/com/wisdomtower/academy/`:
- `ui/games/GameTabScreen.kt`: course list comes from `enrolledGamePackages(profile.educationLevel, ...)` in `ui/games/GameEnrollment.kt`. That is the academic level in the profile, NOT purchases. `onPlay` loads the set with `GameSetsRepository.loadSet(...)` and plays it. No lock or ownership check.
- `ui/games/GameHubScreen.kt`: flow is game, course, subject, chapter list. `ChapterStep` lists chapters from `GameSetsRepository.chapters(context, scopePath)`. A chapter shows "Play" if a published set exists, else "Coming soon". No lock check.
- `data/games/GameHubIndex.kt`: subjects come from `ADMIN_CONTENT_TREE` (`knownSubjects`) or from `game_sets` by `package_id` (`packageSubjects`).
- `data/games/GameSetsRepository.kt`: reads `game_sets` with the anon key (not read in detail yet).
- `data/content/ContentAccess.kt`: the lock and ownership logic used by the learning hubs. Games never call it.

How locks work in `ContentAccess.kt` (read it before coding):
- Lock rows live in `public.content_locks`. Keys: `package:<id>`, `scope:<path>`, `hub:<path>:<hub>`, `chapter:<path>:<hub>:<n>`, `item:<learning_resources.id>`. Modes: open, require_purchase, coming_soon, locked.
- Ownership comes from the RPC `public.my_package_access()` (enrollments + access_grants). `__all__` means everything. The two ECE semesters count as one.
- TELEGRAM PAID BUILD (`BuildConfig.PAID_UI` true): only `item:`, `chapter:` and `package:` rows count, most specific wins. `CourseLocks.itemGate(hubId, chapter, itemId)` returns item row, else chapter row, else package mode, then applies owned / signed-in. Hub cards are never closed.
- PLAY BUILD (`PAID_UI` false): `hubGate` uses hub row, else scope / package mode. With `package:freshman` = require_purchase a non-owner is blocked at the hub card, so item or chapter open rows alone do not help there. Check which build the owner ships before choosing the design. The 2026-11-06 release (1.1.2) is the Telegram build.
- `resolveLocks(basePath, signedIn)` takes a website-style route such as `/academy/freshman/physics` or `/special-packages/electrical-computer-engineering/sem-1/eceg3071` and returns a `CourseLocks`.

Database:
- `game_sets` columns: id, package_id, scope_path, chapter, chapter_title, game ('defense' | 'match'), title, items jsonb, published, sort_order.
- RLS: `gs_public_select_published` lets anon and authenticated read every published row. `gs_admin_all` is admin only. So locked chapters are readable by anyone through the API.
- No `content_locks` key mentions games.
- Counts: freshman 6 defense + 6 match (chapters 1-6), ece-y3-sem-1 35 defense + 35 match (chapters 1-7).
- `public.bak_game_sets_20261106` is a backup from 2026-11-06 (70 rows, ECE only).
- There is a server function `content_guard.resource_visible` (mentioned in `ContentAccess.kt`, guards resource downloads). NOT inspected yet. Look at it before writing the RLS change, it may be reusable.

## 3. Plan
### Step A, app (new branch, pull request, do NOT push to main)
1. In `GameHubScreen.ChapterStep`, resolve locks for the subject (via `ContentAccess.resolveLocks` with the route that matches the package, see `inferPackageAndScope`) and compute a gate per chapter.
2. Lock key to use for a game chapter: `chapter:<scope_path>:games:<n>`. Add a hub id `games` to the hub id set used by lock helpers if needed (`HUB_IDS` in `ContentAccess.kt` is private and only used to strip a trailing hub from a route). Do not break the existing hubs.
3. Gate result: Open = play. PackageRequired = show "Package required" on the chapter card and block the tap (reuse `PURCHASE_TITLE` / `PURCHASE_BODY`, no price, no purchase button, same as the hubs). ComingSoon = show "Coming soon".
4. Also check at `GameTabScreen.onPlay` (defence in depth), so a locked chapter can never start.
5. Signed out: follow the existing rule (the native screens show "Sign in to open").
6. Offline downloads (`GameSetsRepository.download`) must not save a locked chapter.
7. Update STATUS.md / RELEASES.md / docs per repo habit. Code is written but not compiled in CI before merge, so keep the change small. Builds run through manual CI jobs.

### Step B, database
1. Lock rows (after the app change is merged, or before, harmless): insert `chapter:<scope>:games:1` and `:2` with mode `open` for every scope that has a game set, for both games (one row covers both games of that chapter). Chapter 3+ inherits `package:<id>` = require_purchase. Get scopes with `select distinct package_id, scope_path from game_sets where published`.
2. RLS on `game_sets` so locked chapters cannot be fetched without ownership. Replace `gs_public_select_published` with a policy that allows: published AND (chapter has an open lock row OR the user owns the package, via `my_package_access` logic or `content_guard.resource_visible`). Test with an anon key, a signed-in non-owner and an owner. Keep `gs_admin_all`. Back up first (`create table bak_game_sets_YYYYMMDD as select * from game_sets`, enable RLS on it like the other bak tables).
3. Warn: the app reads `game_sets` with the anon key (`GameSetsRepository`, `GameHubIndex.packageSubjects`). After the RLS change those calls need the student's token or the chapter list will lose locked chapters. Decide: keep chapter titles visible (list only chapter, chapter_title, game columns, no `items`) and protect only `items`. Safer option: a view or RPC that returns chapter lists to everyone and `items` only to allowed users.

### Step C, verify
- Student with no purchase: Freshman game chapters 1-2 play, 3+ show "Package required".
- Student who owns Freshman: all chapters play.
- ECE owner of either semester: all ECE chapters play.
- Signed out: sign-in message.
- API test: anon `select items from game_sets where chapter=3` returns nothing after Step B2.

## 4. Done already (2026-11-09, database only)
All through the Supabase connector on `content_locks`, updated_by = `claude-mcp`.
- Freshman: all 21 books open. Chapter 1 and 2 of exams, question banks, flashcards and notes open as item rows (`item:<learning_resources.id>`, 163 rows). Chapter 3+ inherits `package:freshman` = require_purchase.
- ECE Year 3 Sem 1 (`ece-y3-sem-1`): all 21 books open. Chapter 1 and 2 of the four hubs open (64 items) plus 8 notes titled "- 1 ..." or "- 2 ..." that have no chapter number. 27 other no-chapter items still locked (Ch 1-4 exams, Lecture 1 and 2 items, Lab IV overview, Workshop Practice II exam and question bank): owner has not decided.
- Grades 9, 10, 11, 12 and Remedial: only books exist, all 36 open.
- Deleted 4 old rows: `scope:freshman/math-natural` (open), `hub:freshman/math-natural:exams` (open), `scope:ece/sem-1/meng3052` (open), `hub:ece/sem-1/meng3052:exams` (open). They opened whole courses. To restore, insert them again with mode `open`.
- Left alone: `system:enforce` = locked (paid mode on), all `package:*` rows, life-savers (Psychology has one, still locked), `chapter:ece/sem-1/eceg3073:*:1` coming soon, the item-level coming soon rows.
- `ece-y3-sem-2` has no resources yet.

## 5. Open questions for the owner
- Same rule for games (chapters 1-2 free, rest owners)? Assumed yes.
- Which build must carry this, Telegram paid build (`PAID_UI`) or the Play build?
- The 27 ECE items with no chapter number: open or locked?
- Should game chapters 1-2 show as free even when a student has not chosen that package in Settings? (The course list still comes from the profile education level.)

## 6. Notes
- Memory says the Academy Android app code also lives on GitLab (`hiyabteklu720/wisdom-tower-academy-app`) because of an earlier GitHub suspension. The GitHub repo `hiyabteklu/Wisdom-tower-academy-app-packed` had a merge on 2026-11-08, so check which one is the live source before pushing code.
- The lock rules are applied by the app, not the database, except downloads (`content_guard`). That is why Step B2 matters.
