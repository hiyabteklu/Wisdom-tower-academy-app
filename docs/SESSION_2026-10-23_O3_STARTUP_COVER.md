# SESSION 2026-10-23 O3: start-up cover (onboarding and start-up plan, slice O3)

Plan: `docs/ONBOARDING_STARTUP_PLAN.md`. State found at the start of this session: O1 (default look Transparent, `AppBackground.kt`) and O2 (new 3-screen animated `OnboardingScreen.kt`) are already in `main`. O3 to O5 were not started (no `patch_o3_*` script existed).

Owner bug: at app start the loader spins on an empty screen while the top bar and the bottom nav already show, and taps reach the tabs underneath.

Cause (read in `MainActivity.kt`, unchanged from the plan): `MainScreen` called `onReady()` in its first LaunchedEffect (system splash released at once), and the only loader is drawn inside the Scaffold content at zIndex 95, with no touch blocking.

Fix (patch script, NOT yet applied): `scripts/patch_o3_startup_cover.py`, tested on a copy of the real `MainActivity.kt` (4 edits, applies cleanly, second run does nothing).
- A full-screen cover as the last child of the root Box (zIndex 1000, above bars and overlays): the chosen app background, a bigger loader (160 dp gear rings), and it swallows every touch. Visible while `isInitialLoading`; that flag already ends when the first page rendered or at the old 3.5 s fail-safe, so it cannot stay. Fades out in 220 ms.
- The system splash is released after the first frame (`withFrameNanos`), so the first visible frame already has the cover.
- After onboarding (first run) the WebView did not exist yet, so the same gap showed; finishing onboarding now sets `isInitialLoading = true` again (cover until the first page renders, 3.5 s fail-safe).

Plan change (written here and in the plan file): the cover draws the chosen app background (`appBackdrop()`), not a solid navy fill. Reason: owner decision BG-LOADING (the start-up loader must show the chosen look), and the default look is now Transparent. The first plain frame is the look's own base colour (070C16 for Transparent, close to the splash navy).

NOT compiled, NOT run on a phone. To apply: GitHub > Actions > Apply pending patches > Run workflow > script `patch_o3_startup_cover.py` + a new branch name (see `docs/APPLY_PATCHES_WORKFLOW.md`), wait for green, test, merge with a Pull request.

Owner phone test: cold start shows one big loader over the WHOLE screen (no top bar, no bottom nav) until the app is ready, then a short fade; tapping and scrolling during that time does nothing; airplane mode cold start still ends; first install: after the 3 onboarding screens the cover shows until the first page appears.

NEXT: O4 `scripts/patch_o4_main_thread_relief.py` (move the `outerHTML` parse and `WebCacheVault.saveHtmlPage` off the main thread in `onPageFinished`, skip the second 800 ms injection round when the page changed) for the quick-tab-switch freeze. Then O5 phone test report. Open owner questions from the plan (journey order and names, existing Night users, notification permission on the last screen) are still unanswered; defaults are in use.
