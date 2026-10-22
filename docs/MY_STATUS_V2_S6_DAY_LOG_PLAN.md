# MY STATUS V2, S6: day log (weekday chart + "today vs daily goal" ring)

> Written 2026-10-22 by Claude (chat). Owner said **"Yes S6"** (answer to question B of `docs/MY_STATUS_V2_PLAN.md`). Repo is PUBLIC: no secrets, no student data. Rules as every slice: `docs/OWNER_RULE_PLAN_FIRST_PUSH_TO_MAIN.md`, `docs/AGENT_QUOTA_PROTOCOL.md`, `docs/EDITING_LARGE_FILES.md`, `docs/UI_POLISH_TASKS.md` G1 to G9. Owner is not a coder and on the free plan: do every step yourself, one small slice per chat, push to `main`, tell the owner exact taps only.

## 1. Why
`learning_progress` keeps LIFETIME seconds per item, so "minutes today" and a weekday chart cannot be derived honestly. A small own-rows table with one row per student per day makes them REAL. Never estimate (owner rule 2026-10-11, G4).

## 2. S6a DONE 2026-10-22 (live Supabase change, applied by Claude with the owner's yes)
Migration name: **`study_day_log_and_add_study_seconds`** on project `amieczmpqrvavzjikbdi`; SQL record: `docs/sql/2026-10-22_study_day_log.sql`.
- Table `public.study_day_log (user_id, day date, seconds, focus_seconds, updated_at)`, primary key `(user_id, day)`, `on delete cascade` from `auth.users` (so account deletion removes it too). RLS on, 3 policies (select / insert / update own rows, role `authenticated`).
- Function `public.add_study_seconds(p_day date, p_seconds int, p_focus int default 0)`: atomic upsert that ADDS to the day's row for `auth.uid()`; each call and each day clamped to 0..86400; `security invoker` (RLS still applies); execute only for `authenticated` (not `anon`).
- Checked after applying: RLS true, 3 policies, `anon` cannot execute, `authenticated` can. Existing tables untouched.
- Website: does not read or write it. No website change needed.
- Past days are NOT back-filled (impossible with lifetime totals). The chart starts empty and fills from the day the app version with S6b is installed; say so in the UI (no fake bars).

## 3. Slices left
| Slice | What | Files |
|---|---|---|
| **S6b write** | In `LearningProgressRepository.saveProgress`, after the progress write returns OK and `addSeconds > 0`, call the RPC `POST {supabaseUrl}/rest/v1/rpc/add_study_seconds` with the student's token, body `{"p_day":"yyyy-MM-dd","p_seconds":addSeconds,"p_focus":addFocusSeconds}`, `p_day` = the DEVICE's local date (`SimpleDateFormat("yyyy-MM-dd", Locale.US)` in the default time zone). Failure only logs the HTTP code (same style as the file); it must never change the return value of `saveProgress`. One call per save, inside the existing mutex. Big file: patch script, anchor = the line `status == STATUS_OK` before `} catch (e: CancellationException)` in `saveProgress` (check it is unique), add a private `logDay(userId, token, addSeconds, addFocusSeconds)` helper next to `nowIso()`. | `scripts/patch_status_s6b_day_log_write.py` (NEW) |
| **S6c read** | NEW `data/content/StudyDayLogReader.kt`: `fetchRecent(days = 14)` = own rows `select=day,seconds,focus_seconds&day=gte.<14 days ago>&order=day.asc` (same style and result type pattern as `ExamAttemptsReader`). | `StudyDayLogReader.kt` (NEW) |
| **S6d chart + goal ring** | NEW `ui/study/StatusDayPanel.kt`: (1) "Today" ring = today's minutes / `dailyGoalMinutes` (already passed to the status screen from `profiles.daily_study_goal_minutes`), HUD colours via `statusRingColor`; (2) weekday bar chart of the last 7 days from the log, same panel style (`StPanel` look) as the rest of `NativeStatusScreen`; days without a row = empty bar, never a fake value; one muted line "Daily log starts the day this version is installed" while fewer than 7 days exist. Port the LOOK of the website weekday chart in `StudentAnalyticsDashboard.tsx` (no new design). ONE inserted call in `NativeStatusScreen.kt` (patch script, anchor = the existing call of `StatusSubjectsPanel`, marker `MYSTATUS-S6`). Loading spinner, failure = message + Retry (as `StatusSubjectsPanel`). | `StatusDayPanel.kt` (NEW), `scripts/patch_status_s6d_day_panel.py` (NEW) |

Every slice: log `docs/SESSION_2026-10-22_STATUS_S6<letter>.md`, commit message ends with `not compiled` unless CI was seen, tick the table below, tell the owner exact taps (patch runs: GitHub > Actions > Apply pending patches > Run workflow > script name + new branch name > green build > test > merge Pull request). S6b and S6d patches can share one run if the agent has quota (a wrapper script like `patch_status_b3_board.py`).

## 4. Owner test (after S6b is merged and the app used for a while)
Study a few minutes in any hub: Learning > Your status > "Today" ring shows the minutes against your daily goal; the weekday chart shows a bar for today. Next day: yesterday's bar stays. Offline study: the day log may miss those minutes (the app does not queue it); the lifetime numbers on the subject board are unaffected.

## 5. Known limits (honest)
- Minutes are added when a progress save succeeds; a save that fails or is offline does not reach the day log (the existing progress save has the same limit).
- The day is the phone's local date. Changing the phone time zone can split or merge a day.
- Reading speed (WPM), method diagnostic and solve pace stay parked (not measurable).

## 6. Progress
| Slice | Status |
|---|---|
| S6a table + function (live) | **done 2026-10-22, verified** |
| S6b write | open (next) |
| S6c read | open |
| S6d chart + goal ring | open |
