# SESSION 2026-11-06 — U3d: "Study time" panel with Today | Week | Month | Total

Plan: `docs/PLAN_2026-11-06_URGENT_FIVE_FIXES.md` (owner: a day chart exists but no clean Today / Week / Month / Total control; Total hours kept rising).

## What the student sees
A new panel "STUDY TIME" near the top of My Status (right after the four cards, before "Your week"; also in the empty offline skeleton):
- A control **Today | Week | Month | Total** (Week is the default).
- The study time of that range in big numbers, and the range in words (Today, Last 7 days, Last 30 days, All time).
- Today: a bar against the student's own daily goal. Week: a bar per day. Month: a bar per day (30). Total: a bar per month (last 12 months).
  Each bar is green for the focus part and blue for the rest; an amber line is the daily goal (day bars only).
- A Focus box (percent of study time, or a dash when no focus is recorded in that range yet), and three small numbers for Week, Month and Total:
  Active days, Average per active day, Best day.
- If a day is over 8 hours it counts as 8 hours and the panel says so (nothing is deleted).
Real numbers only: days without a row are empty bars, never made-up values.

## Where the numbers come from
The student's own `study_day_log` through `StudyDayLogReader`: saved copy first (works offline), then the server; a failed refresh keeps what is drawn.
The panel reads about 400 days. The reader's row limit was 60 (oldest first), which would hide the newest days of a long history, so the patch raises it to 500.
The existing "Today & this week" panel is left as it is.

## What was written
- `scripts/u3d/StatusRangePanel.kt.txt` (the new screen part, kept as text on main so main still builds; the patch copies it to `ui/study/StatusRangePanel.kt`).
- `scripts/apply_u3d_range_panel.py` (patch script): copies the panel, adds 2 lines to `NativeStatusScreen.kt` (loaded screen and offline skeleton), raises the limit in `StudyDayLogReader.kt`.
  Tested on mock copies: patches all three, second run = "already patched". All-or-nothing anchors. Not run on the real files; the Kotlin is NOT compiled here, so the first build on the branch is the real test.

## Owner steps
GitHub > Actions > **Apply pending patches** > Run workflow > script `apply_u3d_range_panel.py`, branch `fix/u3d-range-panel` > wait for the build > test > merge the pull request.
(Do this after the U3c pull request is merged, or on a separate branch; they touch different files.)

## Test on the phone
1. My Status shows the "STUDY TIME" panel with four buttons; each button changes the numbers and the bars at once.
2. Week: 7 bars with day names; Month: 30 thin bars with a day number every 5th; Total: up to 12 bars with month names.
3. Total is lower than the old "cumulative study time" sentence if you had runaway days (each day counts at most 8 h); the small line says how many days were limited.
4. Aeroplane mode: the panel still shows the saved numbers.
5. Night, Day and Transparent look; large text size.

## Not done here (on purpose)
- The four HUD cards and the "Study progress summary" sentence still add up the old per-resource totals (the 70+ h). Making them use the capped day-log total is the next small slice (U3e).
- Subject share already exists in "By subject"; not repeated.
- Goal ring: Today uses a bar, not a ring (simpler and safer without a compiler here).
