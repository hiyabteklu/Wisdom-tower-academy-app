# PLAN 2026-11-28: sign-up form upgrade + games navigation like My Learning

> Repo is PUBLIC: no secrets. Follow `docs/OWNER_RULE_PLAN_FIRST_PUSH_TO_MAIN.md`: one slice = one commit to `main`, tick the slice here in the same commit, record every live Supabase change here.
> STATUS 2026-11-28: A1, A2, A3 (+A1b), A4, A5 and B1 DONE (A1/A3/A4/A5 written, NOT compiled, NOT tested on the phone). Supabase migrations are LIVE (see A2); A4 and A5 needed no new database change. Gender at sign-up is Male or Female only (owner, see Plan changes). Next: owner builds and tests A3 + A4 + A5 on the phone. B2 can start any time.

## OWNER DECISIONS (2026-11-28, final unless the owner changes them)
1. The student picks the academic level at registration. My Learning and Games then show the SAME packages. This is for all Electrical (ECE Year 1 to 5), Remedial, Grade 9 to 12 and Freshman.
2. For Exit Exam, COC, GAT, EUEE and UAT the Games tab shows "Coming soon" (no games yet).
3. For "Other" the student writes a short text (what they are studying / their level).
4. Gender is only collected, never shown to students. Admins may see it on the Scholars list. Gender choices at registration: ONLY Male and Female (owner, later the same day; there is no "Prefer not to say").
(Open: the "All packages" part in Games was not answered; see Q1.)

