# S5d: phone upload of paid PDFs (server part DONE, app part NOT started)

Date: 2026-10-20. Not compiled / not tested end-to-end yet (server only).

## What exists now (Supabase Edge Function `paid-pdf-upload`, deployed, verify_jwt = true)
- Admin-only: calls `public.am_i_admin()` with the CALLER's own token. Non-admin gets 403.
- Accepts ONE chunk per request (raw bytes, max 5 MB). Max file 50 MB. PDF only (first bytes must be `%PDF-`).
- Request headers: `Authorization: Bearer <user token>`, `apikey`, `x-file-name`, `x-total-size`,
  `x-chunk-start` (byte offset), `x-upload-id` (empty on the first chunk, then the `file_id` returned by chunk 1).
- Answer: `{ status: "ok", file_id, done }`. Errors: `{ status: "error"|"forbidden"|"not_signed_in", message }`.
- Writes ONLY to the private bucket `6ad71e3d00318c09972f`. Uses the existing secret `APPWRITE_PAID_TOKEN_KEY`
  (the Appwrite key `paid-pdf-tokens`, scope `files.write` added by the owner on 2026-10-20).
- The function cannot read, list or delete anything.

## What is still to do (app, next small slice)
1. In the admin Life Savers editor: replace the "paste File ID" hint with a "Choose PDF" button (Android document picker, PDF only).
2. Read the file in 5 MB chunks, POST each to `/functions/v1/paid-pdf-upload`, show a progress bar, then fill the File ID field with the returned `file_id`.
3. Keep the manual File ID field as a fallback. Save stays `appwrite-paid:<id>` (already done in S5c).
4. Test with one real PDF before anything else. If Supabase limits break large PDFs, the Appwrite console upload remains the fallback.

## Also done today (database)
- `learning_resources_hub_check` now allows `life-savers` (it was missing, so Life Savers rows could not be saved).
