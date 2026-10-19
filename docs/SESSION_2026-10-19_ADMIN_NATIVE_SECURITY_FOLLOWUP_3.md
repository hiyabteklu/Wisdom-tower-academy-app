# SESSION 2026-10-19 — ADMIN-native security follow-up, part 3 (no more "run the setup script" messages)

> Repo is PUBLIC: no secrets, no student data. Continues `docs/SESSION_2026-10-19_ADMIN_NATIVE_SECURITY_FOLLOWUP_2.md`.

## What changed (message text only, no logic)
Every admin screen that told an admin to run an old setup SQL script in the Supabase SQL Editor when a table looked missing now says: "Table missing: tell the database owner. Do not run the old setup scripts." Those scripts must not be run on the live database (`docs/SQL_DO_NOT_RUN_ON_LIVE.md` in the website repo).
- APP repo (commits in order): `FreeResourcesRepository.kt` (earlier), `LocksRepository.kt` (`786f088`), `CatalogRepository.kt` (`0de8be5`), `GrantsRepository.kt` (`409d2e0`), `AdminContentRepository.kt` (`36e8c86`). Each file was resent whole with only that string and one header comment line changed.
- WEBSITE repo: `src/components/admin/FreeResourcesPanel.tsx` (`f48db04`): the two save-failed fallback toasts.

## Limits
- `[~]` Not compiled, not seen on a device; CI NOT seen. If a build is red, paste the line that starts with `e: file:///`.
- Not searched: other website admin panels for the same kind of sentence (only the free resources one was found in `src` by code search; the search index may be behind).
- The reference copy `website-reference/` in this repo still holds the old toast text until the Action "Sync website reference" runs.

## Still open (unchanged)
Owner: "Confirm email" check, leaked-password protection, decision on full names in the old leaderboard. Then plan slice N11 (regression pass and phone test).
