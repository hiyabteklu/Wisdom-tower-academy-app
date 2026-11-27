# S5e: choose a BOOK PDF on the phone (2026-11-27)

> Repo is PUBLIC: no secrets.

Owner order: books should be uploaded from the admin screen like Life Savers, not by pasting an Appwrite File ID.

## Why it was not possible before
The old limit (4 MB) was the website on Vercel. The app never goes through Vercel. Life Savers already upload through the Edge Function `paid-pdf-upload` (5 MB chunks, admin only, no key in the app). That function only knew the PRIVATE bucket.

## What changed
- **Live in Supabase (done, version 2):** Edge Function `paid-pdf-upload` now reads an optional header `x-target`. `books` = free Books bucket (`6aa93732001b5373c639`, up to 200 MB, file gets `read("any")` like the other books). No header = private paid bucket, up to 50 MB, exactly as before. Source is NOT in git (it was not before either); re-read it with the Supabase connector (get_edge_function).
- **App (this commit, not compiled):** `PaidPdfUploader.upload(..., target)`, `PaidPdfPicker(target)`, and the Course Content editor shows the picker for the hub `books` too. A book is still saved as `appwrite:<FILE_ID>` (not paid), the size is probed as before.
- The pasted File ID field still works for both hubs.

## Owner test on the phone
Admin Console > Course Content > a package > Books > Add item > Choose PDF from phone > wait for 100% > Save > open the book as a student.
If the upload says "Could not store the file": the Appwrite key in the function secret probably has no write access to the Books bucket. Tell the next agent.

## Still open
- Appwrite plan may cap file size below 200 MB (the function allows more than the plan may).
- `hubHint("books")` in AdminContentRepository still says to paste the File ID (text only).
