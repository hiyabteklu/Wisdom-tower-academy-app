# PLAN 2026-11-28: sign-up form upgrade + games navigation like My Learning

> Repo is PUBLIC: no secrets. Follow `docs/OWNER_RULE_PLAN_FIRST_PUSH_TO_MAIN.md`: one slice = one commit to `main`, tick the slice here in the same commit, record every live Supabase change here.
> STATUS 2026-11-28: A1, A2, A3 (+A1b) and B1 DONE (A1/A3 written, NOT compiled, NOT tested on the phone). Supabase migrations are LIVE (see A2). Next: owner builds and tests A3 on the phone; then A4, A5. B2 can start any time.

## OWNER DECISIONS (2026-11-28, final unless the owner changes them)
1. The student picks the academic level at registration. My Learning and Games then show the SAME packages. This is for all Electrical (ECE Year 1 to 5), Remedial, Grade 9 to 12 and Freshman.
2. For Exit Exam, COC, GAT, EUEE and UAT the Games tab shows "Coming soon" (no games yet).
3. For "Other" the student writes a short text (what they are studying / their level).
4. Gender is only collected, never shown to students. Admins may see it on the Scholars list.
(Open: the "All packages" part in Games was not answered; see Q1.)

## What I verified (read-only, 2026-11-28)
- `public.profiles` had `education_level` with a CHECK that allowed only: Grade 9, Grade 10, Grade 11, Grade 12, Remedial, Freshman, 3rd Year (ECE), Other, Exit Exam, GAT (or null). (`docs/SUPABASE_NOTES.md`)
- CORRECTION found while doing A2: the column `profiles.custom_education_level` (text) ALREADY EXISTED and was unused (0 rows). It is used for the "Other" text; NO new column was added for it.
- `profiles.study_subjects` (text[]) has its own CHECK with the same level list and is used by 54 rows; it was widened too.
- ECE package ids look like `ece-y3-sem-1`; scopes like `ece/y1-sem-N/<id>`. Year 1/2 show freshman content through an alias (`docs/ECE_STATUS_UPDATE_2026-11-23_ALIAS.md`).
- Profiles RLS: `profiles_select_own_or_admin` (SELECT), `profiles_insert_own`, `profiles_update_own`. So a student sees only their own row, an admin sees all; the new gender column is covered. The leaderboard view (`leaderboard_by_scope`) hides everything but the display name.
- Notification audiences (`resolve_notification_audience`, `admin_audience_level_counts`) match `education_level` text. The new level strings only get targeted if the admin picks them; check the admin audience picker lists them (A5 follow-up).
- The database trigger `handle_new_user` only creates the profile row with id, email and full name. It does NOT copy the level from the sign-up metadata, so the app must write the level itself (A3 does, see below). The old sign-up passed the Other TEXT as `education_level` (would break the CHECK); A3 sends "Other" and keeps the text in `custom_education_level`.
- Still to read: `ui/admin/UsersScreen.kt` (Scholars list), the Settings level picker, and the other places that read `educationLevel` text (`ProfileSubjectsSync.kt`, `StudentIdGenerator`, Settings): they must understand `ECE Year N`.

## PART 1: Sign-up form upgrade

### Levels (saved strings)
School: `Grade 9` ... `Grade 12`. University: `Freshman`, `ECE Year 1` ... `ECE Year 5` (`3rd Year (ECE)` stays valid and means Year 3). Exams: `Remedial`, `Exit Exam`, `COC`, `GAT`, `EUEE`, `UAT`. `Other` + a written text (saved in `custom_education_level`, max 80 characters).

