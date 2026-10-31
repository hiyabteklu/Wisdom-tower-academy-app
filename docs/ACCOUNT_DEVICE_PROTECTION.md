# Single-device account protection

## Owner decision
The paid Android app uses one registered Android installation per student account. A second phone is rejected until an administrator resets the registration after verifying the student's identity.

## Current implementation
- Supabase migration: `single_device_account_protection`.
- `public.account_devices`: one row per Auth user, containing a SHA-256 device fingerprint, human-readable model label, current Supabase session ID and timestamps.
- `public.account_device_events`: service-role-only audit trail for registration, same-device login, blocked-device attempt and admin reset.
- `public.register_device_session`: service-role-only atomic registration. First device claims the slot; same device may sign in again; a different fingerprint is rejected.
- `public.admin_list_account_devices` and `public.admin_reset_account_device`: admin-gated database procedures. Reset deletes the old Auth session row as well as the device registration, so the old refresh session cannot reclaim the slot.
- Supabase Edge Function `register-device` (JWT verification enabled): validates the bearer token with Supabase Auth, reads the JWT session_id, and calls the registration procedure using the service role on the server.
- Android `SupabaseAuthClient`: after password sign-in and sign-up, hashes the app-scoped Android ID and calls `register-device`. The raw Android ID and access token are not stored in the registry or logged.
- Native sign-in passes application context into the auth client. A second-device rejection is shown as a normal sign-in error.

## Important limits / not yet complete
- This currently protects the native Android sign-in path. Website/browser sign-in and any alternate authentication paths must be integrated before claiming account sharing is blocked across every client.
- Existing sessions created before this feature are not automatically registered until the student signs in again.
- Device registration does not by itself revoke a previously issued Supabase session. A production admin reset workflow must revoke the old Auth session (global sign-out) before allowing the replacement device to claim the slot.
- Website admin console now has a `Registered Devices` section backed by the admin-only RPCs.
- Do not enable active-device checks in `open_book` / `open_resource` until all supported clients register devices, or existing website users may be locked out.
- Device fingerprints are app-scoped Android identifiers, not hardware attestation. Factory reset, device cloning/rooting, or a determined attacker may bypass them. This is a deterrent to casual account sharing, not DRM.

## Verification still required
1. Compile the Android app with the repository's GitHub Actions workflow.
2. Sign in to a test account on phone A; verify one row is created.
3. Sign in to the same account on phone A again; verify it succeeds and updates the session.
4. Sign in to the same account on phone B; verify it is rejected and an audit event is written.
5. Verify the original account, purchase grants, enrollments and reading progress are unchanged.
6. Implement and test the admin reset with Auth session revocation before production enforcement.
7. Add the website/browser path and admin device-management UI.

Never expose service-role credentials in Android, browser code, logs, or this document.
