# SESSION 2026-11-02 E3: clean floating Prev / Next / Submit

Plan: `docs/READER_EXAM_FLASHCARD_FIX_PLAN.md` (slice E3).

## What was wrong
Files: `app/src/main/assets/notes/quiz.html` and `competition_exam.html`. Prev / Next / Submit were a footer block (`.nav`, `position: sticky`, own dark background). In the hub exam the block sat right under the question, so it moved up and down with short and long questions and looked like a separate cluttered strip.

## What changed (both files, same code)
- `.nav` is now `position: fixed` at the bottom of the screen, so the buttons are in the SAME place on every question. It is a rounded pill bar (`.navin`) with a soft shadow, over the page, not a footer block.
- A spacer (`.navpad`, 84px plus the system gesture-bar inset) is added under the question so the last lines of a long question are never hidden behind the bar.
- Prev / Next / Submit are pills (fully rounded). Same texts as before.
- On the LAST question Next is disabled and Submit Exam (or Finish Practice) lights up green in the same place. Submit is still available on every question, as before.
- The bar sits under dialogs (`z-index` 20, dialogs 50). The bar is only drawn on the question screen: not on the start screen (E2), not in the review, not in the competition Submitted card.
- Logic, answers, grading, timer, texts and the Kotlin side: unchanged.

## Day / night
The bar uses a dark see-through fill (92 percent) like other panels in these pages, so the existing day-mode page script (D8, `AppBackdrop.kt`, works from computed colours) should recolour it with the other dark panels. Not verified on a device: check this first on the phone.

## Owner test on the phone (new build needed)
1. Open a practice or exam, go through a short question and a long question: the three buttons stay in exactly the same spot at the bottom.
2. On a long question scroll to the bottom: the last choice is fully visible above the buttons.
3. Go to the last question: Next is greyed, Submit is green.
4. Switch to the day look: the bar should look like the other panels (tell me if it stays dark).
5. Same checks in a competition exam.

## Supabase
No database change.

Not compiled (HTML/JS asset, no Gradle change).

## Next slice
N1 (notes reader remembers where you stopped). First find how the PDF reader saves the page and where the notes reader lives.
