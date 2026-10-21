# GAMES HUB PLAN (owner decisions, 2026-10-21)

Status: Steps 1–5 done (code). Bottom-bar hide merged (PR #74). Step 6: StudyWorkspace no longer has Climb/Defense FAB or GameLaunch path — games only via Game tab + game_sets. Old climb/defense source files still on disk (delete when owner says). CI failed on step 5 (DefenseEnemy declared twice: legacy DefenseEngine.kt and WisdomDefenseEngine.kt). Fixed by renaming the new one to WisdomEnemy; waiting on CI. Next: step 7 admin bulk paste for game_sets so chapters have content.

## 6. Build order and status
| Step | What | Status |
|---|---|---|
| 1–3 | DB, repo, hub, fullscreen | DONE (CI green) |
| 4 | Match | DONE + wired |
| 5 | Wisdom Defense | DONE + wired, compile fix pushed (WisdomEnemy rename), CI pending |
| 6 | Remove old FABs / GameLaunch from StudyWorkspace | DONE (NOT COMPILED until CI passes) |
| 7 | Admin bulk JSON for game_sets | not started |

## Owner phone test
1. Game tab → Match / Defense → course → subject → chapter (needs published game_sets)
2. Learning hubs: no Tower Climb/Defense FAB
3. Fullscreen play hides bottom bar
