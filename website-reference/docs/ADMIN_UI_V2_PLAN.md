# ADMIN_UI_V2_PLAN.md — every Admin Console screen in the home-grid design (LIVE TASK, owner order 2026-10-16)

> COPY of the master in the APP repo `hiyabteklu/Wisdom-tower-academy-app-packed` -> `docs/ADMIN_UI_V2_PLAN.md`. If the two differ, the app-repo file wins; fix this one. Repo is PUBLIC: no secrets, no student names or emails. This plan EXTENDS `docs/ADMIN_MOBILE_PLAN.md` (slices A0 to A17, layout work, done in code). Rules: inspect first, focused changes, no invented text, label guesses "hypothesis", one commit per slice, log first.

## 1. Owner order (2026-10-16, in the owner's words)
"Anll vercel and github is green, and this is what things look like now. But as you said there is still problems left, the app scroll freezes on the Web view of this admin dashboard, also don't you think there is cropped out something on the top? or why things too close/overlapping/touching the top nav/header in the app, can't they have a little safe space. Also what you did here is fantastic. The admin dashboard you did great job only on the first page of the admin dashboard. But I need this kind of redesign in all of its layers, I mean when clicked, till the final destination also should be suitable for mobile. Same color plate and design language. Make the entire internal ui of the admin dashboard as cool as the image shown above. Push the plan and start execution."

Plain meaning: (a) fix the scroll freeze; (b) a little safe space under the app top bar, nothing touching or cut at the edge; (c) the home grid look (tiles, gold icon chips, big readable type, pills, dark cards) in EVERY layer: section landing, lists, detail views, editors, confirm dialogs, toasts, empty states; (d) same colours and language everywhere.

## 2. Root-cause notes (code reading 2026-10-16; NOT measured on a device)
- F8 (verified, code) `src/app/layout.tsx` wraps `/admin` in the student-site shell. `.site-atmosphere` is a fixed full-screen layer with three `filter: blur(90px)` glows; hidden only for the AI tutor. The admin root is opaque, so it was painted for nothing. HYPOTHESIS: costs frames while scrolling. Fixed in U0 (`display: none` in the admin layout).
- F9 (verified, code) `html { scroll-behavior: smooth }` is global; admin runs `window.scrollTo(0,0)` and the composer `scrollIntoView({behavior: "smooth"})`. HYPOTHESIS: fights touch fling. Fixed in U0 (`scroll-behavior: auto` in the admin layout).
- F10 (verified) other site components run on `/admin` (`GlobalFocusBar` 500 ms interval, `AuthProvider`, `OfflineSync`, `LiveRefresh`, `StructuralBackBridge`, `ServiceWorkerRegister`, `GlobalToolOverlay`). All light; not a proven cause; not changed.
- F11 (hypothesis, app repo, not fixed) Android WebView shares ONE renderer process, so the hidden main WebView may still run its injected scripts (300 ms intervals, `innerText` observers) on the same main thread. Candidate fix: `onPause()` on the MAIN WebView while the admin is open, `onResume()` on close (per-WebView; never `pauseTimers()`, it is global). Slice U9.
- F12 (verified) `touch-action: pan-y` (website commit `abbbdf5`) was live when the owner reported the freeze, so it is not the cure. If scrolling still freezes after U0, remove that one line from `src/app/admin/layout.tsx` first.
- Top edge: the admin WebView sits between the app top bar and bottom navigation; its top edge is a hard clip. U0 adds a 14 px fixed fade; U2 adds 16 px top padding on the home grid and the section view.

