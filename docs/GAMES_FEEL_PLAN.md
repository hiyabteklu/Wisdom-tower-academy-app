# GAMES FEEL PLAN (owner order 2026-10-21): make the two games look and feel like real games

Owner words: the idea is in place, but it does not look like a game. Needed: serious enemy icons, animation, proper timing, proper distance reset on a correct answer, wrong/correct reactions, matched cards that really disappear.

Rules: push every slice straight to `main` (one commit, plan table updated in the same commit). One slice per chat; owner says "Continue". Keep API of engines compatible so the screens still compile. No new dependencies. Respect `GameTheme.reduceMotion` and `isLowEnd` (fewer particles, no shake). Log any CI error in `docs/BUILD_PITFALLS.md` and `docs/GAMES_HANDOFF_FOR_CLAUDE.md`.

## 1. What is wrong now (read from the code, 2026-10-21)

### Wisdom Defense (`ui/games/defense/WisdomDefenseScreen.kt`, `data/games/WisdomDefenseEngine.kt`)
1. Enemies are flat red circles (radius 14px). The empire is one cyan dot. Lane is one flat strip.
2. Wrong answer: nothing happens. The shot turns red and disappears. No enemy reaction, no cost, no sound-synced effect.
3. Correct answer: the enemy just vanishes. No explosion, no hit flash.
4. A new enemy pops in at a random place (progress 0.05 to 0.50) the same frame. No entrance, no distance logic.
5. All enemies freeze while the shot flies (`if (shotProgress != null) return`).
6. Enemy reaching the empire: heart count drops, enemy vanishes. No impact, no screen reaction.
7. Answer buttons are 78dp circles with 11sp text: unreadable for real answers, no pressed/right/wrong state.
8. HUD hearts are plain circles. No progress bar. Result screen is two lines of text.

(Items 2, 4, 5 and the distance part of 3 and 6 are fixed by D1 in the engine. Item 1 is fixed by D2 (art). Items 3 (effects) and 6 (impact) are fixed by D3. Item 7 and the HUD part of 8 are fixed by D4. The result screen part of 8 is fixed by D5.)

### Match (`ui/games/match/MatchScreen.kt`, `data/games/MatchEngine.kt`)
1. Matched pair is removed from `board` at once: no animation, so it looks like nothing happened. (Fixed in the screen by M2.)
2. `fillBoard()` reshuffles the WHOLE board after every match, so all cards jump to new places. (Fixed in the engine by M1.)
3. Wrong pair: red border stays until the next tap, no shake, no clear reset. (Fixed in the screen by M2.)
4. Cards are flat boxes. No lift on select (fixed by M2), no flip, no streak, no progress bar, plain result screen (M3).

## 2. Game feel spec (proposed values, owner may change any number)

### Defense rules
- Distance: 0 = far edge, 1 = empire wall. Enemies walk the whole time, also while a shot flies.
- Enemy walk time from far edge to wall: about 22 s (speed 0.035 to 0.05 per second). Enemies keep at least 0.18 distance apart.
- Shot flight: 0.30 s, with trail.
- CORRECT: the nearest enemy plays a 0.35 s death (flash, burst, shrink). Every other enemy is pushed back 0.10 over 0.4 s (smooth, not a jump). After 0.5 s a new enemy walks in from the far edge (progress 0), never pops in mid-lane. Next question appears with a short slide.
- WRONG: the shot hits a shield in front of the nearest enemy and shatters. That enemy lunges forward 0.08. The tapped answer is greyed out and locked; the player must pick another. 0.4 s cooldown. Hearts are not lost for a wrong answer (only distance).
- ENEMY REACHES THE WALL: impact flash, wall shake, one heart breaks, that enemy dies, all other enemies pushed back 0.15 (mercy so there is no chain loss).
- Enemy types (art, same rules at first): slime (slow, big), bat (fast, small, bobs), golem (slowest, armored look). Chosen by id, drawn with Canvas paths, no image files.
- Perspective: enemies are small at the far edge and grow as they come near (scale 0.5 to 1.3).

### Match rules
- Board keeps its slots. A matched pair leaves; the new pair takes the SAME two slots (no reshuffle).
- Select: card lifts (scale 1.06) with cyan glow. Correct: both cards pulse green 0.25 s, then shrink and fade 0.25 s, then new cards scale in. Wrong: both cards shake and glow red 0.4 s, then reset. Streak counter (x2, x3...). Progress bar on top.

## 3. Slices (one per chat)

