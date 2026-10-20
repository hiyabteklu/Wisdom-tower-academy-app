# NEXT AGENT: START HERE (updated 2026-10-20 ~11:45; S5a DONE, S5b is next)

Read `docs/SECURITY_PLAN_CONTENT_ACCESS.md` first (standing owner decisions at the top: books free, Life Savers PAID, never ask about leaked-password protection / FLAG_SECURE / leaderboard). This file only adds what happened AFTER the last plan push, because the owner had little usage left to resend the whole plan. When you next edit the plan, copy these results into it (tick S5a, add the change-log lines) and delete this file.

## Results since the last plan push
- **S4 phone test PASSED (10:37, owner):** note not saved offline downloads and opens; admin save and delete work; all hubs work. S4 is finished and verified.
- **S5 prerequisite done (10:54, owner):** SECOND, PRIVATE Appwrite bucket `paid-pdfs` (bucket id `6ad71e3d00318c09972f`, project id `6aa9363c000f7ad5ebf7`, endpoint `https://fra.cloud.appwrite.io/v1`). NO roles, file level security OFF (leave it off), encryption on. The public bucket "Wisdom tower academy Resources" (free MoE/MoSHE textbooks) must NOT be touched.
- **S5 token test PASSED (11:00, owner):** a console-made file token opened the PDF on the owner's phone with `...&token=<jwt>`; the same address without `token` was refused. The token parameter is `token`; it works in a bucket with no "Any" role.
- **Owner test file in `paid-pdfs`: file id `6ad71eef0008554eae9f`** (not secret). Its old console token has expired.
- **Appwrite API key DONE (11:32, owner):** new key with ONLY the `tokens.write` scope, saved by the owner as an Edge Function secret in the Supabase dashboard (never in chat or git). The function reads `APPWRITE_PAID_TOKEN_KEY` first and falls back to `APPWRITE_API_KEY`. The secret name was not confirmed by the owner in chat and the connector cannot list secrets: if `book-url` logs `missing configuration` or an Appwrite 401, check the secret name and scope first.

## S5a DONE (2026-10-20, Supabase live; app untouched)
- Migration `s5a_open_book_rpc`: `public.open_book(p_id uuid)` returns `{status: ok|locked|not_found|not_signed_in, storage_path?, gate?}`. Same checks as `open_resource` (published row, admin or `content_guard.resource_visible`). Execute: `authenticated` and `service_role` only (verified: anon cannot run it).
- Migration `s5a_has_file_column`: generated column `learning_resources.has_file` (= storage_path is not null), SELECT granted to anon and authenticated by hand (new-column rule from S4). The admin view `learning_resources_admin` was NOT recreated (it lists columns explicitly; admins do not need `has_file`). Verified: 55 rows with a file (the books); `body_md` and `meta` still hidden from anon and authenticated.
- Edge Function `book-url` (version 1, `verify_jwt = true`, deployed with the connector). `POST {"id": "<learning_resources uuid>"}` with the student's Bearer token. Calls `rpc/open_book` with the STUDENT's token and the anon key, then (only if ok and `storage_path` starts with `appwrite-paid:`) calls Appwrite `POST /tokens/buckets/<paid bucket>/files/<fileId>` with `{expire: <now + 10 min ISO>}` and returns `{status:"ok", url, download_url, expires_at}`. Errors: 401 not_signed_in, 404 not_found, 403 locked (+ gate), 400 not_paid_file, 502 on Appwrite/DB problems. Logs only status codes, never the key.
- NOT YET TESTED END TO END: the Appwrite half (expire date format, `secret` field in the answer, key scope) can only be proven with a real student token. Test it from the owner's phone once S5b is in a build, or add a throw-away row with `storage_path = 'appwrite-paid:6ad71eef0008554eae9f'` in a locked/free test hub and open it. If Appwrite refuses the `Z` date format, send `+00:00` instead of `Z`. Delete any test row afterwards.
- The `life-savers` hub has NO rows yet (only 57 `books` rows). Life Savers rows are created by the owner in S5c, directly with `appwrite-paid:<fileId>` paths.

## What to do next
1. **S5b (app, additive):** `LearningContentRepository.bookUrl(item)`: `appwrite-paid:<fileId>` paths ask `book-url` (POST `{id}` with the student token, see `AuthStateManager.refreshIfNeeded()` pattern in `LearningProgressRepository.kt`); `appwrite:<fileId>` keeps the old public address. `BookFileStore` and the size probe use it. `locked` -> same plain locked message as notes. Token is valid about 10 minutes, so download right away.
2. **S5c (owner + agent):** owner re-uploads each Life Savers PDF into `paid-pdfs`, the admin screen stores `appwrite-paid:<newId>`, owner deletes any old public copies; then hide `appwrite-paid:` paths from the public light list (see plan, S5c).
3. **S6** small items after that.
4. The owner works from a phone only and wants short, plain steps. Push to `main` is allowed (owner rule). The git proxy blocks pushes in some sessions: use the GitHub connector `push_files` and verify.

## Do NOT
- Do not lock books. Do not ask about leaked-password protection (Supabase Pro only, owner is on free tier), `FLAG_SECURE` (stays commented out until the owner finishes the app) or the leaderboard.
- Do not paste any token or API key into git or chat.

## Live Supabase change log lines to copy into the plan
- 2026-10-20 (S5a agent): `s5a_open_book_rpc`, `s5a_has_file_column`, Edge Function `book-url` v1 (verify_jwt true).
