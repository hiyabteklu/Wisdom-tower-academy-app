# Session 2026-10-31 (later) - Admin security screens: two live errors fixed

## What the owner saw on the phone (first run of the native screens)
1. Device Security: `structure of query does not match function result type` (shown twice, one per list call).
2. Exam Integrity: `permission denied for table competition_entries`.

## Causes (verified with read-only SQL on the live project)
1. `admin_list_account_devices` and `admin_list_account_device_events` declare `email text` but select `auth.users.email`, which is `character varying(255)`. Postgres 42804. Registered phones showed "-" because the list call failed.
2. `admin_review_exam_integrity` was SECURITY INVOKER and UPDATEs `competition_entries`; role `authenticated` has only SELECT on that table (the admin UPDATE RLS policy exists but a policy cannot work without the table GRANT). Postgres 42501. The report itself loads because SELECT is granted.

## Fix (applied live, source in docs/sql/fix_admin_security_rpc_types_and_review_privilege.sql)
- Cast to text in both device list functions.
- `admin_review_exam_integrity` is now SECURITY DEFINER, search_path empty, admin check inside, execute revoked from anon/public, granted to authenticated. No UPDATE grant was given to students.
- Checked: all three functions are SECURITY DEFINER where intended, anon cannot execute, authenticated can.

## NOT verified
- The three RPCs were not called as the admin (no admin JWT in this session). Owner must reopen both screens: the red errors should be gone, Device Security should list registered phones, and Mark reviewed on the Exam Integrity attempt should succeed.
- Android app still not confirmed compiled/tested from the previous session beyond the last CI fix (commit 3189430).
- Supabase advisor will flag the review function like the other SECURITY DEFINER admin RPCs; it is intentional.

## Next
1. Owner retests both screens on the phone.
2. Run non-release CI compile check.
3. Remaining checklist from SESSION_2026-10-31_ADMIN_SECURITY_NATIVE.md (device reset only on a test account; confirm a student account cannot see or call admin RPCs).
