# Session 2026-11-04: result card (and confirm dialog) pushed off the screen

Owner report (plain words): in the question bank, the exams and the competition, the result card is not visible. The screen only dims, so the card is somewhere outside the screen. Make the pop-up appear in the middle of the screen.

## Cause (found by reading the code, NOT yet seen on a phone: treat as a strong hypothesis until the owner confirms)
- `assets/notes/richtext.css` (shared by quiz.html, competition_exam.html and the other local pages) got this rule on 2026-11-03 (V2 / V9 arrival motion): `#root { animation: wt-page-arrive 200ms ... both; }`. The last keyframe is `transform: translateY(0)`.
- Fill mode `both` includes `forwards`, so that transform stayed on `#root` for good after the 200 ms.
- In CSS, any transform other than `none` on an ancestor makes that ancestor the containing block of every `position: fixed` child. The pop-up layer `.ovl` (`position: fixed; top/right/bottom/left: 0`, content centred) and the floating bar `.nav` are children of `#root`.
- So the dim and the card were laid out against the whole `#root`, not the screen. After Submit the quiz page also draws the full review list under the card, so `#root` is many screens tall: the dim is seen at the top, the card is centred far below the visible part.
- Timeline fits: the rule was merged on 2026-11-03, the owner saw the problem on 2026-11-04.

## Fix (one rule, one shared file)
- `richtext.css`: fill mode changed from `both` to `backwards`. The first keyframe still paints while the page arrives; when the animation ends the transform is gone, so fixed pop-ups are placed against the screen again. A comment in the file says never to leave a permanent transform, filter, perspective, will-change: transform or contain: paint on `#root` or `body`.
- No other file touched. quiz.html, competition_exam.html and every other page that uses `#root` are fixed by the same line.

## What the owner should test on the phone
1. Question bank: answer a few, tap Finish Practice, confirm: the result card must be centred on the screen with the dim behind it. Close it (X): the review list shows. Tap Score Card: the card is centred again.
2. Exam (hub): same, with Submit Exam.
3. Competition exam: tap Submit Exam: the confirm dialog and the result card must both be centred.
4. The floating Prev / Next / Submit bar must stay stuck to the bottom of the screen while scrolling a long question (it was probably also sliding with the page, same cause).
5. Other pages with pop-ups (flashcards, notes, AI tutor) should also be centred.

## Not verified
- No build, no phone. CSS only, so nothing to compile; `main` CI does not need to change.
- Not read: `flashcards.html`, `index.html`, `tutor.html`, `competition_review.html`, `game_question.html` (the fix is in the shared file, so they are covered if they use `#root`).

## Unsure
- If the card is STILL off-screen after this change, the next suspect is the WebView size in the host screen (a WebView taller than the visible area). Log that here before changing Kotlin.
