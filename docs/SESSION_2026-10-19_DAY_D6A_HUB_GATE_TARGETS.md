# SESSION 2026-10-19 — D6a: Hub gate pop-up and Targets screen (day mode)

> Repo is public: no secrets. Owner order: "continue the open tasks on ui day mode" (next open slice of `docs/DAY_MODE_PLAN.md`). One slice per chat (`docs/AGENT_QUOTA_PROTOCOL.md`). Written by Claude (chat). NOT compiled by the agent, NOT seen on a phone.

## Why only two files
D6 (study tools, readers, hub lists, tutor, PDF reader, dialogs) is 13 files in `ui/study/` that still use `Color.White` (about 300 KB). That is too much for one chat, so D6 is cut into parts. D6a is the two smallest self-contained ones. Left for D6b onwards: `HubItemListScreen.kt` (30 KB), `NativeNotebookScreen.kt` (33 KB), `NativePlannerScreen.kt` (39 KB), `NativeTimerScreen.kt` (28 KB), `NativeCalculatorScreen.kt` (23 KB), `NativeStatusScreen.kt` (31 KB), `NativeCoverPageScreen.kt` (30 KB) and `CoverPageRenderer.kt` (66 KB, the cover art itself: probably stays as drawn, decide later), `NativeTutorScreen.kt` (38 KB), `PdfReaderScreen.kt` (19 KB), `StudyWorkspaceScreen.kt` (27 KB, D3 part 3 already covers it by script).

## What was found (read from `main`, 2026-10-19)
- `HubGateDialog.kt`: card background was `Color(0x4D111B2E)` (30 percent dark navy, see-through: by day it would let the page through), title `Color.White`, amber badge text `FCD34D`, button text `020617` on the cyan button (the day cyan is deep teal, so dark text would be unreadable).
- `NativeTargetsScreen.kt`: all text, icons, panels, pills, the check circle and the white Add button were `Color.White` or white tints or slate greys, so by day it was white on ivory.

## What was written (both files resent whole, small files)
- Night keeps every old literal as the first half of a private `dayNight(night, day)` pair, so night should look exactly as before. Every colour is a function or getter (rule 4: never read once at class load).
- `HubGateDialog`: day card = frosted ivory 92 percent (`F8F5EE`, the overlay rule) plus the soft navy shadow (`wisdomCardShadow`), hairline border from `WisdomBorderWhite`, title `WisdomInk`, badge in the premium gold (fill `F1DFB4`, text `5C3F0A`), button text white on the deep teal.
- `NativeTargetsScreen`: panels and rows use the frosted ivory glass tokens (`WisdomGlassFill`, `WisdomGlassFillQuiet`, `WisdomGlassBorder`) by day; text is `WisdomInk` or `WisdomMuted`; the Add button and the done check are the deep teal with white content; the header icon is `WisdomAccentSky` (night value is exactly the old `38BDF8`). The old odd indentation of the summary block was tidied (same braces, same layout).

## Owner: what to test (both modes: Settings > Display & Theme, moon = Dark, sun = Day)
1. Targets (Study Tools): add a target, choose a priority, tick and untick it, delete it, empty list text, placeholder text, typed text colour.
2. The hub pop-up (open a closed hub, or any hub that shows the message card): title, badge if shown, hub name, sentence, button.
3. Night: both screens look exactly as before.
4. Build: if CI is red, send the error text.

## Known and left on purpose
- `ScreenBackButton` (the back arrow on Targets) lives in the shell files of D4; it is not part of this slice.
- The `DropdownMenu` of the priority picker uses the Material menu colours from the light scheme of D1 (`Theme.kt`); not verified on a phone.
- Contrast numbers are still hand estimates until D9.
- State of the earlier slices: the plan board said D5 was open, but `docs/SESSION_2026-10-19_DAY_D5_ACCOUNT_AUTH.md` exists (script written). The last pull requests I could list are from 2026-10-17 (newest #62), with no pull request from the D3 part 3, D4 or D5 branches. Hypothesis: the three patch scripts (`patch_d3_day_sweep.py`, `patch_d4_day_shell.py`, `patch_d5_day_account_auth.py`) have NOT been run yet. Until they are, those screens (Learning, Settings, My Subjects, top bar, bottom nav, drawer, Account, sign-in) are still dark-styled in Day.

## Next
D6b (the hub lists and the PDF reader are the most visible), then the tools. The owner says "do D6b".
