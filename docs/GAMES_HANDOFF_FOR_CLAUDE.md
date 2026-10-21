# GAMES HANDOFF FOR CLAUDE

## Files created or changed
### G1–G3
- data/games: GameQuestion, GameQuestionLoader, GameStore
- assets/notes/game_question.html, ui/games/common/*
### G4
- data/games/ClimbRewards.kt — XP/stars/timers
- data/games/ClimbTower.kt — floors from light list; sealed never blocks
- data/games/ClimbSession.kt — Loading/Question/Reveal/FloorComplete/Failed

## Phase status
- G0–G4 DONE
- G5 Climb tower view open

## UNSURE
(none yet)

## Known risks
- Boss combined pool is built by host when starting boss session (pass full pool into ClimbSession).
- Floor sealed only if every bank locked.

## CI errors seen (Claude fills this)
(none yet)

## Compile notes
- No Compose in data/games climb files.
- Uses CourseLocks, HubGate, LearningResource from content package.
