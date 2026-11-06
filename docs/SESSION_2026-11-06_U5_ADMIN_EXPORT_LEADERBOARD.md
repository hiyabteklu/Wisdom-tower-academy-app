# SESSION 2026-11-06 — U5: admin Top 3 JPG on the contest leaderboard, no minimum participants, golden poster design

Owner request (2026-11-06): the leaderboard Top 3 JPG download (4:3 and 9:16) must be on the **contest leaderboard (trophy section)**, for
**admins only**, not in Admin > Competitions; it must download **no matter how many participants** (before: at least 3); and the picture
itself must be a **golden celebration poster** that looks good.

## Done (pushed straight to `main`, NOT compiled, NOT tested on a phone)
- Supabase migration `u5_admin_competition_export_no_minimum` (live): `public.admin_competition_export` no longer returns `insufficient_results` /
  `incomplete_results`; it returns `status ok` with 0 to 3 rows in `top` (null score/total become 0, time stays null = "Not recorded"). Still admin-only
  (`private.is_admin()`), still `not_closed` before the window closes. Added `available_results`.
- `data/admin/LeaderboardExportRepository.kt`: accepts 0 to 3 winners.
- `ui/competition/LeaderboardAdminExport.kt` (new): small glass card "Admin: download Top 3 as JPG" with two buttons (4:3 landscape, 9:16 portrait); draws nothing
  unless `AdminAccess.isAdmin()` is true; the app asks where to save.
- `ui/competition/CompetitionLeaderboardScreen.kt`: one line calls the card under the participant count.
- `ui/admin/LeaderboardJpgRenderer.kt` REWRITTEN as the golden poster: navy background with gold glow, light rays, seeded confetti and sparkles, gold frame,
  golden trophy drawn with paths, the word CHAMPIONS, exam title, subject, date, then a podium (2nd left, 1st centre and tallest, 3rd right) with gold, silver and
  bronze blocks; name, score and time above each block. Missing ranks leave their slot empty; nobody ranked = a "NO PARTICIPANTS" card. No phone/email on the image.
  Same two sizes (1600x1200 and 1080x1920). Layout numbers are fixed per format, so check the first real export on a phone for text overlap (long names, long exam titles).

## LEFT
- The old "Export top 3" button is STILL in Admin > Competitions (`CompetitionsScreen.kt`, contest panel). Remove it when the owner confirms the new place works.
- Tick U5 in `docs/PLAN_2026-11-06_URGENT_FIVE_FIXES.md` (U5a design, U5b both sizes, U5c entry point) after the owner's phone test.

## TEST (owner)
1. Wait for the GitHub build to be green. If red, copy the error to the next agent.
2. Signed in as admin: Competitions (trophy) > an ended contest > Leaderboard: the card appears under the participant count; both buttons save a JPG; open both pictures.
3. Signed in as a student: the card is not there.
4. A contest with 0, 1 or 2 participants: the buttons still save an image.

## Live Supabase changes
- 2026-11-06 `u5_admin_competition_export_no_minimum`: replaces function `public.admin_competition_export` (same signature and grants).
