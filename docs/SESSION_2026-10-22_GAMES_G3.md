# SESSION 2026-10-22: GAMES G3 (Match levels)

Owner said "Continue" after G2. Slice G3 of docs/GAMES_COOL_PLAN.md.

## Done
- `MatchScreen.kt`: new last parameter `level: GameLevel = GameLevel.MEDIUM`; the engine gets `MatchTuning.forLevel(level)` (pairs on the board: Easy 3, Medium 4, Hard 6). 10 or more cards use 3 columns (smaller text) so Hard fits without scrolling.
- Easy: after a wrong pair the right partner of the first tapped card glows green for about 0.9 s.
- Hard: timer line `Time m:ss / par m:ss` (par = pairs x 8 s) under the progress bar; it stops when the set is complete.
- `MatchResult.kt`: new params `tuning` and `elapsedSeconds` (defaults = MEDIUM, 0). 3 stars need moves <= ceil(pairs x star3Factor) AND (no par or time under par); 2 stars up to pairs x star2Factor. Shows the level name and, on Hard, time vs par.
- `LevelPickerScreen.kt`: now takes `describe: (GameLevel) -> String`; public `defenseLevelLine` and `matchLevelLine` build the card text from the tuning numbers.
- `GameTabScreen.kt`: Match is now Play > picker > game too; the last level is kept per game (`GameLevelPrefs`).

## Not done / limits
- NOT compiled, NOT seen on a phone. Check the next CI run.
- The Hard timer is wall-clock; it keeps running if the app goes to the background (Match has no pause).
- Results are still not saved (slices R1-R3). Optional item `level` tags still not read (plan section 3).
- Plan table statuses for G1, G2, G3 are not updated in `GAMES_COOL_PLAN.md` (this log is the record).

## Owner phone test
Game tab > Match > course > subject > chapter > Play: "Choose level". Easy = 6 cards, a wrong pair shows the right card in green. Hard = 12 cards in 3 columns, a timer, 3 stars only when fast and few moves.
