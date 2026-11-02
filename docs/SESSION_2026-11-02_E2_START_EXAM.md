# SESSION 2026-11-02 E2: Start Exam button

Plan: `docs/READER_EXAM_FLASHCARD_FIX_PLAN.md` (slice E2).

## What was wrong
File: `app/src/main/assets/notes/quiz.html`. `setQuiz()` called `startTimer()` as soon as the page loaded, so the exam countdown began the moment the exam opened.

## What changed (quiz.html only)
- New flag `started`. For an EXAM (`isExam`) it begins `false`; for a question bank (practice) it begins `true`.
- An exam that is not started shows a start screen: title, number of questions, time (or "No limit"), short rules and a **Start Exam** button. No top bar, no timer and no questions are shown there.
- `startExam()` (Start button) sets `started`, resets the "time used" clock (`startedAt`), starts the countdown (`startTimer()`) and opens question 1.
- `startTimer()` now does nothing until `started` is true. So the countdown and the auto-submit at 0:00 can only happen after Start.
- Retake of an exam goes back to the start screen (countdown does not restart by itself). Retake of a question bank is unchanged.
- Swipe is off on the start screen.
- Question banks (untimed practice) keep opening directly, as before (decision: a Start button there would only add a tap).

## Exam integrity check
Read `EXAM_INTEGRITY_ADMIN.md` and the Kotlin hosts first. The hub exam (`QuizExamScreen.kt`) has no resume and no integrity flags: its state lives in the page only, and the integrity events cover ranked competition attempts only. Nothing there was changed or weakened. Kotlin untouched.

## Competition exams: no Start screen on purpose
`competition_exam.html` / `CompetitionExamScreen.kt` get an entry that already exists and a countdown fixed to the END of the competition (server clock, `remainingMs`). A Start screen there would hide a clock that is already running and would not change the deadline, which is worse and could look like cheating protection being bypassed. If the owner wants a "Start competition" step, it belongs BEFORE the entry is created (the competition hub), not inside this window. Not done, needs the owner's decision.

## Owner test on the phone (new build needed)
1. Open an exam (timed): you see the start screen, nothing counts down. Wait 30 seconds, press Start Exam: the timer begins at the full time.
2. Submit, press Retake Exam: you get the start screen again.
3. Open a question bank (practice): it opens directly on question 1 as before.

## Supabase
No database change.

Not compiled (HTML/JS asset, no Gradle change).

## Next slice
E3 (clean floating Prev / Next / Submit). It touches the same `.nav` / `navBar()` in `quiz.html` and in `competition_exam.html`; the start screen has no nav bar, nothing to change there.
