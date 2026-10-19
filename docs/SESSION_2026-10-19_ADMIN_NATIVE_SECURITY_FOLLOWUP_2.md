# SESSION 2026-10-19 — ADMIN-native security follow-up, part 2 (live checks and website warnings)

> Repo is PUBLIC: no secrets, no student data. Continues `docs/SESSION_2026-10-19_ADMIN_NATIVE_SECURITY_FOLLOWUP.md`. CORRECTION to that file: its two app fixes were pushed in TWO commits (Database Status first, then the Free Resources message), not one. Owner order this session: Supabase connector available, allowed to do the fixes everywhere.

## Live checks (read-only SQL)
1. Competition functions `competition_leaderboard`, `competition_review`, `competition_server_time` (SECURITY DEFINER, empty search_path): all require a signed-in caller and a published contest the caller may access. The leaderboard shows names, scores and times only after the window has closed, never in the live phase. The review returns only the caller's own entry (`user_id = auth.uid()`) and only when the contest is closed; answer keys come out only then. No leak found.
2. View `public.leaderboard_by_scope`: runs with the owner's rights on purpose (it reads other students' results, which row level security hides). It lists user id, full name (or "Student"), best percent, average, attempts and score per scope, readable by any signed-in account, not by anon (already revoked, see `docs/SESSION_2026-10-16_C9_owner_orders.md`). It carried useless write-style grants for signed-in accounts (a grouped view cannot be written to, so no real exposure).
3. Policy scan: no update or delete policy is open to everybody. Open INSERT by design: inquiries and talent applications (anyone, so spam is possible), orders (anyone, but only pending status and not as someone else), and the receipts bucket upload.

## Changes made
1. LIVE DATABASE, migration `leaderboard_view_select_only`: revoked INSERT, UPDATE, DELETE, TRUNCATE, REFERENCES and TRIGGER on `leaderboard_by_scope` from signed-in and anon roles; SELECT stays for signed-in accounts. Checked afterwards: signed-in has SELECT only. No behaviour change for the app or website.
2. WEBSITE repo `main` (commits `52f5446`, `dedda74`, `fc00d1e`): new `docs/SQL_DO_NOT_RUN_ON_LIVE.md` listing every old setup script; "DO NOT RUN ON LIVE" header on `supabase-master.sql`, `free-resources-setup.sql` and `free-resources-rls-fix.sql`. The other setup scripts are covered by the new notes file only.
3. The reference copy `website-reference/docs/` in this repo is refreshed from the website by the Action "Sync website reference"; it was not edited by hand.

## Still open
- OWNER (dashboard only): confirm "Confirm email" is on; switch on leaked-password protection.
- OWNER DECISION: whether signed-in students may see other students' full names through `leaderboard_by_scope` and `get_leaderboard`. Possible fix: show first name plus initial only.
- Anonymous insert spam on inquiries, talent applications and the receipts bucket (rate limits or captcha would be needed).
- Website toasts that still say "Run docs/free-resources-setup.sql" (`FreeResourcesPanel.tsx`, `lib/free-resources.ts` in the website repo): not changed yet.
- The other setup scripts have no header of their own.
- `[~]` The two app commits are not compiled or seen on a device; CI not seen.
