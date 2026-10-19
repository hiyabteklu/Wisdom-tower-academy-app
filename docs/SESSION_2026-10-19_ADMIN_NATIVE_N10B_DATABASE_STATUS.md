# SESSION 2026-10-19 — ADMIN-native N10b (Database Hub replaced by a read-only Database status)

> Repo is PUBLIC: no secrets, no student data. Plan: `docs/ADMIN_NATIVE_PLAN.md` (the slice before the last one on its list, Database and SQL Hub). Previous: `docs/SESSION_2026-10-19_ADMIN_NATIVE_N10_FREE_RESOURCES.md` (commit `2dc28a2`). **Owner order (2026-10-19): "I don't want database access from the admin dashboard, only database health and status should be shown, but no changes, or database access from there. Cuz there are many admins but I'm responsible for the database."** This reverses the plan's idea of a native SQL Hub: there will be none.

## Audit of the website panel (read, not guessed)
File read: `website-reference/src/components/admin/DatabaseHubPanel.tsx`. It never ran SQL. It did four things:
1. Row-count health check of 10 tables with the admin's own login (HEAD request, nothing written). **Kept** (read-only).
2. A fixed card about row level security and one about student ids: plain text, not read from the database. **Dropped in the app** (never show what the database did not return); kept out of the website copy too.
3. The full master schema script (tables, policies, triggers) with a Copy button, setup steps and an **Open SQL Editor** link. **Removed.**
4. No API route and no write call of any kind.

## What was pushed (one commit)
1. NEW `data/admin/DatabaseStatusRepository.kt`: asks for the exact row count of the same 10 tables (names, labels, descriptions copied from the panel) with `HEAD ... select=*` (select=* because some tables have no id column), in parallel. Each table is Active, Table missing (HTTP 404), No permission (401 or 403), Unreachable or Error. A count that could not be read shows a dash.
2. NEW `ui/admin/DatabaseStatusScreen.kt`: header with the sentence that it is read only, Refresh, a health summary (`n / 10`), one card per table. No input field, no button except Refresh.
3. EDIT `data/admin/AdminSupabase.kt`: added `headCount(path)`, which returns the whole reply (the existing `count()` only returns a number, so it cannot tell a missing table from a refusal). Nothing else changed.
4. EDIT `AdminModules.kt` (`database` is native; tile label is now **Database Status**, the id and the `db` alias are unchanged so old links still open it) and `AdminConsoleHost.kt` (Database branch). The website SQL Hub is no longer opened from the app.
5. EDIT `website-reference/src/components/admin/DatabaseHubPanel.tsx`: same panel without the SQL script, Copy button, setup steps, Open SQL Editor link and the two fixed cards.

## Limits (read this)
- `[~]` Written, NOT compiled, NOT run. Check the Actions run of this commit and of N9 and N10.
- **`website-reference` is the reference copy, not the live website.** The live website still shows its old Database and SQL Hub (with the schema script and the SQL Editor link) until the same change is made in the real website repository and deployed. Its admin tab is also still labelled Database and SQL Hub (`website-reference/src/app/admin/page.tsx`, not touched here).
- **Hiding a screen is not access control.** What an admin can read or change is decided by Supabase row level security. The native screens (Payments, Grants, Locks, Catalog, Content, Free Resources, Competitions and others) write straight to Supabase with the admin's own login, exactly like the website panels. The setup scripts in this repo (`free-resources-setup.sql`, the master script that was in the panel) give write rights to ANY signed-in account (`to authenticated using (true)`). `docs/SUPABASE_NOTES.md` says the live policies use `private.is_admin()`. HYPOTHESIS: the live database is stricter than those scripts. Nobody has checked the live policies from here.
- The repo is public and git history keeps the old panel file with the schema script. Removing it from the panel does not remove it from history.
- A student account may also see `No permission` or smaller counts for tables its own policies hide; that is the same on the website.
- Pitfall check by reading: Modifier extensions imported (`background`, `border`, `padding`, `fillMaxSize`, `fillMaxWidth`, `alpha`, `clip`); `items` imported; no composable call in a LazyColumn builder body outside `item` or `items`; smart casts use local vals (`current`, `count`); only core icon `Refresh`; no slash-star inside any comment; no secret.
- HYPOTHESIS: a HEAD request for a table that does not exist answers HTTP 404 through the REST address.
- Plan status line (plan file not resent): 2026-10-19 | Claude | N10b written `[~]`: Database Hub replaced by read-only Database status in the app and in the website reference copy. NEXT: regression pass (N11) and owner phone test; owner decides whether to audit the live row level security policies.

## What the owner should test
1. Admin Console: the last tile now says Database Status. It opens natively; Back returns to the grid. `/admin?tab=database` and `/admin?tab=db` open it too.
2. The screen shows `n / 10` and ten table cards with row counts; there is no SQL text, no Copy button and no link to the SQL Editor anywhere.
3. Refresh reloads the counts. Airplane mode: every card says Unreachable and the count is a dash.
4. Apply the same panel change in the REAL website repository and deploy it, then open the admin on the website and check the SQL script and the editor link are gone.

## NEXT
Regression pass (plan slice N11) and the owner's phone test. Open question for the owner: audit the live Supabase row level security policies (read only) so that write rights really belong to admins only.
