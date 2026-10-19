# SESSION 2026-10-19 — Day mode off, BG 2 renamed Transparent, bottom-nav style pill

> Repo is public: no secrets. Written by Claude (chat). NOT compiled by the agent (CI decides), NOT seen on a phone.

## Owner order
"I hate the day mode entirely, comment it out, only dark and bg_2. And make the switching cool animated iOS like pill like the bottom navigation buttons. From dark to transparent (not BG_2, rename the image and the button so it becomes dark - transparent and vice versa)."

## What changed
- `ui/theme/AppBackground.kt`: `DAY_MODE_OFFERED = false` (was true). Settings now offers Dark and Transparent only; a saved Day choice falls back to Dark. Day look and day colours are NOT deleted (the owner can flip the flag back). The picture option keeps the constant name `Photo2` on purpose (the saved choice is the constant name, so a phone that saved it keeps it), but its label is now "Transparent" and its file is `backgrounds/transparent.jpg` (was "BG 2" / `bg_2.jpg`).
- `ui/settings/BackgroundSection.kt`: the pill now copies `AliveBottomNav` in `MainActivity.kt`: glass track (ink 9 percent), vertical-gradient cyan highlight with a cyan edge, ONE spring clock (damping 0.85, stiffness 420) driving highlight position, tab widths and the name fade, the chosen tab opens its name beside its icon, press squeeze to 0.94, tap haptic. Icons: moon (Dark), drop `Icons.Filled.Opacity` (Transparent).
- `assets/backgrounds/README.txt` updated.

## OWNER MUST DO (the tool used here cannot move a picture file)
On GitHub, rename `app/src/main/assets/backgrounds/bg_2.jpg` to `transparent.jpg` (open the file, tap the pencil / Edit, change the file name, commit). Until then the Transparent look shows the plain dark colour instead of the picture.

## Not changed / notes
- `Icons.Filled.Opacity` is from the extended icons set (already used by the app: DarkMode, Palette). If CI complains about it, swap it for `Icons.Filled.Image`.
- The old files `docs/DAY_MODE_PLAN.md` and `docs/SESSION_2026-10-19_DAY_BG2_ALONGSIDE.md` describe day mode and BG 2; they are history now.

## Owner: what to test
Settings > Background: two choices, moon (Dark) and drop (Transparent). Tap one: the highlight springs across, the name opens beside the chosen icon, a small tap buzz. Close and reopen the app: the choice stays.
