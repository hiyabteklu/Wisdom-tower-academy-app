# SESSION 2026-10-22: GAMES W4 (Wisdom Defense juice)

Continues `docs/SESSION_2026-10-22_GAMES_W3.md`.

## Done (2 commits, `main`)
- `DefenseEffects.kt` (`146e0fb`), no engine change. The streak is counted from the engine events the file already reads: `Killed` adds 1, `WrongHit` and `WallHit` set it to 0. An armor hit that does not kill gives no event, so it neither adds nor breaks the streak.
  - Combo pill "COMBO xN" (gold, top left of the battlefield) while the streak is 2 or more; it pops when the streak grows.
  - Banners in the upper part of the battlefield, 0.9 s: streak 2 "Double!", 3 "Triple!", 4 and more "Unstoppable!". Also "WAVE n / 3" (wave = kills in thirds of the question count, only when the set has 6 or more questions) and "BOSS" when the Hard boss walks in. A banner is only replaced by one of the same or higher priority (boss, wave, streak).
  - Gold glow along the screen edge from streak 3; a red one when the boss walks in (this is the "boss entrance flash" parked in W2). With reduce-motion the glow is weaker and the banner does not pop.
  - Floating "+score" at the enemy that gave it (kill, armor hit, boss). It uses the real score change of `engine.score`, so the level multiplier and the boss 300 show correctly. Not shown at the win bonus.
  - A second, bigger white shockwave ring on a wall hit (next to the red one).
  - Slow-motion on the last kill: `fx.timeScale` is 0.4 for 0.4 real seconds when the kill empties the question queue and the lane. Not with reduce-motion or on low-end phones.
- `WisdomDefenseScreen.kt` (`237705d`): one changed line, `engine.tick(dt * fx.timeScale)`, plus a header comment. Effects and clock still use the real `dt`.

## Not in W4
- No new chime for banners (needs a sound file; the plan said one new chime). Existing sounds and haptics are unchanged.
- Boss death is still drawn at normal size and without the bar (the dying-enemy data has no boss flag; needs a field in `WisdomDefenseEngine.kt`). Still parked.
- The streak is not saved and not shown on the result screen (that is W6 if wanted: `bestStreak` is already kept in the effects object).
- Waves are only a label by progress; the engine has no real waves.

## Evidence
`[~]` written, NOT compiled, NOT seen on a phone. W3 and W4 were pushed on top of each other, so if the build is red, read the first red line: it can come from either slice. All times, sizes and alphas are proposals. One thing to watch: the slow-motion stretches the last death animation (0.35 s of game time takes about 0.9 s), so the win screen comes a little later than before.

## Owner phone test
Play Wisdom Defense. Answer two in a row right: "Double!" and a gold COMBO pill appear, with "+100" rising from the enemy. Three in a row: "Triple!" and a gold glow at the screen edge. A wrong answer or a lost heart removes the pill. At the start "WAVE 1 / 3" shows, then "WAVE 2 / 3" and "WAVE 3 / 3" as you progress. On Hard the boss comes with a red "BOSS" banner and red edge glow. The very last kill runs in slow motion for under half a second. Check that the banners do not cover the question or the enemies too much on a small phone.

## Next
W5 Power-ups is OPTIONAL and needs the owner's yes (see the plan: streak 3 = Freeze, streak 5 = Shield, never bought, off on Easy). If the answer is no, go to W6 Result v2 (`DefenseResult.kt`: level chip, pop stars with confetti on 3, score line with multiplier, New best, Retry / Next level / Done).
