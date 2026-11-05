# Update work status (newest on top; READ with RELEASE_WORKFLOW.md and IN_APP_UPDATE_PLAN.md)

## Correction to the docs (important)
The app does NOT read `version.json`. Release metadata comes from the Supabase table `app_versions`
(version_code, version_name, changelog, apk_url, telegram_url, min_version_code, published), read in
`data/update/UpdateRepository.kt` (`checkLatest`). That is a tiny REST row, so bandwidth is not a problem.
Only the APK file is the big download.
=> Slice 4 is NOT a code change: upload the APK as a Release asset in `hiyabteklu/wisdom-tower-releases`
and put that HTTPS release-asset URL in the `apk_url` column. `version.json` is not needed. The app already
verifies package name, version and signature of the downloaded APK (`verifyDownloadedApk`), so sha256 is optional.

## Files that matter
- `app/src/main/java/com/wisdomtower/academy/ui/update/UpdatePrompt.kt` (update dialog + flow)
- `app/src/main/java/com/wisdomtower/academy/ui/update/UpdateGateScreens.kt`
- `app/src/main/java/com/wisdomtower/academy/data/update/UpdateRepository.kt` (check + download + verify)
- `app/src/main/java/com/wisdomtower/academy/data/update/ApkInstaller.kt`

## Done
- Slice 1 (commit 919f071): UpdatePrompt now has phases Idle / Downloading / Installing / ReadyToInstall.
  Button is disabled while busy and shows real state; downloaded APK is kept in memory and reused by "Install";
  if the installer is not completed the button becomes "Install" (no re-download). Not yet device-tested.

## Next
- Slice 2: silent check behind splash (find where `checkLatest` is called on startup; remove visible "Checking for updates" screen in UpdateGateScreens.kt; wait up to about 1.5 s on the splash, fail open).
- Slice 3: redesign update UI (glass bottom sheet / full screen, filled pill + quiet Telegram link).
- Slice 4: set `apk_url` in Supabase to the releases-repo asset URL (no code).
- Optional later: cold-restart APK reuse (cache check at top of `downloadApk`).
