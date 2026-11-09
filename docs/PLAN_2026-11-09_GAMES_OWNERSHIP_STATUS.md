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
