# SESSION 2026-10-22 STATUS S5: small gauge icon in the course hub lists

> Repo is PUBLIC: no secrets, no student data. Plan: `docs/MY_STATUS_V2_PLAN.md`. Rule: `docs/OWNER_RULE_PLAN_FIRST_PUSH_TO_MAIN.md`.

DONE (one commit on `main`, NOT compiled, NOT run on a device):
- `ui/study/StatusSubjectIcon.kt` (NEW): `StatusSubjectIcon(scopePath, title)`. Small round gauge icon (same look as the S3 row icon). Tap: spinner, reads `ResourceCatalogIndex`, `StudentProgressReader`, `ExamAttemptsReader`, finds the subject by `scopePath`, opens `StatusDetailCard` (subject, chapter and exam views from S4) in place. Read failure or unknown subject = a short message (Toast), never invented numbers.
- `scripts/patch_status_s5_hub_icon.py` (NEW, tested on a synthetic copy, safe to run twice): `HubItemListScreen.kt` is not changed directly. The patch turns the one-line hub header into a Row and adds the icon on its right, only when signed in (marker `MYSTATUS-S5`).

PATCH (needs the owner): GitHub > Actions > **Apply pending patches** > Run workflow > script `scripts/patch_status_s5_hub_icon.py` + new branch name `feat/status-s5-hub-icon` > wait for the build on that branch > test > merge with a Pull request. Until it is merged the new file only compiles, nothing shows. (Not yet added to `docs/PENDING_PATCHES.md`, and the table in `docs/MY_STATUS_V2_PLAN.md` section 8 still says S5 open: both left untouched on purpose to save a whole-file resend; tick them when the patch is merged.)

LEFT: the same icon on the subject cards of the package screens (`PackageDetailScreen.kt`, big file: another patch script; there the subject key is known from package id + slug). S6 only after the owner says yes (plan question B).

TEST (phone, after the patch is merged and the build is green): Learning > open a course > open a hub (for example Books): top right of the one-line hub header there is a small gauge icon. Tap: a spinner, then the subject card slides up (rings, chapters, exam attempts). Offline: a short message, no card.

UNSURE (hypotheses, not verified):
1. `scopePath` of `HubItemListScreen` equals the `scope_path` stored in `learning_resources` (it is what `LearningContentRepository.list` filters by); if the icon says "Nothing to show yet." for a course that has items, compare the two strings.
2. The Toast texts are copied from the S3 panel; no new wording.
