# PLAN 2026-11-28: sign-up form upgrade + games navigation like My Learning

> Repo is PUBLIC: no secrets. PLAN ONLY, no code written yet (owner order). Follow `docs/OWNER_RULE_PLAN_FIRST_PUSH_TO_MAIN.md`: one slice = one commit to `main`, tick the slice here in the same commit, record every live Supabase change here.

## OWNER DECISIONS (2026-11-28, final unless the owner changes them)
1. The student picks the academic level at registration. My Learning and Games then show the SAME packages. This is for all Electrical (ECE Year 1 to 5), Remedial, Grade 9 to 12 and Freshman.
2. For Exit Exam, COC, GAT, EUEE and UAT the Games tab shows "Coming soon" (no games yet).
3. For "Other" the student writes a short text (what they are studying / their level).
4. Gender is only collected, never shown to students. Admins may see it on the Scholars list.
(Open: the "All packages" part in Games was not answered; see Q1. Build the My Learning mirror first.)

## What I verified first (read-only, 2026-11-28)
- `public.profiles` has `education_level` with a CHECK that allows only: Grade 9, Grade 10, Grade 11, Grade 12, Remedial, Freshman, 3rd Year (ECE), Other, Exit Exam, GAT (or null). There is NO gender column and NO column for the "Other" text. (`docs/SUPABASE_NOTES.md`)
- Package ids and scope paths for ECE years are already planned: scopes like `ece/y1-sem-N/<id>` (`docs/ECE_STATUS_UPDATE_2026-11-23_ALIAS.md`, `docs/ADMIN_ECE_Y1_Y5_PLAN.md`). Year 1/2 show freshman content through an alias.
- Games ALREADY follow the level in a basic way: `ui/games/GameEnrollment.kt` (`enrolledGamePackages`, `gameSubjectImage`) and `GameTabScreen.kt` show only the student's packages and reuse the package/subject pictures. So job 2 is an upgrade of the navigation and a shared design, not a new feature.
- `gamePackageIdsForLevel` (games) and `defaultPackageIdsForLevel` (`ui/learning/LearningScreen.kt`) are two copies of the same rule. Both only know "3rd Year (ECE)".
- Profiles are readable by their owner or an admin only (policy), and the leaderboard view hides everything but the display name, so a new gender column is not visible to other students.
- Not yet read (first step of each part): the sign-up fields (probably `ui/auth/NativeSignInScreen.kt` 52 KB and/or `OnboardingScreen.kt`), `GameHubScreen.kt`, `LearningScreen.kt`, `ui/admin/UsersScreen.kt` (Scholars list).

## PART 1: Sign-up form upgrade

### Levels (saved strings)
School: `Grade 9` ... `Grade 12`. University: `Freshman`, `ECE Year 1` ... `ECE Year 5` (`3rd Year (ECE)` stays valid and means Year 3). Exams: `Remedial`, `Exit Exam`, `COC`, `GAT`, `EUEE`, `UAT`. `Other` + a written text.

### Slices
- **A1 (code, no database)** One shared level list in one file (for example `data/auth/AcademicLevels.kt`): saved string, label, group, and a flag `hasGames`. ONE function level -> package ids replaces both copies (My Learning and Games). `hasGames = false` for Exit Exam, COC, GAT, EUEE, UAT, Other: Games shows the Coming soon card (decision 2). What My Learning shows for them stays as today.
- **A2 (database, owner says go, one migration per topic, test in a rolled-back transaction first)** (a) add `profiles.gender` (text, nullable, CHECK Male / Female / Prefer not to say); (b) add `profiles.education_level_other` (text, nullable, length cap about 80); (c) widen the `education_level` CHECK with `ECE Year 1` ... `ECE Year 5`, `COC`, `EUEE`, `UAT` (only widens, no row can break). Existing own-row policies already cover new columns. Admin reading: check that the admin Scholars list reads through a path that includes the new columns. Record the migration names here. Do this BEFORE A3 ships, otherwise saving fails.
- **A3 (app)** Read the real sign-up code first, do not guess. Two short steps: (1) account: name, e-mail, password; (2) about you: gender chips, then the level as grouped cards. Same look as the Course Content folder cards (dark card, cyan border when chosen). Level and gender are required ("Prefer not to say" allowed). Choosing Other shows one text box (required). The existing profile write (PATCH then upsert) gets `gender` and `education_level_other`, nothing else.
- **A4 (app)** Settings uses the SAME level picker, so changing the level moves My Learning and Games together.
- **A5 (app, admin)** Scholars list (`UsersScreen.kt`): show gender (and the Other text) on the scholar card and in the detail. Admins only. Never in the leaderboard, never to students.

### Test on the phone (after each slice)
New test account: sign up with ECE Year 2 and a gender; My Learning and Games show ECE Year 2 only. Change to Grade 10 in Settings: both move. A test account with Exit Exam: Games says Coming soon. A test account with Other: the text is saved. Old accounts (3rd Year (ECE), Freshman) open as before. Admin: the Scholars list shows the gender.

## PART 2: Games navigation like My Learning

### Slices
- **B1 (read only)** Compare `GameHubScreen.kt` with `LearningScreen.kt` and the cards in `ui/packages`. Write the differences (cards, order, ECE year/semester step, lock labels) at the bottom of this file. No code.
- **B2 (code)** Move the package card, semester card and subject card from Learning into shared composables (in `ui/packages`) and use them in both tabs, so the design and thumbnails are the same by construction. Learning must look exactly as today (pure refactor).
- **B3 (code)** Games flow = Learning flow: my package cards > (ECE: year > semester) > subject cards > chapter list with Match and Wisdom Defense buttons. Back goes up one step and the position is remembered. Thumbnails keep coming from `gameSubjectImage`. Levels with `hasGames = false` show the Coming soon card instead.
- **B4 (optional, see Q1)** An "All packages" section under the student's own ones, with the locked / "Package required" label.
- Do NOT touch the game screens, level picker, results, or `GameAccess.gate` (the ownership check stays the authoritative one).

### Test on the phone
Games tab opens on the same package cards as My Learning; tapping through reaches chapters; a chapter of a package the student does not own shows the lock label; Match and Defense still play and save results. Changing the level in Settings changes the Games tab too.

## Order
A1 > A2 (needs owner "go") > A3 > A4 > A5, then B1 > B2 > B3 (> B4). A1 and B1 can start immediately. Each slice ends with a build, then a phone test by the owner.

## Open question
- **Q1** "All packages" in Games: show the other packages as locked cards like Learning, or only the student's own? (Not answered yet. B4 waits for this.)

## Plan changes
(none yet)

## Log
| Date | What | Left |
|---|---|---|
| 2026-11-28 | Plan written and pushed. | |
| 2026-11-28 | Owner decisions 1 to 4 added (levels, Coming soon for exam levels, Other text, gender admin-only). Nothing built, nothing changed in Supabase. | A1 and B1 can start; A2 needs the owner's go. |
