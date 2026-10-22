# SESSION 2026-10-22: GAMES MW part 3 (Match card flip)

Continues `docs/SESSION_2026-10-22_GAMES_MW2.md`. Owner said yes to the card flip (2026-10-22, chat).

## Done (1 commit, `main`)
- `MatchScreen.kt` only: every new card (first board and the two cards that replace a matched pair) flips over from a card back (a cyan "W") to its face in 380 ms while it scales in. The back shows while the card is turned more than 90 degrees, so text is never drawn mirrored. `GameTheme.reduceMotion` skips the flip.
- Decision: the flip is an entrance animation only. The game rules, `MatchEngine.kt` and the Hard board size are unchanged: cards still show their text as soon as the flip ends and are tappable at once. A real face-down memory game (hidden text until tapped) was NOT built because it would change what Match is (matching terms to meanings with text visible). Say so if that is wanted.

## Evidence
`[~]` written, NOT compiled, NOT seen on a phone. Uses `graphicsLayer.rotationY` and `cameraDistance` (both exist in the Compose version used elsewhere in the app for `graphicsLayer`).

## Owner phone test
Open Match: the cards turn over once at the start; after a right pair the two new cards turn over on the same spots; the text is never mirrored; with animations off in developer options nothing flips.
