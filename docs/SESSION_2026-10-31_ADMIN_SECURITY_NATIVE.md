# Session 2026-10-31 — Native Admin Security

## Owner request
Continue the native Admin Console work by adding an in-app dashboard for exam activity, student exits/returns and device-abuse review.

## Repository and scope
- Repository: hiyabteklu/Wisdom-tower-academy-app-packed
- Existing native Admin Console is hosted between the app's navigation bars and is gated by the database-backed AdminAccess check.
- This slice adds two modules to that existing console; it does not replace the website admin dashboard or change student exam scoring.

## Implemented
1. Added AdminSupabase.rpc(), which uses the current signed-in user's access token, refresh handling and anon key. No service-role key is used by the Android client.
2. Added AdminSecurityRepository for:
   - admin_exam_integrity_report
   - admin_review_exam_integrity
   - admin_list_account_devices
   - admin_list_account_device_events
   - admin_reset_account_device
   - admin_review_account_device_event
3. Added native Compose Exam Integrity screen:
   - pending review and flagged-student counts
   - per-attempt leave count, total away time, longest away time and flag count
   - expanded leave/return and flag timeline
   - optional review note and mark-reviewed action
4. Added native Compose Device Security screen:
   - registered phone model label, registration time and last seen
   - blocked new-device sign-in attempts and recent device history
   - review note and mark-reviewed action for blocked attempts
   - confirmed device reset, using the existing session-revoking admin RPC
5. Added Exam Integrity and Device Security tiles to the native Admin Console grid. Their badges count distinct students with pending reviews; failed reads produce no badge rather than a false zero.
6. Added SQL RPC admin_review_account_device_event(bigint,text), with auth.uid() and private.is_admin() checks, restricted execution to authenticated and revoked from anon/public. Applied to the live Supabase project and saved the SQL source in docs/sql/admin_review_account_device_event.sql.

## Verification
- Live SQL introspection confirms the new RPC exists with arguments p_event_id bigint, p_review_note text.
- Live privilege check confirms anon cannot execute it and authenticated can.
- Supabase security advisor reports this function in its general warning category for authenticated-callable SECURITY DEFINER functions. This function is intentionally callable by authenticated admins only; it has explicit auth.uid() and private.is_admin() checks and a fixed empty search_path. Existing advisor findings also include many older admin RPCs and unrelated views/tables.
- Existing RLS-without-policy notices for account_devices and account_device_events are intentional: these tables are service-role-only; native admins use guarded RPCs rather than direct table access.
- Android app has NOT been compiled in this slice and the screens have NOT been tested on a physical device. Do not call this release-verified.
- No APK/release build was triggered.

## Commits
- 5fef5ff8 — authenticated AdminSupabase RPC helper
- 67e1fd58 — native admin security repository
- 6e3d6a8e — native exam-integrity and device-security screens
- 5ef2948f — add security modules to native admin grid
- 9d4ea622 — route modules to native Compose screens
- ba956c3a and 8e94635f — security review count integration / corrected AdminCounts implementation
- 1ef661ae — safe handling of nullable audit fields
- 6ac23c33 — Compose ColumnScope correction
- a94b8257 — pending-review badges on home tiles
- 3819c2fd — SQL source for device event review RPC
- 1c75dc11 — update native admin plan

## Next
Run a non-release Android CI/compile check, then verify as an admin on the owner's phone:
1. open Account > Admin Console > Exam Integrity;
2. inspect a flagged attempt and review it;
3. open Device Security and inspect blocked attempts;
4. test a device reset only on a disposable test account, not a real student account;
5. confirm students cannot see the admin entry or call the admin RPCs.
