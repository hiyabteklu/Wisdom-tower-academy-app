# HANDOFF.md — READ THIS FIRST

> Shared memory for every AI agent (Claude, AI Studio, Codex, anyone) on this project.
> The owner is NOT a coder and is on a free plan with a small quota. Agents get cut off without warning. **LIVE WORK: `docs/COMPETITION_STATUS.md`** (stage board C0 to C10, owner decisions A to G, session log). This file holds the stable plan, rules and specs.
> **SECOND LIVE TASK (owner order 2026-10-15): ADMIN CONSOLE MOBILE REDESIGN.** The in-app Admin Console (the website `/admin` page in the app overlay) must be rebuilt for the phone: dashboard grid, NO horizontal scrolling, smooth scrolling, the app's top bar and bottom navigation visible, same look as the app. Plan, hypotheses, slices A0 to A17 and the phone checklist: **`docs/ADMIN_MOBILE_PLAN.md`** (read it right after the rules below; the website half is in repo `hiyabteklu/Wisdom-tower-academy`, `docs/ADMIN_MOBILE_PLAN.md` and `AGENTS.md`). It runs in PARALLEL with the competition stages; the owner's order overrides every "competition only" sentence below. One slice per chat, start with A0.
> **THIRD LIVE TASK (owner order 2026-10-22): GAME COURSE NAVIGATION.** Choosing a course for a game (Tower Climb, Tower Defense) must look and work exactly like choosing a course in the normal learning section (same category pages, same `SubjectCard` thumbnails); only the items inside the course differ (game items instead of the hub cards). Plan, constraints and slices G0 to G5: **`docs/GAME_COURSE_NAV_PLAN.md`** (read it right after `docs/ADMIN_MOBILE_PLAN.md`; the work happens in the WEBSITE repo `hiyabteklu/Wisdom-tower-academy`, whose `docs/GAME_COURSE_NAV_PLAN.md` is the master copy). One slice per chat, start with G0. The owner's order overrides every "competition or admin only" sentence below.
> **PHASE UPDATE 2026-10-14: THE COMPETITION SPACE IS THE ONLY LIVE WORK.** The owner closed the UI polishing phase (T1 to T9, glass, My Subjects, notifications in one place, Other resources) and ordered it archived (`docs/archive/README.md`). The only task now is building competitions: a trophy icon replaces the top-bar Refresh button, global timed exams with a countdown, a native exam runner, results and a leaderboard. Read in this order: `docs/COMPETITION_STATUS.md`, `docs/COMPETITION_DECISIONS.md` (the owner's answers; it OVERRIDES the plan), `docs/COMPETITION_PLAN.md` (data model, stages, edge cases). Owner order: execute the stages one at a time, in order, without waiting to be named each time. Roles: Claude (chat) plans, writes notes, reviews and keeps status; AI Studio implements; Claude writes big code only when something goes wrong. See section 5.
> **This repository is PUBLIC. Never write passwords, API secrets, keystores, student emails/names, or the owner's personal data anywhere in the repo.**

---

## 0. MANDATORY AGENT PROTOCOL (follow exactly)

This section is the single source of agent rules. `CLAUDE.md` and `AGENTS.md` only point here.

**At the start of every session**
1. Read this file, then `docs/COMPETITION_STATUS.md`, `docs/COMPETITION_DECISIONS.md` and `docs/COMPETITION_PLAN.md`, then `docs/ADMIN_MOBILE_PLAN.md` (second live task), then `docs/GAME_COURSE_NAV_PLAN.md` (third live task), then `docs/UI_POLISH_TASKS.md` ONLY for its global constraints G1 to G9 (still in force), then `docs/BUILD_PITFALLS.md` BEFORE writing any code, `docs/EDITING_LARGE_FILES.md` before touching a big file, and the newest `docs/SESSION_*.md` files (newest first). Read when the task needs them: `docs/SUPABASE_NOTES.md`, `docs/CONTENT_SHAPES.md`, `docs/NATIVE_CONTENT_PLAN.md`, `NATIVE_APP_BRIEF.md`, `ARCHITECTURE.md`, `WEBSITE_CHANGES.md`, `docs/PENDING_PATCHES.md`. Section 2 says which documents are current and which are historical or archived.
2. Do NOT ask the owner to re-explain anything written here or in the competition docs. Ask only what is genuinely new.
3. Work only on the competition stage marked next in `docs/COMPETITION_STATUS.md` (C1 to C10), one at a time, in order, OR on the next open slice of the Admin Console redesign in `docs/ADMIN_MOBILE_PLAN.md` (owner order 2026-10-15), OR on the next open slice of the game course navigation task in `docs/GAME_COURSE_NAV_PLAN.md` (owner order 2026-10-22; all three tasks are live, the owner says which to do in the chat, otherwise do the next competition stage). The owner's order of 2026-10-14 replaces naming each stage: finish one (state the evidence), log it, then start the next. Do not start any task that is neither a competition stage nor an admin-redesign slice nor a game-navigation slice. Archived tasks (T1 to T9 and older) are NOT to be touched.

**While working**
4. If the stage is big, first add a line `IN PROGRESS: C<n>` to `docs/COMPETITION_STATUS.md` (or a new `docs/SESSION_*.md` file), so a cut-off is visible to the next agent.
5. One small commit per stage (or sub-step); commit message says what and why. Update the stage board in the same commit as the code.
6. When the owner states a decision, preference or complaint about the competition, copy it into `docs/COMPETITION_DECISIONS.md` section 4 in their words with the date. Never drop earlier ones.
7. You usually cannot build or run the app. Mark code `[~]` (written, not compiled/tested) until CI is green AND the owner has tested it. Never claim more: say what evidence you have (code read, CI result you actually saw, owner's device report with its date, SQL test output you actually ran) and label guesses "hypothesis".

**Big files (rule 10, owner rule 2026-10-11)**
10. NEVER tell the owner "this file is too big for my tools". The chat tools can only replace whole files, so for a huge file (`MainActivity.kt` about 245 KB) you write a small **patch script** in `scripts/` (exact anchors, safe to run twice, tested on a copy), log it in `docs/PENDING_PATCHES.md`, and tell the owner to run GitHub > Actions > Apply pending patches. Full steps, script template and the list of existing scripts: **`docs/EDITING_LARGE_FILES.md`**. Small and medium files are simply resent whole.

**Rules for code changes (owner, 2026-10-12; they apply to the competition work)**
11. (a) Inspect the relevant code BEFORE changing it. (b) Make focused changes only. (c) Avoid regressions and preserve working behavior: the app is installed and its behavior is approved. (d) Verify changes where possible (CI, the phone checklist in section 9 for the screens touched) and say plainly what could not be verified. (e) No redesign and no architectural rewrite without a written justification the owner accepts. (f) The approved existing design stays in place except for the improvements the owner explicitly requested. (g) Root cause before fix: for bugs, find and log the cause first, then propose the fix. (h) Never invent text, numbers, sizes, prizes or progress values (owner rule 2026-10-11).
12. **FORBIDDEN FILES (owner 2026-10-12).** Never edit, reformat, re-order, add to, delete or "fix": `.env.example` (it must stay exactly the 6 public non-empty `NEXT_PUBLIC_*` keys; it broke the build in run #7 and run #285), anything under `.github/`, `gradle/`, `gradlew*`, `gradle.properties`, `build.gradle.kts`, `settings.gradle.kts`, `app/build.gradle.kts`, ProGuard/R8 and signing settings, `app/src/main/AndroidManifest.xml` (unless the owner's task says so), the legacy web shell files (`server.js`, `package.json`, `vite.config.ts`, `tsconfig.json`, `index.html`, `metadata.json`), `website-reference/**`, existing images/fonts/branding (`logo.png`, `animation.gif`, `branding/`), and the other repositories. If a stage seems to need one of them: stop, write it under UNSURE in the session log, ask the owner. Full list with reasons: `docs/UI_POLISH_TASKS.md` G1. (Exception, owner order 2026-10-15: the admin redesign slices A1, A2 and A5 to A16 edit the WEBSITE repo `hiyabteklu/Wisdom-tower-academy` as `docs/ADMIN_MOBILE_PLAN.md` says. Same exception, owner order 2026-10-22: the game navigation slices G1 to G4 edit the WEBSITE repo as `docs/GAME_COURSE_NAV_PLAN.md` says; `website-reference/**` in this repo is still never edited.)
13. **Do not delete or rename any existing file or asset, add dependencies, or add network hosts** unless the owner's order says so.

**Before you stop, or when quota is nearly gone**
8. Write the final session-log entry in `docs/COMPETITION_STATUS.md` and a `docs/SESSION_*.md` file: what you did (commit ids), what is left, what the owner should test, what you were unsure about. **Log first, everything else after.**
9. Session logs are append-only. Fix mistakes in append-only text by adding a correction line.

**Owner communication rules**
- Plain, non-technical language. Short. Lead with the answer.
- Verified fact vs guess: label guesses "hypothesis".
- Quota is small: use targeted code search instead of reading huge files (`MainActivity.kt` is very large); do not re-read what these docs already say.
- Tools: the owner granted the agent the Supabase connector with read AND write (2026-10-11) and ordered the competition stages executed (2026-10-14). Use it for what the agent can do; ask the owner only for things it cannot do (dashboard settings, Vercel env, Appwrite console, workflow files). Respect `docs/SUPABASE_NOTES.md` rules for any change.

---

## 1. Goal

A premium, native-feeling **Android app** (Kotlin + Jetpack Compose) for Wisdom Tower Academy using the **same accounts, database, storage and content as the live website**, so the owner adds/edits content once and both website and app update. UI, thumbnails and permanent screen designs are packed in the app for speed. Reason: the first WebView-only app had many malfunctions, depended on connection, and risked Google Play rejection.

**Money model (owner, 2026-10-11):** free for the next few weeks. Later the app has NO purchase buttons or prices; purchases happen on the website and unlock the app immediately.

## 2. Document map (which file to trust)

**Current (live)**
- `HANDOFF.md` (this file): rules, goal, stack, specs.
- `docs/COMPETITION_STATUS.md` (stage board and session log), `docs/COMPETITION_DECISIONS.md` (owner answers, overrides the plan), `docs/COMPETITION_PLAN.md` (data model, stages, edge cases).
- `docs/ADMIN_MOBILE_PLAN.md` (second live task, owner order 2026-10-15: Admin Console redesign for the phone; owner words, verified facts, hypotheses, target design, slices A0 to A17, phone checklist, status log). Website copy: repo `hiyabteklu/Wisdom-tower-academy`, `docs/ADMIN_MOBILE_PLAN.md`.
- `docs/GAME_COURSE_NAV_PLAN.md` (third live task, owner order 2026-10-22: course navigation inside the games matches the normal learning section; owner words, verified facts, target design, slices G0 to G5, constraints, phone checklist, status log). Master copy: repo `hiyabteklu/Wisdom-tower-academy`, `docs/GAME_COURSE_NAV_PLAN.md`.
- Supporting: `docs/SUPABASE_NOTES.md`, `docs/BUILD_PITFALLS.md`, `docs/EDITING_LARGE_FILES.md`, `docs/APPLY_PATCHES_WORKFLOW.md`, `docs/CONTENT_SHAPES.md`, `docs/NATIVE_CONTENT_PLAN.md`, `NATIVE_APP_BRIEF.md`, `ARCHITECTURE.md`, `WEBSITE_CHANGES.md`, `PLAY_STORE_CHECKLIST.md`, `docs/PLAY_STORE_LISTING.md`.
- `docs/UI_POLISH_TASKS.md`: ARCHIVED task list (T1 to T6), but its global constraints G1 to G9 stay in force.
- `docs/PENDING_PATCHES.md`: patch method and old patch notes. Its status table is OUT OF DATE. Newer patch scripts (T8 card, P20, T9 motion, floating nav, and the competition ones) are listed in the `docs/SESSION_*.md` files; check `scripts/` and grep `main` for a script's marker to know if it was applied.

**Archived (closed 2026-10-14, see `docs/archive/README.md`; nothing was deleted or renamed)**
- `docs/UI_POLISH_TASKS.md` (T1 to T6), T7, T8 (`docs/T8_MY_SUBJECTS_PLAN.md`), T9, P20, `docs/GLASS_PLAN.md`, `STATUS.md` sections B1 and B3, and the session logs before `docs/SESSION_2026-10-14_F.md`.

**Historical (kept for context; may contradict the current state)**
- `docs/PROGRESS.md` (2026-10-10), `docs/HANDOVER.md`, `docs/FIDELITY_AUDIT.md` (written by a previous agent, may overstate fidelity: re-check, do not trust), `docs/PARITY_AUDIT.md`, `docs/OPEN_QUESTIONS.md`, `docs/WEBSITE_ANALYSIS.md`, `RELEASE_NOTES.txt`.

## 3. Repositories & stack (verified)

| Repo | Role |
|---|---|
| `hiyabteklu/Wisdom-tower-academy` | Website: Next.js 15 + React 19 + Tailwind on Vercel. Source of truth. |
| `hiyabteklu/Wisdom-tower-academy-app-packed` | THIS Android app (Kotlin, Compose). Branch `main`. Has `website-reference/` (read-only snapshot of the website, refreshed by Action "Sync website reference"). |
| `hiyabteklu/Wisdom-tower-academy-app` | Old WebView-only app. Legacy, ignore. |

- Backend: **Supabase** (login + database; free plan, project "Wisdom tower academy", eu-central-1), **Appwrite** (file storage), **Firebase** (push), Gemini (AI tutor), Groq (Explain with AI). Secret keys live only in Vercel env vars.
- Public keys allowed in the app (`.env.example`): the 6 `NEXT_PUBLIC_*` values. The `secrets-gradle-plugin` turns EVERY key in `.env.example` into a `BuildConfig` field with the same name (e.g. `BuildConfig.NEXT_PUBLIC_SUPABASE_URL`). Never add a secret or an empty value there.
- One website host everywhere: `https://www.wisdom-tower-academy.live` (localStorage is per-origin).
- App package `com.wisdomtower.academy`. Key files: `MainActivity.kt` (WebView shell and main screen, huge), `data/auth/AuthStateManager.kt`, `data/auth/SupabaseAuthClient.kt`, `data/repository/AcademyRepository.kt`, `ui/*` screens, `StructuralNav.kt`.
- Website auth: `src/lib/supabase.ts` (storage key `wt-academy-auth-v1`), `src/lib/auth-service.ts`, `src/app/login/page.tsx`, `src/lib/authIdentity.ts` (phone users are stored as `p<digits>@phone.wisdomtower.app`; Ethiopian numbers normalised to 251…).

## 4. HISTORICAL FINDINGS (2026-10-11, code + owner screenshots; kept for context)

Most of these were resolved by native sign-in (S2), the S3 analysis and the S4 audit; they were not re-verified since.

1. **The app had no sign-in of its own.** `AuthStateManager` only copied `wt-academy-auth-v1` from the WebView's localStorage through a JS bridge (`syncAuthSession`, defined in `MainActivity.kt` for both WebViews). The "Scholar Sign In" pop-up was the website's `/login` page shown inside the app (`openToolOverlay`).
2. **Sign-in failed in the app.** Owner: wrong password showed "No account found with this email or phone"; correct credentials just cleared the form. A screenshot showed "No account found" for an email that DOES exist in Supabase Auth, so Supabase's password login failed from inside the app; cause never found.
3. **Website bug (hits website users too):** `auth-service.ts` falls back to a local-only fake login (id `wta_<timestamp>`, empty token, base64 password in localStorage) when Supabase fails, and turns every Supabase failure into "No account found". Fix in `WEBSITE_CHANGES.md` section 1 (NOT applied). LESSON for competitions: never fall back to a local/fake exam or a local-only score.
4. After a successful website login the page runs `window.location.href = "/account"` (full reload): in the app this looked like the form refreshing and the session was not reliably handed to native.
5. The Learning Hub cards showed "Unlock" because the app did not see a signed-in user.
6. Native token handling gaps (before S2): no refresh, tokens in plain SharedPreferences, injection into WebView only when its storage is empty and without `expires_at`/`token_type`.
7. Routes the app called that do NOT exist on the website: `/api/content/catalog` (call removed), `/api/account/profile` (replaced by direct Supabase reads in S8). Existing routes: `api/content/pdf`, `api/content/resources`, `api/notifications` (+`register-device`, `send`), `api/ai`, `api/explain`, `api/fcm-token`, `api/storage`, `api/auth`.
8. `/api/notifications` is called with `?userId=&email=` and no token (insecure): fix in `WEBSITE_CHANGES.md` section 3.
9. `AcademyRepository.CATALOG_PACKAGES` was a hardcoded seed and `getEnrolledPackages()` returned the same 6 packages for every logged-in user (placeholder); the function was removed in S4 (`bc713050`). The website also keeps enrollments locally, so no server source exists yet.
10. Supabase (owner screenshots): free plan, 17 monthly active users, 34/500 MB. The Email provider panel showed NO "Confirm email" switch, so that setting is unconfirmed; phone-style accounts exist (cannot receive email), so confirmation is probably effectively off.
11. `WebCacheVault` treats `supabase.co` URLs as dynamic (not cached): probably not what broke login. Unproven.

## 5. CURRENT STATE AND ACTIVE PHASE (2026-10-14)

- Owner report: everything planned before the competition (native sign-in, content and hubs, profile, notifications, routing patches, fake-content audit, T7 to T9) is done and the app is installed on the owner's device; the current design is approved. Agents have NOT independently verified this (no build or device access).
- Active phase: **COMPETITION SPACE** (stages C0 to C10 in `docs/COMPETITION_STATUS.md`). C0 (decisions A to G accepted as defaults) is done. Stages run one at a time, in order. Key owner decisions: one global window for everybody; one ranked attempt per student plus unlimited practice after the window ends; server-side grading with hidden answer keys; leaderboard with top 3 cards and ranks 4 to 10 expandable; prizes shown only after the window ends; all times in Ethiopia time (EAT); Refresh moves from the top bar into the drawer between Tools and About and Support; the admin dashboard lives in the WEBSITE repo (this repo defines the database contract); security hooks now, full integrity later (C10).
- Archived by the owner on 2026-10-14: UI polishing T1 to T9 and older work. Parked decisions (not being worked on) are listed in `docs/archive/README.md`.
- Second live task since 2026-10-15: Admin Console mobile redesign (`docs/ADMIN_MOBILE_PLAN.md`). It does not change the competition stage order; the owner says in the chat which of the two to work on.
- Third live task since 2026-10-22: game course navigation (`docs/GAME_COURSE_NAV_PLAN.md`), done in the website repo. It does not change the competition stage order either; the owner says in the chat which task to work on.
- Do not mix phases: nothing outside the competition stages, the admin-redesign slices and the game-navigation slices gets started.

## 6. REFERENCE: native sign-in design spec (S2, DONE; copy the same look for any new auth-related screen)

- **Design: copy the website login page** (`website-reference/src/app/login/page.tsx`; owner likes it). Dark navy page; logo + title "Wisdom Tower Academy"; card with cyan top bar, gradient `#131f38 → #0e172a → #0a101d`, white/20 borders, very rounded; two tabs "Sign In" / "Create Account"; fields EMAIL OR PHONE NUMBER (mail/phone icon), PASSWORD (lock icon, show/hide eye, "Forgot password?" link to `/forgot-password`, shown on Sign In only); small notice box with optional action link; big cyan (`#22e0ff`-like cyan-400) button "Sign In →" / "Create Account →" with "Signing In..." / "Creating Account..." while loading; footer "Don't have an account? Create a free account" (and "Already have an account? Sign in"). Create Account adds FULL LEGAL NAME (placeholder "e.g. Abebe Bikila"), ACADEMIC LEVEL dropdown (default Freshman, options from `ACADEMIC_LEVEL_OPTIONS` in `src/lib/academic-levels.ts`, "Other" shows a free-text field), CONFIRM PASSWORD, and a Terms/Privacy checkbox (checked by default on the website). Use the exact texts of that page. Reuse `ui/theme/WisdomComponents.kt`, `Color.kt`, `Type.kt` where they already match. Only use Material icons that exist (see BUILD_PITFALLS).
- **Network**: `SupabaseAuthClient.kt` (password grant, sign-up mirroring the website, refresh). Its `SupabaseAuthResult.Success.sessionJson` is used directly as the WebView localStorage value for `wt-academy-auth-v1` (shape: `access_token, token_type, expires_in, expires_at, refresh_token, user{...}`) and fed to `AuthStateManager.syncAuthSession(...)`.
- **Tokens** are stored encrypted (Android Keystore), refreshed before `expires_at` and on 401; the NEW refresh token is always saved (Supabase rotates them); on refresh failure the user is signed out cleanly. Never invent a local account.

## 7. FAKE-CONTENT RULE (S4 audit, DONE per owner; the rule stays in force)

Owner complaint 2026-10-11: the app showed invented captions/descriptions. Rule: compare with the website source, keep what the website has, remove or replace the rest, ask the owner if unavailable. Any new UI text added for competitions must follow the same rule: every string comes from the contest row (or from the owner's words), never invented. Prizes especially: never hard-coded, never shown before the window ends.

## 8. OPEN QUESTIONS FOR THE OWNER

None block the competition work (A to G are answered). Parked, unrelated decisions are listed in `docs/archive/README.md`. Ask only what is genuinely new and needed for the stage in hand.

## 9. PHONE TEST CHECKLIST (regression checklist; the competition test matrix is section 8 of `docs/COMPETITION_PLAN.md`)

After each UI change, re-run the parts that touch the changed screens: sign in (email), sign in (phone), wrong password message, sign out, stay signed in after closing the app, cards show "Open", each of the 6 cards opens its hub signed in, PDF opens and downloads offline, notifications list, Account shows the real name, airplane-mode behaviour, tab switching has no blinking, ECE package shows the two semester cover cards and then the 7 courses of the chosen semester. For the competition work also check: the top bar still shows the notification bell and drawer button; the drawer opens and every row works, including the moved Refresh row; back navigation still works from every reader and exam; exam timer, submit and close remain reachable. For the admin redesign use the checklist in section 7 of `docs/ADMIN_MOBILE_PLAN.md`. For the game navigation task use section 6 of `docs/GAME_COURSE_NAV_PLAN.md`.

## 10. NATIVE CONTENT (DONE per owner, 2026-10-12)

Plan, decisions, verified data facts and steps N0 to N11 with their status: **`docs/NATIVE_CONTENT_PLAN.md`**. Short version: `learning_resources` is read directly from Supabase with the public key; native lists, flashcards, PDF viewer, quiz/exam runner; short notes in a LOCAL packed page using the website's own math renderer; no purchase UI in the app; the agent has Supabase read/write access (see rules in `docs/SUPABASE_NOTES.md`). Parked: N7, N8, N9b, N11. The competition exam runner (C6) reuses this runner's look and components.
