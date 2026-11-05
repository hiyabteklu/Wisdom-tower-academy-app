# SESSION 2026-11-05: update lock fix (Claude review of the release-hardening work)

> Plain words for the owner. Not compiled, not phone-tested.

## What was wrong
The installed app showed "Update check required" and stayed locked, although no release is published (`app_versions` is empty).
Cause: the Supabase read rule on `public.app_versions` was `published = true OR private.is_admin()` for roles `anon` and `authenticated`.
The logged-out role `anon` has no permission on schema `private`, so the rule raised "permission denied" and PostgREST answered 401.
The update gate runs before login, so every cold start got 401 and the app treated it as "cannot verify".
Supabase logs: 36 x 401 on `/rest/v1/app_versions` (okhttp), also 112 x 401 on `/rest/v1/profiles` (not yet traced).

## Live Supabase change (applied 2026-11-05, verified as role anon: no error)
- Migration `fix_app_versions_anon_read_policy`: dropped "Published app versions are readable, admins read all"; created
  "Anon reads published app versions" (anon, `published = true`) and
  "Authenticated reads published app versions, admins read all" (authenticated, `published = true OR private.is_admin()`).

## Code change (commit 4d86618)
`UpdateRepository.kt`: when the check fails (HTTP error, parse error, blank keys) and no release was ever cached, the result is now `NoPublishedRelease` (app opens) instead of `Unavailable` (blocked).
A cached published release still locks the app offline. A live published newer release or a raised `min_version_code` still locks the app.
This changes section 10.2 of `PLAN_APP_UPDATES.md` ("show a blocking retry state if no trustworthy metadata was ever cached"). Owner decision 2026-11-05: the app locks only when a release was published or the old version was revoked, never because a check failed.

## Open items found in the review
- `profiles` returns 401 for logged-out requests: trace and fix.
- Admin-only rules on `notification_audiences`, `study_plan_pushes`, `study_plan_templates`, `study_plans` use `private.is_admin()` for role `public`: logged-out callers get an error instead of an empty result. Harmless, tidy later.
- `learning_resources.storage_path` is readable by `anon` on the base table, so the file id of the one published `appwrite-paid:` row is visible (the public view masks it). Confirm the Appwrite bucket is private before relying on this; S5 is still open.
- `leaderboard_by_scope` is a SECURITY DEFINER view readable by every signed-in user (names and scores). Intended for the leaderboard; keep in mind.
- The live website code is not in this repo (only a mirror). Confirm the live website uses `open_resource_web` / `open_book_web` and not the legacy doors, otherwise raising `min_version_code` above 3 would break the website.
