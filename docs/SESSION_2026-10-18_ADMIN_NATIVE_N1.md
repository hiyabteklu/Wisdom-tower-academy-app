# SESSION 2026-10-18 — ADMIN-native N1 (native Admin Console shell)

> Repo is PUBLIC: no secrets, no student data. Owner order: build the admin dashboard natively, part by part (it was the website page in a WebView). Plan: `docs/ADMIN_NATIVE_PLAN.md`.

## Read before coding
`NATIVE_APP_BRIEF.md`, `HANDOFF.md`, `docs/BUILD_PITFALLS.md`, `docs/ADMIN_MOBILE_PLAN.md`, `docs/ADMIN_UI_V2_PLAN.md`, `docs/SUPABASE_NOTES.md`, `ui/admin/AdminConsoleScreen.kt`, `data/admin/AdminAccess.kt`, `ui/theme/Color.kt`, and the website `src/app/admin/page.tsx` (from `website-reference/`). Not read: the 12 panel files (each is audited in its own slice), `MainActivity.kt` (not needed, see below).

## What was pushed (one commit)
1. NEW `ui/admin/AdminConsoleHost.kt`: `AdminConsoleScreen(url, onClose, onLeaveToApp, modifier)` with the SAME signature the `ADMIN-host` call site in `MainActivity.kt` already uses, so no patch script and no `MainActivity.kt` change. Shows the native grid; a module opens under a native section bar (back arrow, gold chip, title); Back returns to the grid, Back on the grid closes. A `?tab=<id or alias>` in the url opens that module directly.
2. NEW `ui/admin/AdminHomeScreen.kt`: header, search toggle (placeholder text copied from the website), refresh, 3 stat tiles, 12 module tiles in 3 columns (2 below 340 dp), badges only above zero, Live Academy and Supabase pills. One `LazyColumn`, no horizontal scrolling.
3. NEW `ui/admin/AdminModules.kt`: the 12 modules and their aliases copied from the website's `academyTabs` and `TAB_ALIASES`; `NATIVE_ADMIN_MODULE_IDS` (empty for now).
4. NEW `data/admin/AdminCounts.kt`: pending payments, new inquiries and scholars via PostgREST HEAD with `Prefer: count=exact` and the admin's own token (refresh and one retry on 401, like `AdminAccess.kt`). A failed read gives null and the screen shows a dash.
5. EDIT `ui/admin/AdminConsoleScreen.kt`: only the function name changed (`AdminConsoleScreen` became `internal fun AdminWebSection`) plus a header note. The WebView code is otherwise byte for byte the same.
6. NEW `docs/ADMIN_NATIVE_PLAN.md` (plan, slices N1 to N11, native kit rules, checklist) and this file.

## Evidence and limits
- `[~]` Written, NOT compiled, NOT run on a device. Check the "Build Production AAB & APK" Action for this commit first.
- Pitfall checks done by reading: every `Modifier` extension used is imported (`background`, `border`, `clickable`, `clip`, `alpha`, `padding`, `size`, `height`, `width`, `fillMaxSize`, `fillMaxWidth`, `focusRequester`); `items` is imported from `androidx.compose.foundation.lazy`; no `rememberRipple`; no trailing lambda on a function whose last parameter is not the lambda; no `@Composable` call directly inside a `LazyColumn` builder body (only inside `item` and `items` lambdas); icons used: `Dashboard, Notifications, EmojiEvents, People, CreditCard, VpnKey, Lock, Inventory2, LocalLibrary, Inbox, Storage, Public, Search, Refresh` (filled) and `AutoMirrored.Filled.MenuBook, ArrowBack`. HYPOTHESIS: all of these exist in the project's `material-icons-extended`; `EmojiEvents`, `Notifications`, `Refresh` and `AutoMirrored MenuBook` are already used elsewhere in the app, the rest were NOT grepped. If CI names an unresolved icon, swap it for a known one and log it in `docs/BUILD_PITFALLS.md`.
- HYPOTHESIS: PostgREST answers a HEAD with `Prefer: count=exact` with `Content-Range: 0-N/total` (or `*/0`), as supabase-js `head: true` relies on. Not tested from the app. If a count shows a dash on the phone, this is the first thing to check (and whether RLS lets the admin read `orders`, `inquiries`, `profiles`, which `docs/SUPABASE_NOTES.md` says it does).
- HYPOTHESIS: the website admin still accepts `/admin?tab=<id>` as the section view (it did in `website-reference`, which may be older than the live site).
- The native grid sits on a solid `WisdomBackground` so the screen under the overlay cannot show through; the picture backdrop does not show here (same as the old WebView).
- `AdminWebSection` keeps its own Back handling; because it is only composed inside a module, the grid's Back handler (enabled only on the grid) and the module's never compete.

## What the owner should test
The checklist in section 6 of `docs/ADMIN_NATIVE_PLAN.md`. Most important: the grid appears, the three numbers are real, every tile opens, Back works at both levels.

## NEXT
N2: Inquiries as the first fully native module (pilot of the shared kit). Audit `InquiriesPanel` first.
