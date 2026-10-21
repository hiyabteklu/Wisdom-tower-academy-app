# GAMES HANDOFF FOR CLAUDE

## Phase status
- G0–G5 DONE; G6 play UI on main (workspace wiring may still need apply)
- G7 DONE: DefenseTuning, DefenseEngine, DefenseSprites, DefenseCanvas
- G8 open: ExamPicker, TowerDefenseScreen, DefenseHud, results, wire Defense

## G7 files
- data/games/DefenseTuning.kt — all numbers
- data/games/DefenseEngine.kt — fixed 1/60 step, march-as-timer, power-ups, waves
- ui/games/defense/DefenseSprites.kt — Scout/Standard/Armored + citadel paths
- ui/games/defense/DefenseCanvas.kt — side-view lane, parallax, bolt

## UNSURE
- Armored second question pulls from nextQuestionCursor (may skip a planned wave slot)

## Known risks
- G6 StudyWorkspace/HubItemList wiring may still be missing on main

## CI errors seen (Claude fills this)
(none yet)
