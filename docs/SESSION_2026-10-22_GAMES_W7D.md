# SESSION 2026-10-22: GAMES W7d (hub remembers the level after "Next level")

Continues `docs/SESSION_2026-10-22_GAMES_W7C.md`.

## Done (1 commit, `main`, `2294157`)
- `WisdomDefenseScreen.kt` only: tapping "Next level" now also calls `GameLevelPrefs.save(context, GAME_DEFENSE, nextLevel)`. The level picker reads that same value (`GameLevelPrefs.last`) to pre-select a level, so after playing Easy then Next level to Medium, the next time the picker opens it has Medium selected. No callback into `GameTabScreen.kt` was needed (it is untouched).

## Not covered
The picker cards and chapter rows still refresh only when the student goes back to them (they re-read the saved results then). Match has no Next level button, so nothing to do there.

## Still open
Owner phone-test fixes, MW (Match look lift), the empty boss bar for ~0.1 s at boss death (`DefenseArt.kt`: draw the bar only when `bossFrac > 0f`), `TURRET_TIP_BELOW` check (W7c).

## Evidence
`[~]` written, NOT compiled, NOT seen on a phone. `GameLevelPrefs.save(context, game, level)` was copied from the call in `GameTabScreen.kt`.

## Owner phone test
Play Defense on Easy, win, tap Next level (Medium starts). Exit to the hub and press Play on any chapter: Medium is pre-selected.
