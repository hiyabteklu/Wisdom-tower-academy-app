# GAMES HANDOFF FOR CLAUDE

> Plan: `docs/GAMES_NATIVE_PLAN.md`. Repo PUBLIC.

## Files created or changed
- `app/src/main/java/com/wisdomtower/academy/data/games/GameQuestion.kt` — model + parse + fair shuffle
- `app/src/main/java/com/wisdomtower/academy/data/games/GameQuestionLoader.kt` — getResource door + LRU cache
- `app/src/main/java/com/wisdomtower/academy/data/games/GameStore.kt` — SharedPreferences progress
- `docs/GAMES_NATIVE_PLAN.md` — G0 signatures (see section 15 backup below if stub)
- `docs/GAMES_HANDOFF_FOR_CLAUDE.md` — this file

## Phase status
- G0 Recon: DONE
- G1 Data kit: DONE
- G2–G9: open

## UNSURE
(none yet)

## Known risks
- Plan file may be a short stub; full G0 signatures backup is below. Full multi-section plan was in parent commit 61a2b34.

## CI errors seen (Claude fills this)
(none yet)

## Section 15 backup (Verified signatures from G0)

### LearningContentRepository
`suspend fun list(context, scopePath, hub, full=false): ContentListResult`
`suspend fun getResource(context, id, refresh=false): ContentItemResult`

### LearningResource / ContentItemResult / ContentListResult
See data/content/LearningResource.kt

### CourseLocks
hubMode, hubGate, itemGate; ContentAccess.COMING_SOON_*, PURCHASE_*

### HubItemListScreen
(scopePath, hubId, hubName, courseTitle, onBack, onOpenItem, modifier, locks)

### AuthUser.id via AuthStateManager.currentUser; fallback guest

### LocalLifecycleOwner: androidx.lifecycle.compose.LocalLifecycleOwner
