# Update work status (newest on top; READ with RELEASE_WORKFLOW.md and IN_APP_UPDATE_PLAN.md)

## Slice 2 design (analysed, NOT yet applied)
Where it lives: `MainActivity.kt`, `setContent { ... }` inside `onCreate` (about lines 880-980). The file is ~275 KB,
so the edit must be applied with a real editor / Claude Code / git, not a whole-file API push.

Current behaviour: `LaunchedEffect(Unit)` releases the system splash after the first frame (`keepSplash = false`),
then calls `checkUpdate(true)`. While state is `Checking`, `UpdateCheckingScreen()` is visible ("Checking for required
updates..."). `Unavailable` (check failed) blocks the app with `UpdateCheckBlocked`.

Planned change (4 small edits, all in MainActivity.kt):
1. Add `var gateOpenEarly by remember { mutableStateOf(false) }` next to `updateGateState`.
2. In `checkUpdate`, treat "already open" as `updateGateState == Ready || gateOpenEarly`:
   `val wasAlreadyReady = updateGateState == UpdateGateState.Ready || gateOpenEarly`
   (so a failed check after the 1.5 s timeout fails open instead of locking a running app;
   a later `Published + isForced` result still switches to the mandatory prompt).
3. Replace the startup `LaunchedEffect(Unit)` body with:
   ```kotlin
   LaunchedEffect(Unit) {
       val check = launch { checkUpdate(true) }
       withTimeoutOrNull(1500L) {
           snapshotFlow { updateGateState }.first { it != UpdateGateState.Checking }
       }
       if (updateGateState == UpdateGateState.Checking) gateOpenEarly = true
       androidx.compose.runtime.withFrameNanos { }
       keepSplash = false
   }
   ```
   (imports: `kotlinx.coroutines.withTimeoutOrNull`, `kotlinx.coroutines.flow.first`, `androidx.compose.runtime.snapshotFlow`, `kotlinx.coroutines.launch` if missing)
4. In the `when (updateGateState)`: `Checking` -> `if (gateOpenEarly) MainScreen(...same args as Ready...) else Box(Modifier.fillMaxSize().background(Color(0xFF060B15)))`
   (never visible in practice because the splash is held). Then delete `UpdateCheckingScreen` from
   `UpdateGateScreens.kt` and its import in MainActivity.kt.
Decision to confirm with owner: a REAL failed check within 1.5 s (no network) still shows `UpdateCheckBlocked`
(existing security stance). Only a slow check fails open.

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
- `app/src/main/java/com/wisdomtower/academy/MainActivity.kt` (startup gate, `UpdateGateState`)

## Done
- Slice 1 (commit 919f071): UpdatePrompt now has phases Idle / Downloading / Installing / ReadyToInstall.
  Button is disabled while busy and shows real state; downloaded APK is kept in memory and reused by "Install";
  if the installer is not completed the button becomes "Install" (no re-download). Not yet device-tested.

## Next
- Slice 2: apply the design above (needs an editor that can patch MainActivity.kt).
- Slice 3: redesign update UI (glass bottom sheet / full screen, filled pill + quiet Telegram link).
- Slice 4: set `apk_url` in Supabase to the releases-repo asset URL (no code).
- Optional later: cold-restart APK reuse (cache check at top of `downloadApk`).
