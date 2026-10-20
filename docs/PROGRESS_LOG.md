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
- NEXT: read the CI result for the warmUp commit; phone test of A2 with two accounts (no purchase / granted); then A3 (Locks screen wording, Grants confirmation), A4 test matrix, then B1 and C1.
