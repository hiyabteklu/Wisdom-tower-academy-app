# ONBOARDING + START-UP REDESIGN PLAN (owner order 2026-10-23)

> Repo is PUBLIC: no secrets, no student data. Written by Claude (chat) before any code, because the owner's quota may run out.
> Evidence labels: VERIFIED = read in code on `main` 2026-10-23. HYPOTHESIS = not proven, needs a phone test or log.

## 0. SLICE STATUS (updated 2026-10-23)
- [x] O1: default look Transparent (`AppBackground.kt`, in `main`).
- [x] O2: new 3-screen animated `OnboardingScreen.kt` (welcome, journey, orbit; in `main`).
- [x] O3: `scripts/patch_o3_startup_cover.py` written and tested on a copy; owner runs Actions > Apply pending patches (log: `docs/SESSION_2026-10-23_O3_STARTUP_COVER.md`). NOT applied yet.
- [ ] O4: `scripts/patch_o4_main_thread_relief.py` (next).
- [ ] O5: owner phone test, then fix from the report.

Plan changes: (1) the O3 cover draws the chosen app background (`appBackdrop()`), not solid navy (owner decision BG-LOADING; default look is now Transparent). (2) O3 also brings the cover back after onboarding finishes, because the WebView does not exist before that.

## 1. Owner words (2026-10-23, copied)
- Serious redesign of the onboarding ("boarding") screens. Use animation.
- Screen A: ONE page, continuous journey: Grade 9 to university and exit. Owner's stage list: Grades 9-12, Remedial, COC, GAT, EUEE (also university years and the exit exam).
- Screen B: ONE page, everything together: AI tutor, planner, notes, scholarship, info, tools and resources, as animated icons around our brand. "Accuracy, leading with tech."
- Onboarding shows on a NEW launch only (first run). English only. Colours stay as the current app (navy, cyan, gold).
- SPLASH BUG: the custom loading animation spins on an EMPTY screen while the top bar and bottom nav already show. It should be BIG at start, cover the whole screen, and hide the gap until the app is ready.
- FREEZE BUG: after opening, if the student scrolls or switches tabs (Home, Learning, Games ...) quickly, the app freezes once. Only when acting right after opening.
- DEFAULT LOOK: the "Transparent" look must be the default, not Dark/Night.

## 2. Verified findings (code read)
1. `OnboardingScreen.kt` (12.7 KB, small, editable whole) had FIVE pages, not three: welcome (GIF), "Everything for your journey", "Zero margin for error", "Powerful AI...", "Stay on track" (notification permission). Flags: `hasCompletedOnboarding` / `setOnboardingCompleted` use SharedPreferences `wta_prefs` key `onboarding_done`. (Replaced by O2.)
2. ROOT CAUSE of the empty-screen loader (VERIFIED): in `MainActivity.kt`, `MainScreen` calls `onReady()` in its very first `LaunchedEffect(Unit)` (about line 1591), so the system splash is released at once. The app loader (`CustomCenteredLoader`, zIndex 95, about line 3564) is drawn INSIDE the Scaffold content area, so the top bar and bottom nav are drawn on top of / around it. That is exactly "loader rotating on an empty screen with the nav showing".
3. Start-up timing (VERIFIED): the loader stays while `isInitialLoading`; it ends when the web page reports progress >= 60 or `onPageCommitVisible`, with a 3.0 s wait loop and a 3.5 s hard fail-safe.
4. The loader overlay has no touch blocking (VERIFIED: no `pointerInput` on it), so taps pass through to the tabs and WebView underneath while it shows.
5. Heavy main-thread work after every page (VERIFIED in `onPageFinished`): `document.documentElement.outerHTML` is read through `evaluateJavascript`; its callback (UI thread) runs `JSONTokener(...).nextValue()` on the whole page and `WebCacheVault.saveHtmlPage(...)`; then 7 scripts are injected, and 4 of them again after 800 ms. Quick tab switching repeats this for every page.
6. Default look: `AppBackground.readSaved()` fell back to `AppBackgroundOption.Black` (Night). `AppBackgroundOption.Photo2` is the "Transparent" picture (`backgrounds/transparent.jpg`). (Fixed by O1.)

## 3. Hypotheses (not proven)
- H1 (freeze): main-thread jank from finding 5 plus a WebView load per quick tab tap, while the app is still starting (BrandBytes/GIF decode, backdrop picture decode, cache vault). Needs an ANR/logcat or a phone test after the fix.
- H2: while the loader overlay is up the student can already tap tabs (finding 4); removing that chance may remove most freezes by itself (O3 does this).

