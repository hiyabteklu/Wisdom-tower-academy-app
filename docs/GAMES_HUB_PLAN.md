# GAMES HUB PLAN (owner decisions, 2026-10-21) — READ BEFORE TOUCHING THE GAMES

> Repo is PUBLIC: no secrets, no student data. This is the ONLY live plan (see `docs/PARKED_2026-10-21.md`). Follow `docs/OWNER_RULE_PLAN_FIRST_PUSH_TO_MAIN.md`: one slice = one commit straight to `main`, update this file in the same commit, say "not compiled" when CI was not seen.

Status: STEPS 1–3 DONE and CI green (Actions success for 2a, 2b, 3). STEP 4 Match engine + MatchScreen WRITTEN, NOT COMPILED, NOT WIRED (no switchover yet — students still use GameTabScreen). Next: wire Match through GameHubScreen switchover (section 9) OR step 5 Wisdom Defense, depending on owner; plan order says step 5 after 4, switchover with first playable game.

## 1. Two games (not four)

### 1a. Wisdom Defense (redesign of Tower Defense)
Owner words 2026-10-21, turned into a spec:
- **Cockpit view.** The player looks out from inside a cockpit.
- **Enemies** drive toward the player's empire, each starting at a DIFFERENT distance.
- **Question** is shown at the TOP of the screen.
- **Bullets = the 4 answer choices.** They sit in an ARC around the player (cockpit side).
- **Tap one choice** and it flies STRAIGHT to the NEAREST enemy.
- **Correct:** kills ONLY the targeted enemy (decided: no area clearing).
- **Wrong:** the bullet bounces back; the enemy keeps getting closer until the player answers correctly for it. If it arrives first, it attacks the empire.
- **Background:** the same app background as everywhere else, in all 3 looks (Dark, Day, and the third look) if possible. Use the app's existing background/theme system; do not invent a game-only background.

### 1b. Match (REPLACES Tower Climb)
- 8 cards in a grid on one screen. Tap a card: it highlights. Tap the card that is its answer: the pair is matched. Wrong pair: reset the highlight.
- Implemented proposal: matched pairs leave the board; new pairs fill until the set is finished; wrong pair flashes red and clears selection.
- Same background rule as Defense (app background, 3 looks).
- The old Tower Climb code (`ui/games/climb`, `data/games/Climb*`) stays until Match is wired and the owner says remove it.

A 3rd/4th game may come later (ideas given: Blitz 60-second round, Letter Lock, Duel). Not decided.

## 2. Flow (same for every game)
Bottom nav **Game** tab → pick a game → the student's own courses (the SAME list the Learning tab shows, from the academic level / package chosen at registration) → subjects of that course → chapter list for that subject → play.

## 3. Content
Dedicated `game_sets` rows (not Question Banks / Exams). One set = one chapter + one game. Items shapes in GameSetsRepository.

## 4. Database (DONE 2026-10-21)
`public.game_sets` with RLS for published read.

## 5. Owner decisions (2026-10-21)
See earlier commits; cockpit Defense, Match pairs, package-scoped hub.

## 6. Build order and status
| Step | What | Status |
|---|---|---|
| 1 | Database `game_sets` | DONE |
| 2a | `GameSetsRepository.kt` | DONE (CI green) |
| 2b | `GameHubScreen` + `GameHubIndex` | DONE (CI green), NOT wired to tab |
| 3 | `GameFullScreen` helper | DONE (CI green) |
| 4 | Match engine + screen | WRITTEN, NOT COMPILED, NOT WIRED |
| 5 | Wisdom Defense rewrite | not started |
| 6 | Remove old FABs / old sources | not started |
| 7 | Admin bulk JSON paste for game_sets | not started |

## 7. Plan changes
- 2026-10-21: earlier Question-Banks/Exams plan dropped; reuse feel kit from G0–G9.
- 2026-10-21 evening: step 4 Match files added (`MatchEngine`, `MatchScreen`).

## 8. Already built and live
Game tab still shows old GameTabScreen until switchover (section 9).

## 9. Switchover (with first playable game)
(1) MainActivity Game tab → `GameHubScreen(packageList, onPlay = ...)` (2) hide bottom bar while `GameFullScreen.active` (3) each game calls `GameFullScreenEffect()` (Match already does) (4) onPlay loads set and shows MatchScreen / Defense when ready.

## Match files (step 4)
- `data/games/MatchEngine.kt` — board of up to 8 cards, select/match/wrong, fill from remaining pairs
- `ui/games/match/MatchScreen.kt` — grid UI, AtmosphereBackground as Box child (no content lambda), GameFullScreenEffect, audio/haptics
