# ADMIN_AUDIT.md: "expands below", long lists, tall cards (B2, 2026-10-20)

> Repo is PUBLIC: no secrets, no student data. READ-ONLY audit: no code was changed in this slice.
> Goal (docs/PLAN_2026-10-20_ADMIN_LOCKS_UX_THEME.md, B2): find every place where a tap opens something BELOW the tapped row, every
> long list without a search box, and every tall card that hides its actions far down. Fix pattern = `AdminDetailPanel` (AdminKit.kt, B1a).
> Already converted: Locks (B1a) and Access grants (B1b).

## Files read in this slice (all in `app/src/main/java/com/wisdomtower/academy/ui/admin/`)
`UsersScreen.kt`, `PaymentsScreen.kt`, `InquiriesScreen.kt`, `CatalogScreen.kt`, `CompetitionsScreen.kt`.

## NOT read yet (finish in the next audit slice, B2b)
`BroadcastsScreen.kt`, `BroadcastComposerScreen.kt`, `CourseContentScreen.kt`, `FreeResourcesScreen.kt`, `OverviewScreen.kt`, `DatabaseStatusScreen.kt`,
`AdminHomeScreen.kt`. (The plan says Course content already replaces its list with an editor; that is the earlier, unverified note, not re-checked here.)

## Findings per module `[verified]` = read in the code today

### Competitions (`CompetitionsScreen.kt`) WORST, convert first
- `[verified]` Create form opens INLINE inside its card (`createOpen`): 12 fields, Close / Create at the very bottom.
- `[verified]` The exam picker opens INLINE inside the form (`pickerOpen`), a search box plus up to 30 text rows inside a bordered box (a nested list inside the page).
- `[verified]` Each competition card holds up to 7 stacked action buttons (Hide/Show, Pause/Resume, Start now, Reschedule, Cancel contest, Entries), 2 per row, so a card is about 10 lines tall.
- `[verified]` Reschedule editor (`RescheduleEditor`) opens INLINE below the buttons of that card.
- `[verified]` Entries (`EntriesPanel`) open INLINE below the card: ALL ranked entries of the contest in one column, each with its own Disqualify button and a flag detail that expands again inline (`flagsOpenId`). No search, no limit. Can be hundreds of rows inside one card.
- `[verified]` The list of contests has no search or status filter.
- Fix: compact row (title, phase badge, visible/hidden, start time) that opens a detail panel with the actions; Create = its own panel (Create fixed at the bottom); exam picker = its own panel with the search box; Entries = its own panel with a search box and a count; Reschedule = a small panel with Save fixed at the bottom. Keep every confirm dialog and every call to `CompetitionsRepository` unchanged.

### Inquiries (`InquiriesScreen.kt`)
- `[verified]` Tap a card = `expandedId` opens the four status buttons INSIDE the card (below the message). The message is cut to 3 lines until expanded, and the expanded state is `rememberSaveable`.
- `[verified]` Up to 200 rows, status chips but NO search box.
- `[verified]` Own private copies of `FilterChip`, `MessageCard` and `formatWhen` (the kit has `AdminChoiceChip`, `AdminMessageCard`, `adminFormatWhen`). `AdminToast` and `AdminStatusBadge` are DEFINED here and used by every module: do not delete them, move them to AdminKit.kt first.
- Fix: compact row (name, e-mail, 1 line of the message, status badge) opens a detail panel: full message, the status buttons fixed at the bottom. Add `AdminSearchBox`.

### Payments (`PaymentsScreen.kt`)
- `[verified]` Nothing expands inline. But every order is a tall card (about 10 lines: id, status, package, amount and method, student, phone, e-mail, reference, note, date, receipt, then Approve / Reject) and the main actions sit at the bottom of the card.
- `[verified]` Search and status / method chips exist (good). Approve and Reject already ask for confirmation (keep).
- `[verified]` Own `PaymentsMessage` (duplicate of `AdminMessageCard`).
- Fix: compact row (student, package, amount, status badge) opens a detail panel with all the facts and `View Receipt`, with Approve and Reject FIXED at the bottom. Money related: change layout only, never the approve / reject logic.

### Catalog (`CatalogScreen.kt`)
- `[verified]` The editor REPLACES the list (not below, good) but: (a) the list has no hoisted scroll state, so Back returns to the TOP of the list; (b) the editor is a very long form (12 fields, a 140 dp and a 100 dp text area) with Cancel / Save at the very bottom; (c) it draws its own back arrow header.
- `[verified]` `RoseTone = Color(0xFFFB7185)` is a FIXED pink (the kit's `AdminRose` is day / night aware). This is a C1 (looks) finding.
- Fix: editor in `AdminDetailPanel` with `Save package` fixed at the bottom; the list stays composed underneath (scroll position kept); `RoseTone` becomes `AdminRose`.

### Users (`UsersScreen.kt`)
- `[verified]` Already the right idea: Inspect and Grant views REPLACE the list, the list scroll state is hoisted (`listState`), Back handlers are in place. Search and two sets of filter chips exist.
- `[verified]` Differences from the kit pattern: own `ViewHeader` (a second copy of the back header); the action buttons (Close, Grant New Package, Confirm) are at the END of a long scrolling list, not fixed; the grant view lists every package as a full row above the buttons.
- Fix (low priority): move Inspect and Grant onto `AdminDetailPanel` so the main button is fixed; delete `ViewHeader`.

## Long lists without a search box `[verified]`
- Inquiries (up to 200 rows), Competitions (all contests), Competition entries (all entries of a contest), Locks hub materials (all materials of a hub; the Locks screen lists them all in one column).
- Have search: Users, Payments, Access grants, the exam picker in Competitions.

## Small things found on the way (do not fix in the same slice as a conversion)
- Duplicated helpers: `FilterChip`, `MessageCard`, `PaymentsMessage`, `formatWhen`, `ViewHeader`. Replace them by the kit versions one module at a time (C2 / B3).
- Fixed colours: `CatalogScreen.kt` `RoseTone` (FB7185); `CompetitionsScreen.kt` `CompOrange` (FB923C, a literal: the kit darkens literals in day mode only inside `adminTone`, the plain text uses of it are not covered). List for `docs/ADMIN_LOOKS_AUDIT.md` (C1).

## Recommended order (one slice per chat, owner uses the first two most)
1. B3a Competitions: Create + exam picker panels (the form is the biggest pain), then B3b contest detail + reschedule, then B3c Entries panel with search.
2. B3d Inquiries: detail panel + search.
3. B3e Payments: detail panel with Approve / Reject fixed.
4. B3f Catalog: editor in the panel, scroll position kept.
5. B3g Users (optional), then the modules not read yet (B2b first).
Rule for every slice: reuse `AdminDetailPanel`, keep lock / permission / money behaviour unchanged, push to `main`, update `docs/ADMIN_DASHBOARD_NEXT.md` and `docs/PROGRESS_LOG.md` in the same commit, say "not compiled" until the Actions build is green.
