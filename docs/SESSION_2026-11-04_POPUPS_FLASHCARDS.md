# Session 2026-11-04: solid pop-ups and solid flashcards (owner request)

Owner: "make all popups increase opacity, text overlaps the screen behind; flashcards: the card must not be transparent, subtle blue front, subtle green back."

## Causes found
- Pop-ups: several custom pop-ups had a see-through fill (the Material AlertDialogs were already 95 percent via Theme.kt).
  Timer quit pop-up white 4 percent, Planner and Notebook pop-ups 30 percent, hub gate pop-up 30 percent, exit pop-up 70 percent,
  MainActivity countdown pop-up 40 percent.
- Flashcards: `AppBackdrop.GLASS_JS` turns every dark-navy element background into 16 percent glass, and that included the card faces and the cards stacked behind.

## Done and pushed to main
- `HubGateDialog.kt`, `ExitGuiltDialog.kt`: night 96 percent navy 0F1829 (0xF5), day 97 percent ivory (0xF7).
- `assets/notes/flashcards.html`: the colour now sits on a `::before` layer behind the faces (the glass script only looks at real elements), so the cards are opaque. Front = subtle blue (1B3B6E to 102545), back = subtle green (18493A to 0E2C23), matching border and label colours. Day mode (`html.wt-day`) uses pale blue / pale green tints because the day script turns the text into dark ink.

## Needs the patch workflow (files too big to resend)
`scripts/patch_popups_solid.py` (patch P-popups): NativeTimerScreen.kt, NativePlannerScreen.kt, NativeNotebookScreen.kt, MainActivity.kt. Same near-solid values. Tested on synthetic copies, run twice. Apply with GitHub > Actions > Apply pending patches > script name `patch_popups_solid.py` + a new branch name, wait for green build, then merge.

## Not touched (check on a phone first)
- `StatusDetailCard.kt` and `PaidPurchaseScreen.kt` full-screen dialogs (fill not read).
- Web pop-ups inside quiz / exam pages: `GLASS_JS` already makes a pop-up inside a dim scrim 96 percent navy. If one is still see-through, the owner names the screen.
