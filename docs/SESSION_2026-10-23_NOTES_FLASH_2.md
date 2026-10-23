# SESSION 2026-10-23 (2): notes still show a dark flash between the loader and the text

Owner (screenshots 7:50, order loader, dark navy, text): BG-NOFADE (earlier today) did not fix it.

## Cause (read from `NotesReaderScreen.kt` and `assets/notes/index.html`, NOT run)
- The navy in the screenshot is the page's own colour: `html, body { background: #0a101d }` in index.html.
- The transparent override (`AppBackdrop.clearPageBackground`) runs only at `onPageFinished`, and the WebView was hidden only from `onPageStarted` on.
- `NotesReaderScreen` sets `pageReady` after a blind 1.2 s safety timer. The first WebView of a run can need more than a second to start, so the screen revealed a page that had not finished loading and was still painting its own navy. Then the page finished, became transparent, and the text appeared.
- So the earlier fade theory was probably wrong (the instant reveal stays, it is harmless).

## Fix
1. `NotesReaderScreen.kt`: the WebView is hidden from creation (`alpha = 0f` in the factory), shown by `clearPageBackground` after the glass script ran, or by a 4 s safety.
2. `assets/notes/index.html`: `html, body` background is `transparent`.

## Not done / hypotheses
- Quiz, flashcards, tutor and competition pages have the same page-own dark background but are hidden from `onPageStarted` and have no blind early reveal, so they were left alone. If one of them flashes, apply the same two changes.
- Not compiled, not seen on a phone. If it still flashes, next suspect: the glass script skips pages with more than 4000 elements (long notes), leaving wrappers dark.

## Owner phone test
Open a note after a cold start (force-stop first, the first WebView is the slow one): loader on the picture, then text, no dark frame. Open a second note: same.
