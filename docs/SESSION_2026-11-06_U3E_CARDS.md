# SESSION 2026-11-06 — U3e: the four cards and the summary sentence use the capped total

Plan: `docs/PLAN_2026-11-06_URGENT_FIVE_FIXES.md` (owner: Total hours kept rising, 70+ h, and Focus showed 0%).
U3d (the "Study time" panel, PR #111) and U3c (focus recording, PR #110) are merged. This slice makes the top of My Status agree with that panel.

## What the student sees
- The **Study Time** card and the sentence "Your cumulative study time is ... hours (... minutes)" show the SAME total as the **Total** button of the Study time panel:
  the student's own `study_day_log`, last 400 days, each day counted at most 8 hours. Nothing is deleted.
- If at least one day was over 8 hours the sentence adds: "Days over 8 hours count as 8 hours."
- The **Focus** card uses the focus of the same day log (never more than that day's study time). It shows a dash until the newest app version has recorded some focus.
- While the day log is not read yet (a moment on first open, or offline with nothing saved) the card shows a dash and the sentence says "loading". A student with a day log but no rows gets a real 0.
- **Accuracy** and **Active Streak** are not touched.

## Where the numbers come from
`StudyDayLogReader` (saved copy first, then the server), same window and same 8-hour cap as `StatusRangePanel`, so the card and the Total button cannot disagree.
It reads again by itself when the internet comes back.

## A second fix inside the same patch (found while writing U3e)
The offline copy of the day log was REPLACED by every successful read. The "Today & this week" panel reads only 7 days, so whenever it finished last it left a 7-day copy on the phone.
That would have made the Total button (U3d) and the new cards flash or keep a small number offline. `saveSnapshot` in `StudyDayLogReader.kt` now MERGES by day (days of the new read win, older saved days stay).
No database change.

## What was written
- `scripts/u3e/StatusCappedTotals.kt.txt` (text on main so main still builds; the patch copies it to `ui/study/StatusCappedTotals.kt`): `CappedStudyTotals`, `cappedStudyTotals`, `cumulativeStudySentence`, `rememberCappedStudyTotals`.
- `scripts/apply_u3e_cards.py` (patch script): 7 small edits in `NativeStatusScreen.kt` (one `val cappedTotals`, the sentence line, the two `StHudCards(...)` calls, the function signature, the focus line, the Study Time card) + the merge in `StudyDayLogReader.kt`.
  Tested on a copy of the real `NativeStatusScreen.kt` of main (sha 5a07bba) and a mock of the reader holding the real `saveSnapshot` block: patches all, second run = "already patched", a wrong anchor writes nothing.
  The Kotlin is NOT compiled here, so the first branch build is the real test.

## Owner steps
GitHub > Actions > **Apply pending patches** > Run workflow > script `apply_u3e_cards.py`, branch `fix/u3e-cards` > wait for the build > test > merge the pull request.
If the build fails, send the line that starts with `e: file:///`.

## Test on the phone
1. My Status: the Study Time card, the sentence and the **Total** button of the Study time panel show the same hours (the card has one decimal, the panel shows hours and minutes).
2. If you had runaway days, the number is lower than before and the sentence says "Days over 8 hours count as 8 hours."
3. Focus card: a dash at first; after reading a note or a book with the newest version, it shows a percent that matches the Focus box of the Total button.
4. Aeroplane mode: open My Status once online, then offline: the card and the Total button still show the long total (not just the last 7 days).
5. Accuracy and Active Streak look and count as before. Night, Day and Transparent; large text size.

## Not changed (on purpose, not asked)
- Students whose study happened before the day log existed have that time only in the old per-resource rows. They will see a smaller total than before (the planned default: the old rows are kept, never deleted).
- The "Your week" block and the shareable status picture (`StatusCardData`), the retention panel and the alert and tip texts in `NativeStatusModel.kt` still use the old rows (focus and hours). Say the word if they should follow.
