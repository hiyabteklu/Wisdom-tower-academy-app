# SESSION 2026-10-22: GAMES W7c (shot leaves the turret)

Continues `docs/SESSION_2026-10-22_GAMES_W7B.md`.

## Done (1 commit, `main`, `7491158`)
- `WisdomDefenseScreen.kt` only:
  - New `cockpitOn` (screen width at least `COCKPIT_MIN_WIDTH`, same rule as the cockpit). When on, the shot and its trail start at the turret barrel tip (`TURRET_TIP_BELOW` = 86 dp below the battlefield canvas) instead of the empire crystal.
  - When on, the crystal muzzle flash (`fx.fireMuzzle()`) is not called: the turret already has its own flash and kick. The 2x2 fallback cards keep the old crystal origin and flash.

## Hypothesis (not verified)
The part of the shot drawn below the canvas relies on Compose not clipping a Canvas to its bounds. Also, the cockpit is drawn after the canvas, so its bullets and turret cover the shot there. If a phone clips it, the shot just appears at the bottom edge of the battlefield (no crash). The 86 dp is an estimate from the cockpit layout (8 dp padding + about 78 dp to the barrel tip); adjust `TURRET_TIP_BELOW` after a look at a phone.

## Still open in W7
Hub callback for the last level used (`GameTabScreen.kt`), owner phone-test fixes, MW (Match lift). Small: empty boss bar at the start of the boss death (draw the bar only when `bossFrac > 0f` in `DefenseArt.kt`).

## Evidence
`[~]` written, NOT compiled, NOT seen on a phone.

## Owner phone test
Answer a question: the shot should rise from the turret between the bullets and fly to the monster, with the flash on the turret, none on the crystal.
