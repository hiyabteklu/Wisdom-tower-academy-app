# PROGRESS LOG (newest last). Short on purpose: one line per slice. The big plan is docs/PLAN_2026-10-20_ADMIN_LOCKS_UX_THEME.md.

## 2026-10-20
- S5 / S5b / S5c: paid PDFs in the private bucket, 10-minute addresses from the Edge Function `book-url`. VERIFIED on device.
- DB: `learning_resources_hub_check` allows `life-savers`.
- S5d: Edge Function `paid-pdf-upload` DEPLOYED. App side pushed. CI builds #837 to #844 all GREEN (compiled); not yet tested on a phone (docs/S5d-phone-upload.md).
- A1 DONE (database): `public.my_package_access()` returns `{status, enforced, packages[]}` for the signed-in student only.
- A2 PUSHED, CI build #843 GREEN (compiled), `[~]` not tested on a phone: `ContentAccess.kt` no longer treats every signed-in student as the owner.
  Ownership now comes from `my_package_access()` (enrollments + access grants, `__all__`, ECE semesters count as one,
  enforcement off = everything owned). No answer for this student yet (first launch offline) = nothing owned.
- A2 follow-up (warmUp hook) PUSHED, `[~]` not compiled yet: `WisdomTowerApplication.kt` calls `ContentAccess.warmUp()` (1) whenever the signed-in student id changes
  (app start with a saved session, fresh sign-in) and (2) on every activity resume. Cached 30 s, so a grant added by the admin shows within about 30 s of reopening.
- PLANNER SYNC (owner decision: plans live on the server, sync both ways, phone keeps an offline copy) PUSHED, `[~]` not compiled / not tested:
  `ui/study/PlannerSync.kt` (new) + `NativePlannerState.kt`. Master copy = row of the student in `public.study_plans` (`blocks`, `version`).
  Phone edit = saved on the phone at once, marked dirty, sent with `PATCH ... version = base` (first send = insert, version 1). Refused (server changed)
  or newer server version = server copy REPLACES the phone copy (an admin push overwrites the app). Offline = stays dirty, retried on planner open / next edit / app resume.
  A different student signing in on the phone starts clean. Old phone-only plans are offered to the server once (server wins if it already has a plan).
  Test plan (user bf091ce8-9ab0-48ec-9758-c76e7ff05263 has a 13-block schedule pushed from the database): open Planner = 13 blocks appear; edit one, check `study_plans.version`
  went 1 to 2; admin pushes a plan = the phone shows it after reopening the planner; airplane mode = the planner still opens with the last copy.
- NEXT: read the CI result for the warmUp + planner commits; phone test of A2 with two accounts (no purchase / granted); then A3 (Locks screen wording, Grants confirmation), A4 test matrix, then B1 and C1.
