# G8 workspace wire (apply if not on main)

## StudyWorkspaceScreen.kt

1. Add to `WorkspaceView`:
```kotlin
data class Climb(val hubId: String) : WorkspaceView
data class Defense(val hubId: String) : WorkspaceView
```

2. BackHandler `when (open)` add:
```kotlin
is WorkspaceView.Climb -> WorkspaceView.HubList(open.hubId)
is WorkspaceView.Defense -> WorkspaceView.HubList(open.hubId)
```

3. `HubItemListScreen(...)` add:
```kotlin
onOpenGame = when (shown.hubId) {
    "question-banks" -> ({ view = WorkspaceView.Climb(shown.hubId) })
    "exams" -> ({ view = WorkspaceView.Defense(shown.hubId) })
    else -> null
}
```

4. After Quiz branch:
```kotlin
is WorkspaceView.Climb -> com.wisdomtower.academy.ui.games.climb.TowerClimbScreen(
    scopePath = contentScope, courseTitle = title, locks = locks,
    onBack = { view = WorkspaceView.HubList(shown.hubId) }, modifier = modifier
)
is WorkspaceView.Defense -> com.wisdomtower.academy.ui.games.defense.TowerDefenseScreen(
    scopePath = contentScope, courseTitle = title, locks = locks,
    onBack = { view = WorkspaceView.HubList(shown.hubId) }, modifier = modifier
)
```

## HubItemListScreen.kt

1. Param: `onOpenGame: (() -> Unit)? = null`

2. In Success branch before tools item, if onOpenGame != null and hub is question-banks or exams, item `hub_game_banner` with GameHubBanner.

3. Private GameHubBanner(isClimb, onClick) — cyan gradient row, title Tower Climb / Tower Defense.
