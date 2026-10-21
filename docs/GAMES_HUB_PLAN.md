# GAMES HUB PLAN (owner decisions, 2026-10-21)

Status: Steps 1–7 code on main. CI fixed DefenseEnemy clash (WisdomEnemy). Bottom-bar hide merged. Admin **Game Sets** module: pick subject → list/add/edit/delete sets, bulk paste items JSON (match a/b; defense q/choices/correct). Next: CI for step 7; owner publishes a set and phone-tests Game tab.

## 6. Build order and status
| Step | What | Status |
|---|---|---|
| 1–3 | DB, repo, hub, fullscreen | DONE |
| 4 | Match | DONE |
| 5 | Wisdom Defense | DONE (WisdomEnemy rename) |
| 6 | Remove old FABs | DONE |
| 7 | Admin bulk JSON for game_sets | WRITTEN + wired (NOT COMPILED this session) |

## Admin Game Sets (step 7)
- `AdminGameSetsRepository.kt` — list / save / delete via AdminSupabase
- `AdminGameSetsScreen.kt` — tree → subject → sets → editor with JSON paste
- Module id `game-sets` in AdminModules + AdminConsoleHost branch

## Owner test
1. Admin → Game Sets → subject → Add set → paste JSON → Publish
2. Game tab → Match/Defense → same subject chapter → play
