# SESSION 2026-10-22: GAMES R2 wiring + R3 part 1 (continues the agent that stopped after commit aa30e24)

## Where the previous agent stopped (read from the repo, not guessed)
- Done and pushed: G1, G2, G3 (levels), R1 (`game_bests` + `save_game_result` applied live), and R2 PART ONLY: `GameResultsRepository.kt`, `GameSaveLine.kt`, result screens got the save line and the shared `matchStars` / `matchScore` (commits `d26e6e0`, `aa30e24`).
- NOT done: the two game screens never called the saver and never received the subject path, so nothing was saved yet. No R2 session log existed.
- CI at the time of the stop: #989 in progress, #990 queued (owner screenshot). Not seen finished by this session.

## What this session did (4 commits, all on `main`)
1. `MatchScreen`: new parameter `scopePath` (default empty = save nothing). When the game finishes it calls `GameResultsRepository.save` once (stars = `matchStars`, score = `matchScore`, won = true) and passes `showSave` + `saveOutcome` to `MatchResult`.
2. `WisdomDefenseScreen`: same, `scopePath` parameter, saves on won or lost. Stars = same rule as the result screen (hearts left 1..3 when won, 0 when lost). Passes `levelLabel`, `showSave`, `saveOutcome` to `DefenseResult`. NEW private `defenseScore`: 100 per enemy defeated + 50 per heart left when won, times the level multiplier, clamped 0..100000. This formula is MY PROPOSAL (hypothesis, change after a phone test).
3. `LevelPickerScreen`: new optional `bestFor` parameter; a level card with a result shows "Best" stars and "Played N time(s)".
4. `GameTabScreen`: remembers the subject path of the chapter being played (`gameScope`), passes it to both screens, gives the picker `bestFor` from `GameResultsRepository.localBest`, and calls `flushPending` when the tab opens (sends results that waited offline).

## Evidence level
`[~]` written, NOT compiled by me (no build access). Code read only. Smart casts on local `val phase` / `val ph` follow the pattern already used in the same files. New parameters have defaults, so older callers still compile. Check CI #989 onward and the next run for these commits.

## Known limits
- The picker shows the best kept ON THIS PHONE (`localBest`), not the server rows. A student who reinstalls sees no stars until R3 part 2 reads `game_bests` from the server.
- If the student taps Done in the first moments after the result appears, the save coroutine is cancelled with the screen; the phone-side best is already written but the server row can be missed. Hypothesis: rare, a half second window.
- A signed-out student plays normally; the result says "Sign in to save your results".

## Next (open)
- R3 part 2: best stars line on the chapter rows of the Game tab (`GameHubScreen.kt`), and optionally `bestsFor(scopePath)` from the server (the plan lists it; it was not written in R2).
- Then W1 to W7 (art) and MW as in `docs/GAMES_COOL_PLAN.md` section 6. NOTE: that file's section 9 "All slices OPEN. Next: G1" is OUT OF DATE; G1 to G3, R1, R2 and R3 part 1 are done as above.

## Owner phone test (after CI is green)
1. Signed in: finish a Match and a Defense game. The result shows "Saving..." then "Saved". Match: no level line; Defense: "Easy level" etc.
2. Open the same chapter's Play again: the level card you played shows "Best ..." and "Played 1 time".
3. Turn the internet off, finish a game: "Will save when online". Turn it on, reopen the Game tab: the waiting result is sent.
4. Signed out: the result asks to sign in.
