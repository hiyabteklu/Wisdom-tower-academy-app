# Single-device account protection

## Owner decision
The paid Android app uses one registered Android installation per student account. A second phone is rejected until an administrator resets the registration after verifying the student's identity.

## Current implementation
- Supabase migration: `single_device_account_protection`.
- `public.account_devices`: one row per Auth user, containing a SHA-256 device fingerprint, human-readable model label, current Supabase session ID and timestamps.
- `public.account_device_events`: service-role-only audit trail for registration, same-device login, blocked-device attempt and admin reset. Blocked attempts now include admin review fields (`reviewed_at`, `reviewed_by`, `review_note`).
- `public.register_device_session`: service-role-only atomic registration. First device claims the slot; same device may sign in again; a different fingerprint is rejected.
- `public.admin_list_account_devices` and `public.admin_reset_account_device`: admin-gated database procedures. Reset deletes the old Auth session row as well as the device registration, so the old refresh session cannot reclaim the slot.
- Supabase Edge Function `register-device` (JWT verification enabled): validates the bearer token with Supabase Auth, reads the JWT session_id, and calls the registration procedure using the service role on the server.
- Android `SupabaseAuthClient`: after password sign-in and sign-up, creates or reads a random installation GUID from `context.noBackupFilesDir`, hashes it with the app package name using SHA-256, and calls `register-device`. The raw GUID and access token are not stored in the registry or logged. The GUID is not backed up and resets on uninstall or app-data clearing.
- Native sign-in passes application context into the auth client. A second-device rejection is shown as a normal sign-in error; the newly created rejected Auth session is then revoked through the Auth logout endpoint.
- Website/browser sign-in and registration call the shared `registerBrowserDevice` helper in `website-reference/src/lib/supabase.ts`. A rejected browser session is revoked server-side and removed from the local Supabase client.
- `recoverSession()` rechecks browser registration for ordinary browsers. It skips browser registration inside the Android WebView (`wta-native` / `WisdomTowerApp` user-agent marker), because that session was already registered by the native Android sign-in flow.

## Important limits / not yet complete
- The native Android and ordinary website/browser sign-in paths are now integrated. Alternate sign-in paths, legacy sessions, and any third-party clients must still be audited before claiming every possible access path is covered.
- Existing sessions created before this feature are not automatically registered until the student signs in again.
- Admin reset deletes the registered Auth session row before releasing the device slot. Rejected Android/browser sign-ins also attempt to revoke the newly created local Auth session. This does not invalidate unrelated sessions unless the admin reset targets them.
- Website admin console has a `Registered Devices` / device-security section backed by admin-only RPCs. It lists registered phones, shows every pending blocked-device attempt plus the latest 200 other audit events, counts pending attempts exactly, and lets admins mark each blocked attempt reviewed with a note.
- The website `Exam Integrity` section lists flagged ranked-exam attempts, displays leave count, total away time and longest absence on each row, and shows recent leave/return events directly in the list. All timestamps are rendered in Ethiopia time (EAT). The tab badge counts distinct students awaiting review.
- Migration `enforce_active_device_on_content_open` now checks the registered Auth session in `open_book` and `open_resource`. Accounts with no device row remain backward-compatible; once registered, non-admin access must come from the registered session.
- Device fingerprints are app-scoped Android identifiers, not hardware attestation. Factory reset, device cloning/rooting, or a determined attacker may bypass them. This is a deterrent to casual account sharing, not DRM.
- The model label is manufacturer/model only. The random installation GUID is hashed before transmission and the raw value remains in app-private non-backed-up storage. Disclose the collection and account-security purpose in the privacy policy and Play Console Data safety section; do not add IMEI, serial number, SIM identifiers, MAC address or location collection.

## Verification still required
1. Compile the Android app with the repository's GitHub Actions workflow.
2. Sign in to a test account on phone A; verify one row is created.
3. Sign in to the same account on phone A again; verify it succeeds and updates the session.
4. Sign in to the same account on phone B; verify it is rejected and an audit event is written.
5. Verify the original account, purchase grants, enrollments and reading progress are unchanged.
6. Test the admin reset with Auth session revocation.
7. Test website sign-in and sign-up in a normal browser, then verify a second browser/device is rejected and the rejected Auth session is revoked.
8. Open the website inside the Android app and verify session recovery does not attempt a second browser registration or sign the student out.
9. Test protected book/resource opens from the registered session and from a stale/revoked session.
10. Test the website review queues with a disposable student account and verify badge counts and event timelines.

Never expose service-role credentials in Android, browser code, logs, or this document.
