# In-app update: improvement plan

Status: in-app update WORKS (v1.1.1 test release downloaded, installed, session kept).
Goal: fix the rough edges below. Do slices in order, one commit each. Do NOT scan the whole repo; open only the update-related files (update manager / update screen / splash / version check).

## Observed problems (from device test, v1.1.1)

1. Download ran twice. After the first download hit 100% nothing changed on screen, so the user tapped "Update now" again and it restarted 0 to 100.
2. Only after that did the Android package installer appear.
3. Unknown-source permission prompt and Google Play Protect "App scan recommended" appeared. Both are expected for sideloaded APKs (permission is once per device; Play Protect goes away on Play Store release). Cannot be removed in code.
4. App closes and cold-starts after install. This is normal Android behavior (package replaced). Session stayed logged in, which is good. No fix needed; just make the first launch after update feel intentional.
5. Update card/dialog looks out of our design language; action buttons are poorly designed.
6. "Checking for updates" is visible/flickering on app open.
7. APK hosting on Supabase free tier will burn bandwidth (about 5 GB/month egress; 12 MB APK x hundreds of students).

## Slice 1: update state machine (no double download)

States: Idle -> Downloading(progress) -> Downloaded -> Installing -> Failed(reason).
- Disable the primary button while Downloading/Installing; show state label instead.
- On 100%, immediately move to Downloaded and launch the system installer automatically (no silent gap).
- Cache the APK in app cache dir under a versioned name (wta-<versionName>.apk). If it exists and size matches the server size (or checksum matches), skip download and go straight to install.
- If the user returns from the installer without installing, button becomes "Install" (re-open installer using the cached file), not "Download".
- If "Install unknown apps" is not granted, send user to the settings screen for it once, then resume install on return.
- Failed state: clear message + Retry button + Telegram fallback link.
- Delete old cached APKs after a successful update (on next launch when versionName matches).

## Slice 2: silent update check + splash handoff

- Remove any visible "Checking for updates" UI.
- Run the check in the background during the branded splash/opening animation.
- Splash waits up to about 1.5 s for the check result:
  - Update required (force) -> go straight to the update screen, never show Home first.
  - Optional update -> open the app normally, show a small non-blocking banner/sheet later.
  - Offline / timeout / error -> fail open, open the app normally, retry on next resume.
- Throttle: don't re-check more than once per few hours unless the app was force-updated.
- No flicker: splash and update screen share the same background so the transition is seamless.

## Slice 3: update UI in our design language

- Replace the pop-up card with a glass bottom sheet (or full-screen on force update) that follows Night / Day / Transparent themes.
- Buttons: one filled pill for "Update now" (turns into progress ring/bar while downloading); one quiet text link for "Get APK from Telegram". No heavy borders, consistent radius with the rest of the app.
- Show version, short "What's new" list, size (about 12 MB), and clear status text per state (Downloading / Installing / Failed).
- Keep copy plain, no jargon.

## Slice 4: move APK hosting off Supabase

Why not private GitHub: the app would need a token to download from a private repo, and that token would ship inside the APK. Not acceptable.

Chosen approach (now): a SEPARATE PUBLIC repo that holds only release files, source stays private.
- Repo e.g. `wisdom-tower-releases` (public), containing only:
  - `version.json` (latest versionCode, versionName, force flag, minSupportedVersionCode, notes[], apkUrl, sizeBytes, sha256, telegramUrl)
  - APK published as a GitHub Release asset (preferred) so the URL is stable.
- App reads `https://raw.githubusercontent.com/<owner>/wisdom-tower-releases/main/version.json` and downloads `apkUrl` from the release.
- Verify sha256 after download before installing.
- Caveat: anyone with the link can download the APK. Fine as long as no secrets are baked into the build.

Later option (more control): Cloudflare R2 bucket (10 GB free storage, zero egress fees) behind a custom domain; only the two URLs in `version.json`/config change.
Keep Telegram as the fallback button.

Release flow per version:
1. Bump versionCode/versionName.
2. Build signed APK in CI.
3. Create release in the releases repo, attach APK, compute sha256.
4. Update `version.json` last (so clients never see a version whose APK isn't uploaded yet).

## Notes

- Repo `Wisdom-tower-academy-app` is currently PUBLIC on GitHub. If it should be private, change it in repo Settings; the app must not depend on it for downloads (see Slice 4).
- Work in small pushed slices; push plan/code early because AI usage limits run out fast.