### Slices
- **A1 DONE (2026-11-28, not compiled)** New `data/auth/AcademicLevels.kt`: the level list, `eceYearOf`, `resolvePackages(level, packageList)` and `hasGames(level)`. The Games tab now uses it (`GameEnrollment.kt`), and `GameTabScreen.kt` shows "Games are coming soon for your level." for Exit Exam, COC, GAT, EUEE, UAT and Other. BUG FIXED on the way: Games opened the FIRST ece package (Year 1) for a Year 3 student; now it opens the package of the student's year.
- **A1b DONE (2026-11-28, not compiled, with A3)** `ui/learning/LearningScreen.kt`: the private `defaultPackageIdsForLevel` and the ECE special case are removed; My Learning uses `AcademicLevels.resolvePackages(userProfile?.educationLevel, packageList)`. Same result as before for every old level.
- **A2 DONE (2026-11-28, LIVE on project "Wisdom tower academy")** Each change was first run inside a transaction that was rolled back (passed, 237 profiles, no row broken). Then applied as two migrations:
  - `profiles_add_gender`: column `profiles.gender` (text, nullable) with CHECK `profiles_gender_check` (Male / Female / Prefer not to say); CHECK `profiles_custom_education_level_len` (custom_education_level at most 80 characters).
  - `profiles_widen_education_levels`: `profiles_education_level_check` and `profiles_study_subjects_valid` now also allow `ECE Year 1` to `ECE Year 5`, `COC`, `EUEE`, `UAT` (`education_level` also keeps `Other`). Only widened, no existing value became invalid.
  Checked after: the gender column exists and the existing own-row/admin policies cover it. The Scholars list reads `profiles` directly (admin policy), so A5 only needs to show the field.
- **A3 DONE (2026-11-28, NOT compiled, NOT tested)** Three commits:
  1. New `ui/auth/SignUpAboutStep.kt` (step 2 UI: gender chips Male / Female / Prefer not to say, level as grouped cards School / University / Exams / Other from `AcademicLevels.ALL`, one text box for Other, max 80) and new `data/auth/SignUpProfileWriter.kt` (right after the account is created, PATCHes the student's own profiles row with `education_level`, `gender`, and for Other `custom_education_level`, using the student's own token; POST upsert if no row; best effort, never blocks sign-up).
  2. `ui/auth/NativeSignInScreen.kt`: Create Account is two steps. Step 1 "Your account": full name, e-mail or phone, password, confirm, terms, button Continue. Step 2 "About you": gender + level cards, button Create Account, "Back" link. Gender and level are required; Other needs the text. The old dropdown and the old default level (Freshman) are gone (no default now). Sign In unchanged. `SupabaseAuthClient.signUp` was NOT changed (it still sends the level in the sign-up metadata and to the website register route; for Other it now sends the word "Other").
  3. A1b above.
  Risks to check on the phone: (a) the website register route `/api/auth/register` might reject or ignore `ECE Year N` (its result is ignored; the app write comes after it and wins); (b) other screens that read the level text (Settings, `ProfileSubjectsSync.kt`, student ID) may not know `ECE Year N` yet.
- **A4 (app)** Settings uses the SAME level picker (AcademicLevels + SignUpAboutStep style cards), so changing the level moves My Learning and Games together. Read the Settings code and the other `educationLevel` readers first (see risk b).
- **A5 (app, admin)** Scholars list (`UsersScreen.kt`): show gender (and the Other text) on the scholar card and in the detail. Admins only. Never in the leaderboard, never to students. Also check the notification audience picker offers the new levels.

### Test on the phone (after each slice)
New test account: sign up with ECE Year 2 and a gender; My Learning and Games show ECE Year 2 only. Change to Grade 10 in Settings: both move. A test account with Exit Exam: Games says Coming soon. A test account with Other: the text is saved. Old accounts (3rd Year (ECE), Freshman) open as before. Admin: the Scholars list shows the gender.
Test for A1 alone (no database change needed): an old Year 3 account opens the Year 3 package in Games (it used to open Year 1); an account with Exit Exam or GAT sees "Games are coming soon for your level."
Test for A3: Create Account shows "Step 1 of 2" with name, e-mail/phone, passwords, terms and a Continue button; wrong or empty fields show the notice and stay on step 1; Continue opens step 2 (gender chips, level cards); Back returns to step 1 with the typed values kept; Create Account without gender or level shows a notice; choosing Other shows the text box and needs text. After sign-up check in Supabase that the new row has the level, gender and (for Other) the text. Sign In still works.

## PART 2: Games navigation like My Learning

