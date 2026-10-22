# SESSION 2026-10-22: GAMES W6 (Wisdom Defense result v2)

Continues `docs/SESSION_2026-10-22_GAMES_W4.md`. W5 (power-ups) was skipped on purpose: the plan says it needs the owner's yes first and none was given.

## Done (2 commits, `main`)
- `DefenseResult.kt` (`984611d`):
  - Level as a small chip under the title (was a grey line).
  - Stars pop in with a little overshoot (spring) instead of a plain fade. Reduce-motion: they just show.
  - Confetti: 36 coloured pieces fall for about 2 s, started after the third star has popped in. Only with 3 stars and not with reduce-motion. Positions come from the piece number (no random), it takes no touches.
  - Score line "Score 450" in amber, plus "x1.5 level bonus" when the multiplier is not 1. The number is the same one that is saved (`defenseScore`).
  - Buttons: after a win on Easy or Medium the order is **Next level: Medium / Hard** (main button), Retry, Done. Otherwise Retry (main) and Done as before.
  - "New best!" and the saved tick were already there from R2 (`GameSaveLine`), left as is.
- `WisdomDefenseScreen.kt` (`92e20f3`): new `currentLevel` (starts as the `level` parameter). Every place that used `level` in the body now uses `currentLevel`: tuning, engine, saved result, skin, atmosphere. Next level sets `currentLevel` to the next one and starts a new attempt. Passes `score`, `scoreMultiplier`, `nextLevelLabel`, `onNextLevel` to the result.

## Behaviour to know
- Next level is offered only after a **win**, not after a loss.
- Results are saved with the level that was really played, so the best-per-level rows stay correct.
- The hub does not hear about the level change: "last level used" in the level picker stays at what was picked there. Also the picker and chapter rows only refresh when the student goes back to them. Changing this needs a callback into `GameTabScreen.kt`, not done.

## Not in W6
- No dedicated "win" sound for 3 stars (existing Victory sound plays).
- Streak is not on the result screen (`DefenseEffects.bestStreak` exists but is not passed on).

## Evidence
`[~]` written, NOT compiled, NOT seen on a phone. Since W3 the pushes pile up on each other (W3, W4, W6 all unbuilt as far as this log knows), so if the build is red read the first red line first. Things most likely to need a fix: `GameLevel.values()` use, the `rotate(...)` call and `return@Canvas` in the confetti, the lambda in `goNext`. All sizes, times and the 36 pieces are proposals.

## Owner phone test
Win a game on Easy with 3 hearts: stars pop one after another, then confetti falls; you see "Easy level" chip, the Score line, "New best!" if it is one, then the buttons Next level: Medium, Retry, Done. Tap Next level: the game restarts on Medium (purple enemies, fewer hearts). Win on Hard: no Next level button. Lose a game: only Retry and Done, no stars. With reduce-motion on in the phone: no pop and no confetti.

## Next
W7 Polish: low-end pass (particle counts, no slow-mo and no shake on cheap phones; W4 slow-mo already skips them), a reduce-motion check of every new effect, and fixes from the owner's phone test. Also the parked items: boss death at boss size (field in `WisdomDyingEnemy`), shot leaving the turret, hub callback for the level. And MW (Match look lift) is still open.
