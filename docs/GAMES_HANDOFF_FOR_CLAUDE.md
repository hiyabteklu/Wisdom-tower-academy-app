# GAMES HANDOFF FOR CLAUDE

## Phase status
- G0–G5 DONE
- G6 IN PROGRESS: play UI files on main; ClimbPlayScreen + TowerClimbScreen + StudyWorkspace + HubItemList wiring

## G6 files
- ClimbHud, FloorSceneCanvas, FloorResultSheet, AtticScreen (committed)
- ClimbPlayScreen.kt, TowerClimbScreen.kt (play + attic host)
- StudyWorkspaceScreen: WorkspaceView.Climb/Defense + onOpenGame
- HubItemListScreen: onOpenGame + GameHubBanner

## UNSURE
- Defense is placeholder until G8
- Jump-to-me scroll index with reverseLayout

## Known risks
- ClimbPlayScreen records floor clear once via LaunchedEffect(resultRecorded)
- Boss pool loads all open-floor banks (may be slow on first open)

## CI errors seen (Claude fills this)
(none yet)

## Compile notes
- GamePowerUpChip, GameQuestionCard, GameFrameLoop, GameAudio, GameHaptics used from common
- HubItemList: onOpenGame default null; Canvas/Brush for banner
- StudyWorkspace: fully qualified TowerClimbScreen path to avoid new imports if needed
