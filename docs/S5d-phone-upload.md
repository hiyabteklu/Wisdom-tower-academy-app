# S5d: phone upload of paid PDFs (server DONE, app WRITTEN but NOT compiled, nothing tested end-to-end)

Date: 2026-10-20.

## Server (deployed): Supabase Edge Function `paid-pdf-upload`, verify_jwt = true
- Admin-only: calls `public.am_i_admin()` with the CALLER's own token. Non-admin gets 403.
- Accepts ONE chunk per request (raw bytes, max 5 MB). Max file 50 MB. PDF only (first bytes must be `%PDF-`).
- Request headers: `Authorization: Bearer <user token>`, `apikey`, `x-file-name`, `x-total-size`,
  `x-chunk-start` (byte offset), `x-upload-id` (omitted on the first chunk, then the `file_id` returned by chunk 1).
- Answer: `{ status: "ok", file_id, done }`. Errors: `{ status: "error"|"forbidden"|"not_signed_in", message }`.
- Writes ONLY to the private bucket `6ad71e3d00318c09972f`. Uses the existing secret `APPWRITE_PAID_TOKEN_KEY`
  (Appwrite key `paid-pdf-tokens`, scope `files.write` added by the owner on 2026-10-20).
- It cannot read, list or delete anything.

## App (pushed to main, `[~]` NOT compiled, NOT tested on a device)
- `data/admin/PaidPdfUploader.kt`: reads the chosen PDF in 5 MB chunks, posts each to the function, reports progress, returns the File ID.
- `ui/admin/PaidPdfPicker.kt`: "Choose PDF from phone" button + progress bar (Android document picker, PDF only).
- `ui/admin/CourseContentScreen.kt`: the Life Savers editor shows the picker under the File ID field; on success the File ID is filled in.
  The admin still taps Save; `AdminContentRepository.save()` stores it as `appwrite-paid:<id>` (S5c). Books are unchanged.

## To do next
1. If CI fails, fix the compile errors in the three files above first.
2. Test with one small real PDF, then a large one (50 MB limit). If Supabase size/time limits break large PDFs,
   the Appwrite console upload plus the pasted File ID stays as the fallback.
3. Unverified assumption: Appwrite accepts the chunked upload exactly as the function sends it
   (`Content-Range` + `x-appwrite-id` on chunks after the first). The first real upload proves or disproves it.
4. Optional later: use the same picker for Books (public bucket, different function) only if wanted.

## Also done today (database)
- `learning_resources_hub_check` now allows `life-savers`.
