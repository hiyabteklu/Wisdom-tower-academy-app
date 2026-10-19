# SESSION 2026-10-19 — D4: top bar, bottom nav, drawer, tool header, study-timer dialog (day mode)

> Repo is public: no secrets. Owner order: "continue the open tasks on UI day mode". The next open slice in `docs/DAY_MODE_PLAN.md` was D4. Written by Claude (chat). NOT compiled by the agent, NOT seen on a phone.

## What was found (read from `main`, 2026-10-19)
- The shell lives in `MainActivity.kt` (261 KB), not in `StructuralNav.kt` (that file only holds JS strings and URL rules, no colours).
- Nine private top-level vals at the top of `MainActivity.kt` (`Accent`, `Muted`, `CardSurface`, `CardBorder`, `CardBorderSubtle`, `TextPrimary`, `DarkOnCyan`, `BarBg`, `AccentDark`) were plain values, read once at class load. They never followed the day / night switch, so by day the bar icons, tab tints and drawer text would stay night colours.
- Literal dark surfaces and `Color.White` text in the shell: top bar glass (white 4 percent), the line under it, the title, the timer pill, the tool overlay header (`070D18`, white title, light red Close), the drawer (dim, gradient, border, title, rows, footer), the nav track (white 9 percent) and the study-timer dialog (title, Pause / Resume / Stop colours).
- Not checked: whether the D3 part 3 Action (`patch_d3_day_sweep.py`) has been run. It is the last commit on `main` as a script only. D4 touches a different file, so the two do not collide.

## What was written
`scripts/patch_d4_day_shell.py` (28 edits). Tested twice on the real `MainActivity.kt` from `main` (commit `0360f81`): first run patched, second run changed nothing. Bracket and parenthesis balance of the file is identical before and after (96 changed lines). Every anchor matches exactly once or the script stops without writing.
- The nine vals become getters. At night each returns exactly the old literal (or the token that equals it). By day they return the Ivory & Navy tokens.
- A small helper `dayNight(night, day)` keeps the old literal as the night value in every pair, so night should look the same. Hypothesis until you check it on the phone.
- Day look: bars, nav track, drawer, tool header and timer dialog are 92 percent frosted ivory with navy ink text. The nav track gets a hairline border by day only. The drawer dim is navy 40 percent. The Close and Stop reds use the darker day rose so they stay readable on ivory. The timer pill amber uses the day amber. Resume text on the teal button is white by day.
- Footer text in the drawer is a little stronger by day (85 percent instead of 45) so it can be read.

## Owner: what to do
1. GitHub > Actions > **Apply pending patches** > Run workflow > script `patch_d4_day_shell.py`, branch `fix/day-d4-shell`.
2. Wait for the green build on that branch. If the build fails, send the error text.
3. Test on the phone, in BOTH modes (Settings > Display & Theme: moon = Dark, sun = Day):
   - Day: top bar (menu icon, logo border, title, trophy, bell), line under the bar, bottom nav (selected tab teal with label, unselected muted, track visible), open the drawer (dim, panel, title, TOOLS and SUPPORT labels, every row, press a row, footer text), start the study timer and check the small pill in the bar and the Focus Session dialog (Pause, Resume, Stop, Open in Study Hub), open a tool from the drawer and check the header (title, Close).
   - Night: the same screens look exactly as before.
4. Merge the Pull request.

## Known and left on purpose
- The loading spinner (gear and arcs, bright cyan `00E5FF`) is unchanged. By day it is low contrast on ivory. It belongs with the loading and splash pass, probably D7.
- The red notification dot, the unused `NavTrackTop` and the pale red fill of the Stop button are unchanged (red reads on both).
- Dialogs elsewhere (account deletion, hub gate) still wait for D6 and D7.
- Contrast numbers are still hand estimates until D9.

## Next
D5 (Account, native sign-in, forgot password). The owner says "do D5".