| Slice | Area | What | Status |
|---|---|---|---|
| D1 | Defense engine | Timing and distance: enemies move during shots, spaced spawn from far edge, push-back on correct, lunge and lock on wrong, mercy push-back on wall hit, enemy states (Walking, Dying) and event list for the screen. API stays compatible. | DONE (engine only, CI not seen yet) |
| D2 | Defense art | Canvas monsters (slime, bat, golem), perspective scale, bobbing, wall/empire with shield, lane and background layers | DONE (written and pushed 2026-10-21, NOT compiled by the agent, CI and phone test pending). New file `ui/games/defense/DefenseArt.kt`; `WisdomDefenseScreen.kt` canvas now calls it |
| D3 | Defense effects | Shot with trail, explosion particles (reuse `GameParticles`), hit flash, shield shatter, wall shake, red vignette, heart-break, floating text | DONE (pushed 2026-10-21, commit `b3bd46b`; owner screenshot shows CI run #937 green; phone test pending). New file `ui/games/defense/DefenseEffects.kt`; the screen uses it. |
| D4 | Defense UI | 2x2 answer cards (readable, locked/wrong/correct states), question panel slide, HUD with hearts and progress bar | DONE (pushed 2026-10-21, commit `6c78b01`; owner screenshot shows CI run #938 green; phone test pending). Only `WisdomDefenseScreen.kt` changed |
| D5 | Defense result | Victory/defeat screen: stars, killed, hearts left, Retry and Done | DONE (pushed 2026-10-21, commit `68cd044`; owner screenshot shows CI run #939 green; phone test pending). New file `ui/games/defense/DefenseResult.kt`; `WisdomDefenseScreen.kt` uses it. |
| M1 | Match engine | Stable slots, no reshuffle, expose last matched and last wrong ids, streak | DONE (engine only, pushed 2026-10-21, commit `616c903`, NOT compiled by the agent, CI pending). Only `MatchEngine.kt` changed |
| M2 | Match animation | Select lift, match pulse then fade out then new cards in, wrong shake and reset | DONE (pushed 2026-10-21, NOT compiled by the agent, CI and phone test pending). Only `MatchScreen.kt` changed (card view is still private in that file) |
| M3 | Match HUD and result | Progress bar, streak, result screen with stars and Retry | DONE (pushed 2026-10-21, commit `1b0fa1c`, NOT compiled by the agent, CI and phone test pending). New files `ui/games/match/MatchHud.kt` and `ui/games/match/MatchResult.kt`; `MatchScreen.kt` uses them. |
| P1 | Polish | Sound and haptic mapping, low-end and reduce-motion check, owner phone test fixes | OPEN (next) |

Order is D1 to D5, then M1 to M3, then P1. Owner may reorder by saying "do P1".

## 4. Owner phone test (after each slice)
- Defense: enemies walk the whole time; right answer = kill + others pushed back + new enemy walks in from far; wrong answer = enemy lunges + answer locked; wall hit = heart breaks.
- Match: matched cards visibly leave, others stay in place, wrong pair shakes then resets.
- D2 look: three different monsters (green slime, purple bat with flapping wings, grey golem), small far away and bigger near the wall; a ring under the enemy the next shot hits; lane in perspective with moving dashes; battlement wall with tower and glowing crystal; shots start from the crystal; a killed enemy flashes white, shrinks and fades.
- D4 look: top row has Exit, a progress bar with "killed / total" and the hearts; the question sits in a rounded panel that slides down when it changes; four answer cards in a 2x2 grid UNDER the battlefield (the wall is no longer covered), each with a letter A to D; a wrong answer turns grey with a red edge and a cross and cannot be tapped again; the card whose shot is flying lights up; a card gets slightly smaller while pressed.
- M1 look (engine only): after a correct match the two new cards appear in exactly the same two places and the other six cards do not move; at the end of the set the last cards just disappear.
- M2 look: a tapped card grows a little and glows cyan; a right pair pulses green (0.25 s), shrinks and fades (0.25 s), then the two new cards scale in on the same spots; a wrong pair: BOTH cards shake and glow red for about 0.4 s, then go back to normal by themselves; the last pair leaves and then the "Set complete!" screen shows. With animations turned off in Android developer options, nothing animates and the swap is instant.
- M3 look: under the title row there is a green progress bar that fills smoothly after every match (the "Tap a card, then its match" hint is gone); from 2 matches in a row an amber "x2" chip sits at the right end of the bar and pops each time the streak grows; a wrong pair makes it disappear. Result screen: "Set complete!", three stars that pop in one by one, three tiles (Moves, Best streak, Accuracy), the line "N pairs matched", then Retry (starts a fresh game of the same set) and Done. Stars: 3 stars when moves are at most pairs x 1.25 (rounded up), 2 stars up to pairs x 2, otherwise 1.

## 5. Plan changes
- D1 (2026-10-21): engine constants live in `WisdomDefenseEngine.companion` (SHOT_TIME 0.30, DEATH_TIME 0.35, WRONG_COOLDOWN 0.40, KILL_PUSH_BACK 0.10, WRONG_LUNGE 0.08, WALL_MERCY_PUSH 0.15, MIN_ENTRY_GAP 0.18, MIN_FOLLOW_GAP 0.06). Never more enemies than questions left, so every enemy can be killed. A follower cannot run into the enemy in front (gap 0.06). New engine fields for the screen: `dying`, `lockedChoices`, `questionsLeft`, `drainEvents()`, `WisdomEnemy.kind`. The screen is NOT changed in D1, so a locked wrong answer is not greyed out yet (D4 does that); tapping it again does nothing.
- D2 (2026-10-21): art helpers are `internal` functions in `DefenseArt.kt` (`drawDefenseField`, `drawDefenseEmpire`, `drawDefenseEnemy`, `drawDefenseTargetRing`, and the position helpers `defenseLaneX/Y`, `defenseUnit`, `defenseMuzzleY`). The screen got a `clock` (seconds) for animation. Enemy x now spreads a little with distance. The old dark curve over the bottom was removed. The screen still does NOT use `drainEvents()`, `lockedChoices` or `reduceMotion` (D3 and D4; P1 checks reduce-motion). Answer buttons still overlap the wall at the bottom until D4 re-lays them out. Not seen on a phone: monster sizes (`defenseUnit` factor 0.065) and shot start height (`defenseMuzzleY` 0.70) may need a tweak.
- D4 (2026-10-21): layout is now a column: HUD row, question panel, battlefield canvas (weight 1), 2x2 answer cards (72dp high, fixed). The old round `ChoiceBullet` is gone (replaced by private `AnswerCard`). The progress bar uses `killed / total`, where total = `engine.questionsLeft` read once when the engine is created. The old "Killed N" text at the bottom was removed (the bar shows it). Question slide uses an `Animatable` and is skipped when `reduceMotion` is on. Answer card states done: normal, pressed, firing (own shot in the air), locked (wrong). NOT done: a green "correct" state on the card, because the question changes at once on a correct answer (the kill effects show it instead); say so if the owner wants it, it needs a short engine delay. Not seen on a phone: card height 72dp and 13sp text may be tight for long answers (max 3 lines, then ellipsis); the canvas is now shorter than before, so monster sizes may need a tweak.
- M1 (2026-10-21): new `MatchEngine` fields for the screen: `streak`, `bestStreak`, `lastMatchedCards` (the two cards that just left, text included), `lastMatchedSlots` (their positions), `lastNewCardIds` (the cards that took those slots, empty when the set ran out), `lastWrongIds` (BOTH cards of a wrong pair, cleared on the next tap). `board` stays a `List<MatchCard>` but is now in slot order: a match replaces the two cards in place, so grid keys keep every other card where it is. When no pairs are left the two slots are removed and the rest shift up. Start board is shuffled once; each new pair is placed with term and meaning in random order. The engine swaps at once, so the screen has to hold the matched cards for the pulse (done in M2).
- M2 (2026-10-21): `MatchScreen` keeps its own `shown` list (starts as `engine.board`). On a match it marks the two old ids as `leaving` (green pulse 0.25 s, shrink and fade 0.25 s), waits 520 ms, then swaps each old card for the matching new card (same position) and clears `leaving`. A counter `busy` holds the "Set complete!" screen until the last swap is done. Leaving cards cannot be tapped; all other cards stay tappable during the animation. Wrong pair: `wrongIds = engine.lastWrongIds` (both cards), shake 3 times by 8dp, cleared after 450 ms (or at the next tap). Select lift is scale 1.06 with a cyan shadow. New cards scale in from 0.6 over 220 ms (this also plays once for the first board). `GameTheme.reduceMotion` skips shake, pulse, scale-in and the 520 ms wait. Streak and progress bar are NOT shown yet (M3). Not seen on a phone: the 520 ms wait and the cyan shadow colour (shadow colours need Android 9 or newer, older phones just show a grey shadow).
- M3 (2026-10-21): `MatchHud.kt` has `internal MatchProgressBar(matched, total, streak, reduceMotion)` (bar fills with a 350 ms tween, 0 ms when reduce-motion; a fixed 52dp slot at the end holds the private `StreakChip`, shown from streak 2, pops 1.35 to 1.0 over 220 ms). `MatchResult.kt` has `internal MatchResult(pairsTotal, moves, bestStreak, reduceMotion, onRetry, onDone)` with its own private tile and star helpers (copies of the Defense ones, so nothing in `DefenseResult.kt` was touched). `MatchScreen` got an `attempt` counter: the engine and all screen state are keyed on `(set.id, attempt)`, so Retry (`attempt++`) starts a fresh engine on the same set. The top row now shows `matched/total` on the right and the bar sits under it; the old hint text was removed. The zero-pairs case ("No pairs in this set" with Done) now lives in `MatchResult`. The bottom Exit button is kept. Not seen on a phone: bar height 10dp, chip colour (amber at 20% fill) and the star thresholds (owner may change them).
