# SESSION 2026-10-22: GAMES W3 (Wisdom Defense cockpit)

Continues `docs/SESSION_2026-10-22_GAMES_W2.md`.

## Done (2 commits, `main`)
- NEW `ui/games/defense/DefenseCockpit.kt` (`19dabc4`), pure UI, no engine change:
  - `DefenseCockpit(...)`: a small turret at the bottom centre and the four answers as bullets in an arch around it. A lower left, B upper left, C upper right, D lower right (the two upper bullets sit a little closer to the middle, so the four trace an arch over the turret). Height 144 dp plus padding, about the same as the old 2x2 cards.
  - Tap a bullet: it fires (same `engine.fire`, same sound, haptic and muzzle effect as before). While the shot flies, the tapped bullet lifts off and fades.
  - Press and hold a bullet: the full answer shows in a pop-up above the cockpit; letting go closes it and fires nothing. This is the "long answers are never cut" rule of the plan. The bullet text itself shows 2 lines.
  - Wrong (locked) bullets: greyed with a red edge and a cross, cannot be fired, can still be held to read.
  - Letters A to D stay on the bullets. Each bullet has button semantics with a click action (TalkBack).
  - `drawDefenseTurret(...)`: base, barrel that kicks back when a shot flies, pulsing core, short muzzle flash at the barrel tip. With reduce-motion: no kick, no pulse.
  - `COCKPIT_MIN_WIDTH = 320.dp`.
- `WisdomDefenseScreen.kt` (`e48ce2a`): header comment, import `BoxWithConstraints`, and the answer block. On widths of 320 dp or more it draws `DefenseCockpit`; narrower it draws the old 2x2 `AnswerCard` grid unchanged. Both call one `fireChoice` lambda (the old onClick body). Nothing else changed.

## Not in W3 (be honest)
- The shot still starts at the empire crystal on the wall (`defenseMuzzleY`), not at the new turret, because the turret lives in the area below the battlefield canvas. The turret recoils and flashes in sync with the shot, but the bullet does not visibly leave the turret barrel. Fixing that means drawing the shot across both areas (a bigger change). Parked, say so if wanted.
- The bullet that is tapped lifts off in place; it does not fly the whole way to the enemy.
- Plan said "bullets in an ARC with a circular fan"; two rows in an arch were used so the text stays readable at 12 sp on a 360 dp wide phone.

## Evidence
`[~]` written, NOT compiled, NOT seen on a phone. Things most likely to need a fix after the first build: (1) `Modifier.offset(x = Dp, y = Dp)` and `Dp * Float` arithmetic in `DefenseCockpit.kt`; (2) the pop-up uses `graphicsLayer { translationY = -size.height - 8.dp.toPx() }` to sit above the cockpit, check it is not clipped and sits above the row on the phone; (3) the long press must not also fire the answer (tap and long press are handled by one `detectTapGestures`, so it should not). All sizes (bullet 40 percent of the width, 60 dp high, turret 16 percent of the width) are proposals.

## Owner phone test
Play Wisdom Defense, any level. Under the lane you now see a small turret in the middle and four rounded bullets with the answers around it, B and C above, A and D beside the turret. Tap one: the turret kicks back and flashes, the bullet lifts and the shot flies. Wrong answer: that bullet turns grey with a cross. Hold a bullet with a long answer: the full text appears above. Check that all four are easy to hit with a thumb and the wall is still visible. If the screen is very narrow you get the old four cards. Tell me if the build fails: send the red line of the build log.

## Next
W4 Juice (`DefenseEffects.kt`): combo meter, kill banners from streak 2, screen-edge pulse, shockwave on a wall hit, floating score numbers, slow-motion on the last enemy, wave banner. Includes the parked W2 items (boss entrance banner, boss death at boss size).
