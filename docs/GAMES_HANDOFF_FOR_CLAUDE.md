# GAMES HANDOFF FOR CLAUDE

## Phase status
- G0–G5 DONE
- G6 Climb play + wire-up open

## G5 files on main
- ui/games/climb/OwlPainter.kt
- ui/games/climb/TowerMapView.kt
- ui/games/climb/TowerClimbScreen.kt

## UNSURE
- Jump-to-me reverseLayout scroll index may need visual tweak.

## Known risks
- Not wired into StudyWorkspace yet (G6 section 8).

## CI errors seen (Claude fills this)
(none yet)

## Compile notes
- TowerMapView: import foundation.lazy.items; clickable; background; height; padding
- Icons.AutoMirrored.Filled.ArrowBack, Icons.Filled.Lock
- ClimbTowerBuilder.build, GameStore.getClimb
