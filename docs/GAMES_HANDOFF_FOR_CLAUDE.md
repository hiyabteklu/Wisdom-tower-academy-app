# GAMES HANDOFF FOR CLAUDE

## Phase status
- G0–G5 done; G6 play UI on main
- G7 Defense engine + scene done
- G8 Defense flow UI on main (ExamPicker, TowerDefenseScreen, Hud, Result)
- **Workspace wire still needed** — see docs/GAMES_WIRE_G8.md (StudyWorkspace + HubItemList). Local agent copies exist with full patches.

## G8 files on main
- ui/games/defense/DefenseHud.kt
- ui/games/defense/ExamPickerScreen.kt
- ui/games/defense/DefenseResultScreen.kt
- ui/games/defense/TowerDefenseScreen.kt

## Owner test after wire lands
1. Learning → course → Question Banks → Tower Climb banner → climb a floor
2. Learning → course → Exams → Tower Defense banner → Exam Run or Endless

## UNSURE
- Armored second-hit question index may skip a wave slot
- StudyWorkspace/HubItemList full file push may exceed tool size; use GAMES_WIRE_G8.md

## CI errors seen (Claude fills this)
(none yet)