## What I verified (read-only, 2026-11-28)
- `public.profiles` had `education_level` with a CHECK that allowed only: Grade 9, Grade 10, Grade 11, Grade 12, Remedial, Freshman, 3rd Year (ECE), Other, Exit Exam, GAT (or null). (`docs/SUPABASE_NOTES.md`)
- CORRECTION found while doing A2: the column `profiles.custom_education_level` (text) ALREADY EXISTED and was unused (0 rows). It is used for the "Other" text; NO new column was added for it.
- `profiles.study_subjects` (text[]) has its own CHECK with the same level list and is used by 54 rows; it was widened too.
- ECE package ids look like `ece-y3-sem-1`; scopes like `ece/y1-sem-N/<id>`. Year 1/2 show freshman content through an alias (`docs/ECE_STATUS_UPDATE_2026-11-23_ALIAS.md`).
- Profiles RLS: `profiles_select_own_or_admin` (SELECT), `profiles_insert_own`, `profiles_update_own`. So a student sees only their own row, an admin sees all; the new gender column is covered. The leaderboard view (`leaderboard_by_scope`) hides everything but the display name.
- Notification audiences (checked in A5, 2026-11-28): `resolve_notification_audience` matches `education_level` as text without regard to letter case (a level chip, or a custom group's `match_levels`). `admin_audience_level_counts` counts the levels that really exist in `profiles` (so the new levels appear by themselves once students use them). The table `notification_audiences` has NO CHECK on `match_levels`, so any level text can be saved. Only the app's fixed level list needed widening (done in A5).
- The database trigger `handle_new_user` only creates the profile row with id, email and full name. It does NOT copy the level from the sign-up metadata, so the app must write the level itself (A3 does, see below). The old sign-up passed the Other TEXT as `education_level` (would break the CHECK); A3 sends "Other" and keeps the text in `custom_education_level`.
- READERS CHECKED while doing A4 (2026-11-28, read-only):
  - Settings (`ui/settings/SettingsScreen.kt`) has NO level picker. Since 2026-10-19 it holds app settings only; the level is edited on the PROFILE screen (`ui/account/AccountScreen.kt`, card 2). So A4 changed the Profile screen, not Settings.
  - `ProfileSubjectsSync.kt` does not read the level. It writes `my_subject_keys` and package labels (`Freshman`, `3rd Year (ECE)`, `Grade N`) into `study_subjects`; all of these are still valid in the widened CHECK. Nothing to change.
  - `MySubjectsStore.packageForLevel` already maps any text containing "ece" to the ECE subjects package, so `ECE Year N` works. Levels with no package (Remedial, Exit Exam, COC, GAT, EUEE, UAT, Other) give no picks, and Status keeps counting everything (same as before for Remedial and Other).
  - `StudentIdGenerator` only prints the level text on the student ID card; `ECE Year 2` shows as written.
  - `AuthStateManager.saveProfile` updates the shared profile state at once, so My Learning and Games follow a level change on the Profile screen without a restart.

## PART 1: Sign-up form upgrade

### Levels (saved strings)
School: `Grade 9` ... `Grade 12`. University: `Freshman`, `ECE Year 1` ... `ECE Year 5` (`3rd Year (ECE)` stays valid and means Year 3). Exams: `Remedial`, `Exit Exam`, `COC`, `GAT`, `EUEE`, `UAT`. `Other` + a written text (saved in `custom_education_level`, max 80 characters).

### Slices
- **A1 DONE (2026-11-28, not compiled)** New `data/auth/AcademicLevels.kt`: the level list, `eceYearOf`, `resolvePackages(level, packageList)` and `hasGames(level)`. The Games tab now uses it (`GameEnrollment.kt`), and `GameTabScreen.kt` shows "Games are coming soon for your level." for Exit Exam, COC, GAT, EUEE, UAT and Other. BUG FIXED on the way: Games opened the FIRST ece package (Year 1) for a Year 3 student; now it opens the package of the student's year.
- **A1b DONE (2026-11-28, not compiled, with A3)** `ui/learning/LearningScreen.kt`: the private `defaultPackageIdsForLevel` and the ECE special case are removed; My Learning uses `AcademicLevels.resolvePackages(userProfile?.educationLevel, packageList)`. Same result as before for every old level.
- **A2 DONE (2026-11-28, LIVE on project "Wisdom tower academy")** Each change was first run inside a transaction that was rolled back (passed, 237 profiles, no row broken). Then applied as two migrations:
  - `profiles_add_gender`: column `profiles.gender` (text, nullable) with CHECK `profiles_gender_check` (Male / Female / Prefer not to say); CHECK `profiles_custom_education_level_len` (custom_education_level at most 80 characters). The app now offers only Male and Female; the CHECK was left as it is (it only allows more, nothing breaks, and tightening a live CHECK needs the owner's go).
  - `profiles_widen_education_levels`: `profiles_education_level_check` and `profiles_study_subjects_valid` now also allow `ECE Year 1` to `ECE Year 5`, `COC`, `EUEE`, `UAT` (`education_level` also keeps `Other`). Only widened, no existing value became invalid.
  Checked after: the gender column exists and the existing own-row/admin policies cover it. The Scholars list reads `profiles` directly (admin policy).
- **A3 DONE (2026-11-28, NOT compiled, NOT tested)** Three commits:
  1. New `ui/auth/SignUpAboutStep.kt` (step 2 UI: gender chips Male / Female only, level as grouped cards School / University / Exams / Other from `AcademicLevels.ALL`, one text box for Other, max 80) and new `data/auth/SignUpProfileWriter.kt` (right after the account is created, PATCHes the student's own profiles row with `education_level`, `gender`, and for Other `custom_education_level`, using the student's own token; POST upsert if no row; best effort, never blocks sign-up).
  2. `ui/auth/NativeSignInScreen.kt`: Create Account is two steps. Step 1 "Your account": full name, e-mail or phone, password, confirm, terms, button Continue. Step 2 "About you": gender + level cards, button Create Account, "Back" link. Gender and level are required; Other needs the text. The old dropdown and the old default level (Freshman) are gone (no default now). Sign In unchanged. `SupabaseAuthClient.signUp` was NOT changed (it still sends the level in the sign-up metadata and to the website register route; for Other it now sends the word "Other").
  3. A1b above.
  Risks to check on the phone: (a) the website register route `/api/auth/register` might reject or ignore `ECE Year N` (its result is ignored; the app write comes after it and wins); (b) other screens that read the level text: CHECKED in A4, nothing else needed changing.
- **A4 DONE (2026-11-28, NOT compiled, NOT tested)** Three commits. Where the level is changed: the PROFILE screen (see "READERS CHECKED"; Settings has no level picker).
  1. `data/auth/AcademicLevels.kt`: new `normalize(level)` (the old `3rd Year (ECE)` becomes `ECE Year 3`; list spelling for other letter cases) and `isUniversityLevel(level)` (Freshman, every ECE year, Remedial, Exit Exam, COC, GAT = pick a university from the list; Grade 9 to 12, EUEE, UAT and Other = type a school name). New `data/auth/ProfileLevelSync.kt`: reads and writes `custom_education_level` of the student's own row (same pattern as `ProfileSubjectsSync`; blank clears it; at most 80 characters).
  2. `ui/account/AccountScreen.kt` (card 2 "Academic Level & Institution"): the old 8 level pills are replaced by the SAME list as the sign-up (`AcademicLevels.ALL`), shown as pills in four groups School / University / Exams / Other (the Profile screen keeps its own pill look, no redesign). The Other text box is limited to 80 and is read from `custom_education_level` (old accounts: from `bio` "Custom Level: ..." as before). Save writes `education_level` as before, and the Other text to `custom_education_level` (only when the level is Other or an old text must be cleared); `bio` is still rewritten for Other, so the website keeps working. A level change moves My Learning and Games together. The old public vals `EDUCATION_LEVEL_OPTIONS` and `UNIVERSITY_LEVELS` still exist (now built from `AcademicLevels`) in case another file uses them.
  3. This plan file.
  Choices I made (change them if the owner disagrees): Exit Exam, COC and GAT use the university list (they are exams for university students); EUEE and UAT type a school name. Old accounts with `3rd Year (ECE)` are shown as `ECE Year 3` and saved as that on the next Save (same package, the database accepts it).
  Risks to check on the phone: (a) Save on the Profile screen now makes one extra write for the Other text; if it fails the toast shows its message; (b) switching between a university level and a school level clears the institution (same rule as before).
- **A5 DONE (2026-11-28, NOT compiled, NOT tested)** Admin side only; nothing changed in Supabase. Four commits (3 code, then the plan file):
  1. `data/admin/UsersRepository.kt`: `ScholarItem` has two new optional fields `gender` and `customEducationLevel`; the Scholars list now also reads the columns `gender` and `custom_education_level` (the list still falls back to the short column list if the server answers 400).
  2. `ui/admin/UsersScreen.kt`: the scholar PROFILE panel shows "Gender (admins only)" next to Target exam. Gender is NOT on the list row (kept compact) and nowhere a student can see. A level of Other shows as `Other: <text>` on the row and in the panel; the search box also finds the Other text. BUG FIXED on the way: the level chips Grade 12 and Grade 11 never matched anybody (the chip id `grade-12` was compared with the saved `grade 12`); now they match. A new chip `ECE` shows every ECE level (Year 1 to 5 and the old 3rd Year (ECE)).
  3. `data/admin/ComposerRepository.kt`: the fixed level list of the push audience picker (`ACADEMIC_LEVELS`) is now the same list as the sign-up (`AcademicLevels.ALL`) plus the old `3rd Year (ECE)`, so an admin can aim a push at ECE Year 2, Exit Exam, COC and so on. The live counts and the matching in the database need no change (see "What I verified").
  Not done on purpose: no gender in the leaderboard, no gender on the student side, no gender filter chip (not asked). If the owner wants a Gender filter or a gender count on the Overview, say so.
  Risks to check on the phone: (a) the Scholars list must still load (it now asks for two more columns; both exist); (b) the level chips row B now has three chips (Freshman, Remedial, ECE).
- **A3-fix DONE (2026-11-28, NOT compiled, NOT tested)** Owner order: gender at sign-up is Male or Female only. `ui/auth/SignUpAboutStep.kt`: `GENDER_OPTIONS` is now `Male`, `Female` (two chips; the "Prefer not to say" chip is gone). Nothing else changed: the level list and the Other box stay as decided (owner decision 3). No database change (the CHECK still accepts the three words).

### Test on the phone (after each slice)
New test account: sign up with ECE Year 2 and a gender; My Learning and Games show ECE Year 2 only. Change to Grade 10 on the Profile screen (card 2, then Save Profile Changes): both move. A test account with Exit Exam: Games says Coming soon. A test account with Other: the text is saved. Old accounts (3rd Year (ECE), Freshman) open as before. Admin: the Scholars list shows the gender.
Test for A1 alone (no database change needed): an old Year 3 account opens the Year 3 package in Games (it used to open Year 1); an account with Exit Exam or GAT sees "Games are coming soon for your level."
Test for A3: Create Account shows "Step 1 of 2" with name, e-mail/phone, passwords, terms and a Continue button; wrong or empty fields show the notice and stay on step 1; Continue opens step 2 (gender chips Male and Female only, level cards); Back returns to step 1 with the typed values kept; Create Account without gender or level shows a notice; choosing Other shows the text box and needs text. After sign-up check in Supabase that the new row has the level, gender and (for Other) the text. Sign In still works.
Test for A4: open Profile, card 2: the levels show in four groups (School, University, Exams, Other) and your current level is selected (an old `3rd Year (ECE)` account shows ECE Year 3). Pick ECE Year 2, pick the university, Save Profile Changes: the toast says Saved; My Learning and Games now show only ECE Year 2. Pick Grade 10: the university is cleared, a school name box appears; after Save both tabs show Grade 10. Pick Other, type a text, Save, close and reopen the app: the text is still there; in Supabase `custom_education_level` holds it. Pick Exit Exam: Games says Coming soon.
Test for A5 (admin account): open Users (Scholars). The list loads. Tap a scholar made in the A3 test: the panel shows Level, and "Gender (admins only)" with the value. A test account with Other shows `Other: <its text>` and the search box finds it. Tap the chip Grade 12 (or Grade 11): the real Grade 12 students appear (before, none did). Tap ECE: only ECE levels. In Broadcasts, when you make a custom audience, the level choices include ECE Year 1 to 5, Exit Exam, COC, GAT, EUEE and UAT.

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
Games tab opens on the same package cards as My Learning; tapping through reaches chapters; a chapter of a package the student does not own shows the lock label; Match and Defense still play and save results. Changing the level on the Profile screen changes the Games tab too.

## Order
Owner builds and tests A3 > A4 > A5, and B2 > B3 (> B4). B2 can start now. Each slice ends with a build, then a phone test by the owner.

## Open questions
- **Q1** "All packages" in Games: show the other packages as locked cards like Learning, or only the student's own? (Not answered. B4 waits.)
- **Q2** B3 moves the game choice (Match / Wisdom Defense) from the first step to the chapter row, so the flow is the same as Learning. OK? (Plan assumes yes. If not, keep the game picker first and only fix the pictures and the ECE step.)

## Plan changes
- 2026-11-28: no new "Other" text column; the existing unused `custom_education_level` is used instead.
- 2026-11-28: the level is written by the app (`SignUpProfileWriter`) after sign-up, because the database trigger does not copy it from the sign-up metadata. `SupabaseAuthClient` was left untouched.
- 2026-11-28 (A4): the plan said "Settings uses the same level picker". Settings has no level picker; the level is edited on the Profile screen. A4 was done there. The Settings accordion was not touched.
- 2026-11-28 (A4): the Other text on the Profile screen is read and written through the new `ProfileLevelSync` (own small file) instead of changing `SupabaseAccountClient` / `UserProfile`, so the main profile client stays untouched (same idea as `ProfileSubjectsSync`).
- 2026-11-28 (A5): the plan only said "show gender and the Other text". Two small extras were added because they were in the same files and broken or missing: the Grade 11 / Grade 12 level chips now work, and there is an ECE chip.
- 2026-11-28 (A3-fix, owner order): gender at sign-up is only Male or Female. The "Prefer not to say" choice was removed from the app. The database CHECK was not tightened.

## Log
| Date | What | Left |
|---|---|---|
| 2026-11-28 | Plan written and pushed, owner decisions 1 to 4 added. | |
| 2026-11-28 | A1 done (AcademicLevels, Games uses it, Coming soon, Year 3 bug fixed). B1 done (findings above). Not compiled. Nothing changed in Supabase. | A2 needs the owner's go; A1b goes with A3. |
| 2026-11-28 | Owner said go. A2 done: migrations `profiles_add_gender` and `profiles_widen_education_levels` live (tested in a rolled-back transaction first). | A3 (+A1b) next. |
| 2026-11-28 | A3 + A1b written and pushed (3 commits: SignUpAboutStep + SignUpProfileWriter, NativeSignInScreen two steps, LearningScreen shared rule). Not compiled, not tested. | Owner: build and phone test; then A4, A5. |
| 2026-11-28 | A4 written and pushed (3 commits: AcademicLevels + ProfileLevelSync, Profile screen level list and Other text, plan). Level readers checked, nothing else needed changing. Nothing changed in Supabase. Not compiled, not tested. | Owner: build and phone test A3 + A4; then A5. |
| 2026-11-28 | A5 written and pushed (4 commits: UsersRepository, UsersScreen, ComposerRepository, plan). Audience matching checked in Supabase read-only (no change needed). Not compiled, not tested. | Owner: build and phone test A3 + A4 + A5. B2 can start any time. |
| 2026-11-28 | A3-fix: sign-up gender is Male or Female only (SignUpAboutStep). Not compiled, not tested. | Owner: build and phone test. B2 can start any time. |
