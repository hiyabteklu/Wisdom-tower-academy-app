# SESSION 2026-10-19: third pill "Transparent"

Owner order: "Now add the third pill, transparent". This answers the open question in `docs/DAY_MODE_PLAN.md` section 8 (Day replaces BG 2, or sits next to it): **it sits next to it**. Settings now offers Night | Day | Transparent.

## What was written
`scripts/patch_d12_third_pill_transparent.py`: one line in `ui/theme/AppBackground.kt`, the `options` list becomes Night, Day, Transparent (`AppBackgroundOption.Photo2`, file `assets/backgrounds/transparent.jpg`, checked present, 676 KB). Nothing else changes: the Settings pill (`ui/settings/DisplaySection.kt`) makes one equal slot per option and the highlight slides over `options.size` slots, so three entries need no UI edit.

## Behaviour (read from the code, not seen on a phone)
- Transparent keeps its old look: the picture with the 35 percent dark shade, see-through glass, NIGHT colours (`isLight` is false, so `AppThemeMode.isDark` is true).
- A phone that saved Transparent before keeps it (the saved name is in the list again); a saved Night or Day is unchanged.
- With `DAY_MODE_OFFERED = false` the list is Night, Transparent as before (the Play Store gate in the plan section 7 still works).
- Not verified: whether the word "Transparent" fits one slot at 15 sp on a narrow phone (about 117 dp per slot on a 360 dp screen; I estimate it fits). If it is cut, change `fontSize = 15.sp` in `DisplaySection.kt`.

## Tested
On a fixture with the exact block: patched, then "already patched". NOT compiled.

## Owner steps
Actions > Apply pending patches > run `patch_d12_third_pill_transparent.py` (it touches a different file from D10 and D11, so it can share their branch). Build green, then Settings: three pills, tap each, check the app follows (Night black, Day ivory, Transparent picture with see-through cards).

## Note for the D9 audit
Transparent is a third look to audit in its own right (night colours over a photo). The Day audit of 2026-10-19 did not cover it.
