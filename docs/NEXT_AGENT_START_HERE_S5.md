# NEXT AGENT: START HERE (updated 2026-10-20 ~12:00; S5a DONE, S5b PUSHED (not compiled), S5c is next)

Read `docs/SECURITY_PLAN_CONTENT_ACCESS.md` first (standing owner decisions at the top: books free, Life Savers PAID, never ask about leaked-password protection / FLAG_SECURE / leaderboard). This file only adds what happened AFTER the last plan push, because the owner had little usage left to resend the whole plan. When you next edit the plan, copy these results into it (tick S5a and S5b, add the change-log lines) and delete this file.

## SECRET NAME (CONFIRMED BY THE OWNER, do not confuse)
- The ONLY Appwrite secret in Supabase (Edge Functions > Secrets) is **`APPWRITE_PAID_TOKEN_KEY`**. It is an Appwrite API key named `paid-pdf-tokens` with ONLY the `tokens.write` scope, created by the owner 2026-10-20 11:30.
- `book-url` reads `APPWRITE_PAID_TOKEN_KEY` (the fallback to `APPWRITE_API_KEY` in the code is only a safety net; that secret does NOT exist). Do not create a second Appwrite secret, do not rename this one, do not use the old Appwrite key `Wisdom-Tower-Main`.
- The free books never use any key: their bucket is public (role "Any"), the app builds the plain address from ids. That is why the Supabase Secrets page has no other Appwrite key.
- The value never goes into chat or git. The connector cannot list or read secrets; if `book-url` logs `missing configuration` or an Appwrite 401, ask the owner to re-save the secret under exactly this name.

## Results since the last plan push
- **S4 phone test PASSED (10:37, owner):** note not saved offline downloads and opens; admin save and delete work; all hubs work. S4 is finished and verified.
- **S5 prerequisite done (10:54, owner):** SECOND, PRIVATE Appwrite bucket `paid-pdfs` (bucket id `6ad71e3d00318c09972f`, project id `6aa9363c000f7ad5ebf7`, endpoint `https://fra.cloud.appwrite.io/v1`). NO roles, file level security OFF (leave it off), encryption on. The public bucket "Wisdom tower academy Resources" (free MoE/MoSHE textbooks) must NOT be touched.
- **S5 token test PASSED (11:00, owner):** a console-made file token opened the PDF on the owner's phone with `...&token=<jwt>`; the same address without `token` was refused. The token parameter is `token`; it works in a bucket with no "Any" role.
- **Owner test file in `paid-pdfs`: file id `6ad71eef0008554eae9f`** (not secret). Its old console token has expired.
- **Appwrite API key + secret DONE (11:32, owner):** see "SECRET NAME" above.

## S5a DONE (2026-10-20, Supabase live)
- Migration `s5a_open_book_rpc`: `public.open_book(p_id uuid)` returns `{status: ok|locked|not_found|not_signed_in, storage_path?, gate?}`. Same checks as `open_resource` (published row, admin or `content_guard.resource_visible`). Execute: `authenticated` and `service_role` only (verified: anon cannot run it).
- Migration `s5a_has_file_column`: generated column `learning_resources.has_file` (= storage_path is not null), SELECT granted to anon and authenticated by hand (new-column rule from S4). The admin view `learning_resources_admin` was NOT recreated (it lists columns explicitly; admins do not need `has_file`). Verified: 55 rows with a file (the books); `body_md` and `meta` still hidden from anon and authenticated.
- Edge Function `book-url` (version 1, `verify_jwt = true`). `POST {"id": "<learning_resources uuid>"}` with the student's Bearer token. Calls `rpc/open_book` with the STUDENT's token and the anon key, then (only if ok and `storage_path` starts with `appwrite-paid:`) calls Appwrite `POST /tokens/buckets/<paid bucket>/files/<fileId>` with `{expire: <now + 10 min ISO>}` and returns `{status:"ok", url, download_url, expires_at}`. Errors: 401 not_signed_in, 404 not_found, 403 locked (+ gate), 400 not_paid_file, 502 on Appwrite/DB problems. Logs only status codes, never the key.
- NOT YET TESTED END TO END: the Appwrite half (expire date format, `secret` field in the answer, key scope) can only be proven with a real student token. If Appwrite refuses the `Z` date format in `expire`, send `+00:00` instead of `Z` (look at the function logs: `book-url: appwrite token http <status> <type>`).
- The `life-savers` hub has NO rows yet (only 57 `books` rows). Life Savers rows are created by the owner in S5c, directly with `appwrite-paid:<fileId>` paths.

