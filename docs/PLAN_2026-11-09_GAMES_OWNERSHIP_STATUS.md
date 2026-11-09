# STATUS: games ownership (read with PLAN_2026-11-09_GAMES_OWNERSHIP.md)

Updated 2026-11-09.

## Step A (app) written on branch `games-ownership-app`, NOT compiled, NOT tested on a phone
- NEW `data/games/GameAccess.kt`: `gates(scopePath, chapters)`, `gate(scopePath, chapter)`, `message(gate)`, `label(gate)`. Uses `ContentAccess.resolveLocks(route, signedIn = true)` and `CourseLocks.itemGate("games", chapter, "") ?: hubGate("games")`. `signedIn = true` is deliberate (a signed-out student gets locked chapters closed, open rows still open). Unknown scopes (not freshman, grade, remedial, ece) return Open because the lock logic does not know them.
- CHANGED `ui/games/GameHubScreen.kt`: chapter cards show "Package required" or "Coming soon" instead of "Play", no download icon when locked, tap calls new `onLocked(gate)`.
- CHANGED `ui/games/GameTabScreen.kt`: `onPlay` calls `GameAccess.gate(...)` before `loadSet`; `onLocked` shows the message text. This check is the authoritative one.
- `ContentAccess.kt` was NOT changed.
- Compile risks to check first in CI: `HubGate`, `ContentAccess.COMING_SOON_*` and `PURCHASE_*` constants are public; `CourseLocks.itemGate` and `hubGate` are public.

## Step B1 (database) done
`content_locks`: 14 rows `chapter:<scope>:games:1` and `:2`, mode `open`, for every scope that has game sets: freshman/logic and ece/sem-1/{eceg3051, eceg3061, eceg3071, eceg3081, eceg3101, econ1011}. Chapter 3+ inherits `package:freshman` / `package:ece-y3-sem-1` (require_purchase). Old app builds ignore these rows.

## Step B2 (database read rule on game_sets) NOT done on purpose
Changing `gs_public_select_published` would break the released app: it reads `game_sets` with the anon key only (`GameSetsRepository`, `GameHubIndex.packageSubjects`). Needs a decision and an app change to send the student's token first. Safer shape: keep the chapter list (chapter, chapter_title, game) public, protect `items` only (view or RPC, using `my_package_access()` or `content_guard.resource_visible`).

## Known gaps
- Until B2, a student who knows the API can still read locked `game_sets.items` with the public key.
- Play build: chapter rows decide only when present; otherwise the course / package mode applies (a non-owner is closed on require_purchase). Matches the plan.
- Offline with no saved lock rows: locked and unlocked chapters both show Package required for a package the student cannot be proven to own (same fail-closed rule as the hubs).
- Game scopes outside freshman, grade, remedial, ece (uat, coc, gat, exit-exam) are open until `routeForScope` and `ContentAccess.inferPackageAndScope` learn them. None of them has game sets today.
- Not yet done: RELEASES.md / STATUS.md entry, phone test, CI build.

---

## UPDATE 2026-11-09 (later): step B2 split in two, first half DONE (correction to the B2 text above)

### B2a (database, additive, DONE, applied with the Supabase connector, nothing the released app uses was changed)
Three functions in `public`, all `security definer`, `search_path = pg_catalog, public`, execute granted to `anon` and `authenticated` only:
- `game_set_chapters(p_scope_paths text[])` returns `chapter, chapter_title, game` (no `items`).
- `get_game_set(p_scope_paths text[], p_chapter int, p_game text)` returns `id, game, chapter, chapter_title, title, items` for ONE set, only when `content_guard.resource_visible(package_id, scope_path, 'games', chapter, id)` is true. That function reads `chapter:<scope>:games:<n>`, then hub, scope and package rows, and checks ownership (enrollments and access grants, the two ECE semesters count as one, `__all__` counts as everything).
- `game_set_subjects(p_package_id text)` returns the distinct `scope_path` values of published sets.
Tested with SQL (rolled back, nothing stored): anon gets freshman/logic chapter 1 (1 row) but not chapter 3 (0 rows) and not ECE chapter 4 (0 rows); an owner of freshman gets chapter 3 (1 row); a signed-in student with no package gets chapter 1 but not chapter 3. Chapter list as anon: 12 rows for freshman/logic.

### B2a app side (written, NOT compiled, NOT tested)
- `GameSetsRepository.kt`: `chapters` calls `game_set_chapters` (public key); `loadSet` and `download` call `get_game_set` with the signed-in student's own token (refresh first, one retry after 401), public key when signed out. An empty answer (locked chapter) is never saved to the phone and shows as "no usable set". Row shapes are the same as before, so parsing and the saved copies are unchanged.
- `GameHubIndex.kt`: `packageSubjects` calls `game_set_subjects`.
- Compile risks to check first in CI: `AuthStateManager.refreshIfNeeded()` and `refreshIfNeeded(force = true)` (same calls as in `ContentAccess.kt`), `AuthStateManager.currentUser.value?.accessToken`.
- Other readers of `game_sets` in the app were NOT searched for. Before B2b, search the whole app for `rest/v1/game_sets` and move any other reader to the functions.

### B2b (database, NOT done on purpose): close the open read on the table
Replace `gs_public_select_published` (anon and authenticated may read every published row) so only admins read the table (`gs_admin_all` stays). Do this ONLY after a build with the B2a app code is released and the old builds are retired (in-app update), because every old build reads the table directly and would show empty game lists. Steps: back up (`create table bak_game_sets_YYYYMMDD as select * from game_sets`, enable RLS like the other bak tables), drop the policy, test with the anon key (`select items from game_sets` must return nothing, the three functions must still work), keep the previous policy text below so it can be restored.
Policy to restore if needed: `create policy gs_public_select_published on public.game_sets for select to anon, authenticated using (published = true);`

### Still not done
- Pull request (needs the owner to tap Submit), CI build, phone test.
- `RELEASES.md` and `STATUS.md` entries.
- B2b above (owner decision on retiring old builds).
