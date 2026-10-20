# SESSION 2026-10-20 (admin B1 / B2) HANDOFF TO THE NEXT AGENT

> Repo is PUBLIC: no secrets, no student data. Written because the owner's chat quota ran out. Read order stays: `docs/OWNER_RULE_PLAN_FIRST_PUSH_TO_MAIN.md`, `docs/ADMIN_DASHBOARD_NEXT.md` (the working plan), `docs/ADMIN_AUDIT.md`, `docs/PROGRESS_LOG.md`, `docs/BUILD_PITFALLS.md`.

## What this chat did (all pushed to `main`)
- B1a: `AdminDetailPanel` added to `AdminKit.kt`; the Locks screen uses it. CI #849 and #850 GREEN (owner showed the Actions page). Not tested on a phone.
- B1b: Grants screen on the same panel (package picker panel, grant detail panel with Revoke / Revoke all fixed at the bottom). Commit message starts "Admin B1b (Grants)". CI RESULT NOT SEEN (the agent had no tool to read Actions). FIRST THING TO DO: ask the owner for a screenshot of the Actions list, or read it yourself; if red, fix it before anything else and log it in `docs/BUILD_PITFALLS.md`. Only risk areas I know of: `AccessGrantsScreen.kt` (new `DetailLine`, `GrantRow`, smart casts on `detailGrant.note` / `.grantedBy`, both are `val` in the data class `AccessGrantItem`, so they should compile).
- B2: read-only audit `docs/ADMIN_AUDIT.md` (Competitions, Inquiries, Payments, Catalog, Users read; 7 files not read).
- Docs updated: `docs/ADMIN_DASHBOARD_NEXT.md`, `docs/PROGRESS_LOG.md`.

## The panel API you must reuse (do not write another one)
`AdminDetailPanel(title: String, onBack: () -> Unit, modifier = Modifier, subtitle: String? = null, actions: (@Composable () -> Unit)? = null, content: @Composable ColumnScope.() -> Unit)` in `ui/admin/AdminKit.kt`.
- Draw it as the LAST child of the `Box` that holds the list (before the toast). Keep the list composed underneath so its scroll position and typed text survive.
- It has its own `BackHandler`, registered after the screen's and the host's, so System Back closes the panel first. It swallows taps.
- `actions` is the bar fixed at the bottom (main button). Content scrolls on its own and starts at the top.
- Pattern to copy: `LocksScreen.kt` (`LockTarget`, `LockRow`, `detail` state) and `AccessGrantsScreen.kt` (`pickerOpen`, `grantDetail`, `GrantRow`, `DetailLine`). Lift the toast with `bottom = 88.dp` while a panel with a bottom bar is open.

## NEXT SLICE: B3a Competitions (create form + exam picker as panels). One slice per chat.
File: `ui/admin/CompetitionsScreen.kt` (about 900 lines; resend the whole file with `push_files`, there is no patch tool for it, the file is small enough).
1. The "Create a Competition" card keeps only the intro text and the `New competition` button. Tapping it sets `createOpen = true` and opens a panel "New competition" (subtitle "Competition") that holds the 12 fields exactly as they are now; `Create competition` is the `actions` button fixed at the bottom (keep `submitCreate()`, `createError()`, `confirmCreate` dialog and every `CompetitionsRepository` call unchanged; keep the `rememberSaveable` form state so a typed form survives).
2. The exam picker (`pickerOpen`, `pickerQuery`) becomes a SECOND panel over the create panel (opened from the "Exam from the exam hub" field): search box plus the exam rows, tap one = select and close the picker only. Draw the picker panel after the create panel so its Back handler wins.
3. After a successful create: `createOpen = false` (already done in `doCreate`) so the panel closes.
4. Do NOT touch contest actions, reschedule, entries or flags in this slice (those are B3b and B3c: compact contest row + detail panel with the actions; reschedule as a small panel with Save fixed; entries as a panel with a search box and a count; flag details stay inside the entry).
5. Keep: Ethiopia-time handling, the `Switch` toggles, `AdminConfirmDialog` / `AlertDialog` flows, `AdminRose`, texts. Layout change only. No new colours (the literal `CompOrange` is a C1 finding, leave it).
6. Phone test to write into `docs/ADMIN_DASHBOARD_NEXT.md`: tap New competition = panel opens at the top; pick an exam = back to the form with the other fields still filled; Back closes picker first, then the form; Create is always visible at the bottom.

## Slices after that (order in `docs/ADMIN_AUDIT.md`)
B3b/B3c Competitions rest, B3d Inquiries (detail panel + search), B3e Payments (Approve / Reject fixed, logic unchanged), B3f Catalog (editor in the panel, fixed pink to `AdminRose`), B3g Users. Also B2b (read the 7 unread admin files), C1 (looks audit), A4 (owner runs `docs/A4_TEST_MATRIX.md` on the phone, results to `docs/A4_RESULTS.md`).

## Rules and traps that cost time (from this chat)
- The agent sandbox cannot compile. Every commit message ends with `not compiled` until the owner shows a green Actions run. Re-read each import: every `Modifier.xxx` needs its own import; trailing-lambda only on the LAST parameter (use named `onClick = {...}`); no `/*` inside any comment (Kotlin nests comments); only real Material icons (`Icons.AutoMirrored.Filled.ArrowBack` and `KeyboardArrowRight` exist and are used).
- `search_code` on GitHub was unreliable (stale index). Read files directly with `get_file_contents`.
- `AdminToast` and `AdminStatusBadge` are defined in `InquiriesScreen.kt` and used by every module: never delete them; move to `AdminKit.kt` first if Inquiries is rewritten.
- Day / Night: use kit colours (`AdminRose`, theme tokens), not literals.
- Talk to the owner in plain words, short. He is not a coder, on a free tier, rotates accounts. State what changed and what to test on the phone, nothing more.
- Owner's Supabase connector: one project, `Wisdom tower academy` (id `amieczmpqrvavzjikbdi`, eu-central-1). Record any live database change (migration name + one line) in the plan file. This chat made NO database changes.
