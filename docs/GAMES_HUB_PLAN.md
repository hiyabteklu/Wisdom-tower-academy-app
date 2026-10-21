# GAMES HUB PLAN (owner decisions, 2026-10-21)

Status: Steps 1–3 CI green. Step 4 Match written. Switchover: GameTabScreen now hosts GameHubScreen + Match load/play (MainActivity call site unchanged). Bottom-bar hide script: `scripts/patch_game_fullscreen_bottom_bar.py` (run via Apply pending patches workflow, or apply by hand). Defense still "coming soon" from hub. Next: CI for Match + switchover; then step 5 Wisdom Defense; then run bottom-bar patch if not applied.

## 6. Build order and status
| Step | What | Status |
|---|---|---|
| 1 | Database `game_sets` | DONE |
| 2a | GameSetsRepository | DONE (CI green) |
| 2b | GameHubScreen + GameHubIndex | DONE (CI green) |
| 3 | GameFullScreen | DONE (CI green) |
| 4 | Match engine + screen | WRITTEN; host wired in GameTabScreen; NOT COMPILED this commit |
| 5 | Wisdom Defense rewrite | not started |
| 6 | Remove old FABs | not started |
| 7 | Admin bulk JSON for game_sets | not started |

## Switchover notes
- Game tab still calls `GameTabScreen(packageList, onSelectPackage)` — body now opens GameHub + Match.
- `onSelectPackage` unused (PackageDetail path when selectedPackage != null still exists for Learning).
- MainActivity bottomBar should use `!GameFullScreen.active` — apply `scripts/patch_game_fullscreen_bottom_bar.py`.
- MatchScreen already calls `GameFullScreenEffect()`.

## Owner phone test (after green CI + sets in DB)
1. Game tab → Match → course → subject → chapter with a published match set → play
2. Exit returns to hub; Back works
3. Defense chapter → "coming soon" message