## 4. Design

### Screen 1 (welcome, brand, short)
Logo GIF big with soft animated cyan glow rings. Existing text only: "Welcome to Wisdom Tower Academy" and "Your complete academic companion for Ethiopia."

### Screen 2: the journey (one continuous animation)
A glowing line draws down the page; each stage lights up when the line reaches it, a bright dot travels along the line. Stops, in the owner's words: Grades 9-12, EUEE, Remedial, Freshman, University, Exit Exam; COC and GAT shown as two extra glowing chips. ORDER IS A PROPOSAL: owner must confirm the order and the exact stage names before release (accuracy rule). Final line only if the owner approves a text.

### Screen 3: everything in one place
The brand logo in the centre, two orbit rings turning slowly in opposite directions, icons on them: AI Tutor, Planner, Notes, Scholarship, Info, Tools, Resources. Icons only from names already compiling in the project (AutoAwesome, CalendarMonth, EditNote, School, Info, Calculate, MenuBook) unless checked. Tap on an icon shows its one-word label. One button: "Start learning" (asks the notification permission as today), plus "Skip" on screens 1 and 2.

### Motion rules
Only `graphicsLayer` / alpha / Canvas progress (cheap on budget phones). Respect the system "remove animations" setting. Swipe and Next button as today.

### Start-up fix (MainActivity, patch script)
- A full-screen start-up cover at the ROOT of `MainScreen` (above top bar and bottom nav): the chosen app background (plan change, was solid `BarBg` navy), big loader (160 dp), swallows all touches, stays until `isInitialLoading` is false (first page rendered, existing 3.5 s fail-safe stays), then fades out in 220 ms.
- Keep the system splash until the first frame of this cover is drawn (call `onReady()` after one frame, not before).
- O4: move the `outerHTML` parse and `WebCacheVault.saveHtmlPage` to a background thread; skip the second 800 ms injection round if the page changed.
- `MainActivity.kt` is 264 KB: edit ONLY by patch script (`docs/EDITING_LARGE_FILES.md`), exact anchors, safe to run twice, tested on a copy.

### Default look
`AppBackground.kt`: default in `readSaved()` is `Photo2` (Transparent). Phones that already saved Night or Day keep their choice; only phones with no saved choice (new installs) get Transparent. Cold-start navy of the system splash stays (theme XML is a protected area).

## 5. Slices (one per chat)
- O1: `AppBackground.kt` default to Transparent (tiny, direct commit). DONE.
- O2: new `OnboardingScreen.kt` (3 screens, animated). Whole-file replace. Same function names and the two SharedPreferences helpers unchanged. DONE.
- O3: patch script `scripts/patch_o3_startup_cover.py` (cover, onReady timing, touch block). Owner runs Actions > Apply pending patches. WRITTEN, NOT APPLIED.
- O4: patch script `scripts/patch_o4_main_thread_relief.py` (HTML cache off the main thread). Same way.
- O5: owner phone test (checklist below), then fix from the report.

## 6. Owner phone test
1. Fresh install (or clear data): onboarding shows once, 3 screens, swipe and Next work, Skip works, "Start learning" asks notification permission, second launch skips onboarding; after onboarding the big cover shows until the first page appears.
2. Cold start: a big loader covers the WHOLE screen (no top bar, no bottom nav visible) until the app is ready, then fades.
3. Right after opening, tap tabs fast and scroll hard 10 times: no freeze. If it still freezes: send the time it happened (logcat or an "App not responding" screenshot).
4. New install with no saved choice: background is Transparent. Settings still lets you pick Night or Day.
5. Airplane mode cold start: cover still ends (offline page shows).

## 7. Open questions for the owner
1. Confirm the journey ORDER and names: Grades 9-12, EUEE, Remedial, Freshman, University, Exit Exam, then COC and GAT chips.
2. Should existing users who saved Night be moved to Transparent? Default plan: NO (respect their choice).
3. Keep the notification permission request on the last screen? Default: yes.

## 8. Rules checked
No forbidden file is edited (`.env.example`, `.github/`, gradle files, manifest, themes XML, branding assets). No new dependency, no new network host, no invented numbers. All text above comes from the owner's words or existing onboarding text.
