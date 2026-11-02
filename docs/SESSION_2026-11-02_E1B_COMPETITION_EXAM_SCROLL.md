# SESSION 2026-11-02 E1b: competition exam number board no longer jumps

Plan: `docs/READER_EXAM_FLASHCARD_FIX_PLAN.md` (slice E1b, finishes E1).

## What was wrong
File: `app/src/main/assets/notes/competition_exam.html`. It has the same `draw()` as `quiz.html`: the whole screen is rebuilt with `root.innerHTML = ...` after every tap. That threw away the question-number strip (so it went back to number 1 and then slid to the current number = the shaking) and the page scroll (a long question went back to the top after every answer pick, cross-out or flag).

## What changed (competition_exam.html only)
The same three changes as `quiz.html` (see `SESSION_2026-11-02_E1_NAVIGATOR_REVIEW_SCROLL.md`):
- `draw()` remembers the page scroll and the strip scroll before rebuilding and puts both back right after.
- The strip moves (smoothly) only when the current number is not fully visible. The old `scrollIntoView` was removed.
- The page still goes to the top on purpose when: a new question opens (Next/Prev/number, already the case), the exam is first shown (`setQuiz`), and when the server result arrives (`wtSubmitResult`, success).

## Competition review
The competition exam has NO question-by-question review page (after submit it shows only the small Submitted card and the score card; the server grades and the page holds no answer key). So problem 5 (Solution / Explain with AI sending you to question 1) cannot happen here. Nothing to fix there.

## Not changed
Answers, saving, grading, countdown, integrity flags (copy/cut reports), texts, and the Kotlin side. `website-reference/` not touched (not a priority).

## Owner test on the phone (new build needed)
1. Open a competition exam with many questions, go to question 23, press Next: the number strip stays still and just shows 24.
2. Tap a far number in the strip: it slides there, no jump to 1.
3. On a long question, scroll down, pick an answer or press the cross-out button: the screen stays where it is.

## Supabase
No database change.

Not compiled (HTML/JS asset, no Gradle change).

## Next slice
E2 (Start Exam button). Remember: read `EXAM_INTEGRITY_ADMIN.md` and `EXAM_INTEGRITY_REVIEW.md` first, and check `competition_exam.html` too (its countdown runs to the competition end, so a Start screen there must not delay or extend that deadline).
