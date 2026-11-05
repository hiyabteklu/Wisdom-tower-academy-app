# SESSION 2026-11-05: book-url fallback key removed (Claude)

> Plain words for the owner. Not compiled, not deployed by this commit.

## What changed
`supabase/functions/book-url/index.ts` now reads only `APPWRITE_PAID_TOKEN_KEY`. The old fallback to `APPWRITE_API_KEY` is removed. This closes Note B in `docs/SESSION_2026-11-05_UPDATE_LOCK_FIX.md` section 4.

## Evidence the secret exists
Owner screenshot of Supabase > Edge Functions > Secrets (2026-11-05) lists `APPWRITE_PAID_TOKEN_KEY` under Custom secrets. The Functions page lists three functions: `book-url`, `paid-pdf-upload`, `register-device`.

## Effect
- Paid PDFs open exactly as before when the secret is valid.
- If the secret were ever deleted, `book-url` answers 500 "Server not configured" (paid PDFs close for everyone) instead of silently using a broader key.

## LEFT
- Redeploy `book-url` in Supabase: the repo change does nothing until the function is deployed.
- Test: open the one published paid PDF in the app after the deploy.
- Not verified: that the value of `APPWRITE_PAID_TOKEN_KEY` is the narrow key (file-token scope on the paid bucket only). A screenshot only shows its digest.
- `paid-pdf-upload` and `register-device` are not in this repo; their source and secrets were not checked.
