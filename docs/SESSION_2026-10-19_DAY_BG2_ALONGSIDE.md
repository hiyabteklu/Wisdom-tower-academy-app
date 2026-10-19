# SESSION 2026-10-19 — BG 2 offered together with Day

> Repo is public: no secrets. Written by Claude (chat). NOT compiled by the agent, NOT seen on a phone.

## Owner order
After the day-mode screenshots (light text left on the light page, black thumbnails on ivory looked wrong): "instead of inverting other colors, enable the BG_2 simultaneously with day mode, so I'll try different backgrounds. No big rewrites." He also sent a screenshot of `app/src/main/assets/backgrounds/` (only `README.txt` and `bg_2.jpg`; `bg_2.jpg` re-uploaded 13 hours before).

## What changed (one file, `ui/theme/AppBackground.kt`, resent whole; only the lines below differ)
- `AppBackground.options` is now Dark, BG 2, Day (was Dark, Day). With `DAY_MODE_OFFERED = false` it is Dark, BG 2 (was Dark only), the look before day mode came back.
- Doc comments on `DAY_MODE_OFFERED`, `AppBackgroundOption` and `options` updated to match.
- Nothing else: `BackgroundSection.kt` already builds the pill from `AppBackground.options` (picture icon for BG 2, moon for Dark, sun for Day), so the pill just gets a third slot.
- BG 2 is `isLight = false`, so it uses the night colours (light text), like before D1. Day is unchanged.

## Not changed / notes
- A saved BG 2 choice is read again (it was falling back to Dark since D1).
- BG 1 and BG 3 files are not in `assets/backgrounds/`; their enum entries stay unoffered. Blue needs no file: to try it, add `AppBackgroundOption.Midnight` to the list in `options`.
- `README.txt` in `assets/backgrounds/` still says day mode was removed (out of date, not edited).

## Owner: what to test
Settings > Background: three icons (moon, picture, sun). Tap the picture icon: BG 2 with white text everywhere, cards as in night. Tap the sun: Day as before. Moon: Dark. Close and reopen the app: the choice stays.
