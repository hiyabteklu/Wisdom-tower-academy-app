# SESSION 2026-11-03 F2: Home Banner admin control center

Implemented and pushed F2 to main.

- Added a native Home Banner module to the admin console.
- Admins can view the live banner and last-change date, choose a new picture, preview its centered 16:9 crop, and publish only after confirmation.
- Images are resized to 1280×720, JPEG-compressed below 200 KB, and uploaded to the `academy-home-banners` bucket using the signed-in admin JWT.
- Back to default requires confirmation and bumps the banner version so student caches switch back to the bundled Academy image.
- Created `academy_home_banner` with a single public-readable settings row. Settings updates are RLS-restricted to `private.is_admin()` and only the image URL, version, default flag, and timestamp are writable.
- Created a public-read JPEG bucket with a 200 KB file limit. Storage insert/update policies require `private.is_admin()`; no broad storage write policy was added.
- Revoked broad table privileges on the settings table, then granted only SELECT to anon/authenticated and column-level UPDATE to authenticated admins.

Verified the settings table permissions and storage object policies in the Academy Supabase project. The student banner cache will pick up a published change on the next Home open/version check. No APK build was run by the agent.

Next: manual Telegram APK build and phone verification of the full eight-slice plan.
