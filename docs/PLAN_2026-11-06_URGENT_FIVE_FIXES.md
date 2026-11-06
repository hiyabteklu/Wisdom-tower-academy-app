# PLAN 2026-11-06 — URGENT: five fixes before the next release

> Repo is PUBLIC: no secrets, no student data. API keys never go in this repo or in a database table.
> Owner rule: plan first, push every slice straight to `main`, one slice = one commit, tick this file in the same commit
> (see `docs/OWNER_RULE_PLAN_FIRST_PUSH_TO_MAIN.md`). Talk to the owner in plain words.

Status: **U1a (audit) DONE. Next: U1b (app gate).** No app code changed yet.

## Context
- Update screen (slices 1-3) is merged (PR #106 and #107) and built. Not device-tested yet (needs a newer release, see U0).
- Next release = these five fixes. Do them in the order below (most risky first).

## U0 — Version number (answer to "isn't it in gradle?")
Yes. The app's own version lives in `app/build.gradle.kts` (`versionCode`, `versionName`; at the time of writing 5 and "1.1.1").
The update check compares the `version_code` in the Supabase table `app_versions` with the installed app's `versionCode`.
So the release recipe is:
1. Bump `versionCode` (+1 or more) and `versionName` in `app/build.gradle.kts`, push to main, let CI build the signed APK.
2. Upload that APK as a Release asset in `hiyabteklu/wisdom-tower-releases`.
3. In the app's Admin > App Releases, save a DRAFT first (version code = the new number, version name, notes, direct APK URL,
   Telegram link, minimum supported code). Publish ONLY after installing the APK once and checking the link works.
   Publishing a newer version blocks every older app until it updates.
- [ ] U0 done when: gradle bumped as the LAST commit before release, after U1-U5 are merged.

## U1 — Login first for anything that talks to the database (do first)
Problem: AI tutor (and other database features) open for signed-out users.

### U1 audit (2026-11-06, read from code on main)
Main finding: **the AI tutor route needs no sign-in at all.** `data/content/AiTutorRepository.kt` posts to
`<site>/api/ai/tutor` with NO login token, and its own comment says "the route needs no sign-in". So anyone with the app
(or the URL) can use up the free AI quota. An app-side gate alone is not enough; the server must check the token too.

Tools already in the code that we reuse (do not invent new ones):
- `AuthStateManager.isLoggedIn` (StateFlow), `AuthStateManager.currentUser.value?.accessToken`,
  `AuthStateManager.refreshIfNeeded()` (renews the token before it expires).
- `ui/theme/WisdomMessageCard.kt`: shared message card with `showSignIn = true`; `onSignIn = null` uses the app's normal
  sign-in route (`SignInNavigator`). This is the gate's look; works in Night/Day/Transparent.

| # | Place (file) | What it calls | Signed-out today | Needed |
|---|---|---|---|---|
| 1 | `ui/study/NativeTutorScreen.kt` (global tutor overlay; opened from drawer/Learning tile via `NativeToolRequests.requestTutor`, from `LearningScreen`, `MainActivity`) | `AiTutorRepository.stream` | opens and works | show sign-in card instead of chat |
| 2 | `ui/study/NotesReaderScreen.kt` ("Ask AI tutor" button, `requestTutorWithContext`) | same | opens | hide/replace the button with sign-in card |
| 3 | `ui/study/QuizExamScreen.kt` (AI explain after a question) | `AiExplainRepository` -> same route | works | sign-in card in the explain panel |
| 4 | `ui/competition/CompetitionReviewScreen.kt` (AI explain in review) | `AiExplainRepository` | works (competitions need login anyway; verify) | same gate |
| 5 | Server route `/api/ai/tutor` (web repo `Wisdom-tower-academy-web`) | AI providers | accepts anyone | require `Authorization: Bearer <Supabase token>`, verify it, else 401 |
| 6 | `AiTutorRepository` and `AiExplainRepository` | request has no auth header | - | send the token; if no session return a "sign in" event, not "Failed" |

Already gated (seen in code, only re-check on device): Your Status (`NativeStatusScreen` has `isLoggedIn`), notifications
(sign-in card), hub lists and course locks (`ContentAccess.staticLocks(basePath, loggedIn)`).
Still TO CHECK in U1b/U1c (not yet read): Planner/Notebook/Targets sync, Competitions hub entry, Leaderboard, downloads,
free-resource entries (owner decides which stay open), Supabase RLS on `progress`, `ai_*`, planner, notes tables.

- [x] U1a audit written (this table)
- [ ] U1b app gate: one `SignedOutGate` composable (wraps `WisdomMessageCard(showSignIn=true)`), used by rows 1-4; repositories add the token (row 6)
- [ ] U1c server + database: route requires and verifies the token (row 5); check RLS on the tables above; record each change under "Live Supabase changes"

## U2 — Change the main AI tutor model without releasing an app update
Facts: the app calls the website route `/api/ai/tutor` (repo `hiyabteklu/Wisdom-tower-academy-web`); the AI keys already live on
the website server, not in the app. So the switch belongs in that route; the app needs no change.
Design:
1. Supabase table `ai_config` (one small row, readable only by the server, writable only by the owner/admin):
   `primary_provider`, `primary_model`, `fallback_order` (ordered list), `enabled`, `updated_at`. No keys in it.
