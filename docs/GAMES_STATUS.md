# GAMES STATUS (single source of truth for the games, written 2026-10-22)

> Read this first. It replaces the stale tables in `docs/GAMES_COOL_PLAN.md` (section 6 and 9 still say OPEN) and `docs/GAMES_FEEL_PLAN.md`. Facts below come from the session logs in `docs/SESSION_2026-10-22_GAMES_*.md`, the owner's CI screenshots and read-only checks of the live database on 2026-10-22. Nothing here was run on a phone by an agent.

## Build (CI) at 2026-10-22, about 11:41 owner time
- Runs #1007 to #1024 (W4 to MW part 2 and their session logs): GREEN.
- #1025 (MW part 3 card flip, `f7bc633`), #1026 (W5 engine, `341dbbf`) and #1027 (W5 screen, `9c1db75`): still IN PROGRESS in the screenshot. Result NOT seen. If one is red, the owner pastes the first line starting with `e: file:///`; fix that before anything else.

## Slices (all written; code is `[~]` until CI is green and the owner has tested it)
| Area | Slices | State |
|---|---|---|
| Feel | D1 to D5 (Defense), M1 to M3 (Match), P1 (sound, haptics) | written, CI green, phone test fixes open |
| Levels | G1 (data), G2 (picker + Defense), G3 (Match) | written, CI green |
| Results | R1 (database), R2 (app saver), R3 (show bests) | written; R1 verified live (see below) |
| Defense look | W1 atmosphere, W2 enemies, W3 cockpit, W4 juice, W6 result v2 | written, CI green |
| Polish | W7a low-end confetti, W7b boss size on death, W7c shot from turret, W7d Next level saves level | written, CI green. A full low-end and reduce-motion pass of every effect is NOT logged |
| Match look | MW1 stars pop and confetti, MW2 level chip | written, CI green |
| Match flip | MW3 new cards flip from a card back (entrance only, rules unchanged) | written, owner said yes, CI #1025 not seen |
| Power-ups | W5 Freeze (3 in a row) and Shield (5 in a row), Medium and Hard only | written, owner said yes, CI #1026 and #1027 not seen |

## Live database (read-only checks, 2026-10-22)
- `game_bests`: row level security ON, function `save_game_result` exists, 2 rows saved, so the result saver has worked at least twice from the app.
- `game_sets`: 2 rows, both published, both for chapter 1 of `ece/sem-1/eceg3071`: Defense with 6 questions, Match with 8 pairs. NO other chapter has a game set.

## What is left, in order
1. Build result of #1025 to #1027 (fix first if red).
2. Owner phone test of everything. Never tested on a phone: levels, result saver, cockpit, juice, power-ups, card flip. Checklists are in each session log and in `docs/GAMES_COOL_PLAN.md` section 7.
3. CONTENT: only one chapter has game sets. Every other chapter shows nothing to play. Also the Defense set has 6 questions, and the Hard boss needs at least 8 (`BOSS_MIN_QUESTIONS`), so Hard never shows its boss on that set. More sets are added in Admin > Game Sets (paste JSON).
4. Owner phone-test fixes for P1 and the W slices, as he reports them.
5. Optional, not started: the `level` tag on questions (`GAMES_COOL_PLAN.md` section 3), leaderboards for game results, game minutes in "Your status", a real face-down Match mode.

## Other tasks (not games)
- Owner order 2026-10-21 (`docs/PARKED_2026-10-21.md`): every other plan is PARKED (competition C0 to C10, notifications v2, admin console redesign, day mode, Play Store prep, UI motion, native content leftovers). Their status boards were not re-verified and do not show current state: `docs/COMPETITION_STATUS.md` stops at 2026-10-15.
- Game course navigation on the WEBSITE (`docs/GAME_COURSE_NAV_PLAN.md`, owner order 2026-10-22): NOTHING implemented. Its log says next is G0 (inspect, no code). It is a separate task from the native games above.
