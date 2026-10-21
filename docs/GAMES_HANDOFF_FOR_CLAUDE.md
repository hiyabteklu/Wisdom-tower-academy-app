# GAMES HANDOFF FOR CLAUDE (kept current by the agent that writes the game code)

> Plan: `docs/GAMES_NATIVE_PLAN.md`. Repo is PUBLIC: no secrets, no student data.
> Purpose: lets Claude (chat) fix a red CI build in a few tool calls. Update it in EVERY commit.

## Files created or changed
- `docs/GAMES_NATIVE_PLAN.md` — G0 done; section 15 filled (see backup below if file was emptied)
- `docs/GAMES_HANDOFF_FOR_CLAUDE.md` — this file

## Phase status
- G0 Recon: DONE (signatures copied from real code)
- G1–G9: open

## UNSURE
(none yet)

## Known risks
- Brief empty push of GAMES_NATIVE_PLAN.md; section 15 backup lives here until plan is fully restored.

## CI errors seen (Claude fills this)
(none yet)

## Section 15 backup (Verified signatures from G0)

### LearningContentRepository (data/content/LearningContentRepository.kt)
```
suspend fun list(context: Context, scopePath: String, hub: String, full: Boolean = false): ContentListResult
suspend fun getResource(context: Context, id: String, refresh: Boolean = false): ContentItemResult
```
Lists always light. Heavy content only via getResource. Locked -> ContentItemResult.Failure.

### LearningResource (data/content/LearningResource.kt)
```
data class LearningResource(id, packageId, scopePath, hub, title, chapter: Int?, sortOrder, contentType, storagePath, bodyMd, metaJson, itemCount: Int? = null)
sealed class ContentListResult { Success(items, fromCache); Failure(message) }
sealed class ContentItemResult { Found(item, fromCache); NotFound; Failure(message) }
```

### CourseLocks / ContentAccess (data/content/ContentAccess.kt)
```
enum class HubLockMode { Open, RequirePurchase, ComingSoon }
enum class HubGate { Open, ComingSoon, PackageRequired }
class CourseLocks { fun hubMode(hubId); fun hubGate(hubId); fun itemGate(hubId, chapter, itemId): HubGate? }
ContentAccess.COMING_SOON_TITLE/BODY/BUTTON, PURCHASE_TITLE/BODY/BUTTON
```

### HubItemListScreen (ui/study/HubItemListScreen.kt)
```
fun HubItemListScreen(scopePath, hubId, hubName, courseTitle, onBack, onOpenItem, modifier = Modifier, locks: CourseLocks? = null)
```
Only caller: StudyWorkspaceScreen. G6/G8 add onOpenGame: (() -> Unit)? = null.

### StudyWorkspaceScreen (ui/study/StudyWorkspaceScreen.kt)
```
private sealed interface WorkspaceView {
  HubList(hubId); Pdf(item, hubId); Note(item); Cards(item); Quiz(item, isExam, hubId)
}
internal fun contentScopePath(basePath: String): String?
```
G6/G8 add Climb/Defense cases.

### Auth
```
data class AuthUser(id, email?, fullName?, accessToken?, refreshToken?, createdAt?)
AuthStateManager.currentUser: StateFlow<AuthUser?>
Student id: AuthStateManager.currentUser.value?.id ?: "guest"
```

### Theme (ui/theme/Color.kt)
WisdomCyan, WisdomCyanDark, WisdomBackground, WisdomDark, WisdomNavy, WisdomCard, amber via WisdomDarkPalette.amber.

### LocalLifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver

### Haptics
LocalView.current.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK / LONG_PRESS / REJECT)
