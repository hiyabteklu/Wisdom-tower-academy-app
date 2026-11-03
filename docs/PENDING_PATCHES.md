# PENDING_PATCHES.md — owner report, session log and exact edits (status checked 2026-10-12)

> READ THIS RIGHT AFTER `STATUS.md`. Why this file exists: `MainActivity.kt` is 244 KB and `STATUS.md` is 38 KB, and the chat tools can only replace whole files, so edits to them are made by patch scripts in `scripts/`, run by GitHub > Actions > **Apply pending patches** (workflow file `.github/workflows/apply-pending-patches.yml`, installed). Method: `docs/EDITING_LARGE_FILES.md`.
> Repo is PUBLIC: no secrets, no personal data.

## PATCH STATUS TABLE (checked 2026-10-12 against pull requests, branches and a code search of `main`)

| Patch | Script | What it does | Status |
|---|---|---|---|
| P1 to P3 | `apply_pending_patches.py` | hub opening, notification bell, refresh button | APPLIED (PR #1 merged 2026-10-11) |
| Guides screen | `patch_guides_screen.py` | native Guides screen entries | APPLIED (PR #2 merged) |
| P5 | `patch_ece_root_route.py` | ECE root link opens the native ECE screen (offline) | APPLIED (PR #3 merged 2026-10-11) |
| P6 | `patch_guides_universities_filters.py` | Universities guide: "In-Depth Guides" chip, "All Ethiopian Regions" chip, "Detailed Guides" filter | **RUN, NOT MERGED**: branch `fix/guides-universities-filters` (bot commit `77e2a1f`) exists, no pull request yet; `main` does not contain it (code search for `detailedOnly` and "In-Depth Guides" found nothing). Owner: open a pull request from that branch to `main`, check the build is green, merge |
| P7 | `patch_native_tutor_entries.py` | native AI tutor entries | APPLIED (PR #4 merged 2026-10-12) |
| P8 | `patch_native_calculator_entries.py` | native calculator entries | APPLIED (PR #5 merged 2026-10-12) |
| P9 | `patch_native_timer_entries.py` | native Focus Timer entries and shell ticking | APPLIED (PR #6 merged 2026-10-12) |
| P10 | `patch_native_planner_entries.py` | native Study Planner entries | APPLIED (PR #7 merged 2026-10-12) |
| P16 | `patch_u1_global_tool_overlays.py` | U1 Option A: global drawer and study tool overlays at zIndex 120f; drawer screens at zIndex 25f | APPLIED locally in MainActivity.kt |
| P17 | `patch_t1_tool_overlay_back.py` | T1: tool overlay back navigation and phone Back button priority fix | APPLIED locally in MainActivity.kt |
| U2-FIX1 | `patch_app_update_u2_lifecycle_scope.py` | Fix unresolved lifecycleScope compile error in updater resume callback by importing LifecycleOwner extension and coroutine launch | WRITTEN, OWNER RUN REQUIRED on branch `fix/update-u2-lifecycle-scope`; build/test, then PR merge |

Helper scripts (not patches): `generate_catalog_snapshot.py`, `generate_icons.py`, `pack_mainactivity.py`, `restore_mainactivity.py` (run by the build, keeps a valid `MainActivity.kt` on disk).

Not in this repo: `WEBSITE_CHANGES.md` sections 1 to 3 (website login fix and others) are NOT applied to the website repo; they need owner approval and the Supabase "Confirm email" answer.

Correction line (2026-10-12): the older text of this file said P5 was "WRITTEN, NOT YET APPLIED"; that was true when written, but PR #3 was merged the same day.

## A. OWNER REPORT 2026-10-11 (from the phone, in the owner's words; copy to STATUS.md section A)

- 2026-10-11 — "notification, refresh buttons doesn't work, comes several minutes later after click."
- 2026-10-11 — "cross linking is not working, example can't open courses from packages, learning or home, it only works from learning section sometimes."
- 2026-10-11 — "there are fabricated pages, log your result, manual input + face tracker... etc." (screenshot of the study workspace: Performance Tracker / Log Score / Recent Activity & Drills / Learning Hubs subtitle). "Everywhere like this it comes from nothing, fabricated." The owner's point 4 was never sent (blank); the "face tracker" was NOT found in the app code (hypothesis: a page of the website inside the WebView). Ask the owner for a screenshot of it.
- 2026-10-11 — "Continue with what's important first."
- 2026-10-11 — Owner chose to make `MainActivity.kt` smaller (split into several files) so any tool can edit it; agreed to a GitHub-Action-based patching method.
- 2026-10-11 — Owner (after the ECE report): "document this method, so any future agent won't say I can't push to big file" → `docs/EDITING_LARGE_FILES.md`.

## B. SESSION LOG entry (copy to STATUS.md section C)

- 2026-10-11 | Claude (chat, owner on free plan) | DONE: S12 (new item, owner report above). (a) commit `8fb3e15` `NotificationRepository.kt`: removed the invented "Welcome… All content is unlocked" notification (it also made the unread dot always red), added `isRefreshing` / `lastError` flows, URL-encoded the query, a failed or empty reply is no longer silent. (b) commit `8cef475` `StudyWorkspaceScreen.kt` resent whole: removed the invented "Performance Tracker", "Log Score" dialog (scores were only in local SharedPreferences `wt_study_history`), "Recent Activity & Drills", the hub subtitle "All 6 academic study formats with verified curriculum coverage", and the wrong grade labels. The website has NO manual score entry: `AcademicResultSaver.tsx` is "Progress Tracker" and reads real rows from Supabase `academic_results` + `getScopeStats`. Kept the header, a "Learning Hubs" label (website wording) and the 6 hub cards (names and descriptions verified equal to `website-reference/src/data/academy.ts`). (c) commits `5186ac7`, `fe75b0a` `docs/PENDING_PATCHES.md` (this file), `e049fd3` / `4f2dfd9` pointers in `CLAUDE.md` / `AGENTS.md`. (d) commit `4df5de5` `scripts/apply_pending_patches.py` applies P1 to P3 mechanically (tested on a copy of the real `MainActivity.kt`: applies, second run changes nothing). Root causes found by reading `MainActivity.kt`: see P1 to P3 | LEFT: run the patch script (P1 to P3 fix cross-linking, the bell and the refresh button) [DONE since: PR #1]; then split `MainActivity.kt` (plan: JS constants lines ~231-770 to `WebScripts.kt`, bottom nav/loaders/dialogs lines ~3871-4541 to `ShellComponents.kt`, then cut `MainScreen` (lines ~950-3870) into smaller composables, one step per commit with a green build in between); S4 (d) rest: `GuidesData.kt`, Home/Settings wording, `docs/FIDELITY_AUDIT.md`; native real "Progress Tracker" only if the owner wants it (read `docs/SUPABASE_NOTES.md` for the `academic_results` policies first); hub cards on the website also show an item count (`ResourceHubGrid.tsx` `hubCounts`), the native cards do not | TEST: owner should check that the GitHub Action build is green after `8cef475` (also covers everything uncompiled before). On a phone: Packages > Grade 9 > Open Study Hubs: only the header, "LEARNING HUBS" and 6 hub cards; no Performance Tracker; after P1: tapping a hub opens it; after P2/P3: bell shows the notifications page, refresh works on every tab | NOTES/UNSURE: not compiled. `StudyHistoryRecord`, `RecordScoreDialog`, `StudyHistoryItemRow`, `getGradeLabel` were deleted (grep showed no other user). Old scores users typed are still in the phone's SharedPreferences `wt_study_history` but are no longer shown (harmless). I could not append to `STATUS.md` (38 KB, whole-file replace only). P3 in the script does NOT call `AuthStateManager.refreshIfNeeded`: it is a `suspend` function and cannot be called from a button click without a coroutine scope.

## P1. Opening a course/hub from a package or from Learning does nothing (cross-linking)  — APPLIED in `main` (PR #1)

Cause (verified in code): `StudyWorkspaceScreen` is drawn at `zIndex(18f)` over everything. Its `onOpenHub` sets `activeStudyUrl` and calls `navigateTo(hubUrl, 1)` but never closes the workspace (`activeWorkspace`) or the package page (`selectedPackage`). `navigateTo` does not clear them either. So the website hub loads in the WebView UNDER the workspace overlay: the tap looks dead, and it only "works" when something else happens to clear the overlay.

Find (about line 3017):
```kotlin
onOpenHub = { _, hubUrl ->
    activeStudyUrl = hubUrl
    navigateTo(hubUrl, 1)
},
```
Replace with:
```kotlin
onOpenHub = { _, hubUrl ->
    activeWorkspace = null
    selectedPackage = null
    activeStudyUrl = hubUrl
    navigateTo(hubUrl, 1)
},
```
Side effect to know: Back from the hub now goes to the Learning tab, not back to the package page (hypothesis: acceptable; if the owner wants back-to-package, keep `selectedPackage` and add a back-handler step).

## P2. Notification bell opens a page nobody sees  — APPLIED in `main` (PR #1)

Cause (verified in code): the bell calls `navigateTo(".../notifications", null)`. `tabIndexForUrl` returns the CURRENT tab for any `/notifications` URL, and no `activeStudyUrl` is set, so the WebView loads the page hidden under the native screen (zIndex 10). It only becomes visible later when the user switches tabs (the "comes several minutes later" report).

Find (the bell `onClick`, about line 1921):
```kotlin
requestNotificationPermission()
NotificationRepository.markAllRead()
navigateTo("https://www.wisdom-tower-academy.live/notifications", null)
```
Replace with:
```kotlin
requestNotificationPermission()
NotificationRepository.markAllRead()
val notifUrl = "https://www.wisdom-tower-academy.live/notifications"
resetOverlays()
activeStudyUrl = notifUrl
navigateTo(notifUrl, 1)
```
(`resetOverlays` and `activeStudyUrl` are declared above the top bar in the same composable; if the compiler says otherwise, move this lambda below them.)

## P3. Refresh button refreshes a hidden page  — APPLIED in `main` (PR #1)

Cause (verified in code): the top-bar Refresh button only reloads the hidden WebView (`HARD_REFRESH_JS` + `wv.reload()`). On Home, Learning, Packages, Account and Settings the native screen covers it, so nothing visible changes, and native data (notifications, catalog) is never refreshed.

In the Refresh `IconButton` `onClick` (about line 1872), right after `view.performHapticFeedback(...)`, insert (the script does exactly this):
```kotlin
if (!(selectedIndex == 1 && activeStudyUrl != null)) {
    NotificationRepository.refreshNotifications(currentUser?.id, currentUser?.email)
    AcademyRepository.refreshCatalog()
    Toast.makeText(context, "Refreshing…", Toast.LENGTH_SHORT).show()
    return@IconButton
}
```
`Toast`, `AcademyRepository` and `NotificationRepository` are already imported in `MainActivity.kt`. Once the notifications screen is native, show `NotificationRepository.isRefreshing` / `lastError` there (added in commit `8fb3e15`). Token refresh is NOT included on purpose (`AuthStateManager.refreshIfNeeded` is `suspend`).

## P4. Not patched, to check later (hypothesis)

`tabIndexForUrl` sends every path containing "academy" or "learning" to tab 1 (Learning). `handleGlobalNav` checks package paths first, so package links are fine, but any other `/academy/...` link opened through `navigateTo` lands on the Learning tab. Test after P1 to P3 before changing it.

## P5. ECE root link opens the website (online only) from every entry point except the Home card  — APPLIED in `main` (PR #3, merged 2026-10-11)

Owner report 2026-10-11: ECE is the only package that does not work offline. Cause (verified in code): `handleGlobalNav` in `MainActivity.kt` (about line 1664) only matches catalog package paths. The website ECE root `/academy/special-packages/electrical-computer-engineering` is not one, so it falls to the last branch and opens the website page in the WebView (needs internet). The Home card was already fixed by pointing it at the catalog path `.../sem-1` (commit `22087969`, `HomeScreen.kt`); other entry points (drawer, guides, deep links, anything that calls `handleGlobalNav` with the root path) still went online.

Fix: in the `catalogPackages.find { ... }` of `handleGlobalNav`, after the line `it.path.equals(pathPart, ignoreCase = true) ||` add
```kotlin
(pathPart.trimEnd('/').equals("/academy/special-packages/electrical-computer-engineering", ignoreCase = true) && it.id == "ece-y3-sem-1") ||
```
so the root opens package `ece-y3-sem-1`, the native screen that lists Semester 1 and Semester 2 (all 14 courses) and the six hub cards.

Script: `scripts/patch_ece_root_route.py` (commit `598aa2e`).

## P6. Universities guide gaps  — RUN, NOT MERGED (branch `fix/guides-universities-filters`)

Script `scripts/patch_guides_universities_filters.py`. Adds to the native Universities guide the three header chips ("N Institutions", "M In-Depth Guides", "All Ethiopian Regions") and a "Detailed Guides" filter button, as on `website-reference/src/app/academy/universities/page.tsx`. Edits `GuidesData.kt` (new field `UniversityDetail.detailed`) and `GuidesScreen.kt`. The workflow produced the branch (bot commit `77e2a1f`, 2026-10-11). Owner steps: pull request from `fix/guides-universities-filters` to `main`, wait for a green build, test (Guides > Universities: three chips, "Detailed Guides" filter), merge.

## P7 to P10. Native tutor, calculator, timer and planner entries  — APPLIED in `main` (PR #4 to #7, merged 2026-10-12)

`patch_native_tutor_entries.py` (P7), `patch_native_calculator_entries.py` (P8), `patch_native_timer_entries.py` (P9), `patch_native_planner_entries.py` (P10). Details are in `docs/SESSION_2026-10-12_*.md` (AJ, AK). P9 requires P7 and P8; P10 requires P7 to P9 (all merged). CI results of these builds were not visible to the agents.

## P16. U1 Option A: Global drawer and study tools accessibility — APPLIED in `MainActivity.kt`

Script: `scripts/patch_u1_global_tool_overlays.py`.
Fixes the U1 bug where the AI Tutor and drawer study tools failed to open while inside a course, package, or reading content (PDF reader, Notes reader, Flashcards, Quiz / Exam).
- Study tools (`NativeTutorScreen`, `NativeCalculatorScreen`, `NativeTimerScreen`, `NativePlannerScreen`, `NativeNotebookScreen`, `NativeTargetsScreen`, `NativeStatusScreen`) are rendered at `zIndex(120f)` as global overlays in `MainActivity.kt` directly over the active workspace, passing `courseContext = activeWorkspace?.first` to the AI Tutor.
- Tapping Back (or device back button) clears `activeStudyTool = null` and leaves the student on the exact book/course/quiz page without losing progress.
- Drawer screens (`AboutScreen`, `ContactScreen`, `FaqScreen`, `PrivacyScreen`, `TermsScreen`) are elevated from `zIndex(16f)` to `zIndex(25f)` so they sit above `StudyWorkspaceScreen` (`18f`).

