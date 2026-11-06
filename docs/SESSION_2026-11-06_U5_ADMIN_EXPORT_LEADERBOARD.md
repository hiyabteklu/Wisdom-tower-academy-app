# SESSION 2026-11-06 — U5: admin Top 3 JPG on the contest leaderboard, no minimum participants

Owner request (2026-11-06): the leaderboard Top 3 JPG download (4:3 and 9:16) must be on the **contest leaderboard (trophy section)**, for
**admins only**, not in Admin > Competitions; and it must download **no matter how many participants** (before: at least 3).

## Done (pushed straight to `main`, NOT compiled, NOT tested on a phone)
- Supabase migration `u5_admin_competition_export_no_minimum` (live): `public.admin_competition_export` no longer returns `insufficient_results` /
  `incomplete_results`; it returns `status ok` with 0 to 3 rows in `top` (null score/total become 0, time stays null = "Not recorded"). Still admin-only
  (`private.is_admin()`), still `not_closed` before the window closes. Added `available_results`.
- `data/admin/LeaderboardExportRepository.kt`: accepts 0 to 3 winners.
- `ui/admin/LeaderboardJpgRenderer.kt`: draws whoever exists; nobody ranked = a "NO PARTICIPANTS" card. Design otherwise unchanged (the golden U5 redesign is still open).
- `ui/competition/LeaderboardAdminExport.kt` (new): small glass card "Admin: download Top 3 as JPG" with two buttons (4:3 landscape, 9:16 portrait); draws nothing
  unless `AdminAccess.isAdmin()` is true; the app asks where to save.
- `ui/competition/CompetitionLeaderboardScreen.kt`: one line calls the card under the participant count.

## LEFT
- The old "Export top 3" button is STILL in Admin > Competitions (`CompetitionsScreen.kt`, contest panel). Remove it when the owner confirms the new place works.
- Golden celebration design (U5a to U5c in `docs/PLAN_2026-11-06_URGENT_FIVE_FIXES.md`): not started. Tick U5 there when done.

## TEST (owner)
1. Wait for the GitHub build to be green. If red, copy the error to the next agent.
2. Signed in as admin: Competitions (trophy) > an ended contest > Leaderboard: the card appears under the participant count; both buttons save a JPG.
3. Signed in as a student: the card is not there.
4. A contest with 0, 1 or 2 participants: the buttons still save an image.

## Live Supabase changes
- 2026-11-06 `u5_admin_competition_export_no_minimum`: replaces function `public.admin_competition_export` (same signature and grants).
