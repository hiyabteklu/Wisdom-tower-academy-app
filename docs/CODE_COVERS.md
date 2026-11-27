# Code covers (owner request 2026-11-27)

Goal: draw the 16:9 Canva covers with code so the app carries no picture file for them (smaller app, new subjects need no new image).

## How it works
- A cover is a plain string `code-cover://<id>` (see `ui/packages/CodeCover.kt`: `codeCoverUri`, `isCodeCover`, `codeCoverLabel`).
- `OpenCardLayout` (`ui/packages/OpenCard.kt`) draws `CodeCover(...)` in the 16:9 slot when the image is such a string; every other image loads through Coil as before.
- `eceYearCover(year)` in `EceCoverAsset.kt` returns the code cover for Year 1 only. Years 2 to 5 still use their picture or `ece.jpg`.
- Only fonts already in `res/font` (Outfit Black title, Outfit Bold logo text). No new dependency, no new file in assets.

## State
- Slice 1 (Year 1 only): PUSHED to branch `code-cover-ece-y1`, NOT compiled, NOT seen on a phone. Merge to `main` only after the owner has looked at it.
- Next if the owner likes it: switch Years 2 to 5 (change `eceYearCover`), then semester covers and the Freshman course thumbnails (one `code-cover://` id + label each, or a small spec for subject covers).
- UNSURE: the title letters use Outfit Black squeezed sideways, so they will not be pixel identical to the Canva font (a condensed font such as Anton would be closer, needs a .ttf in `res/font`).
- To undo: return `eceCoverAsset("ece-y$year.jpg", "ece.jpg")` for every year in `eceYearCover`.
- Note: the Canva file for year 3 says "3ND"; the code label is "3RD".
