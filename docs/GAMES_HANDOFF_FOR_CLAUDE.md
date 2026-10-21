# GAMES HANDOFF FOR CLAUDE

> **LIVE TASK (owner order 2026-10-21): GAME FEEL.** Read `docs/GAMES_FEEL_PLAN.md`. Do the next OPEN slice (D1 first), one slice per chat, push to `main`, update the plan table in the same commit. The owner is on a free plan: plain short words, answer first. This overrides the old "do not rewrite the games" line in CLAUDE.md for the two live games only (Wisdom Defense, Match).

## Phase status
- G0–G8 done (game code + StudyWorkspace FAB entry for Climb/Defense)
- G9 polish: low-end ~30fps frame loop, particleCap factory, full inventory
- G10 not started
- GAME TAB DONE (owner order 2026-10-21): bottom-nav "Packages" tab is now "Game". `ui/games/GameTabScreen.kt` (+ `GameLaunch` hand-over), the StudyWorkspace change and the MainActivity patch `scripts/patch_game_tab.py` are ALL in `main` (PR #73 merged, commit 4a2fd16, build #913 green). Not yet tested on a phone by the owner.
- LIVE GAMES NOW (docs/GAMES_HUB_PLAN.md): Wisdom Defense (`ui/games/defense/WisdomDefenseScreen.kt`, `data/games/WisdomDefenseEngine.kt`) and Match (`ui/games/match/MatchScreen.kt`, `data/games/MatchEngine.kt`). Engines are plain Kotlin; screens redraw with a `frame`/`tick` counter because engine fields are not Compose state.

## Entry points
- Bottom nav **Game** tab → choose Tower Climb or Tower Defense → choose a course → course page (subject list, same PackageDetailScreen as before) → choose a subject → the game starts at once (`GameLaunch.kind` is read once by StudyWorkspaceScreen; Back from the game closes the workspace)
- Question Banks list → bottom FAB **Tower Climb** → TowerClimbScreen
- Exams list → bottom FAB **Tower Defense** → TowerDefenseScreen
- Optional in-list banner: docs/GAMES_HUB_PATCH.md
- The old Packages catalogue screen (`PackagesScreen`) is no longer reachable from the bottom nav (the file is kept). Its course page is still opened from the Learning tab (it sets `selectedIndex = 2`, which is now the Game tab).

## File inventory

### data/games
GameQuestion.kt, GameQuestionLoader.kt (LRU 12), GameStore.kt,
ClimbRewards.kt, ClimbTower.kt, ClimbSession.kt,
DefenseTuning.kt, DefenseEngine.kt,
GameHubIndex.kt, GameSetsRepository.kt, MatchEngine.kt, WisdomDefenseEngine.kt

### ui/games
GameTabScreen.kt (bottom Game tab + `GameLaunch`), GameHubScreen.kt

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
ExamPickerScreen.kt, DefenseResultScreen.kt, TowerDefenseScreen.kt, WisdomDefenseScreen.kt

### ui/games/match
MatchScreen.kt

### study
StudyWorkspaceScreen.kt — Climb/Defense views + FAB + game-from-tab start
HubItemListScreen.kt — optional onOpenGame (GAMES_HUB_PATCH.md)

## Owner test list
1. Question Banks → Tower Climb FAB → sealed lock message → open floor → climb play
2. Exams → Tower Defense FAB → Exam Run / Endless → result + missed review
3. Background mid-play → pauses; return resumes
4. Offline after one open → cached content still plays
5. Low-RAM / animator scale 0 → still playable
6. Game tab: Tower Climb → course → subject → game opens; Back returns to the course page; same for Tower Defense

## UNSURE
- Jump-to-me reverseLayout index may need visual tweak
- Armored second-hit question may skip a wave slot
- StudyWorkspace comments stripped for size; hub grid uses openGrid("hubs", …)
- Game tab: a package whose subject opens a workspace without a content scope (web-only hub) will not start the game; it just shows the hub grid
- `GameLaunch.kind` could stay set if the owner backs out in the middle of the Game tab flow and opens a Learning workspace; GameTabScreen clears it whenever it appears

## Memory / safety
- Loader cache 12; Audio.release on leave; FrameLoop stops on ON_PAUSE; particles pooled 24/80

## CI errors (Claude fills)
- 2026-10-21, runs #899 to #906 (`:app:compileReleaseKotlin`), all in `StudyWorkspaceScreen.kt`:
  `307:61 Too many arguments for 'fun AtmosphereBackground(modifier: Modifier = ...)'` plus three or four `@Composable invocations can only happen from the context of a @Composable function` (lines 308, 384, 393, 406).
  - Cause: the rewritten screen called `AtmosphereBackground(modifier = ...) { ...content... }`. `AtmosphereBackground` takes ONLY `modifier` and has NO content lambda. The trailing `{ }` made everything inside it a non-composable lambda, which caused the other errors (they were knock-on errors of line 307).
  - Fix (commit daf920b): `Box(modifier = modifier.fillMaxSize()) { AtmosphereBackground(modifier = Modifier.fillMaxSize()); ...content... }`. This is the pattern every other screen in the app uses.
  - DO NOT: never put a content lambda after `AtmosphereBackground`. Always draw it as the first child of a `Box`.
  - Result: runs #907 to #913 are green, so the game files compiled too.
