# 2026-11-04 — Admin Remedial and EUEE content navigation

## Scope
Expose Remedial in the existing Course Content and Content Locks trees, and make the existing Entrance Questions branch explicit as EUEE in the admin navigation. Reuse the current admin screens, repositories, question JSON editor, and lock implementation; no new parallel dashboard or database schema.

## Verified source mapping
- Remedial package id: `remedial`.
- Remedial subjects from `website-reference/src/data/remedial.ts`: English (`english`), Maths (`maths`), Physics (`physics`), Chemistry (`chemistry`), Biology (`biology`), History (`history`), Geography (`geography`).
- Remedial scope: `remedial/{subject-id}`; same six learning hubs as the Academy subjects.
- EUEE remains the existing `entrance-questions` branch: stream > subject > year (2014–2019) > Exams, with `entrance/{stream}/{subject}/{year}` scopes.
- The Course Content editor already stores question-bank and exam questions in `learning_resources.meta` JSON. The existing Content Locks screen uses the same navigation tree and package id.

## Changes
- Updated `app/src/main/java/com/wisdomtower/academy/data/admin/AdminContentTree.kt`: added the Remedial package and seven subjects; changed the admin label to `EUEE · Entrance Questions`.
- Updated `website-reference/src/data/admin-nav.ts`: mirrored the same Remedial branch and EUEE label so website and native navigation stay aligned.
- No Supabase schema, rows, access policies, secrets, or student data changed.

## Verification
- Source paths and package ids were checked against the website Remedial page, `remedial.ts`, `packages.ts`, and `entrance-questions.ts`.
- Android build / GitHub Actions and device runtime checks are pending. Do not call this release-verified until CI and the owner phone check pass.
- Live Supabase project is not currently visible through the connected Supabase account, so no live database inspection or mutation was attempted.
