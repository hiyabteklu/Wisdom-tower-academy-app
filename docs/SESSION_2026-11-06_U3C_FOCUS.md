# SESSION 2026-11-06 — U3c: Focus is now recorded

Plan: `docs/PLAN_2026-11-06_URGENT_FIVE_FIXES.md` (U3a finding: the app never wrote focus, so Focus stayed 0%).

## Check first (found in main today)
Both `NotesReaderScreen.kt` and `PdfReaderScreen.kt` on `main` already use `rememberActiveStudyTimer`, so the U3b patch is applied and merged.
The U3b phone tests (docs/SESSION_2026-11-06_U3B_STUDY_TIME.md) are still the owner's.

## Focus in plain words (default, no question to the owner)
A second counts as **focus** when the student touched, scrolled or turned a page in the last **3 minutes**.
Continuous reading with no pause longer than 3 minutes = all focus. After 3 minutes with no touch the time still counts as
study time, but not as focus, until the next touch. Coming back to the screen counts as a touch. Focus is never more than study time.
Old rows are not changed: their focus stays as it is (U3d decides how to show old days).

## What was written
- `ui/study/ActiveStudyTimer.kt` (updated on main): new `touch()`, `takeSlice(maxSeconds)` (study seconds + focus seconds) and
  `StudySlice`; `take()` still works. Logic checked with a small simulation (all-active 2 min = 120/120; 10 min with no touch =
  600/180; screen off in the middle = only active time counted).
- `scripts/apply_u3c_focus.py` (patch script, the two readers are big files):
  - Notes: the 30 s flush and the leave flush now send `addFocusSeconds`; page scroll (`onPosition`, `onProgress`) and a tutor question count as touches.
  - PDF book: finger down and finger moving on the book count as touches; `saveReading` sends `addFocusSeconds`.
  Tested on mock copies of the current anchor text: patches both files, second run = "already patched", a missing anchor = error and
  no file changed. Not run on the real files yet.
- No database change and no change to `LearningProgressRepository`: it already adds `addFocusSeconds` to `learning_progress.focus_seconds`
  and to `study_day_log` (function `add_study_seconds`, which handles focus).

## Owner steps
GitHub > Actions > **Apply pending patches** > Run workflow > script `apply_u3c_focus.py`, branch `fix/u3c-focus` > wait for the build > test > merge the pull request.

## Test on the phone
1. Open a note, scroll now and then for 2 minutes, leave: My Status shows about 2 minutes of study and Focus well above 0%.
2. Open a note, do not touch the screen for 5 minutes (screen on), then leave: study time about 5 minutes, but only the first 3 minutes count as focus.
3. Same two tests with a PDF book (touching includes scrolling and turning pages).
4. Screen off or app in the background: nothing is counted (U3b), focus too.
5. Focus can only be seen in My Status after U3d, or in the Supabase table `study_day_log` (`focus_seconds`).

## Not done here (on purpose)
- Flashcards, quizzes, videos and the AI tutor still write no study time (not asked).
- Old totals are not touched; U3d reads the day log with each day capped.
