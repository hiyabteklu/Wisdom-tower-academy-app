# SESSION 2026-11-05: update lock fix, release-table hardening, review of the release-hardening work (Claude)

> Plain words for the owner. Not compiled, not phone-tested. Live Supabase changes are listed below.

## 1. What was wrong (the "Update check required" lock)
The installed app showed "Update check required" and stayed locked, although no release is published (`app_versions` is empty).
Cause: the read rule on `public.app_versions` was `published = true OR private.is_admin()` for roles `anon` and `authenticated`.
The logged-out role `anon` has no permission on schema `private`, so the rule raised "permission denied" and PostgREST answered 401.
The update gate runs before login, so every cold start got 401, and the app treated 401 like "cannot verify" (plan section 10.2 told it to block).
Supabase logs: 36 x 401 on `/rest/v1/app_versions` from the app (okhttp).

## 2. Live Supabase changes (all applied 2026-11-05 and verified)
1. `fix_app_versions_anon_read_policy`: replaced the single read rule by two:
   - "Anon reads published app versions" (anon, `published = true`)
   - "Authenticated reads published app versions, admins read all" (authenticated, `published = true OR private.is_admin()`)
2. `harden_release_table_and_admin_only_policies`:
   - `app_versions`: revoked INSERT/UPDATE/DELETE/TRUNCATE/REFERENCES/TRIGGER from `anon`; revoked TRUNCATE/REFERENCES/TRIGGER from `authenticated`. Writes were already blocked by RLS; the grants were never needed.
   - Admin-only rules on `notification_audiences`, `study_plan_pushes`, `study_plan_templates`, `study_plans` moved from role `public` to `authenticated` (logged-out callers got an error instead of no rows). Rule expressions are unchanged.
3. Rollback-only test (nothing kept, table still has 0 rows) with a staged release (min = 4):
   - anon reads the published row: yes (1 row). anon insert: blocked (permission denied).
   - signed-in non-admin student reads it: yes. student insert: blocked by RLS.
   - legacy `open_resource` (legacy client counted as v3): `update_required`, min 4.
   - `open_resource_v2` with version 3: `update_required`. With version 4: normal lookup (`not_found` for a random id).

## 3. Code change (commit 4d86618)
`UpdateRepository.kt`: when the check fails (HTTP error, parse error, blank keys) and no release was ever cached, the result is now `NoPublishedRelease` (app opens) instead of `Unavailable` (blocked).
A cached published release still locks the app offline. A live published newer release or a raised `min_version_code` still locks the app.
This replaces the rule in section 10.2 of `PLAN_APP_UPDATES.md` ("show a blocking retry state if no trustworthy metadata was ever cached").
Owner decision 2026-11-05: the app locks only when a release was published or the old version was revoked, never because a check failed.

## 4. Review results
- `profiles` 401 (112 requests): real students whose access token expired (JWT `exp` hours to days old). `fetchProfile` treats 401 as "session expired" and applies nothing. Not a database fault. The token comes from the stored web session; it is not refreshed while the WebView is closed. Not a release blocker.
- `learning_resources` 403 (12 requests, one phone, 2026-11-04 13:45-13:48): an old web client asked for `select=*`, which includes the hidden `body_md`/`meta` columns. No `select=*` call has been seen since. The column protection works as designed.
- `book-url` Edge Function (v3, source in `supabase/functions/book-url/index.ts`) is sound: it checks access with the student's own JWT, never a service key; file id is validated; the Appwrite token lives 10 minutes; the key stays server-side.
  - Note A: `client_type: "web"` is supplied by the caller and uses `open_book_web`, which has no version gate. This does not affect old v3 phones (they send no `client_type`, so they hit the legacy gate and are blocked after the minimum is raised). A modified app could still skip the version check, but only for books the account is already entitled to. Accepted limit; version gating only stops honest old installs.
  - Note B: the function falls back to `APPWRITE_API_KEY` when `APPWRITE_PAID_TOKEN_KEY` is missing. Make sure the dedicated narrow key is set, and then remove the fallback.
- Security Advisor (2026-11-05): no urgent findings. 47 "signed-in users can run SECURITY DEFINER function" warnings are the admin/competition/content doors, each checks `private.is_admin()` or the student's own id inside. `learning_resources_admin` is filtered to admins. `leaderboard_by_scope` shows names and scores of all students to any signed-in user (leaderboard feature, intended). Leaked-password protection is off (owner decision: do not ask).
- Workflow `build-apk.yml`: manual dispatch only, refuses to build without the production keystore secrets, publishes a versioned prerelease asset plus an Actions artifact. No CI run was found for recent commits, so build health is unknown.
- `UpdatePrompt.kt`: no Later/outside-tap/Back dismissal, Telegram fallback rechecks the version on return, install permission handoff works as designed. The user can still press Home; the lock is per screen, not a device lock.

## 5. Still open (owner or later slice)
- **Paid file id:** `learning_resources.storage_path` is readable by `anon` on the base table, so the file id of the one published `appwrite-paid:` row is visible (the public view masks it). Confirm in Appwrite that the paid bucket is private (file security on, no public read). If it is private the id alone is useless. Revoking the column would also break old v3 apps, so it needs a plan first.
- **Website:** the live website code is not in this repo (only a mirror). Confirm the live site uses `open_resource_web`/`open_book_web`. If it still calls `open_resource`/`open_book`, raising `min_version_code` above 3 will break the website.
- **Cached releases never expire:** a published release is cached on every phone that saw it. If you publish a wrong or broken row, phones that already saw it stay locked even after you unpublish (by design, to stop accidental unpublish from lifting a real mandatory update). Only publish after the APK link works and the APK passed the install-over-v3 test; the fix for a wrong row is a higher version.
- **Signing key:** confirm the production keystore in the GitHub secrets is the SAME key that signed the already distributed v3 APK. If not, nobody can update in place.
- **CI/device:** no CI run or phone test has been confirmed for the R2-R7 work.
