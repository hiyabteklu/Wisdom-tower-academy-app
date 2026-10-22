# SESSION 2026-10-22 STATUS S5B: gauge icon on the package subject cards + ONE combined patch

> Repo is PUBLIC: no secrets, no student data. Plan: `docs/MY_STATUS_V2_PLAN.md`. Continues `docs/SESSION_2026-10-22_STATUS_S5.md`.

DONE (one commit on `main`, NOT compiled, NOT run on a device):
- `ui/packages/OpenCardWithStatus.kt` (NEW): the normal `OpenCard` plus the small gauge icon (`StatusSubjectIcon`) on its top right corner, only when signed in. Compiles on its own, shows nothing until the patch is applied.
- `scripts/patch_status_s5_both.py` (NEW, tested on a synthetic copy of the call sites, safe to run twice): first runs the S5 hub patch (`patch_status_s5_hub_icon.py`), then turns the subject cards of `PackageDetailScreen.kt` (Freshman, ECE courses, Grades 9 to 12, Remedial) into `OpenCardWithStatus(..., subject.path, isLoggedIn)`. One run = both halves of S5.

PATCH (needs the owner): GitHub > Actions > **Apply pending patches** > Run workflow > script `scripts/patch_status_s5_both.py` + new branch name `feat/status-s5-icons` > wait for the green build on that branch > test > merge with a Pull request.

LEFT: tick S5 in `docs/MY_STATUS_V2_PLAN.md` section 8 after the merge (left untouched now to avoid a whole-file resend). Not covered on purpose: Entrance Questions cards (stream/subject/year drill-down) and the single card of other packages (COC, UAT ...). S6 only after the owner says yes (plan question B).

TEST (phone, after merge + green build, signed in): Packages > Freshman: every subject card has a small gauge icon top right; tap it: spinner, then the subject card (rings, chapters, exams). Same on Grade 9 to 12, Remedial, and an ECE semester. Tapping the card itself still opens the subject as before. Learning > a course > a hub (Books ...): gauge icon at the right of the one-line header.

UNSURE (hypotheses, not verified):
1. The `path` of each subject card equals `scope_path` in `learning_resources`; if the icon says "Nothing to show yet." for a subject with content, compare the two strings.
2. `isLoggedIn` of `PackageDetailScreen` must be passed as true by the caller in `MainActivity.kt` (not read); if no icon shows on a signed-in phone, check that call.
3. The icon sits over the card picture; it has its own translucent circle, no new colours.
