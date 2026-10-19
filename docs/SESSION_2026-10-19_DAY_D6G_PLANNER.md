# SESSION 2026-10-19 — D6g: Study Planner (day mode)

> Repo is public: no secrets. Owner order: "Continue" (after D6f). One slice per chat. Written by Claude (chat). NOT compiled by the agent, NOT seen on a phone.

## What was found (read from `main`, 2026-10-19)
`ui/study/NativePlannerScreen.kt` (39 KB): every colour was a literal (white tints for the page card, grid, rail, chips; white titles and block titles; slate greys; cyan accents; navy header cells `0B1528` / `0E213D`; solid white buttons with dark text; dark editor at 30 percent navy; red now-line and Delete). By day that is white on ivory. State and storage live in `NativePlannerState` (not touched).

## What was written (file resent whole, commit `249d641`; texts, rules, palettes' `classes` strings and behaviour copied unchanged)
- Private `dayNight(night, day)` plus named getters (`Pl...`) at the bottom of the screen code. Night = the exact old literals.
- Day: page card, grid, hour rail, chips = ivory glass; header cells = strong glass, today's header and column = a faint teal tint; hour lines = ink at 10 percent (half-hour 5 percent); titles, "Now", block titles = ink; small texts = muted slate; selected day chip, "Add Block", "Create / Save", selected day in the editor = solid deep teal with white text; today dot = teal (white on a selected chip); the +Nm helper in use = day gold (`F1DFB4` fill, `5C3F0A` text); the red now-line, Delete and its border = day rose; sun / twilight / moon icons = day amber / orange / violet; "Minute-Accurate" tag and the 12-hour read-out = teal; editor = ivory 95 percent (`F2F8F5EE`) with the glass edge; the selected colour swatch ring = ink.
- The six block colours (fill and line, alpha 20 / 80 percent) are kept in both modes; the block title is ink by day (white at night, because `WisdomInk` is white at night).
- Brackets and layout unchanged (this file was already well-formed). New imports: `AppThemeMode`, `WisdomAccentAmber`, `WisdomAccentOrange`, `WisdomAccentRose`, `WisdomAccentViolet`, `WisdomCyan`, `WisdomGlassBorder`, `WisdomGlassFill`, `WisdomGlassFillQuiet`, `WisdomGlassFillStrong`, `WisdomInk`, `WisdomMuted` (all exist in `ui/theme/Color.kt`).

## Owner: what to test (both modes: Settings > Display & Theme)
1. Planner: title, "Minute-Accurate" tag, the three jump icons and "Now", "Add Block".
2. Day chips: Full Week selected / unselected, a day with blocks (count badge), today's dot.
3. Grid: day headers (today highlighted), "GMT+3" corner, hour labels and lines, today's column tint, the now-line, tap an empty hour.
4. Add a block of each colour: title readable on every tint (this is the one to look at by day). Tap a block to edit.
5. Editor: title field and placeholder, day buttons, the two time fields (and the system time picker), +30m to +120m (the one in use), colour swatches, Delete, Cancel, Create / Save.
6. Night looks exactly as before. If CI is red, send the error text.

## Not verified (hypotheses)
- Contrast of block titles (ink) on the amber, emerald and rose tints by day, and white on the day rose / deep teal buttons, was not measured (D9 will).
- Time picker (`TimePickerDialog`) is a system dialog and follows the phone theme, not the app's Day / Night switch (same as before).
- The D3 part 3, D4 and D5 patch scripts may still not have been run (see D6a and D6f logs).

## Still open in D6 (D6h onwards)
`NativeStatusScreen.kt` (31 KB), `NativeCoverPageScreen.kt` (30 KB; `CoverPageRenderer.kt` is the cover art, probably stays), `NativeTutorScreen.kt` (38 KB); `DownloadGauge.kt` unchecked.

## Board note
`docs/DAY_MODE_PLAN.md` still ends at D6a; D6b to D6g live in their session files until a later slice folds them in.

## Next
D6h: Status. The owner says "Continue".
