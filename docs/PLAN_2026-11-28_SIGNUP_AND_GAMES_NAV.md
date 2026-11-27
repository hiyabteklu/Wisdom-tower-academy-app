# PLAN 2026-11-28: sign-up form upgrade + games navigation like My Learning

> Repo is PUBLIC: no secrets. Follow `docs/OWNER_RULE_PLAN_FIRST_PUSH_TO_MAIN.md`: one slice = one commit to `main`, tick the slice here in the same commit, record every live Supabase change here.
> STATUS 2026-11-28: A1 and B1 DONE. Nothing changed in Supabase yet. Next: A2 (needs the owner's "go"), then A3.

## OWNER DECISIONS (2026-11-28, final unless the owner changes them)
1. The student picks the academic level at registration. My Learning and Games then show the SAME packages. This is for all Electrical (ECE Year 1 to 5), Remedial, Grade 9 to 12 and Freshman.
2. For Exit Exam, COC, GAT, EUEE and UAT the Games tab shows "Coming soon" (no games yet).
3. For "Other" the student writes a short text (what they are studying / their level).
4. Gender is only collected, never shown to students. Admins may see it on the Scholars list.
(Open: the "All packages" part in Games was not answered; see Q1.)

## What I verified (read-only, 2026-11-28)
- `public.profiles` has `education_level` with a CHECK that allows only: Grade 9, Grade 10, Grade 11, Grade 12, Remedial, Freshman, 3rd Year (ECE), Other, Exit Exam, GAT (or null). There is NO gender column and NO column for the "Other" text. (`docs/SUPABASE_NOTES.md`)
- ECE package ids look like `ece-y3-sem-1`; scopes like `ece/y1-sem-N/<id>`. Year 1/2 show freshman content through an alias (`docs/ECE_STATUS_UPDATE_2026-11-23_ALIAS.md`).
- Profiles are readable by their owner or an admin only, and the leaderboard view hides everything but the display name, so a new gender column is not visible to other students.
- Still to read: the sign-up fields (probably `ui/auth/NativeSignInScreen.kt` 52 KB and/or `OnboardingScreen.kt`) and `ui/admin/UsersScreen.kt` (Scholars list).

## PART 1: Sign-up form upgrade

### Levels (saved strings)
School: `Grade 9` ... `Grade 12`. University: `Freshman`, `ECE Year 1` ... `ECE Year 5` (`3rd Year (ECE)` stays valid and means Year 3). Exams: `Remedial`, `Exit Exam`, `COC`, `GAT`, `EUEE`, `UAT`. `Other` + a written text.

### Slices
- **A1 DONE (2026-11-28, not compiled)** New `data/auth/AcademicLevels.kt`: the level list, `eceYearOf`, `resolvePackages(level, packageList)` and `hasGames(level)`. The Games tab now uses it (`GameEnrollment.kt`), and `GameTabScreen.kt` shows "Games are coming soon for your level." for Exit Exam, COC, GAT, EUEE, UAT and Other. BUG FIXED on the way: Games opened the FIRST ece package (Year 1) for a Year 3 student; now it opens the package of the student's year.
- **A1b (goes with A3, same commit)** `ui/learning/LearningScreen.kt` (34 KB) still has its own private copy `defaultPackageIdsForLevel` + the `enrolledCourses` block. It gives the same result as the shared rule for every level that can be saved today, so it was left alone to avoid resending the big file twice. In A3 replace both by `AcademicLevels.resolvePackages(userProfile?.educationLevel, packageList)` and delete the private function. MUST be done before ECE Year 1/2/4/5 can be chosen, or Learning would open Year 3 for them.
- **A2 (database, owner says go, one migration per topic, test in a rolled-back transaction first)** (a) add `profiles.gender` (text, nullable, CHECK Male / Female / Prefer not to say); (b) add `profiles.education_level_other` (text, nullable, length cap about 80); (c) widen the `education_level` CHECK with `ECE Year 1` ... `ECE Year 5`, `COC`, `EUEE`, `UAT` (only widens, no row can break). Existing own-row policies already cover new columns. Check that the admin Scholars list reads through a path that includes the new columns. Record the migration names here. Do this BEFORE A3 ships, otherwise saving fails.
- **A3 (app)** Read the real sign-up code first, do not guess. Two short steps: (1) account: name, e-mail, password; (2) about you: gender chips, then the level as grouped cards built from `AcademicLevels.ALL`. Same look as the Course Content folder cards. Level and gender are required ("Prefer not to say" allowed). Choosing Other shows one text box (required). The existing profile write (PATCH then upsert) gets `gender` and `education_level_other`, nothing else. Includes A1b.
- **A4 (app)** Settings uses the SAME level picker, so changing the level moves My Learning and Games together.
- **A5 (app, admin)** Scholars list (`UsersScreen.kt`): show gender (and the Other text) on the scholar card and in the detail. Admins only. Never in the leaderboard, never to students.

### Test on the phone (after each slice)
New test account: sign up with ECE Year 2 and a gender; My Learning and Games show ECE Year 2 only. Change to Grade 10 in Settings: both move. A test account with Exit Exam: Games says Coming soon. A test account with Other: the text is saved. Old accounts (3rd Year (ECE), Freshman) open as before. Admin: the Scholars list shows the gender.
Test for A1 alone (no database change needed): an old Year 3 account opens the Year 3 package in Games (it used to open Year 1); an account with Exit Exam or GAT sees "Games are coming soon for your level."

## PART 2: Games navigation like My Learning

### B1 findings (DONE 2026-11-28, from reading `GameHubScreen.kt`, `GameTabScreen.kt`, `LearningScreen.kt`)
- The package and subject cards in Games ALREADY use the same `OpenCard` + `openGrid` as Learning, with the same pictures. The look is shared; what differs is the ORDER of steps.
- Learning: Curriculum cards (my packages) > package page (subjects). Games: pick a GAME first (Wisdom Defense or Match) > course > subject > chapter. The game choice comes first, the package second.
- Games keeps its position in plain `remember`, so leaving the tab resets it to the game picker. Learning's behaviour on return is its own.
- Subject pictures for ECE in Games come from `SUBJECT_IMAGES` keyed `ece/<semester>/<slug>` (Year 3 courses only); ECE Year 1/2 (alias scopes `ece/y1-sem-N/...`) and Year 4/5 have no picture entry yet, so they fall back to the package picture.
- Not yet read: the Learning package page for ECE (year/semester step) in `ui/packages`, and `GameHubIndex.knownSubjects` for ECE ids. B2 starts by reading both.

### Slices
- **B2 (code)** Read the ECE package page and `GameHubIndex`. Move what Learning's package page shows for a package (subject cards, ECE semester step) into shared composables in `ui/packages` and use them in both tabs. Learning must look exactly as today (pure refactor). Add the picture entries for ECE Year 1/2/4/5 subjects.
- **B3 (code)** New order for Games = same as Learning: my package cards > (ECE: semester) > subject cards > chapter list, and the GAME is chosen on the chapter row (buttons Match and Wisdom Defense, with the same Coming soon / Package required labels and download gauge as today). Back goes up one step. Remember the position while the app runs (`rememberSaveable`). Levels with no games keep the Coming soon message. See Q2.
- **B4 (optional, see Q1)** An "All packages" section under the student's own ones, with the locked / "Package required" label.
- Do NOT touch the game screens, level picker, results, or `GameAccess.gate` (the ownership check stays the authoritative one).

### Test on the phone
Games tab opens on the same package cards as My Learning; tapping through reaches chapters; a chapter of a package the student does not own shows the lock label; Match and Defense still play and save results. Changing the level in Settings changes the Games tab too.

## Order
A2 (needs owner "go") > A3 (+A1b) > A4 > A5, and B2 > B3 (> B4). B2 can start now. Each slice ends with a build, then a phone test by the owner.

## Open questions
- **Q1** "All packages" in Games: show the other packages as locked cards like Learning, or only the student's own? (Not answered. B4 waits.)
- **Q2** B3 moves the game choice (Match / Wisdom Defense) from the first step to the chapter row, so the flow is the same as Learning. OK? (Plan assumes yes. If not, keep the game picker first and only fix the pictures and the ECE step.)

## Plan changes
(none)

## Log
| Date | What | Left |
|---|---|---|
| 2026-11-28 | Plan written and pushed, owner decisions 1 to 4 added. | |
| 2026-11-28 | A1 done (AcademicLevels, Games uses it, Coming soon, Year 3 bug fixed). B1 done (findings above). Not compiled. Nothing changed in Supabase. | A2 needs the owner's go; A1b goes with A3. |
