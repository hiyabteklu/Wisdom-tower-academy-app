# SESSION 2026-10-23: background at start, and black flash when notes load

Owner report (two screenshots, 2026-10-23, Transparent picture look):
1. At app start the picture shows only in the middle; the status bar, header and bottom bar stay black. Switching the look in Settings and back fixes it instantly.
2. In Notes the loader and the chosen background stay on the blank screen, but the background turns black for a moment right before the text is painted.

NOTHING BELOW IS COMPILED OR SEEN ON A PHONE. Both causes are HYPOTHESES from reading the code (MainActivity.kt was not opened, only the places that call `appBackdrop()`).

## 1. Start-up picture (AppBackground.kt)
- The picture is decoded on a thread after the first draw. The screenshot shows the copy behind the bars (root copy in `Theme.kt`) with the plain colour and the copy in the middle (loader overlay in `MainActivity.kt`) with the picture, so the root copy was not redrawn when the picture became ready.
- Fix: `appBackdrop()` now reads the picture version while composing and uses it in the draw block, so every copy is rebuilt and redrawn when a picture is ready; the version bump is written on the main thread.
- Not done: decoding before the first frame (would remove the one plain-colour frame completely).

## 2. Black flash in notes (AppBackdrop.kt, NotesReaderScreen.kt)
- The note page was revealed with an animated alpha (Compose 220 ms, plus 140 ms in `clearPageBackground`). Hypothesis: a transparent WebView at a fractional alpha is drawn through an offscreen layer and shows black until its content is painted.
- Fix: the reveal is instant (alpha 0 to 1, no fractional values). `hidePageUntilReady` and `clearPageBackground` do the same for quiz, flashcards, tutor and competition pages. Only the loader still fades.
- If the black still shows: next suspect is the page itself (reveal before `window.setNote` has painted); then the reveal should wait for the page's `onReady` message in every WebView screen.

## Owner phone test
Cold start with the picture look: the picture must cover the status bar, header and bottom bar from the first moment (one plain frame at the very start is possible). Open a note, a quiz and flashcards: loader on the picture, then the content appears with no black frame in between.
