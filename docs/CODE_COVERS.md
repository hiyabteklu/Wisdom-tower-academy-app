# Code covers (owner request 2026-11-27)

Goal: draw the 16:9 Canva covers with code so the app carries no picture file for them (smaller app, new subjects need no new image).

## How it works
- A cover is a plain string `code-cover://<id>` (see `ui/packages/CodeCover.kt`: `codeCoverUri`, `isCodeCover`, `codeCoverLabel`).
- `OpenCardLayout` (`ui/packages/OpenCard.kt`) draws `CodeCover(...)` in the 16:9 slot when the image is such a string; every other image loads through Coil as before.
- `eceYearCover(year)` in `EceCoverAsset.kt` returns the code cover for ECE Years 1 to 5 (labels in `CODE_COVER_LABELS`).
- Only fonts already in `res/font` (Outfit Black title, Outfit Bold logo text). No new dependency, no new file in assets.

## State
- Slice 1 (Year 1): owner saw it on a phone and approved (2026-11-27).
- Slice 2 (Years 2 to 5 + merge to `main`): PUSHED to `main` 2026-11-27, NOT compiled, not yet seen on a phone for Years 2 to 5. Old ECE picture files are still in the app (not deleted).
- UNSURE: `ECE_TRACK_COVER_ASSET` (`EceTrackCover.kt`) is `eceYearCover(3)` and now returns a `code-cover://` string. If any screen passes it straight to Coil instead of `OpenCard`, that spot shows blank: check on the phone (Learning > Curriculum).
- UNSURE: the title letters use Outfit Black squeezed sideways, so they are not pixel identical to the Canva font (a condensed font such as Anton would be closer, needs a .ttf in `res/font`).
- Next: see `docs/CODE_COVERS_NEXT_AGENT_PROMPT.md` (every other card picture, welcome card excluded).
- To undo: return `eceCoverAsset("ece-y$year.jpg", "ece.jpg")` for every year in `eceYearCover`.
- Note: the Canva file for year 3 says "3ND"; the code label is "3RD".
