# SESSION 2026-10-22: GAMES G2 (level picker) + build fix

Owner order: fix the failed build, then continue G2 (docs/GAMES_COOL_PLAN.md).

## Build fix (commit 4b3816b)
- CI error: `:app:compileReleaseKotlin`, `Redeclaration: DefenseTuning` in `DefenseTuning.kt:6:8` (`object`) and `GameLevel.kt:29:12` (`data class`). Every other error (`Unresolved reference MEDIUM / level / hintAfterWrong / scoreMultiplier / wrongLunge / boss / armoredGolem / speedScale` in `WisdomDefenseEngine.kt`) was a knock-on of that one.
- Cause: slice G1 added `data class DefenseTuning` to `GameLevel.kt`, but `DefenseTuning.kt` already has `object DefenseTuning` (old Tower Defense numbers, used by `DefenseEngine.kt` and `DefenseCanvas.kt`).
- Fix: the G1 class is now `WisdomDefenseTuning` (in `GameLevel.kt` and `WisdomDefenseEngine.kt`). The old object is untouched.
- DO NOT: never give a new top-level class/object a name that already exists in the same package (`data/games`). Search the package first.

## G2 done
- NEW `ui/games/LevelPickerScreen.kt`: "Choose level" with Easy / Medium / Hard cards, tap to select, big Play button. Text numbers are read from `WisdomDefenseTuning`.
- NEW `data/games/GameLevelPrefs.kt`: last level per game on the phone (SharedPreferences `wta_game_levels`).
- `GameTabScreen.kt`: Defense is Play > picker > game. Match is unchanged (no picker until G3).
- `WisdomDefenseScreen.kt`: new last parameter `level: GameLevel = GameLevel.MEDIUM`; engine gets `maxHearts`, `maxEnemies` and `tuning` from the level; Easy hint line shows under the question after a wrong answer. Retry keeps the level.

## Not done / honest limits
- NOT compiled and NOT seen on a phone. Check the next CI run.
- `TowerDefenseScreen` (Exams FAB) and the old `DefenseEngine` do not use levels (by design).
- Optional `level` tag on items (plan section 3, `GameLevelPool`) is still NOT read by `GameSetsRepository.parseSet`; sets use all items on every level.
- Plan table rows: G1 and G2 need the status words updated; the numbers in the plan table (enemies 2/3/4) differ from the code (2/4/5). The code numbers are what the picker shows.

## Owner phone test
Game tab > Tower Defense > course > subject > chapter > Play: "Choose level" appears. Easy = 5 hearts, slow, hint after a wrong answer. Hard = fast, up to 5 enemies, boss at the end. Close and reopen: your last level is pre-selected.
