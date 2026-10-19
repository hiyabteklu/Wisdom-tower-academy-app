# SESSION 2026-10-19 — ADMIN-native N8a (Push Broadcasts: history and scheduled list)

> Repo is PUBLIC: no secrets, no student data. Plan: `docs/ADMIN_NATIVE_PLAN.md`. Previous: `docs/SESSION_2026-10-19_ADMIN_NATIVE_N7_COURSE_CONTENT.md` (it left the order to the owner; the plan table lists Push Broadcasts next, and the owner said "continue making such admin dashboard native", so this slice follows the plan table). Done in the order of the plan: audit, then code.

## Why only half of the module
Push Broadcasts is five web files (panel 9 KB, composer v2 21 KB, block editor 10 KB, audience picker 10 KB, scheduled list 9 KB) plus a send route. One slice per chat (HANDOFF, quota). N8a = the read and cancel half, N8b = the composer. Until N8b the native screen has a card that opens the website composer inside the app.

## Audit of the website panel (read, not guessed)
Files read: `NotificationsPanel.tsx`, `NotificationComposerV2.tsx`, `ScheduledNotificationsPanel.tsx`, `api/notifications/route.ts`, `lib/notifications.ts`.
- **History (list)**: website API route, NOT plain Supabase. `GET /api/notifications?admin=true` with `Authorization: Bearer <admin access token>`; returns `{ok, notifications[]}` (newest 100; fields id, title, body, type, target, url, createdAt). Needs a website call with the admin token, which the app had no helper for.
- **History (delete)**: `DELETE /api/notifications?id=...` with the admin token; the route answers `{ok}` (it returns true even when the database call failed, so the app reloads the list after every delete and shows what is really there).
- **Scheduled list**: database function `admin_list_scheduled_notifications_v2` (`p_limit` 50), admin's own login. **Cancel**: `admin_cancel_scheduled_notification` (`p_id`). Both plain Supabase RPC, no secret.
- **Composer (N8b, not done)**: send now = `POST /api/notifications/send` with the admin token (server pushes through Firebase); schedule and edit = functions `admin_schedule_notification_v2` / `admin_update_scheduled_notification_v2`; uses a block editor, a 3-category chooser, a path picker (`lib/notification-paths`), an audience picker with live counts, a datetime in Ethiopia time. Still to read for N8b: `NotificationBlockEditor.tsx`, `NotificationAudiencePicker.tsx`, `lib/notification-authoring.ts`, `lib/notification-v2.ts`, `lib/notification-paths.ts`, `api/notifications/send/route.ts`.

## What was pushed (one commit)
1. NEW `data/admin/AdminWebApi.kt`: `get` and `delete` for website API routes with the admin Bearer token (refresh first, one retry on 401, same as `AdminSupabase.kt`).
2. NEW `data/admin/BroadcastsRepository.kt`: `loadHistory(origin)`, `deleteHistory(origin, id)`, `loadScheduled()`, `cancelScheduled(id)`, the website's error wording (`friendlyScheduleError`), `formatEat` (Ethiopia time, reads the offset in the text) and `audienceText` (same rule as the website).
3. NEW `ui/admin/BroadcastsScreen.kt`: header with Refresh, a card "New notification" with an "Open the composer" button, two chips Scheduled (n) and History (n), cards with status and category badges, EAT time, audience, link, last error, and Cancel (waiting rows only) or Delete, each behind a confirm dialog and a toast.
4. EDIT `AdminModules.kt` (`notifications` is native) and `AdminConsoleHost.kt` (Push Broadcasts branch; `composerOpen` shows the website panel `?tab=notifications` under the section bar, Back returns to the native list; it resets when the module changes).

## Deliberate differences from the website
- The website's "Edit" on a waiting scheduled row is not here (it loads the row into the composer; comes with N8b).
- Deleting a history row asks for confirmation (the website deletes at once).
- The website's FCM developer guide box (endpoint and sample code) is not shown: it is documentation for a developer, not an admin action.
- Lists are two chips plus one list instead of two stacked panels, so there is one vertical scroller and no long page.
- Empty-state sentences copied from the website; the second line under each ("Notifications you schedule for later appear here.", "Sent notifications appear here.") is new helper text.

## Evidence and limits
- `[~]` Written, NOT compiled, NOT run. Check the Actions run of this commit.
- Pitfall check by reading: every Modifier extension used is imported (`background`, `border`, `padding`, `fillMaxSize`, `fillMaxWidth`, `alpha`, `clip`); `items` imported; no composable call in a LazyColumn builder body outside `item` or `items`; named arguments everywhere; smart casts use local vals (`sched`, `hist`, `cancelItem`, `deleteItem`, `category`, `link`, `error`); icon `Refresh` is core; `async` imported for the two parallel loads inside `LaunchedEffect`; no slash-star inside any comment; no secret.
- HYPOTHESIS: the admin check on `GET /api/notifications?admin=true` accepts the app's login token the same way as the website's (the route calls `requireAdmin`, which I did not read). A refusal shows the server's message on the Scheduled or History card.
- HYPOTHESIS: the two database functions accept a call through `rpc/<name>` with the JSON body names `p_limit` and `p_id` (taken from the website calls), and `send_at` / `created_at` come back with an offset or `Z`; `formatEat` reads either and assumes UTC when there is none.
- HYPOTHESIS: the extra header `Prefer: count=none` on the two RPC calls is harmless (it is a valid PostgREST preference; the shared `post()` helper requires one).
- Plan status line (plan file not resent): 2026-10-19 | Claude | N8a written `[~]`: Push Broadcasts native for history and scheduled list (cancel, delete); composer still the website panel. NEXT: N8b composer, or the owner picks another module (Competitions, Free Resources, Database and SQL Hub).

## What the owner should test
1. Admin Console > Push Broadcasts opens natively: header, the New notification card, the two chips with counts.
2. Scheduled: the list matches the website (status, category, time in Ethiopia time). Cancel a TEST waiting item: question, toast "Cancelled.", the row changes to cancelled.
3. History: the list matches the website; open one with a link; delete a TEST item: question, toast "Deleted.", it is gone after the reload.
4. "Open the composer" shows the website composer under the section bar; send or schedule a TEST item; Back returns to the native screen and the new item shows after the reload.
5. Airplane mode: a card with "Could not reach the server" and Try again.
6. Back on the native screen returns to the admin grid; Back on the grid closes the admin.

## NEXT
N8b (composer: read the six files listed above first), or the owner picks. Remaining on the website: Competitions, Free Resources (uploads), Database and SQL Hub (probably stays web).