## 3. Design language (read from the home grid in `src/app/admin/page.tsx`; do not invent a palette)
- Page `bg-wisdom-dark` (`#0a101c`). Cards `rounded-2xl border border-white/10 bg-wisdom-card` (`#1c283c`). Muted text `text-wisdom-muted`. No `backdrop-filter`, no animation loops, no big shadows.
- Icon chip `h-10 w-10 rounded-xl border border-amber-400/20 bg-amber-500/10 text-amber-300` (GOLD). Cyan stays for the primary action pill and links.
- Tile (navigation inside a section): the card, icon chip on top, 13 px semibold label, optional count badge top right.
- Pills `min-h-[44px] rounded-full`: ghost `border-white/10 bg-wisdom-card text-wisdom-muted`, primary `bg-cyan-500 text-wisdom-dark font-bold`, gold `border-amber-400/30 bg-amber-500/15 text-amber-200`, danger `border-rose-400/30 bg-rose-500/15 text-rose-200`.
- Badges `rounded-full border px-1.5 py-0.5 text-[11px] font-extrabold tabular-nums` with a tone (neutral, gold, cyan, green, red).
- Type: titles 16 to 18 px bold, body 14 px, labels 12 px, nothing under 11 px. Touch targets at least 44 px.
- Editors and confirmations are BOTTOM SHEETS (rounded top corners, dim backdrop WITHOUT blur, internal vertical scroll with `overscroll-behavior: contain`), not centred pop-ups. Toasts are one slim bar at the bottom.
- Safe space: 16 px side gutters, 16 px under the app bar at scroll 0, 24 px at the bottom.

## 4. The shared kit: `src/components/admin/ui.tsx`
`AdminPageHeader`, `AdminCard`, `AdminStat` / `AdminStatRow`, `AdminTile` / `AdminTileGrid`, `AdminRow` (tappable record card), `AdminPill`, `AdminChips`, `AdminSearch`, `AdminField` with `AdminInput` / `AdminTextarea` / `AdminSelect`, `AdminSheet`, `AdminBadge`, `AdminEmpty`, `AdminToast`, `AdminKeyValue`, `AdminSkeleton` (static bars). Rules for the kit and every panel: no `<header>`, `<footer>` or `<nav>` (app-mode CSS hides `header` and `footer`); no raw `'` `"` `>` `}` characters in JSX text (ESLint `react/no-unescaped-entities` fails the Vercel build); no `animate-*`, no `backdrop-*`, no `overflow-x-*`.

## 5. Slices (each pushed alone, logged in the status list)
U0 shell fixes and the top fade; U1 the kit; U2 `page.tsx` shell (new section bar, top padding, no box-in-a-box); U3 Overview (`AnalyticsPanel`); U4 Push Broadcasts (`NotificationsPanel`, `NotificationComposerV2`, `NotificationBlockEditor`, `NotificationAudiencePicker`, `ScheduledNotificationsPanel`); U5 Competitions (`CompetitionsAdminPanel`, `EntryFlagsInfo`); U6 Scholars and Users (`UsersPanel` incl. sheets); U7 Payments (`PaymentsPanel`, `src/app/admin/payments/page.tsx`); U8 Access Grants, Content Locks, Pricing and Catalog; U9 Course Content, Free Resources, Inquiries, Database Hub (+ app patch for F11 if freezes remain); U10 regression pass and the owner's phone test.

Per panel, three layers down to the last screen: (1) landing: header, stat tiles, filter chips, search; (2) records: `AdminRow` cards with the gold chip, status badge, one-line meta; (3) detail and edit: `AdminSheet` with grouped fields and pill actions, confirm sheet for destructive actions. Behaviour, data calls, permissions and texts are NOT changed.

## 6. Working rules
- npm cannot reach its registry from the chat sandbox, so a real `next build` is not possible there. Gate before every push: a shim type check (syntax, unknown names, unused imports, kit prop mismatches; see `docs/SESSION_2026-10-16_UI_A.md` in the app repo) plus a grep audit for the forbidden patterns in section 4. Vercel is the real compiler: read its result first; if red, fix first.
- Do not edit `src/app/globals.css` (student pages share it); admin-only CSS goes in `src/app/admin/layout.tsx`.
- Resend whole panel files only after reading them fully; keep every handler, state variable and Supabase call as it was.

## 7. Acceptance checklist (owner's phone)
1. A visible gap under the app top bar; rows fade at the top edge instead of being cut.
2. 30 seconds of scrolling in Overview, Scholars and Users, Payments, Push Broadcasts: no freeze.
3. Every module in the home-grid language (gold chips, dark cards, pills).
4. One editor and one destructive action in three modules open as bottom sheets and close with Back.
5. No sideways movement; body text at least 14 px.
6. Deep links still work: `/admin?tab=competitions`, `/admin?tab=notifications`.

## Status (append only)
- 2026-10-16 | Claude (chat) | Plan written in both repos; U0 pushed (this commit) | NOTES: F8 to F12 are code reading only.