### B1 findings (DONE 2026-11-28, from reading `GameHubScreen.kt`, `GameTabScreen.kt`, `LearningScreen.kt`)
- The package and subject cards in Games ALREADY use the same `OpenCard` + `openGrid` as Learning, with the same pictures. The look is shared; what differs is the ORDER of steps.
- Learning: Curriculum cards (my packages) > package page (subjects). Games: pick a GAME first (Wisdom Defense or Match) > course > subject > chapter. The game choice comes first, the package second.
- Games keeps its position in plain `remember`, so leaving the tab resets it to the game picker. Learning's behaviour on return is its own.
- Subject pictures for ECE in Games come from `SUBJECT_IMAGES` keyed `ece/<semester>/<slug>` (Year 3 courses only); ECE Year 1/2 (alias scopes `ece/y1-sem-N/...`) and Year 4/5 have no picture entry yet, so they fall back to the package picture.
- B2 reading so far: the Learning package page is `ui/packages/PackageDetailScreen.kt` (18 KB). The ECE step is built there from private helpers (`eceSemesterEntriesFor`, `EceCourseCard`, `EceSemesterEntry`) plus `EceTreeData.kt`, `EceCourseCovers.kt` and `openGrid`/`OpenCard`; ECE state is `eceSemesterId` / `eceStreamId` in `rememberSaveable`. Still to read: `GameHubIndex.knownSubjects` for ECE ids.

### Slices
- **B2 (code)** Read `GameHubIndex`. Move what Learning's package page shows for a package (subject cards, ECE semester step) into shared composables in `ui/packages` and use them in both tabs. Learning must look exactly as today (pure refactor). Add the picture entries for ECE Year 1/2/4/5 subjects.
- **B3 (code)** New order for Games = same as Learning: my package cards > (ECE: semester) > subject cards > chapter list, and the GAME is chosen on the chapter row (buttons Match and Wisdom Defense, with the same Coming soon / Package required labels and download gauge as today). Back goes up one step. Remember the position while the app runs (`rememberSaveable`). Levels with no games keep the Coming soon message. See Q2.
- **B4 (optional, see Q1)** An "All packages" section under the student's own ones, with the locked / "Package required" label.
- Do NOT touch the game screens, level picker, results, or `GameAccess.gate` (the ownership check stays the authoritative one).

### Test on the phone
Games tab opens on the same package cards as My Learning; tapping through reaches chapters; a chapter of a package the student does not own shows the lock label; Match and Defense still play and save results. Changing the level in Settings changes the Games tab too.

## Order
Owner builds and tests A3 > A4 > A5, and B2 > B3 (> B4). B2 can start now. Each slice ends with a build, then a phone test by the owner.

## Open questions
- **Q1** "All packages" in Games: show the other packages as locked cards like Learning, or only the student's own? (Not answered. B4 waits.)
- **Q2** B3 moves the game choice (Match / Wisdom Defense) from the first step to the chapter row, so the flow is the same as Learning. OK? (Plan assumes yes. If not, keep the game picker first and only fix the pictures and the ECE step.)

## Plan changes
- 2026-11-28: no new "Other" text column; the existing unused `custom_education_level` is used instead.
- 2026-11-28: the level is written by the app (`SignUpProfileWriter`) after sign-up, because the database trigger does not copy it from the sign-up metadata. `SupabaseAuthClient` was left untouched.

## Log
| Date | What | Left |
|---|---|---|
| 2026-11-28 | Plan written and pushed, owner decisions 1 to 4 added. | |
| 2026-11-28 | A1 done (AcademicLevels, Games uses it, Coming soon, Year 3 bug fixed). B1 done (findings above). Not compiled. Nothing changed in Supabase. | A2 needs the owner's go; A1b goes with A3. |
| 2026-11-28 | Owner said go. A2 done: migrations `profiles_add_gender` and `profiles_widen_education_levels` live (tested in a rolled-back transaction first). | A3 (+A1b) next. |
| 2026-11-28 | A3 + A1b written and pushed (3 commits: SignUpAboutStep + SignUpProfileWriter, NativeSignInScreen two steps, LearningScreen shared rule). Not compiled, not tested. | Owner: build and phone test; then A4 (Settings picker + other `educationLevel` readers), A5. |
