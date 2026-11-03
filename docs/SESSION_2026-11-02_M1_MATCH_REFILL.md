# SESSION 2026-11-02 M1: match game refills at once, new cards are not the next pair

Plan: `docs/READER_EXAM_FLASHCARD_FIX_PLAN.md` (slice M1, owner problem 9). Read first: `GAMES_FEEL_PLAN.md` (M1-M3), `MatchEngine.kt`, `MatchScreen.kt`.

## Cause (read from the code, not run)
On 2026-10-24 the match game was changed to "random arrivals": a matched pair left TWO EMPTY slots, and a loop in `MatchScreen.kt` asked the engine for ONE card every 0.6 to 2.8 seconds (0.25 to 0.9 s when stuck), into a random empty slot. Plus a 0.52 s wait before the matched cards even left. That is exactly the owner's report: cards disappear, new cards come slowly, blank slots in between. The goal of that change (not predictable) is right; the way (empty slots and a timer) is what felt bad.

## What changed (2 code files, no other file)
- `data/games/MatchEngine.kt`: when a pair is matched, the two freed slots are refilled AT ONCE inside `select()`. Each new card is picked on its own from the whole shuffled deck (so a question is not delivered together with its own answer, and which card goes to which slot is random). About 4 in 10 picks are the partner of a card already on the board (same value as before), so matches keep coming. The last empty slot of the board is never filled so that a full board has no matchable pair (same guard as before, now in `placeOne`). New engine fields: `lastRefills` (which card took which slot) and `deckCount`. `spawnNext()` and `canSpawn` are kept (unused now) so nothing else breaks.
- `ui/games/match/MatchScreen.kt`: the arrival loop is removed. The screen shows the matched pair for the green pulse and fade (0.14 s + 0.14 s, swap after 0.32 s instead of 0.52 s), then puts `engine.lastRefills` into the SAME slots; they flip in from the card back as every new card did, so a slot is a card back right away, never an empty box. `refills` is copied at tap time, because a quick second match would overwrite `engine.lastRefills` before the swap. At the very end of the set (deck empty) the empty slots are hidden and the remaining cards move up, so no blank outlines are left.

## Safety checks
- Solvability: every card in the deck or on the board has its partner in the deck or on the board. A full board always gets at least one matchable pair (guard on the last empty slot). With the deck empty, all remaining board cards have partners on the board. So the game cannot get stuck. Not run, read from the code.
- Result screen, stars, streak, Easy hint, Hard timer, save: untouched (they read `matchedCount`, `moves`, `streak`, `phase`, `board`).
- `GAMES_FEEL_PLAN.md` was not edited. Its M1/M2 text ("new pair takes the same slots") is again partly true: the same two slots are refilled at once, but with two random cards, not the next pair.

## Decisions to confirm
- Delay 0.32 s before the new cards show (was 0.52 s). Cards flip in over 0.38 s after that. If it still feels slow, say "faster" and the swap can be immediate (the matched cards would just vanish and the new ones flip in).
- 40 percent partner chance kept as it was. Lower = more guessing, higher = more matches in a row.

Not compiled, not tested on a device. Watch the next CI run.

## Owner test on the phone (new build)
1. Match a pair: the two cards pulse green and fade very fast, and two new card backs appear in THE SAME two places and flip over. No empty box at any time.
2. Match 10 pairs in a row: no blank slots, and a new card is not always the partner of the other new card.
3. Look for matches: some new cards have a partner already on the board, some do not, and sometimes you have to wait for a card to come from a later match.
4. Near the end of a set: the last cards just move up, no empty outlines; the result screen shows after the last pair.
5. Easy level: after a wrong pair the right partner still lights green; Hard level: timer still runs.

## Next slice
T1 (AI tutor at the end of each note).
