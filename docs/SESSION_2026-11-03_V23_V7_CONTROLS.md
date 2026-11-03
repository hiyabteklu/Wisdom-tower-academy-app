# Session 2026-11-03 V23 — V7 controls: hub pills, back button, switch, checkbox: patch script written (Claude, chat)

Append-only. Updates the V7 row of `docs/SESSION_2026-11-03_V18_STATUS.md` and the status line in `docs/SESSION_2026-11-03_V22_V7_TOPBAR_PRESS.md`.

## Start of this session
- Owner reported: the V9, V8 and V7-topbar patches were run, the build passed, the app works. Pull requests #85, #86, #87 are merged on `main`.

## What was read (code read of `main`, a4a4930)
- `ToolPill` (Download all, Downloading x of y) and `ScreenBackButton` (back arrow of every study tool and hub list) are in `ui/study/HubItemListScreen.kt` (38 KB, not `MainActivity.kt`). Both had a ripple, no press scale and no haptic.
- The Settings `ToggleRow` already presses in and ticks (`wisdomPress` on the row). Its Material `Switch` already glides its thumb; it had no selection pop.
- The only `Checkbox` in the app screens is the terms checkbox of `NativeSignInScreen.kt`. One more `Switch` exists in `ui/admin/CompetitionsScreen.kt` (admin screen), left alone on purpose.
- `wisdomPress(interactionSource)` already exists in `WisdomMotion.kt` (scale 0.97, light release haptic), so no new press code was written.

## Written
1. Patch script `scripts/patch_v7_controls_pop.py` (marker `V7 controls pop`). It creates `ui/theme/WisdomSelectionPop.kt` and makes 7 edits in 3 files:
   - `Modifier.wisdomSelectionPop(selected, from)`: when `selected` flips to true the control starts at `from` (0.8, or 0.88 for the switch) and springs to full size (damping 0.55). Nothing on first draw, nothing when it turns off.
   - `HubItemListScreen.kt`: `ToolPill` and `ScreenBackButton` use one shared interaction source for `wisdomPress` and their clickable. Ripple, click, labels and sizes unchanged.
   - `SettingsScreen.kt`: the `Switch` gets the pop.
   - `NativeSignInScreen.kt`: the terms `Checkbox` gets the pop.
2. Nothing else in `main` is changed by this commit (the script is only added).

## Tested
Script run twice on a fresh clone of `main`: first run "patched (7 edits in 3 files, plus WisdomSelectionPop.kt)", second run "already patched". Braces balanced in all four files; parentheses keep the same difference as before the patch. Not compiled, not seen on a phone.

## OWNER STEPS
GitHub > Actions > Apply pending patches > Run workflow > script `patch_v7_controls_pop.py`, branch `fix/v7-controls-pop`. Wait for the green build, then merge the pull request.
Test on the phone:
1. Open any hub list with downloads: tap and hold "Download all": it shrinks a little and springs back, one light vibration on release.
2. In any study tool, tap and hold the round back arrow: same press feel, and Back still works.
3. Settings: flip a switch, the switch pops a little; the row still presses in.
4. Sign in screen, Create account side: tick the terms box, it pops.

## V7 still open
Plain `.clickable` rows that have no press feel (found by search, not yet listed one by one), the admin switch, chip select haptic, tab change haptic. Next slice in the plan is V5 (grow-from-card), which needs a `MainActivity.kt` patch and one chat of its own.
