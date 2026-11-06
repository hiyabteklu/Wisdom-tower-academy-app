# Update work status (newest on top; READ with RELEASE_WORKFLOW.md and IN_APP_UPDATE_PLAN.md)

## State: all code slices written, NOTHING built or device-tested yet
Test everything together once the two branches below are merged.

### To merge (in this order, any order works, they touch different files)
1. Branch `feat/update-ui-redesign` (PR into main): slice 3 + cold-restart APK reuse.
   - `ui/update/UpdatePrompt.kt`: full-screen gate (card on app background, filled pill "Update now/Install" button,
     quiet "Get APK from Telegram" link). Logic is unchanged from slice 1.
   - `data/update/UpdateRepository.kt`: top of `downloadApk` reuses a verified cached APK, else downloads.
2. Branch from Actions > "Apply pending patches": script `apply_slice2.py`, branch `feat/silent-update-check`.
   Edits `MainActivity.kt` + `UpdateGateScreens.kt` (silent check behind splash, 1.5 s, fail open).
   Script lives in `scripts/` (the workflow only runs `scripts/<name>`). `tools/apply_slice2.py` is an identical copy.

### Remaining (no code)
- Slice 4: upload the APK as a Release asset in `hiyabteklu/wisdom-tower-releases`, put the HTTPS asset URL in
  `app_versions.apk_url` (and set version_code / version_name / changelog / published). No `version.json` needed.

### Test checklist
- Cold start online / slow network / airplane mode (with and without a cached release).
- Forced update: button goes Update now -> Downloading -> Opening installer... -> Install (if installer is left).
- Close the app during install, reopen: Update now should NOT re-download (cache reuse).
- Back button does nothing on the update screen.

## Correction to the docs (important)
The app does NOT read `version.json`. Release metadata comes from the Supabase table `app_versions`
(version_code, version_name, changelog, apk_url, telegram_url, min_version_code, published), read in
`data/update/UpdateRepository.kt` (`checkLatest`). That is a tiny REST row, so bandwidth is not a problem.
Only the APK file is the big download. The app verifies package name, version and signature of the downloaded APK
(`verifyDownloadedApk`), so sha256 is optional.

Note on failures: `checkLatest` already fails open. If the check fails and no release was ever cached, the app opens
(`NoPublishedRelease`). It only stays locked when a newer release was previously seen (live or cached).

## Files that matter
- `app/src/main/java/com/wisdomtower/academy/ui/update/UpdatePrompt.kt` (update screen + flow)
- `app/src/main/java/com/wisdomtower/academy/ui/update/UpdateGateScreens.kt`
- `app/src/main/java/com/wisdomtower/academy/data/update/UpdateRepository.kt` (check + download + verify)
- `app/src/main/java/com/wisdomtower/academy/data/update/ApkInstaller.kt`
- `app/src/main/java/com/wisdomtower/academy/MainActivity.kt` (startup gate, `UpdateGateState`)

## Done
- Slice 1 (commit 919f071): UpdatePrompt phases Idle / Downloading / Installing / ReadyToInstall.
- Slice 2: patch script written and dry-run tested on a copy (not applied to the repo yet).
- Slice 3 + cache reuse: pushed to `feat/update-ui-redesign` (not compiled).
