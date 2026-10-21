# GAMES HANDOFF FOR CLAUDE

## Phase status
- G0–G5 DONE
- G6 play UI on main (ClimbPlayScreen, TowerClimbScreen, Hud, Scene, Result, Attic)
- G6 WIRING PENDING: StudyWorkspaceScreen.kt and HubItemListScreen.kt edits are in agent working copies but may not be on main yet (file size push limits).

## What Claude must apply if missing on main

### StudyWorkspaceScreen.kt
1. Add to WorkspaceView:
```
data class Climb(val hubId: String) : WorkspaceView
data class Defense(val hubId: String) : WorkspaceView
```
2. BackHandler when: Climb/Defense -> HubList(hubId)
3. HubItemListScreen(..., onOpenGame = when(hubId) { "question-banks" -> { view = Climb(...) }; "exams" -> { view = Defense(...) }; else -> null })
4. when branches:
```
is WorkspaceView.Climb -> TowerClimbScreen(scopePath=contentScope, courseTitle=title, locks=locks, onBack={ view = HubList(shown.hubId) }, modifier=modifier)
is WorkspaceView.Defense -> placeholder until G8
```

### HubItemListScreen.kt
1. Add param `onOpenGame: (() -> Unit)? = null`
2. When Success and items not empty and hub is question-banks or exams, show GameHubBanner item that calls onOpenGame

## UNSURE
- Defense placeholder until G8

## CI errors seen (Claude fills this)
(none yet)
