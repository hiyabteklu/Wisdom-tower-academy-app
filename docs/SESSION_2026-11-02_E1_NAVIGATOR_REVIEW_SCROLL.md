# SESSION 2026-11-02 E1: number board no longer jumps, review keeps your place

Plan: `docs/READER_EXAM_FLASHCARD_FIX_PLAN.md` (slice E1, part 1 of 2).

## What was wrong (found in the code)
File: `app/src/main/assets/notes/quiz.html` (practice and exams, also the review after submit).
`draw()` rebuilds the whole screen with `root.innerHTML = ...` after every tap. Two side effects:
1. The question-number strip (`.pills`) was thrown away and created again, so its scroll went back to the start (question 1), then the old code slid it to the current number. That was the shaking.
2. The page scroll was lost on every redraw. In review mode all questions are in one long page, so pressing Official solution or Explain with AI (and the AI answer arriving later) put the page back at the top = question 1.

## What changed (quiz.html only)
- `draw()` remembers the page scroll and the strip scroll before rebuilding and puts both back right after.
- The strip moves (smoothly) only when the current number is not fully visible. The old `scrollIntoView` was removed, it could also move the page.
- On purpose the page still goes to the top when: a new question opens (Next/Prev/number), Retake, Submit, and when a review filter is picked (All / Missed / Flagged).
- No change to answers, grading, timer, texts or the Kotlin side.

## Not done yet (E1 part 2)
`app/src/main/assets/notes/competition_exam.html` very likely has the same `draw()` and the same bug (competition exams). Do the same fix there as slice **E1b**. The website copy `website-reference/` is not a priority.

## Owner test on the phone (new build needed)
1. Open a 40-question practice or exam, go to question 23, press Next: the number strip must stay still and just show 24.
2. Tap a far number in the strip: the strip slides to it, no jump to 1.
3. Submit, open Review All, scroll to question 20, press Official solution and Explain with AI: the screen must stay on question 20.

## Supabase
No database change.

Not compiled (HTML/JS asset, no Gradle change).
