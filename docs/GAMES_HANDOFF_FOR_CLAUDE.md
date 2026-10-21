# GAMES HANDOFF FOR CLAUDE

## Phase status
- G0–G8 done (game code + StudyWorkspace FAB entry for Climb/Defense)
- G9 polish: low-end ~30fps frame loop, particleCap factory, full inventory
- G10 not started

## Entry points
- Question Banks list → bottom FAB **Tower Climb** → TowerClimbScreen
- Exams list → bottom FAB **Tower Defense** → TowerDefenseScreen
- Optional in-list banner: docs/GAMES_HUB_PATCH.md

## File inventory

### data/games
GameQuestion.kt, GameQuestionLoader.kt (LRU 12), GameStore.kt,
ClimbRewards.kt, ClimbTower.kt, ClimbSession.kt,
DefenseTuning.kt, DefenseEngine.kt

### ui/games/common
GameTheme.kt (isLowEnd, reduceMotion, particleCap, zones),
GameClock.kt (GameFrameLoop + foreground pause + low-end 30fps),
GameAudio.kt, GameHaptics.kt, GameParticles.kt,
GameButtons.kt, GamePauseOverlay.kt, GameQuestionCard.kt, GameHubBanner.kt

### ui/games/climb
OwlPainter.kt, TowerMapView.kt, TowerClimbScreen.kt,
ClimbHud.kt, FloorSceneCanvas.kt, FloorResultSheet.kt,
ClimbPlayScreen.kt, AtticScreen.kt

### ui/games/defense
DefenseSprites.kt, DefenseCanvas.kt, DefenseHud.kt,
ExamPickerScreen.kt, DefenseResultScreen.kt, TowerDefenseScreen.kt

### study
StudyWorkspaceScreen.kt — Climb/Defense views + FAB
HubItemListScreen.kt — optional onOpenGame (GAMES_HUB_PATCH.md)

## Owner test list
1. Question Banks → Tower Climb FAB → sealed lock message → open floor → climb play
2. Exams → Tower Defense FAB → Exam Run / Endless → result + missed review
3. Background mid-play → pauses; return resumes
4. Offline after one open → cached content still plays
5. Low-RAM / animator scale 0 → still playable

## UNSURE
- Jump-to-me reverseLayout index may need visual tweak
- Armored second-hit question may skip a wave slot
- StudyWorkspace comments stripped for size; hub grid uses openGrid("hubs", …)

## Memory / safety
- Loader cache 12; Audio.release on leave; FrameLoop stops on ON_PAUSE; particles pooled 24/80

## CI errors (Claude fills)
- 2026-10-21, runs #899 to #906 (`:app:compileReleaseKotlin`), all in `StudyWorkspaceScreen.kt`:
  `307:61 Too many arguments for 'fun AtmosphereBackground(modifier: Modifier = ...)'` plus three or four `@Composable invocations can only happen from the context of a @Composable function` (lines 308, 384, 393, 406).
  - Cause: the rewritten screen called `AtmosphereBackground(modifier = ...) { ...content... }`. `AtmosphereBackground` takes ONLY `modifier` and has NO content lambda. The trailing `{ }` made everything inside it a non-composable lambda, which caused the other errors (they were knock-on errors of line 307).
  - Fix (commit daf920b): `Box(modifier = modifier.fillMaxSize()) { AtmosphereBackground(modifier = Modifier.fillMaxSize()); ...content... }`. This is the pattern every other screen in the app uses.
  - DO NOT: never put a content lambda after `AtmosphereBackground`. Always draw it as the first child of a `Box`.
  - If the next run shows NEW errors in the game files (ui/games/...), they were hidden behind these; paste the next `e: file:///` line.
