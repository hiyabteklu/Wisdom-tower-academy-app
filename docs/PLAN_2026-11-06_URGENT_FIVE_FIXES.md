# PLAN 2026-11-06 — URGENT: five fixes before the next release

> Repo is PUBLIC: no secrets, no student data. API keys never go in this repo or in a database table.
> Owner rule: plan first, push every slice straight to `main`, one slice = one commit, tick this file in the same commit
> (see `docs/OWNER_RULE_PLAN_FIRST_PUSH_TO_MAIN.md`). Talk to the owner in plain words.

**SCOPE RULE (owner, 2026-11-06): do ONLY what is written for each fix. Do NOT gate, change or ask about anything else.**
Navigation, hubs, Other resources, free resources and every visible feature stay exactly as they are. Do not ask the owner
questions he did not ask for; use the defaults written below.

Status: **U1a audit done. U1b patch applied by the owner on branch `fix/u1b-tutor-signin` (build, phone test and merge of the pull request are the owner's). U2a + U2b done (web route reads `ai_config`, switched OFF). U3a audit done. U3b written: ONE owner tap left: run patch `apply_u3b_study_time.py` (see U3b). Next: U3c (focus). U2c is the owner's switch test. U1c waits for the release.**

## Context
- Update screen (slices 1-3) is merged (PR #106 and #107) and built. Not device-tested yet (needs a newer release, see U0).
- Next release = these five fixes. Do them in the order below.

## U0 — Version number
The app's own version lives in `app/build.gradle.kts` (`versionCode`, `versionName`; 5 and "1.1.1" at the time of writing).
The update check compares `version_code` in the Supabase table `app_versions` with the installed app's `versionCode`.
Release recipe:
1. Bump `versionCode` and `versionName` in `app/build.gradle.kts` as the LAST commit before release; CI builds the signed APK.
2. Upload the APK as a Release asset in `hiyabteklu/wisdom-tower-releases`.
3. In Admin > App Releases save a DRAFT first (version code, name, notes, direct APK URL, Telegram link, minimum code).
   Publish ONLY after installing the APK once and checking the link. Publishing a newer version blocks every older app.
- [ ] U0 done when gradle is bumped after U1-U5 are merged.

## U1 — AI tutor asks for sign-in first (AI TUTOR ONLY)
Owner words: "I said AI tutor only. The app's visible features and free navigation up to the hubs stay as they were."

### U1 audit (2026-11-06)
Main finding: **the AI tutor route needs no sign-in at all.** `data/content/AiTutorRepository.kt` posted to
`<site>/api/ai/tutor` with NO login token. So anyone can use up the free AI quota; an app-only gate is not enough, the
website route must check the login too.

Every way into the AI tutor ends in ONE screen, `ui/study/NativeTutorScreen.kt` (drawer, Learning tile, "Ask AI tutor" button
in a note via `NativeToolRequests.requestTutor / requestTutorWithContext`). So one gate in that screen covers all of them.

| Place | Today | Done by |
|---|---|---|
| `NativeTutorScreen` (all entries above) | opened for everyone | U1b gate: signed out -> `TutorSignInGate` |
| `AiTutorRepository.stream` | no token | U1b: sends `Authorization: Bearer <login token>`; no login = no request |
| Website route `/api/ai/tutor` (web repo `Wisdom-tower-academy-web`) | accepts anyone | U1c: require + verify the token (401 otherwise) |

NOT touched (not asked): quiz "Explain with AI", note "Summary", competition review explain, Your Status, notifications, hubs,
course locks, free/Other resources, navigation. (They use the separate `/api/ai/explain` route.)

### U1b — app side (written 2026-11-06)
- `data/content/AiTutorRepository.kt` (pushed to main): token header, refresh before use, no login = no request.
- `ui/study/TutorSignInGate.kt` (pushed to main): shared `WisdomMessageCard` with Sign in pill + Back. Sign in closes the screen
  then opens the normal sign-in route (`SignInNavigator`).
- `NativeTutorScreen.kt` is a big file, so the 2-place edit is a patch script: `scripts/apply_u1b_tutor_gate.py`
  (adds 2 imports + "if signed out show TutorSignInGate and return" before `PauseShellWhileShown()`).
  **Owner ran it (2026-11-06): Actions > Apply pending patches > `apply_u1b_tutor_gate.py`, branch `fix/u1b-tutor-signin`.**
  Checked in the branch on 2026-11-06: the gate (`AuthStateManager.isLoggedIn` -> `TutorSignInGate`) is in `NativeTutorScreen.kt`.
  Still the owner's: wait for the build, test, then merge the pull request. Until it is merged the gate is not in `main`.
- Test on the phone: signed out -> open AI Tutor from the drawer, from the Learning tile and from a note -> sign-in card each
  time (Night, Day, Transparent); tap Sign in -> sign-in page, no tutor screen left on top; signed in -> tutor answers as before.
- [x] U1a audit  - [x] U1b files written  - [x] U1b patch applied on its branch  - [ ] U1b build + phone test + merge (owner)

### U1c — server side (next, but only after the release)
Website repo `Wisdom-tower-academy-web`, route `/api/ai/tutor`: read the `Authorization` bearer token, verify it with Supabase
(get-user), reject with 401 when missing/invalid. Do this AFTER the new app is released: apps older than U1b send no token and
would stop getting tutor answers. Record any Supabase change under "Live Supabase changes".
- [ ] U1c

## U2 — Change the main AI tutor model without releasing an app update
Facts: the app calls the website route `/api/ai/tutor`; the AI keys already live on the website server. So the switch belongs
in that route; the app needs no change.
Design:
1. Supabase table `ai_config` (one small row, readable only by the server, writable only by the owner/admin):
   `primary_provider`, `primary_model`, `fallback_order` (ordered list), `enabled`, `updated_at`. No keys in it.
2. The route reads this row on each request with a short cache (about 60 s), so a change goes live in about a minute.
   If the table cannot be read, it uses a safe default built into the website.
3. Automatic fallback: on rate-limit (429) or server error, try the next provider in `fallback_order`.
4. Owner changes it in the Supabase table editor first; later (optional U2d) a small native Admin screen "AI provider".
5. Keys stay in the website host's environment variables, one per provider. Never in GitHub or in `ai_config`.
Default provider order if the owner says nothing: the providers whose keys already exist on the website host.

### U2a/U2b — done 2026-11-06
- Supabase migration `u2a_ai_config_table`: table `public.ai_config`, exactly one row (`id = true`), seeded with `enabled = false`
  (so nothing changes today). RLS on: only academy admins (`private.is_admin()`) can read/insert/update; the website server reads
  it with the service role key (already a website host variable `SUPABASE_SERVICE_ROLE_KEY`).
  `primary_provider` is one of `gateway`, `gemini`, `groq`; `fallback_order` is a JSON list like
  `[{"provider":"gemini","model":"gemini-3.1-flash-lite"},{"provider":"groq","model":"llama-3.3-70b-versatile"}]`.
- Web repo `Wisdom-tower-academy-web`: `src/lib/aiTutorConfig.ts` (new) + `src/app/api/ai/tutor/route.ts` (8 lines added, 2 removed).
  When `enabled = true` the owner's primary + fallback list is tried FIRST, and the built-in default order is always kept at the END,
  so a short or wrong list cannot leave the tutor with no model. A bad provider or model name in the table is skipped.
  The table is read with a 1.5 s limit and a 60 s cache; if the read fails the built-in order is used (the student never waits).
  The existing loop already moves to the next model on any error (429, 5xx, timeout).
- How the owner switches (U2c): Supabase > Table editor > `ai_config` > set `primary_provider`, `primary_model` (and optionally
  `fallback_order`), then set `enabled` to true. Live in about a minute. Set `enabled` back to false to return to the built-in order.
  The provider's key must already exist on the website host (gateway: `AI_GATEWAY_API_KEY_TUTOR`, gemini: `GEMINI_API_KEY`,
  groq: `GROQ_API_KEY_TUTOR`); a provider without its key is skipped.
- [x] U2a table + RLS  - [x] U2b route reads config + fallback (web repo)  - [ ] U2c test switch (needs the Vercel deploy of the web repo)  - [ ] U2d admin screen (optional)

## U3 — My Status: check the numbers, then better screen
Owner report: Total hours keeps rising (70+ h), Focus shows 0%. A day chart exists but there is no clean
Today / Week / Month / Total control.
Known from code: `ui/study/StatusBreakdown.kt` computes `focusPct = focusSeconds / studySeconds` from `progress` rows
(`focus_seconds` vs `total_seconds`).

### U3a — audit findings (2026-11-06, read-only; counts and sums only, no student rows copied)
Who writes time (whole app, searched `addSeconds` / `addFocusSeconds`): ONLY two screens, through
`LearningProgressRepository.saveProgress` -> `learning_progress.total_seconds` and, in the same call, `study_day_log` via the
function `add_study_seconds` (the function itself is correct: it caps a day at 86400 s and handles focus).
1. **Focus is never written by the app.** No screen anywhere passes `addFocusSeconds`, so every app session adds study time and
   zero focus time. In the database: `learning_progress` 812 rows / 101 students, 113.9 h total, only 3.5 h focus (92 rows; this
   can only come from the website). `study_day_log` 147 days / 54 students: 82.6 h total, **0.0 h focus on every day**. So Focus 0%
   is a missing writer, not a display bug.
2. **Time is counted too generously and without limit in the notes reader.** `NotesReaderScreen.kt`: a 30 s ticker adds
   `elapsedRealtime - lastFlush`, with no cap and no check that the screen is on or the app is in front; the same sum is added
   once more when the screen is left. If the phone sleeps or the app sits in the background with the note open, the next flush
   adds the whole gap (hours) in one save.
3. **The PDF reader counts wall-clock time too.** `PdfReaderScreen.kt` `saveReading`: seconds since the last save, capped at 600 s
   per save, but a save happens on every page change and when the reader closes, and nothing stops counting while the app is in
   the background or the screen is off. A phone left open can add 10 minutes per page turn and 10 more on closing.
4. **The big totals are a few runaway sessions, not normal studying.** 7 of 812 progress rows hold 67.2 h of the 113.9 h (59 %);
   the largest single row is 20.8 h; 2 rows are over 12 h. In the day log, 4 days over 6 h hold 40.6 h of 82.6 h (49 %); 2 days
   are 10 h or more; the longest day is 715 min (the number the owner saw). The typical day is tiny: median 3.4 min, average
   33.7 min. No day exceeds 24 h (the 86400 s cap holds).
5. Videos, flashcards, quizzes and the AI tutor write no time at all (only notes and PDF books do).
Conclusion: "Total hours keeps rising" = idle/background/sleep time counted by the two readers. Fix the writers first (U3b),
then define and write focus (U3c), then the screen (U3d).
- Default for old totals (no question to the owner): keep every row, never delete; the new Total/Week/Month view reads the day
  log with each day capped at a sane limit (default 8 h) so the 4 runaway days stop dominating.

### U3b — fix time counting (written 2026-11-06)
- `ui/study/ActiveStudyTimer.kt` (new, pushed to main): `ActiveStudyTimer` counts only between resume and pause of the screen's
  lifecycle (screen off and app in the background both pause it); `take(600)` hands out the counted seconds, at most 10 minutes per
  save, and restarts from zero. `rememberActiveStudyTimer(key)` wires it to the screen.
- `NotesReaderScreen.kt` and `PdfReaderScreen.kt` are big, so the small edits are a patch script: `scripts/apply_u3b_study_time.py`
  (notes: `flushNoteProgress` adds `timer.take(600)`; PDF: `saveReading` adds `timer.take(600)`). Tested on mock copies; all-or-nothing
  anchors; safe to run twice. Details and phone tests: `docs/SESSION_2026-11-06_U3B_STUDY_TIME.md`.
  **Owner runs: Actions > Apply pending patches > script `apply_u3b_study_time.py`, branch `fix/u3b-study-time`.**
  Wait for the build, test, then merge the pull request.
- Old totals are not changed (default above).
- [x] U3b files + patch written  - [ ] U3b patch applied + build + phone test + merge (owner)

Plan for the rest:
2. (done in U3b) count only while the screen is on and the app is in the foreground; cap one flush at 10 min; one reader at a time.
   STILL TO DO in U3c: define "focus" in plain words and record it.
3. Old wrong totals: default = recompute from the day log, never delete.
4. New screen: one control **Today | Week | Month | Total**; clear graphs (bars per day/week, goal ring, study vs focus split,
   subject share). Real numbers only; unknown = dash.
- [x] U3a audit + findings  - [x] U3b written  - [ ] U3c fix/define focus  - [ ] U3d range control + graphs

## U4 — Scholarship guide design (Other resources)
Problem: every line is its own "Good to know" card. Code: `ui/guides/GuidesScreen.kt`.
Plan: group the lines into real categories (Who can apply, Documents needed, Deadlines, How to apply step by step, Money and
benefits, Tips and warnings), one heading + one card per category with a tidy list; "Good to know" at most once, for real tips.
Same layout for the other guides if they share the code. Keep Night/Day/Transparent and text-size settings. Only the design of
this guide changes; its place in navigation does not.
- [ ] U4a regroup content  - [ ] U4b new layout

## U5 — Golden celebration for the top 3 (leaderboard) as a JPG, 4:3 and 9:16
Facts: `ui/admin/LeaderboardJpgRenderer.kt` draws a 9:16 (1080x1920) leaderboard JPG used only from Admin > Competitions, plain
navy. The owner does not see a celebration design in the app.
Plan: rework the renderer into a gold/trophy celebration for ranks 1-3 (podium or medals, gold glow, confetti, names, points, logo,
competition name and date) in 4:3 and 9:16; make export easy to find (admin export of both sizes; default also a share button
for top-3 students). No phone/email on the image.
- [ ] U5a design renderer  - [ ] U5b both sizes  - [ ] U5c entry points

## Live Supabase changes (record every one: migration name + one line)
- 2026-11-06 `u2a_ai_config_table`: new table `public.ai_config` (one row, `enabled = false`, admin-only RLS, `updated_at` trigger). No existing table touched.

## Plan changes
- 2026-11-06: U1 gains a server part (U1c) because the tutor route accepted anyone.
- 2026-11-06 (owner): scope rule added; U1 narrowed to the AI tutor only, extra audit items and questions removed.
- 2026-11-06: U2a + U2b written and pushed (table + web route). U2c is the owner's switch test.
- 2026-11-06: U3a audit done: cause found in the two readers (uncapped/background time) and a missing focus writer.
- 2026-11-06: U3b written (ActiveStudyTimer + patch script for the two readers).
