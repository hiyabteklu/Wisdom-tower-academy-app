# SESSION 2026-10-22: GAMES W2 (Wisdom Defense enemies)

Continues `docs/SESSION_2026-10-22_GAMES_W1.md`.

## Done (2 commits, `main`)
- `a646526` `DefenseArt.kt` only (+171 / -29):
  - `DefenseSkin` + `defenseSkin(level)`: colours of slime, bat and golem per level (Easy friendly green, Medium purple, Hard ember). Default skin is Medium, so any caller that passes nothing looks as before.
  - `drawDefenseEnemy` got `skin`, `armor`, `boss`, `bossFrac`, `spawnT` (all with defaults).
  - Portal flash: soft glow behind the enemy plus a white ring that shrinks while it is at the far edge. Boss gets a bigger rose glow.
  - Bat: a second, faint pair of wings at another flap phase (wing blur).
  - Golem: stomp dust at both feet when a foot lands; armor plates in gold-edged steel. `armor >= 2` = shoulders and chest, `armor == 1` = chest only, so plates come off as armor is cracked.
  - Boss: golem drawn 1.6 times bigger, red health bar above the head, filled to `bossFrac`.
- `5f4f9fc` `WisdomDefenseScreen.kt` (+15 / -2): `skin` computed once per level; walking enemies get `skin`, `e.armor`, `e.boss`, `bossFrac = (e.armor + 1) / BOSS_HITS`, and `spawnT` (strong at progress 0, gone at 0.10). Dying enemies get `skin`.

## No engine change
The plan said "small engine hook for armor hits". Not needed: `WisdomEnemy` already has `armor` and `boss` (G1), so W2 only reads them. Boss bar math: armor starts at 2, so the bar shows 3/3, then 2/3, then 1/3, and the kill removes it.

## Not in W2 (plan said more)
- Boss "entrance roar flash" and the final wave banner: only the bigger red portal glow was done. The banner belongs to W4 (wave banners).
- Slime squish already existed from D2, left as is.
- Known gap: `WisdomDyingEnemy` has no `boss` flag, so the boss death animation is drawn at normal size and without the bar. Fix needs one field in the engine file. Not done here.
- Known quirk: the 3 enemies that start on the lane at 0.45 / 0.27 / 0.09 are not at the far edge, so only the one at 0.09 shows a short portal flash at the start. Harmless.

## Evidence
`[~]` written, NOT compiled, NOT seen on a phone. Before writing this log the call sites were checked by reading: `drawDefenseEnemy` parameters match the two calls in `WisdomDefenseScreen.kt`, `defenseSkin` exists, `e.armor`, `e.boss` and `WisdomDefenseEngine.BOSS_HITS` exist. CI runs #1001 and #1002 were still in progress when this was written, so the build result is unknown. Colours, sizes (1.6x boss, bar width) and alphas are proposals.

## Owner phone test
Play Wisdom Defense on each level. Easy slimes are green, Medium purple, Hard orange-red, bats and golems change colour too. Enemies get a soft glow when they enter at the top. Bats look blurry when flapping. Golems kick up a little dust. On Hard, some golems wear plates; the first correct answer on one takes the shoulder plates off, the second takes the chest plate, and only then does it die. The last enemy on Hard is a big golem with a red bar that drops by a third per correct answer. If anything is hard to see on a small phone, say which level and which enemy.

## Next
W3 Cockpit (`WisdomDefenseScreen.kt` + new `DefenseCockpit.kt`): the riskiest slice, do it alone in its own chat. Keep the 2x2 cards as the fallback.
