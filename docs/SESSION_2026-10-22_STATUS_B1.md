# SESSION 2026-10-22 STATUS B1: remove the gauge icons (hubs + subject cards)

> Repo is PUBLIC: no secrets, no student data. Plan: `docs/MY_STATUS_V2_SUBJECT_BOARD_PLAN.md` (slice B1). **Do NOT run `scripts/patch_status_s5_both.py` or `scripts/patch_status_s5_hub_icon.py` any more: they are obsolete (they add the icons B1 removes).**

DONE (one commit on `main`, NOT run on the real repo):
- `scripts/patch_status_b1_remove_icons.py` (NEW, tested on synthetic copies: result of the hub part is byte-identical to the original header, package part gives the 4 original `OpenCard(...)` calls, second run does nothing). It (1) restores the one-line hub header in `HubItemListScreen.kt` (removes the `MYSTATUS-S5` icon) and (2) turns the 4 `OpenCardWithStatus(...)` calls in `PackageDetailScreen.kt` back into `OpenCard(...)`. Does nothing for a part that was never applied.
- `StatusSubjectIcon.kt` and `OpenCardWithStatus.kt` are left in place on purpose (unused after the patch, harmless); delete `OpenCardWithStatus.kt` in B3 once the build is green.

PATCH (needs the owner): GitHub > Actions > **Apply pending patches** > Run workflow > script `scripts/patch_status_b1_remove_icons.py` + new branch name `feat/status-b1-remove-icons` > wait for the green build > test > merge with a Pull request. If S5 was already merged, run it on `main`'s latest; if the S5 branch was never merged, this changes nothing (and the icons never reach `main`).

LEFT: B2 (`StatusSubjectBoard.kt`), B3 (wire into `StudyWorkspaceScreen.kt`). The B1 row of the plan table (section 7) is still marked open: tick it after the merge.

TEST (phone, after merge): Learning > a course > Books / Notes / Flashcards: no gauge icon in the header. Packages > Freshman: no gauge icon on the subject cards, cards open as before.
