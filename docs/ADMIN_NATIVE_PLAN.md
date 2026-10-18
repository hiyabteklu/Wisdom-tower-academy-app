# ADMIN_NATIVE_PLAN.md — Admin Console rebuilt natively, part by part (LIVE TASK, owner order 2026-10-18)

> Repo is PUBLIC: no secrets, no student names or emails. This plan EXTENDS `docs/ADMIN_MOBILE_PLAN.md` and `docs/ADMIN_UI_V2_PLAN.md` (the web redesign, kept as the fallback). Rules of `HANDOFF.md` section 0 still apply (inspect first, focused changes, no invented text, label guesses "hypothesis", one commit per slice, log first, `docs/BUILD_PITFALLS.md` before any Compose code).

## 1. Owner order (2026-10-18, in the owner's words; never delete, only append)
"Look at my github projects wisdom tower academy app packed, build the admin dashboard natively part by part, currently it's web view from the Website"

Plain meaning: replace the website `/admin` page shown in a WebView by Jetpack Compose screens, one slice at a time. This reverses the old rule "admin stays on the website" in `NATIVE_APP_BRIEF.md` section 4 for the admin only; the owner's order wins. The WebView host stays as the fallback for every module that is not native yet (NATIVE_APP_BRIEF rule 8).

## 2. Architecture (decided in N1)
- Entry point unchanged: `AdminConsoleScreen(url, onClose, onLeaveToApp, modifier)` is still called by `MainActivity.kt` (patch `ADMIN-host`). It now lives in `ui/admin/AdminConsoleHost.kt`. No `MainActivity.kt` edit and no patch script were needed.
- `ui/admin/AdminConsoleScreen.kt`: the old WebView host, renamed `AdminWebSection` (internal). Used for every module whose id is not in `NATIVE_ADMIN_MODULE_IDS` (`ui/admin/AdminModules.kt`).
- `ui/admin/AdminHomeScreen.kt`: native home grid (3 stat tiles, 12 module tiles, search toggle, Live Academy and Supabase pills) and `AdminSectionBar`.
- `ui/admin/AdminModules.kt`: the 12 modules (ids, labels, aliases copied from `website-reference/src/app/admin/page.tsx`), `adminModuleForTab()` for deep links such as `/admin?tab=competitions`.
- `data/admin/AdminCounts.kt`: the 3 real counts, read with the signed-in admin's own token.
- Per module slice: add `ui/admin/<Module>Screen.kt` (+ a repository in `data/admin/`), add the id to `NATIVE_ADMIN_MODULE_IDS`, and add the branch in `AdminConsoleHost.kt`. The website panel stays reachable as fallback until the owner approves the native screen.

## 3. Data access (verified from `docs/SUPABASE_NOTES.md`, NOT yet tested from the app)
- Admin reads and writes go straight to Supabase REST with the admin's own access token (RLS uses `private.is_admin()`; the app button uses `am_i_admin()`). No secret key is needed or allowed in the app.
- Anything that needs a server secret must call a website API route (`website-reference/src/app/api/`) with the admin session, never a key: push sending (Firebase), SMS or e-mail, Appwrite file writes, any service-role operation. Each slice must audit its web panel first and list which calls are plain Supabase and which are API routes.
- Public repo: record table and column names only, never rows.

## 4. Modules and slices (one per chat, in this order; recommended order, the owner may change it)
| Slice | Module (website panel, size) | Notes |
|---|---|---|
| N1 | Shell: home grid, real counts, section bar, deep links | DONE in code `[~]`, see status |
| N2 | Inquiries (`InquiriesPanel`, 5 KB) | smallest panel: pilot for the shared native kit (record row, detail sheet, status chips, toast) |
| N3 | Payments (`PaymentsPanel`, 20.7 KB, plus `/admin/payments`) | approve/reject, receipts (public bucket links); audit whether approval needs an API route |
| N4 | Scholars and Users (`UsersPanel`, 28 KB) | list, search, inspect, edit |
| N5 | Overview (`AnalyticsPanel`, 23 KB) | stat tiles and charts (Canvas, no chart library) |
| N6 | Access Grants (10 KB), Content Locks (14 KB), Pricing and Catalog (13.7 KB) | |
| N7 | Push Broadcasts (`NotificationsPanel` 9 KB, composer v2 21 KB, block editor 10 KB, audience picker 10 KB, scheduled 9 KB) | sending needs the website API route |
| N8 | Competitions (`CompetitionsAdminPanel`, 33.6 KB) | follow `docs/COMPETITION_ADMIN_CHECKLIST.md` |
| N9 | Course Content (18 KB), Free Resources (25 KB) | file uploads need a file chooser (none exists in the app WebViews today) |
| N10 | Database and SQL Hub (29.5 KB) | probably stays web: arbitrary SQL needs a server route; decide with the owner |
| N11 | Regression pass and owner phone test | checklist in section 6 |

## 5. Native kit (grows with N2, reused by every slice; do not invent a second one)
Colours only from `ui/theme/Color.kt` tokens (they follow day and night mode). Gold icon chip (`WisdomAccentAmber`), cards `WisdomCard` with `WisdomBorderWhite` 1 dp border, pill buttons at least 44 dp high, body text at least 14 sp, labels at least 12 sp, nothing under 11 sp. Editors and confirmations are bottom sheets. One vertical scroller per screen, no horizontal scrolling. Material icons only if they exist (BUILD_PITFALLS rule d). Never show a number the database did not return (a failed read shows a dash).

## 6. Acceptance checklist (owner's phone)
1. Account > Admin Console opens the native grid between the app's top bar and bottom navigation; only the app's own top bar is above it.
2. The three counts and the badges match the website admin home; a failed read shows a dash, not a number.
3. Every one of the 12 tiles opens; native ones open natively, the rest open their website panel under a back bar; Back returns to the grid; Back on the grid closes the admin.
4. The search icon filters the tiles; the Refresh icon reloads the counts.
5. Deep links: a notification linking to `/admin?tab=competitions` opens Competitions directly, Back goes to the grid.
6. Scrolling is smooth in the grid; no sideways movement.
7. Student accounts still do not see the Admin Console button.

## Status (append only; newest last)
- 2026-10-18 | Claude (chat) | N1 written: native home grid, real counts, section bar, deep links, WebView fallback per module. Files: `ui/admin/AdminConsoleHost.kt` (NEW), `AdminHomeScreen.kt` (NEW), `AdminModules.kt` (NEW), `data/admin/AdminCounts.kt` (NEW), `AdminConsoleScreen.kt` (function renamed to `AdminWebSection`, internal, rest unchanged). `[~]` NOT compiled, NOT seen on a device. | NEXT: owner checks the CI run of this commit, then N2 (Inquiries).
