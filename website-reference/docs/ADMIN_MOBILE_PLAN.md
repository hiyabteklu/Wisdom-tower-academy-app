# ADMIN_MOBILE_PLAN.md (WEBSITE half): Admin Console redesign for the phone (LIVE TASK, owner order 2026-10-15)

> The MASTER plan is in the APP repo: `hiyabteklu/Wisdom-tower-academy-app-packed` -> `docs/ADMIN_MOBILE_PLAN.md` (owner order, hypotheses H1 to H6, target design, slice table, acceptance checklist). This file holds what a WEBSITE agent must know without opening the app repo. Keep both in step. This repo is PUBLIC: no secrets, emails or student names.
> This half was missing from the repo after the planning chat hit its quota (the app half was pushed in commit 2419bab); it was written and pushed in the next chat.

## 1. Owner order (2026-10-15, owner's words; append only)
1. "the admin dashboard in mobile view is terrible, it should be like the desktop view but in the mobile screen. I hate thin long verticals"
2. "redisign the ui to be suitable for our mobile view. No one is using admin dashboard in Website. So make it feel it was mode for the mobile."
3. "No horizontal long scrolling to find the categories (content, notification, etc.., make them fit in Mobile screen, compact ui and vertical scrolling only. Cuz the horizontal scrolling is what freeze the web view always in the app"
4. "Currently you can't even scroll reliably, it's freezing, so resolve any conflicting behaviors with the app. It should scroll smoothly."
5. "Currently it looks awkward overlay, why the apps bottom and top navigation disappeared, and it is just being cropped out the small texts etc..."
6. "Make the ui similar to the app and plan this in details on the repo (app and web) so any agent won't miss either"

Meaning: phone-first admin (desktop is NOT a goal), modules visible at a glance as a dashboard grid, ZERO horizontal scrolling anywhere in `/admin`, smooth scrolling, same look as the app (dark navy, rounded, pill buttons), no cropped or tiny text.