## S5b PUSHED (2026-10-20, commit 86b31f6, app only, NOT compiled by the agent; the owner's CI builds it)
Only TWO files changed (on purpose: the three screens that show PDFs were left alone):
- `data/content/LearningContentRepository.kt`: `appwriteViewUrl(storagePath)` returns the MARKER `wtpaid://<fileId>` for `appwrite-paid:<fileId>` paths (free `appwrite:` paths unchanged: real public address). New `const val PAID_URL_PREFIX = "wtpaid://"`. New `suspend fun paidBookUrl(resourceId)`: POST `<supabase>/functions/v1/book-url` with `{id}`, the anon key as `apikey` and the student's token as Bearer (`refreshIfNeeded()`, retry once on 401 with `force = true`, same pattern as `askDoor`). Returns the `url` (must start with `https://`). Throws `BookLockedException` (new top-level class, extends IllegalStateException) for 401 / 403 / status locked, IOException for network trouble, IllegalStateException otherwise.
- `data/content/BookFileStore.kt`: `ensureFile` checks the saved file first (a saved paid PDF opens offline with no server call), and ONLY THEN swaps the marker for `paidBookUrl(key)` (key = the resource id, which both callers already pass: `item.id` / `cacheKey`). `probeFileSize` returns null for a marker (no public address to probe).
- Callers unchanged and still correct: `HubItemListScreen` (`downloadItem` and the size probe, books hub only), `StudyWorkspaceScreen` -> `PdfReaderScreen(url = appwriteViewUrl(...), cacheKey = item.id)`. A refused paid file shows the existing generic texts ("Could not load this PDF." / "Download failed..."); a nicer locked text can come later by catching `BookLockedException` in `PdfReaderScreen`.
- If CI is red: paste the line starting with `e: file:///` to the next agent and log it in `docs/BUILD_PITFALLS.md`. Things worth checking first: `AuthStateManager.refreshIfNeeded(force = true)` returns Boolean (used the same way in `askDoor`), `const val` inside the `object`.

## What to do next
1. **Owner:** wait for CI, install the build. Nothing paid exists yet, so nothing visible changes (free books must open and download exactly as before: please check ONE book).
2. **S5c test, owner + agent (also the end-to-end test of S5a + S5b):** in Admin > Course Content create ONE test item in the Life Savers hub (content type `pdf`) with `storage_path` = `appwrite-paid:6ad71eef0008554eae9f` (the test file in `paid-pdfs`), published. On the phone as a student: tap it; the PDF must download and open. Then lock it from Admin > Content locks and check it is refused. If it fails, read the `book-url` logs (connector `get_logs` / `query_logs`, source `function_edge_logs`).
3. **S5c real move:** the owner re-uploads each Life Savers PDF into `paid-pdfs`, the admin screen stores `appwrite-paid:<newId>` as `storage_path`, the owner deletes any old copies from the public bucket. Then hide `appwrite-paid:` paths from the public light list (plan, S5c: the list may return `storage_path` only when it does not start with `appwrite-paid:`; `has_file` already exists for "has a file").
4. **S6** small items after that.
5. The owner works from a phone only and wants short, plain steps. Push to `main` is allowed (owner rule). The git proxy blocks pushes in some sessions: use the GitHub connector `push_files` and verify.

## Do NOT
- Do not lock books. Do not ask about leaked-password protection (Supabase Pro only, owner is on free tier), `FLAG_SECURE` (stays commented out until the owner finishes the app) or the leaderboard.
- Do not paste any token or API key into git or chat.

## Live Supabase change log lines to copy into the plan
- 2026-10-20 (S5a agent): `s5a_open_book_rpc`, `s5a_has_file_column`, Edge Function `book-url` v1 (verify_jwt true). Secret `APPWRITE_PAID_TOKEN_KEY` set by the owner in the dashboard.
- 2026-10-20 (S5b agent): no database change (app only, commit 86b31f6).
