# GAMES HUB PLAN (owner decisions, 2026-10-21)

Status: Steps 1–3 CI green. Step 4 Match written + switchover host. Step 5 Wisdom Defense engine + cockpit screen WRITTEN and wired in GameTabScreen (NOT COMPILED this commit). Bottom-bar hide still needs `scripts/patch_game_fullscreen_bottom_bar.py`. Next: CI; step 6 remove old FABs when owner confirms; step 7 admin bulk paste for game_sets (needed to load real content).

## 6. Build order and status
| Step | What | Status |
|---|---|---|
| 1 | Database `game_sets` | DONE |
| 2a | GameSetsRepository | DONE (CI green) |
| 2b | GameHubScreen + GameHubIndex | DONE (CI green) |
| 3 | GameFullScreen | DONE (CI green) |
| 4 | Match | DONE host wired; CI green for engine/screen |
| 5 | Wisdom Defense | WRITTEN + wired; NOT COMPILED |
| 6 | Remove old FABs | not started |
| 7 | Admin bulk JSON for game_sets | not started |

## Defense files (step 5)
- `data/games/WisdomDefenseEngine.kt` — multi-enemy approach, nearest-only kill, hearts, win/lose
- `ui/games/defense/WisdomDefenseScreen.kt` — question top, canvas road/enemies, 4 choice bullets, AtmosphereBackground, GameFullScreenEffect

## Owner phone test (after green CI + published sets)
1. Game → Match → course → subject → chapter with match set
2. Game → Wisdom Defense → same with defense set
3. Correct kills nearest only; wrong lets enemies keep coming; 0 hearts = lose