## 2. What the website code did BEFORE slice A1 `[verified]` (`src/app/admin/page.tsx`, one client component, 12 tabs)
- Own sticky `<header>` with `backdrop-blur-md`, an `animate-pulse` dot, back arrow, 'Live Academy', 'Supabase', 'Logout' (a second header next to the app's own bar).
- Tab ribbon = `overflow-x-auto` + inner `flex min-w-max` (the long horizontal scroller the owner hates), plus a 'Filter admin sections...' input.
- Workspace wrapped in `rounded-3xl ... shadow-2xl backdrop-blur-sm` (second blur layer).
- Hard-coded text 'All 10 modules operational' (wrong: 12 tabs; not a real check; forbidden by the fake-content rule).
- Every tab tap did `router.replace` + `history.replaceState` + a localStorage write.
- Access gate: `isAdminEmail(u.email)` in the page, `requireAdmin` (Bearer token) in the API routes, `private.is_admin()` in the database, `am_i_admin()` for the app button. DO NOT change any of these in this task.
- Panels live in `src/components/admin/` (bytes): Competitions 33.6k, Database Hub 29.5k, Users 28.0k, Free Resources 24.8k, Analytics 23.1k, Notification composer v2 21.1k, Payments 20.7k, Content 18.1k, Locks 13.9k, Catalog 13.7k, Access Grants 10.4k, Audience picker 9.9k, Notification block editor 10.1k, Notifications 9.1k, Scheduled notifications 9.1k, Inquiries 5.0k. Not yet audited for wide tables or horizontal scrollers.

## 3. Target design for the shell (decided; change only with the owner's OK)
- **Home (`/admin` with no `?tab=`):** compact strip with REAL counts only (Scholars, Pending payments, New inquiries; shown as a dash until loaded, never a made-up number), then a grid of module tiles: 3 columns from 340 px wide, 2 below. Icon on top, label under it (max 2 lines), small count badge where a real count exists. A search icon toggles the filter field (no wide always-visible input). Quiet footer row: 'Live Academy' and 'Supabase dashboard' (from the old header). 'Logout' is NOT in the admin: signing out is the Account screen's job.
- **Section view (`/admin?tab=<id>`):** one slim solid sticky row (back chevron, section title), then the panel full width. No ribbon, no own header, no blur, no pulse.
- **Deep links keep working:** `?tab=<id>` and every alias in `TAB_ALIASES` (notifications, push links and the competition checklist use them). Opening `?tab=` goes straight to the section; Back (chevron or phone Back) returns to the grid. Sections are opened with `history.pushState`, so the WebView's own back stack does the right thing.
- The last-opened tab is no longer restored from localStorage (a refresh on `/admin` shows the grid).

## 4. Rules for every panel (no exceptions; slices A5 to A16)
1. Zero horizontal scrolling: no `overflow-x-auto` strips, no `min-w-max`, no wide `<table>`. Tables become stacked cards (title row, then label/value pairs). Chips use `flex-wrap`, a segmented control or a select. Long ids and urls wrap or truncate.
2. Compact, not thin-and-long: 2-column field pairs for short fields, collapsible groups for rare fields, cards that show the key facts in 3 to 4 lines.
3. One vertical scroller: the page scrolls; only short bounded lists may scroll (max-height + `overscroll-behavior: contain`).
4. Performance: no `backdrop-filter`, no `animate-*` loops, no big shadows on scrolling content, no sticky blur. Glass look through colour and border only.
5. Readable: body at least 14 px, labels at least 12 px, touch targets at least 44 px, pill buttons, nothing under 11 px. Inputs at 14 px or more.
6. Safe area: respect `env(safe-area-inset-*)` and keep 16 px bottom breathing room.
7. No invented text, numbers or statuses.
8. Layout only: keep behaviour, API calls and admin permissions exactly as they are.
9. Before touching a panel, search it for `overflow-x`, `min-w-`, `<table`, `backdrop-`, `animate-` and log the hits.

## 5. Global CSS guard (slice A2, admin only)
`html, body { overflow-x: hidden; }` only AFTER the real overflow causes are fixed (never as the fix), `touch-action: pan-y` on the admin root, `overscroll-behavior-y: contain`, `-webkit-tap-highlight-color: transparent`, `word-break: break-word` on cards. Scope it to the admin so student pages are untouched.

## 6. Slices (website side; app slices A0, A3, A4 and A17 are in the app repo plan)
| Slice | What | Done when |
|---|---|---|
| A1 | New shell in `src/app/admin/page.tsx` only (grid home, section view, no ribbon/header/blur/pulse, fake status line removed, deep links kept). Panels untouched. | Vercel build green; ribbon gone; every module reachable by tap |
| A2 | Admin-only CSS guard (section 5) and the fix for any scroll problem proven by app slice A0 | no sideways overflow at 320, 360 and 412 px |
| A5 | Competitions panel | passes section 4 |
| A6 | Notifications (Push Broadcasts, composer v2, scheduled) | passes section 4 |
| A7 | Users | passes section 4 |
| A8 | Payments | passes section 4 |
| A9 | Overview (Analytics) | passes section 4 |
| A10 | Content: Academy folders (Grades 9 to 12, Entrance Questions, Freshman, Special packages) as a compact 2-column grid of folder cards, not 4 tall rows | passes section 4 |
| A11 to A16 | Free Resources, Locks, Catalog, Access Grants, Inquiries, Database Hub | each passes section 4 |

One slice per chat (free quota). Push to `main` in one commit per slice with a log entry. Vercel builds every push; a TypeScript error fails the deploy (only ESLint is ignored), so keep types clean: no local build is possible from the agent sandbox.

## 7. Status (append only; newest last)
- 2026-10-15 | Claude (chat) | App half of the plan pushed earlier (commit 2419bab); this website half was missing and is now pushed. Slice A1 written in the same session (see `docs/STATUS.md`). Not compiled.
