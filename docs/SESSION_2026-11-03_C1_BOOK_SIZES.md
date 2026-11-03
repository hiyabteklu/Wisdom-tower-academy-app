# SESSION 2026-11-03 C1: immediate book sizes

Implemented and pushed C1 to main.

- Added `size_bytes` to `learning_resources`; student light-list reads can read the scalar and authenticated admins can write it.
- Updated `learning_resources_admin` to include the new field while retaining its existing admin-only filter.
- The app reads size_bytes in the same light-list request as the title; its existing list cache keeps the size available offline.
- Saving a book with an Appwrite File ID measures the file with HEAD / a one-byte Range fallback and stores the byte count.
- Opening Admin > Course Content > Books backfills old rows with missing sizes. It checks response headers only and does not download PDF bodies.
- Verified the new table column, admin-view column, and SELECT/INSERT/UPDATE column privileges in the Wisdom tower academy Supabase project. Advisors ran; the existing SECURITY DEFINER view finding for learning_resources_admin was already documented in the security plan and was not altered.

No APK build was run by the agent. Owner should run the manual Telegram APK workflow and check a new book size on a student phone, then open Admin > Course Content > Books to let the old-book backfill run.

Next: D1 — app-level downloads that continue after leaving the reader.
