# SESSION 2026-10-19 — D6d: Scientific Calculator (day mode)

> Repo is public: no secrets. Owner order: "Continue" (after D6c). One slice per chat. Written by Claude (chat). NOT compiled by the agent, NOT seen on a phone.

## What was found (read from `main`, 2026-10-19)
`NativeCalculatorScreen.kt` (23 KB): all the UI colours were literals (white tints, cyan `67E8F9` / `22D3EE` / `06B6D4`, purple `D8B4FE` / `A855F7`, slate `94A3B8` / `CBD5E1`, amber `FCD34D`, rose `FDA4AF`, a dark display `4D040813`, white title, the "=" key with dark text on bright cyan). By day those are pale colours on ivory, so keys and results would be unreadable. The parser (`ScientificEvaluator`) is about two thirds of the file and has no colours.

## What was written (file resent whole; the parser was copied unchanged)
- A private `dayNight(night, day)` and named getters (`CcAccent`, `CcAccentBg`, `CcAccentEdge`, `CcPurpleText`, `CcPurpleBg`, `CcSlate400`, `CcSlate300`, `CcAmberText`, `CcRoseText`, `CcCardFill`, `CcDisplayFill`). Night = the exact old literal, so night should look as before.
- Day: card = ivory glass with a teal edge; keys = lighter ivory glass with the glass edge; digit keys = a faint navy tint and bold; function keys, results and mode text = deep teal; constants and RAD = deep purple; Ans and backspace = deep amber; AC = deep rose; "=" = deep teal with white text; display = the quiet glass.
- Cleanup: the old file had odd indentation inside the card header; braces and layout are the same (re-counted: tail still closes card, scroll column, main column, Box, function).
- The key colours were read once per key in the old code (local vals inside the composable); now they are getters read while composing, so they follow the switch.

## Owner: what to test (both modes: Settings > Display & Theme)
1. Calculator: title, icon, card, DEG / RAD toggle, the display with the faint example line (`sin(30) = 0.5`), and every key group (functions, constants, digits, operators, backspace, AC, Ans, %, =).
2. Type `sin(30)` and press `=`: result 0.5 in the result colour. Press RAD and check the toggle colour.
3. Night: all of it looks exactly as before. If CI is red, send the error text.

## Still open in D6 (D6e onwards)
`NativeTimerScreen.kt` (28 KB), `NativeNotebookScreen.kt` (33 KB), `NativePlannerScreen.kt` (39 KB), `NativeStatusScreen.kt` (31 KB), `NativeCoverPageScreen.kt` (30 KB; `CoverPageRenderer.kt` is the cover art, probably stays), `NativeTutorScreen.kt` (38 KB); `DownloadGauge.kt` unchecked.

## Board note
`docs/DAY_MODE_PLAN.md` still ends at D6a; the D6b, D6c and D6d rows live in their session files until a later slice folds them in.

## Next
D6e: Timer. The owner says "Continue".
