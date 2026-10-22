# SESSION 2026-10-22: GAMES W5 (Defense power-ups)

Owner said yes to W5 (2026-10-22, chat). Follows `docs/GAMES_COOL_PLAN.md` section 5, row W5.

## Done (2 commits, `main`)
1. Engine + new chip file (`341dbbf`): `WisdomDefenseEngine.kt` got `streak`, `freezeReady`, `freezeLeft`, `shieldReady`, `shieldActive`, `shieldBlocks`, `powerUpsOn` (false on Easy), `useFreeze()`, `useShield()`. Rules: 3 correct answers in a row earn a Freeze (one held at a time), 5 in a row earn a Shield; a wrong answer sets the streak to 0. Freeze: for 3 s enemies do not walk and nothing spawns (pushes from kills still play out). Shield: when on, it absorbs the next hit on the wall (no heart lost, that enemy still dies, no wall-hit effect for it, the mercy push still happens). New file `ui/games/defense/DefensePowerUps.kt`: the chip strip (Freeze, Shield, a small "Streak n/3: Freeze" hint). No new `WisdomEvent` was added (so `DefenseEffects.kt` is untouched and cannot break).
2. Screen (this commit): `WisdomDefenseScreen.kt` lays the chips over the bottom of the battlefield (no layout change), draws an icy tint and frame while frozen, a glowing line along the wall while the Shield is on, a short white flash when it blocks, and plays a chirp when a power-up is earned or used.

## Not changed
Result screen, score, stars and saved results (power-ups do not change score). Easy has no power-ups.

## Evidence
`[~]` written, NOT compiled, NOT seen on a phone. Guesses: the wall line uses `defenseLaneY(h, 1f)` as the wall height (may sit a little off); chip size and the 10sp hint may need a tweak; sounds reuse Combo, Correct and Hit.

## Owner phone test (Medium or Hard)
Answer 3 right in a row: a "Freeze" chip appears above the cockpit; tap it: the field turns icy and enemies stand still about 3 s. Answer 5 right in a row (without a wrong one): a "Shield" chip appears; tap it: a glowing line shows along the wall; let an enemy reach the wall: no heart is lost, a white flash shows and the line goes. A wrong answer resets the streak (the hint text shows the count). Easy shows no chips.