2. The route reads this row on each request with a short cache (about 60 s), so a change goes live in about a minute.
   If the table cannot be read, it uses a safe default built into the website.
3. Automatic fallback: if the primary answers with rate-limit (429) or a server error, try the next provider in `fallback_order`.
   Free tiers run out, so this is the main protection.
4. How the owner changes it: first from the Supabase table editor on the phone/web; later (optional slice U2d) a small native
   Admin screen "AI provider" that edits that one row.
5. Keys stay in the website host's environment variables (one per provider). Adding a provider = add its key there + one row value.
   Never put keys in GitHub or in `ai_config`.
Alternative considered: a JSON file in GitHub fetched at run time. Rejected as main option: public repo, slower to change,
no per-user safety. Supabase row is instant and private.
Also check `AiExplainRepository` (uses the same route) and the cache version in it so a model change does not serve stale answers.
- [ ] U2a table + RLS (record migration name)  - [ ] U2b route reads config + fallback (web repo)  - [ ] U2c test switch  - [ ] U2d admin screen (optional)

## U3 — My Status: check the numbers, then better screen
Owner report: Total hours keeps rising (70+ h), Focus shows 0%. Day chart exists (Today & This week) but there is no clean
Today / Week / Month / Total control.
Known from code: `ui/study/StatusBreakdown.kt` computes `focusPct = focusSeconds / studySeconds` from `progress` rows
(`focus_seconds` vs `total_seconds`). Focus 0% with 75 h of study means `focus_seconds` is probably never written
(or only by the Focus timer) while `total_seconds` is written by every reader/video session.
Plan:
1. **Audit against the database FIRST** (no UI change yet): find who writes `total_seconds`, `focus_seconds`, and the day log
   (`S6` day-log tables). Query Supabase for the owner's rows: sum per day vs lifetime, any day over 24 h, duplicate/overlapping
   sessions (the weekday chart shows 715 and 573 minutes on two days, which looks too high), sessions that never stop (app in
   background, screen off, reader left open). Write findings in this file.
2. Fix the writing side: count time only while the screen is on and the app is in the foreground; stop on pause/background/idle;
   cap one session; never let two screens count at the same time. Define "focus" in plain words (for example: time with the Focus
   timer running, or reading with no leaving the app) and record it so it is measured, not guessed.
3. Decide with the owner what to do with old wrong totals (hide, recompute from day log, or reset). Do not delete without asking.
4. New screen: one control **Today | Week | Month | Total**. Clear graphs: a bar chart per day/week, a ring for goal, study vs
   focus split, subject share. Real numbers only; a value that cannot be known shows a dash (owner rule G4).
- [ ] U3a audit + findings  - [ ] U3b fix time counting  - [ ] U3c fix/define focus  - [ ] U3d range control + graphs

## U4 — Scholarship guide design (Other resources)
Problem: every line is its own "Good to know" card, so the screen is repetitive and does not group information.
Code: `ui/guides/GuidesScreen.kt` (the string "Good to know" is repeated per line there).
Plan:
1. Read the scholarship guide data and group the lines into real categories (for example: Who can apply, Documents needed,
   Deadlines and dates, How to apply step by step, Money and benefits, Tips and warnings). One heading per category, lines
   as a tidy list inside ONE card per category. Show "Good to know" only once at most, for genuine tips.
2. Check the other guides (universities, etc.) use the same layout so the fix is shared, not scholarship-only.
3. Keep Night, Day, Transparent looks and text-size settings working.
- [ ] U4a regroup content  - [ ] U4b new layout

## U5 — Golden celebration for the top 3 (leaderboard) as a JPG, 4:3 and 9:16
Facts: `ui/admin/LeaderboardJpgRenderer.kt` already draws a 9:16 (1080x1920) leaderboard JPG, used only from the Admin
Competitions screen, and the style is plain navy. The owner does not see a celebration design in the app.
Plan:
1. Rework the renderer: gold/trophy celebration look for ranks 1-3 (podium or medals, gold glow, confetti), clear names and
   points, school logo, competition name and date. Two sizes: 4:3 and 9:16.
2. Make it easy to find: admin can export both sizes from the competition; students get a "Share my result / top 3" entry on the
   leaderboard screen when they are in the top 3 (confirm with owner whether students or only admin should download).
3. No student phone number or email on the image; names only as already shown on the leaderboard.
- [ ] U5a design renderer  - [ ] U5b both sizes  - [ ] U5c entry points

## Questions for the owner (answer when convenient, agents continue with the default meanwhile)
- AI providers you want in the chain, in order (default: whatever keys already exist on the website host).
- Should students also download the top-3 image, or only admin? (default: admin export + a share button for top 3 students)
- Old wrong study hours: hide / recompute / reset? (default: recompute from the day log, never delete)
- Which free resources may stay open without sign-in? (default: only the public guides/resources the owner already marked free)

## Live Supabase changes (record every one: migration name + one line)
- none yet

## Plan changes
- 2026-11-06: U1 gains a server part (row 5) because the tutor route currently needs no login; an app-only gate would not protect the quota.
