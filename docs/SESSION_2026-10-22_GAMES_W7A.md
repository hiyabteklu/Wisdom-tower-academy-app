# SESSION 2026-10-22: GAMES W7a (low-end confetti)

Continues `docs/SESSION_2026-10-22_GAMES_W6.md`. First part of W7 (Polish) from `docs/GAMES_COOL_PLAN.md`.

## Done (1 commit, `main`)
- `DefenseResult.kt`: the confetti has 36 pieces on a normal phone and 14 on a low-end phone (`GameTheme.isLowEnd`, same switch the other Defense effects use). The result screen reads it itself, so `WisdomDefenseScreen.kt` is NOT touched and the call from it is unchanged.

## Checked by reading the code (no change needed)
- `DefenseEffects.kt` already skips shake and slow-motion on low-end and with reduce-motion, halves particles on low-end (a third with reduce-motion), and uses a softer edge pulse and no pop on the combo and banner with reduce-motion.
- `ResultStar`: no pop with reduce-motion. Confetti: not shown with reduce-motion.
- `WisdomDefenseScreen.kt` passes `lowEnd` and `reduceMotion` to the atmosphere, the effects and the cockpit.

## Still open in W7 (not done here)
- Boss death at boss size (field in `WisdomDyingEnemy`), the shot leaving the turret (now it starts at the muzzle point at the middle of the lane canvas), hub callback for the last level used (needs `GameTabScreen.kt`).
- Fixes from the owner's phone test: none reported yet.
- MW (Match look lift) is still open.

## Evidence
`[~]` written, NOT compiled, NOT seen on a phone. GitHub Actions runs #1008 to #1012 (W4 hook, W4 log, W6 x3) were still "In progress" when this was written; the last green run seen was #1007 (W4 juice). If any of them is red, fix the first red line before testing.

## Owner phone test
Win on Easy with 3 hearts on a cheap phone: stars pop, then a lighter confetti (14 pieces). Normal phone: same as W6.
