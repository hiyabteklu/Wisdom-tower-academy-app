# SESSION 2026-10-19 — D7b: app-start loader in MainActivity.kt (day mode)

> Repo is public: no secrets. Owner order: "Continue" (after D7a). One slice per chat. Written by Claude (chat). NOT compiled by the agent, NOT run on the repo by the agent, NOT seen on a phone.

## What was found (read from `main`, 2026-10-19, full file `MainActivity.kt` read)
- The D4 patch is on `main` (`dayNight(...)` helper and the `AppThemeMode` import exist), so D4 was applied.
- The private `CustomCenteredLoader()` / `FuturisticGearRings()` (about lines 4496 to 4640) draw the same neon rings, ticks and arcs as the public copy `BrandSplashLoader.kt`. They are used twice: the start-up / navigation loader and the tool overlay loading.
- Both call sites draw the loader on `.appBackdrop()`, the chosen app background, so by day the loader sits on ivory. No solid dark fill behind it (BG-LOADING). So the only day problem is the loader colours themselves.

## What was written
`scripts/patch_d7b_day_app_loader.py` (5 edits, all inside `FuturisticGearRings`). Tested twice on the real `MainActivity.kt` from `main`: first run patched 5 edits, second run "already patched". Bracket balance of the file is identical before and after. Every anchor matches exactly once or nothing is written.
- `val isDark = AppThemeMode.isDark` read while composing (follows the switch).
- Base ring, gear ticks, outer arc and inner arc: night keeps the old literals; day uses teal `0B7285`, sky `0369A1`, indigo `4338CA` (same values as D7a, so the public and private loaders match).
- Nothing else in the file changes. `CustomCenteredLoader()` and the call sites are untouched.

## Owner: what to do
1. GitHub > Actions > **Apply pending patches** > Run workflow > script `patch_d7b_day_app_loader.py`, branch `fix/day-d7b-app-loader`.
2. Wait for the green build on that branch. If it fails, send the error text.
3. Test on the phone: Settings > Display & Theme, Day: close the app fully and open it again. The start-up loader (rings and ticks) should be dark teal and blue and clearly visible on the ivory page. Open a tool from the drawer (for example Calculator) and watch its loading. Then Night: the loader must look exactly as before.
4. Merge the Pull request.

## Not verified (hypotheses)
- Whether the saved Day choice is already read when the very first frame of the start-up loader is drawn. If the first frames show the dark page and then switch, that is a start-up order question, not a colour one; send a screenshot or tell me.
- The brand GIF in the middle (`brand/animation.gif`, forbidden file) cannot be seen from here; it may still look wrong on ivory.

## Left in D7
Splash and onboarding screens, native competitions, native admin console. Next chat should list the files first (search for the competition and onboarding screens) and cut them into parts like D6.
