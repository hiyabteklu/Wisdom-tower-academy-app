# SESSION 2026-11-03 F1: dynamic Home banner loader

Implemented and pushed F1 to main.

- Added `HomeBannerRepository` to read the single banner settings row, cache the remote JPEG in the app's private cache, and compare the version before downloading.
- Home draws the cached file immediately when present; a small background request checks for a newer version.
- If the banner is default, unavailable, offline, or invalid, the bundled `images/home/academy.jpg` remains the fallback.
- Signed-in and signed-out welcome cards use the same image source and preserve the existing 16:9 `OpenCardLayout` design.
- The settings table / public storage bucket are created in F2; until then, the loader safely falls back to the bundled image.

No APK build was run by the agent. Test cold start, cached image display, offline fallback, and a changed banner version after F2 is live.

Next: F2 — admin Home Banner control center and database/storage policies.
