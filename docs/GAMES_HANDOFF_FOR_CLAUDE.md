# GAMES HANDOFF FOR CLAUDE

## Phase status
- G0–G5 DONE (OwlPainter on main; TowerMapView + TowerClimbScreen in this commit if present)
- G6 Climb play + wire-up open

## G5 files
- ui/games/climb/OwlPainter.kt (committed)
- ui/games/climb/TowerMapView.kt — LazyColumn reverseLayout, five zone looks, owl on current, sealed chains
- ui/games/climb/TowerClimbScreen.kt — list question-banks, build tower, floor sheet, seal dialog, Jump to me

## UNSURE
- Jump-to-me animateScrollToItem index with reverseLayout may need visual tweak.

## Known risks
- Screens not wired into StudyWorkspace yet (G6 section 8).
- If TowerMapView/TowerClimbScreen missing on main after this commit, restore from agent artifacts games_src/climb/.

## CI errors seen (Claude fills this)
(none yet)
