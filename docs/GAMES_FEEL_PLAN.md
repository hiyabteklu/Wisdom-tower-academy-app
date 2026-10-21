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

(Items 2, 4, 5 and the distance part of 3 and 6 are fixed by D1 in the engine. The screen still draws the old flat look until D2 to D4.)

### Match (`ui/games/match/MatchScreen.kt`, `data/games/MatchEngine.kt`)
1. Matched pair is removed from `board` at once: no animation, so it looks like nothing happened.
2. `fillBoard()` reshuffles the WHOLE board after every match, so all cards jump to new places.
3. Wrong pair: red border stays until the next tap, no shake, no clear reset.
4. Cards are flat boxes. No lift on select, no flip, no streak, no progress bar, plain result screen.

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
| D2 | Defense art | Canvas monsters (slime, bat, golem), perspective scale, bobbing, wall/empire with shield, lane and background layers | OPEN (next) |
| D3 | Defense effects | Shot with trail, explosion particles (reuse `GameParticles`), hit flash, shield shatter, wall shake, red vignette, heart-break, floating text | OPEN |
| D4 | Defense UI | 2x2 answer cards (readable, locked/wrong/correct states), question panel slide, HUD with hearts and progress bar | OPEN |
| D5 | Defense result | Victory/defeat screen: stars, killed, hearts left, Retry and Done | OPEN |
| M1 | Match engine | Stable slots, no reshuffle, expose last matched and last wrong ids, streak | OPEN |
| M2 | Match animation | Select lift, match pulse then fade out then new cards in, wrong shake and reset | OPEN |
| M3 | Match HUD and result | Progress bar, streak, result screen with stars and Retry | OPEN |
| P1 | Polish | Sound and haptic mapping, low-end and reduce-motion check, owner phone test fixes | OPEN |

Order is D1 to D5, then M1 to M3, then P1. Owner may reorder by saying "do M1".

## 4. Owner phone test (after each slice)
- Defense: enemies walk the whole time; right answer = kill + others pushed back + new enemy walks in from far; wrong answer = enemy lunges + answer locked; wall hit = heart breaks.
- Match: matched cards visibly leave, others stay in place, wrong pair shakes then resets.

## 5. Plan changes
- D1 (2026-10-21): engine constants live in `WisdomDefenseEngine.companion` (SHOT_TIME 0.30, DEATH_TIME 0.35, WRONG_COOLDOWN 0.40, KILL_PUSH_BACK 0.10, WRONG_LUNGE 0.08, WALL_MERCY_PUSH 0.15, MIN_ENTRY_GAP 0.18). Never more enemies than questions left, so every enemy can be killed. A follower cannot run into the enemy in front (gap 0.06). New engine fields for the screen: `dying`, `lockedChoices`, `questionsLeft`, `drainEvents()`, `WisdomEnemy.kind`. The screen is NOT changed in D1, so a locked wrong answer is not greyed out yet (D4 does that); tapping it again does nothing.
