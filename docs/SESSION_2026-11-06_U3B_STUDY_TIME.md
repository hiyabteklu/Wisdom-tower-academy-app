# SESSION 2026-11-06 — U3b: study time counts only while the reader is in front

Plan: `docs/PLAN_2026-11-06_URGENT_FIVE_FIXES.md` (U3a findings: notes reader added uncapped elapsed time, PDF reader added wall-clock
time, neither stopped on screen-off or background).

## What was written
- `ui/study/ActiveStudyTimer.kt` (new, pushed to main): `ActiveStudyTimer` (counts only between resume and pause, `take(maxSeconds)`
  hands out the counted seconds and starts again) and `rememberActiveStudyTimer(key)` (resumes on ON_RESUME, pauses on ON_PAUSE and
  when the screen leaves). The app already uses the same lifecycle pattern in `SecureContentCaptureProtection.kt`.
- `scripts/apply_u3b_study_time.py` (patch script, needed because `NotesReaderScreen.kt` and `PdfReaderScreen.kt` are big):
  - NotesReaderScreen: `NoteProgress` gets a `timer`; `flushNoteProgress` adds `timer.take(600)` instead of `now - lastFlush`.
  - PdfReaderScreen: `sessionStart` is replaced by the timer; `saveReading` adds `timer.take(600)`.
  Tested on mock copies of the anchor text (patch, second run = nothing, a missing anchor = error and no change). Not run on the
  real files yet.

## Owner steps
GitHub > Actions > **Apply pending patches** > Run workflow > script `apply_u3b_study_time.py`, branch `fix/u3b-study-time` > wait for the
build > test > merge the pull request.

## Test on the phone
1. Open a note, read 1 minute, press Home, wait 5 minutes, come back, leave the note: My Status shows about 1 minute, not 6.
2. Open a note, turn the screen off for 5 minutes, turn it on, leave: the 5 minutes are not counted.
3. Same two tests with a PDF book (turn pages once before leaving).
4. Normal reading still adds time: 2 minutes of reading shows about 2 minutes.

## Not done here (on purpose)
- Old wrong totals are not touched (U3d reads the day log with each day capped).
- Focus time is still not written (U3c).
- `docs/PENDING_PATCHES.md` was not edited (it is a large file); this note and the plan carry the record.
