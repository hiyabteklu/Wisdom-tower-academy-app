# GAMES HANDOFF FOR CLAUDE

> **START HERE: `docs/GAMES_STATUS.md` (2026-10-22). It says what is done, what is unverified and what is left.** The older notes below are history and may be out of date.
>
> LIVE TASK (owner order 2026-10-21): the native games only (Wisdom Defense and Match). One small slice per chat, push straight to `main`, owner is on a free plan: plain short words, answer first. Everything else is parked (`docs/PARKED_2026-10-21.md`).

## Phase status (history)
- G0 to G8 done (game code + StudyWorkspace FAB entry for Climb/Defense)
- G9 polish: low-end ~30fps frame loop, particleCap factory, full inventory
- GAME TAB DONE (owner order 2026-10-21): bottom-nav "Packages" tab is now "Game". `ui/games/GameTabScreen.kt` (+ `GameLaunch` hand-over), the StudyWorkspace change and the MainActivity patch `scripts/patch_game_tab.py` are in `main` (PR #73, commit 4a2fd16, build #913 green).
- LIVE GAMES (docs/GAMES_HUB_PLAN.md): Wisdom Defense (`ui/games/defense/WisdomDefenseScreen.kt`, `data/games/WisdomDefenseEngine.kt`) and Match (`ui/games/match/MatchScreen.kt`, `data/games/MatchEngine.kt`). Engines are plain Kotlin; screens redraw with a `frame`/`tick` counter because engine fields are not Compose state.
- Since then (see GAMES_STATUS.md): game feel, levels, result saver, Defense cockpit and juice, Match flip, Defense power-ups.

## Entry points
- Bottom nav **Game** tab > choose Tower Climb or Tower Defense > choose a course > course page > choose a subject > the game starts at once (`GameLaunch.kind` is read once by StudyWorkspaceScreen; Back from the game closes the workspace). The newer Game tab flow is course > subject > chapter > level picker > game (see `GameTabScreen.kt`, `GameHubScreen.kt`, `LevelPickerScreen.kt`).
- Question Banks list > bottom FAB **Tower Climb** > TowerClimbScreen
- Exams list > bottom FAB **Tower Defense** > TowerDefenseScreen
- The old Packages catalogue screen (`PackagesScreen`) is no longer reachable from the bottom nav (the file is kept).

## File inventory

### data/games
GameQuestion.kt, GameQuestionLoader.kt (LRU 12), GameStore.kt, ClimbRewards.kt, ClimbTower.kt, ClimbSession.kt, DefenseTuning.kt, DefenseEngine.kt, GameHubIndex.kt, GameSetsRepository.kt, MatchEngine.kt, WisdomDefenseEngine.kt, GameLevel.kt, GameResultsRepository.kt

### ui/games
GameTabScreen.kt (bottom Game tab + `GameLaunch`), GameHubScreen.kt, LevelPickerScreen.kt

### ui/games/common
GameTheme.kt (isLowEnd, reduceMotion, particleCap, zones), GameClock.kt (GameFrameLoop + foreground pause + low-end 30fps), GameAudio.kt, GameHaptics.kt, GameParticles.kt, GameButtons.kt, GamePauseOverlay.kt, GameQuestionCard.kt, GameHubBanner.kt

### ui/games/climb
OwlPainter.kt, TowerMapView.kt, TowerClimbScreen.kt, ClimbHud.kt, FloorSceneCanvas.kt, FloorResultSheet.kt, ClimbPlayScreen.kt, AtticScreen.kt

### ui/games/defense
DefenseArt.kt, DefenseAtmosphere.kt, DefenseCockpit.kt, DefenseEffects.kt, DefenseHud.kt, DefenseResult.kt, DefensePowerUps.kt, WisdomDefenseScreen.kt (plus the older DefenseSprites.kt, DefenseCanvas.kt, ExamPickerScreen.kt, DefenseResultScreen.kt, TowerDefenseScreen.kt)

### ui/games/match
MatchScreen.kt, MatchHud.kt, MatchResult.kt

### study
StudyWorkspaceScreen.kt (Climb/Defense views + FAB + game-from-tab start), HubItemListScreen.kt (optional onOpenGame)

## UNSURE (from earlier sessions, not re-checked)
- Jump-to-me reverseLayout index may need visual tweak
- Armored second-hit question may skip a wave slot
- A package whose subject opens a workspace without a content scope (web-only hub) will not start the game
- `GameLaunch.kind` could stay set if the owner backs out in the middle of the Game tab flow and opens a Learning workspace; GameTabScreen clears it whenever it appears
- W5 wall line height uses `defenseLaneY(h, 1f)`, may sit a little off

## Memory / safety
- Loader cache 12; Audio.release on leave; FrameLoop stops on ON_PAUSE; particles pooled 24/80

## CI errors (Claude fills)
- 2026-10-21, runs #899 to #906 (`:app:compileReleaseKotlin`), all in `StudyWorkspaceScreen.kt`: `AtmosphereBackground(modifier = ...) { content }` is wrong. `AtmosphereBackground` takes ONLY `modifier` and has NO content lambda. Fix (commit daf920b): `Box(modifier = modifier.fillMaxSize()) { AtmosphereBackground(modifier = Modifier.fillMaxSize()); ...content... }`. Runs #907 to #913 were green.
