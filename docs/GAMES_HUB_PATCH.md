# Minimal HubItemListScreen patch (apply if file not updated)

## 1. Signature — add last param
```kotlin
locks: CourseLocks? = null,
onOpenGame: (() -> Unit)? = null
```

## 2. Inside `current is ContentListResult.Success -> {` BEFORE hub_list_tools item:
```kotlin
if (onOpenGame != null && current.items.isNotEmpty() &&
    (hubId == "question-banks" || hubId == "exams")
) {
    item(key = "hub_game_banner") {
        com.wisdomtower.academy.ui.games.common.GameHubBanner(
            isClimb = hubId == "question-banks",
            onClick = onOpenGame
        )
    }
}
```

GameHubBanner is already on main at ui/games/common/GameHubBanner.kt.
