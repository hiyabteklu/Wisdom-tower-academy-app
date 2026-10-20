# PROGRESS LOG (newest last). Short on purpose: one line per slice. The big plan is docs/PLAN_2026-10-20_ADMIN_LOCKS_UX_THEME.md.

## 2026-10-20
- S5 / S5b / S5c: paid PDFs in the private bucket, 10-minute addresses from the Edge Function `book-url`. VERIFIED on device.
- DB: `learning_resources_hub_check` allows `life-savers`.
- S5d: Edge Function `paid-pdf-upload` DEPLOYED. App side pushed, `[~]` not compiled / not tested (docs/S5d-phone-upload.md).
- A1 DONE (database): `public.my_package_access()` returns `{status, enforced, packages[]}` for the signed-in student only.
- A2 PUSHED, `[~]` not compiled / not tested: `ContentAccess.kt` no longer treats every signed-in student as the owner.
  Ownership now comes from `my_package_access()` (enrollments + access grants, `__all__`, ECE semesters count as one,
  enforcement off = everything owned). No answer for this student yet (first launch offline) = nothing owned.
  New `ContentAccess.warmUp()` is NOT called anywhere yet; until it is, the first course screen of a session asks the server
  itself (a short "Package required" flash is possible before the answer arrives on a slow network).
  TO DO next in A2: call `warmUp()` after sign-in and when the app comes to the front (find the right place in MainActivity /
  AuthStateManager).
- NEXT: read the CI result for S5d + A2; then A3 (Locks screen wording, Grants confirmation), A4 test matrix, then B1 and C1.
