# PLAN 2026-11-28: sign-up form upgrade + games navigation like My Learning

> Repo is PUBLIC: no secrets. PLAN ONLY, no code written yet (owner order). Follow `docs/OWNER_RULE_PLAN_FIRST_PUSH_TO_MAIN.md`: one slice = one commit to `main`, tick the slice here in the same commit, record every live Supabase change here.

## What I verified first (read-only, 2026-11-28)
- `public.profiles` has `education_level` with a CHECK that allows only: Grade 9, Grade 10, Grade 11, Grade 12, Remedial, Freshman, 3rd Year (ECE), Other, Exit Exam, GAT (or null). There is NO gender column. (`docs/SUPABASE_NOTES.md`)
- Package ids and scope paths for ECE years are already planned: scopes like `ece/y1-sem-N/<id>` (`docs/ECE_STATUS_UPDATE_2026-11-23_ALIAS.md`, `docs/ADMIN_ECE_Y1_Y5_PLAN.md`). Year 1/2 show freshman content through an alias.
- Games ALREADY follow the level in a basic way: `ui/games/GameEnrollment.kt` (`enrolledGamePackages`, `gameSubjectImage`) and `GameTabScreen.kt` show only the student's packages and reuse the package/subject pictures. So job 2 is mostly an upgrade of the navigation and a shared design, not a new feature.
- `gamePackageIdsForLevel` (games) and `defaultPackageIdsForLevel` (`ui/learning/LearningScreen.kt`) are two copies of the same rule. Both only know "3rd Year (ECE)", and return nothing for Exit Exam and GAT.
- Not yet read (first step of each part): the sign-up fields (probably `ui/auth/NativeSignInScreen.kt` 52 KB and/or `OnboardingScreen.kt`), `GameHubScreen.kt`, `LearningScreen.kt`.

## PART 1: Sign-up form upgrade

### Wanted
New design with: gender; academic level = ECE Year 1 to 5, Grade 9 to 12, Remedial, Exit Exam, GAT, Other (Freshman stays because existing students use it).

### Slices
- **A1 (code, no database)** One shared level list in one file (for example `data/auth/AcademicLevels.kt`): the exact saved strings, the label, the group, the icon. Strings: `ECE Year 1` ... `ECE Year 5`, plus the existing ones. `3rd Year (ECE)` stays valid and means Year 3, so old accounts keep working. ONE function level -> package ids replaces both copies (My Learning and Games). Exit Exam and GAT: see question Q2.
- **A2 (database, owner approves, one migration per topic, test in a rolled-back transaction first)** (a) add `profiles.gender` (text, nullable, CHECK Male / Female / Prefer not to say); (b) widen the `education_level` CHECK with `ECE Year 1` ... `ECE Year 5` (it only widens, no row can break). Existing own-row policies already cover a new column. Record the migration names here. Do this BEFORE A3 ships, otherwise saving fails.
- **A3 (app)** Read the real sign-up code first, do not guess. Redesign as two short steps: (1) account: name, e-mail, password; (2) about you: gender chips, then the level as grouped cards (School: Grade 9 to 12; University: Freshman, ECE Year 1 to 5; Exams: Remedial, Exit Exam, GAT; Other). Same look as the Course Content folder cards (dark card, cyan border when chosen). One level is required, gender is required, "Prefer not to say" is allowed. The app already writes the profile with a PATCH then an upsert; add `gender` to that write only.
- **A4 (app)** Settings uses the SAME level picker component, so changing the level there moves the packages in My Learning and Games at once (both already read `profile.educationLevel`).

### Test on the phone (after each slice)
New test account: sign up with ECE Year 2 and a gender; check the profile saved; My Learning shows ECE Year 2 only; change to Grade 10 in Settings and see it move. Old account (3rd Year (ECE) or Freshman) still opens as before.

## PART 2: Games navigation like My Learning

### Wanted
When the student picks a level, the package goes to My Learning; the Games tab shows only what is in My Learning, plus the all-packages part, with the SAME thumbnails and card design as the real packages in Learning (package > semester > subject).

### Slices
- **B1 (read only)** Compare `GameHubScreen.kt` with `LearningScreen.kt` and the cards in `ui/packages`. Write the differences (cards, order, ECE year/semester step, lock labels) at the bottom of this file. No code.
- **B2 (code)** Move the package card, semester card and subject card from Learning into shared composables (in `ui/packages`) and use them in both tabs, so the design and thumbnails are the same by construction. Learning must look exactly as today (pure refactor, nothing else changes).
- **B3 (code)** Games flow = Learning flow: my package cards > (ECE: year > semester) > subject cards > chapter list with Match and Wisdom Defense buttons. Back goes up one step and the position is remembered. Thumbnails keep coming from `gameSubjectImage`, but package and semester cards use the shared ones.
- **B4 (code)** An "All packages" section under the student's own ones, same as Learning: other packages show the locked/"Package required" label and open nothing. See Q1.
- Do NOT touch the game screens, level picker, results, or `GameAccess.gate` (the ownership check stays the authoritative one).

### Test on the phone
Games tab opens on the same package cards as My Learning; tapping through reaches chapters; a chapter of a package the student does not own shows the lock label; Match and Defense still play and save results. Changing the level in Settings changes the Games tab too.

## Order
A1 > A2 (needs owner "go") > A3 > A4, then B1 > B2 > B3 > B4. A1 and B1 can start immediately. Each slice ends with a build, then a phone test by the owner.

## Questions for the owner (nothing is blocked, answer when you can)
- **Q1** "All packages" in Games: show the other packages as locked cards like Learning does, or hide them? (Plan assumes locked cards like Learning.)
- **Q2** What should an Exit Exam or GAT student see in My Learning and Games? Today the rule returns nothing for them (the folders exist in Course Content: EUEE, GAT, Exit Exam, but they hold no rows yet).
- **Q3** "Other": save only the word Other, or also ask a short text (school or field)? (Plan: only the word, no extra field.)
- **Q4** Is gender used anywhere (certificates, leaderboard, reports) or is it only collected? (Plan: only collected, never shown to other students.)

## Plan changes
(none yet)

## Log
| Date | What | Left |
|---|---|---|
| 2026-11-28 | Plan written and pushed. Nothing built, nothing changed in Supabase. | A1 and B1 can start; A2 needs the owner's go. |
