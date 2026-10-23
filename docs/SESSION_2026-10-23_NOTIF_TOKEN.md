# Session 2026-10-23: notifications request sends the student's token

Owner order: make the welcome notification say the student's first name ("Hey Dave, you made it").

## Website (repo `hiyabteklu/Wisdom-tower-academy`, `main`, commit `a9df663`, `[~]` not compiled, not tested live)
- NEW `src/lib/welcome-notification.ts`: welcome title/body/rich blocks (callout, dividers) and `fillFirstName`.
- `src/lib/notifications.ts`: the built-in welcome notice uses it (url now `/notifications?open=notif_welcome_system`).
- `src/app/api/notifications/route.ts` (GET): with a verified Bearer token, `{first_name}` in title/body is filled from the profile name (`first_name`, else `full_name`, else `name`, first word); without a token the token is dropped ("Hey, you made it").

## This repo
- `NotificationRepository.refreshNotifications` now adds `Authorization: Bearer <access token>` when the student is signed in (after `AuthStateManager.refreshIfNeeded`). Signed out, or if the refresh throws: the old `?userId=&email=` request, no header.
- Side effect (intended, closes HANDOFF finding 8 / `WEBSITE_CHANGES.md` section 3 for the app side): with a token the server returns every notice addressed to that student, including `payment` and `general` rows, not only admin/material/contest.

## Evidence
- Code only. `[~]` written, NOT compiled (CI result not seen), not tested on a phone.

## Test
1. Wait for the website build (Vercel) and the app build (CI) to be green.
2. Phone, signed in: open the bell. The welcome card should read "Hey <FirstName>, you made it"; open it: callout and dividers show.
3. Signed out: the guest card shows (no list), as before.
