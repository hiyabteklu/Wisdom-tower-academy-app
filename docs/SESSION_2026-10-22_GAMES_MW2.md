# SESSION 2026-10-22: GAMES MW part 2 (Match level chip)

Continues `docs/SESSION_2026-10-22_GAMES_MW1.md`.

## Done (1 commit, `main`, `dd2286b`)
- `MatchScreen.kt` only: a small level chip (`level.label`, cyan, rounded) in the top row between the chapter title and the "matched/total" count. No extra height, so the Hard board still fits without scrolling. `MatchHud.kt` was not touched.

## Not done (left open on purpose)
Card back pattern and flip on select. It changes how every card is drawn in `MatchScreen.kt` (`MatchCardView`) and cards currently show their text face up, so a flip would need a new "face down" state in the game rules too. That is a design change, not polish: ask the owner before doing it.

## Evidence
`[~]` written, NOT compiled, NOT seen on a phone. The chip text uses the same `GameLevel.label` as the Defense chip.

## Owner phone test
Open Match on each level: the chip (Easy / Medium / Hard) shows next to the count, the title is still readable, Hard still shows all 12 cards without scrolling.
