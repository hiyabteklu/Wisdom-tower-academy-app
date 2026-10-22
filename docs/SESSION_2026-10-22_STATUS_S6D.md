# SESSION 2026-10-22 STATUS S6D: Today ring + 7-day weekday bars

> Repo is PUBLIC: no secrets, no student data. Plan: `docs/MY_STATUS_V2_S6_DAY_LOG_PLAN.md`. S6a live; S6b script and S6c reader are on main (S6b script not yet run).

DONE (one commit on `main`, NOT compiled, NOT run on a device):
- `ui/study/StatusDayPanel.kt` (NEW, compiles on its own, used by nothing until the patch runs): `StatusDayPanel(dailyGoalMinutes)`. Reads `StudyDayLogReader.fetchRecent(7)`. Shows the "Today" ring (today's minutes / daily goal; emerald at 100%, amber from 50%, sky below) and 7 weekday bars (minutes per day, today highlighted). Days with no row = thin empty bar, never a made-up value. Muted line while fewer than 7 rows exist. Loading spinner; failure = `WisdomMessageCard` with Retry. Day/night colours via the same rule as `NativeStatusScreen`. Goal 0 = no ring fill and the text "No daily goal set".
- `scripts/patch_status_s6d_day_panel.py` (NEW, safe twice): one inserted call after `StatusSubjectsPanel(...)` in `NativeStatusScreen.kt`, marker `MYSTATUS-S6`.
- `scripts/patch_status_s6_day_log_all.py` (NEW wrapper): runs S6b then S6d in one go.

PATCH (needs the owner, one run): GitHub > Actions > **Apply pending patches** > Run workflow > script `scripts/patch_status_s6_day_log_all.py` + new branch name `feat/status-s6-day-log` > green build > merge the Pull request.

UNSURE: (1) this panel does not use `statusRingColor` (name not checked in the repo), it has its own 3-colour rule; swap later if wanted. (2) First compile may flag an import or name (e.g. `WisdomMessageCard` params): fix from the CI log, small.

TEST (phone, after merge): Learning > Your status > below the subject board: "Today & this week" panel. Study 1-2 minutes, reopen Your status: ring and today's bar show the minutes. Past days stay empty until they are really studied.

PROGRESS: S6a done, S6b script on main (run it with the wrapper), S6c done, S6d done (not compiled).
