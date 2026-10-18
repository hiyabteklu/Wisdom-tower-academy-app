# SESSION 2026-10-18 — ADMIN-native N5 (Overview)

> Repo is PUBLIC: no secrets, no student data. Plan: `docs/ADMIN_NATIVE_PLAN.md` (N5 row). Previous: `docs/SESSION_2026-10-18_ADMIN_NATIVE_N4.md`. Owner order: "Continue".

## Audit of the website panel (read, not guessed)
Files read: `AnalyticsPanel.tsx`, `lib/admin-data.ts` (`fetchAdminStats`).
- Data: six plain Supabase reads with the admin login (profiles 500, orders 500, enrollments 200, inquiries 200, and two exact counts: `question_explanations`, `academic_results`). No API route, no secret.
- Shown: Today's Pulse, 4 KPI cards, 4 small cards, 7-day order bars, payment method share, top 8 packages, stream share, activity feed (filter All, Orders, Grants, Inquiries, Scholars, newest 16).
- Computed but never shown (not carried over): education level share, status split.

## What was pushed (one commit)
1. EDIT `data/admin/AdminSupabase.kt`: added `count()` (HEAD request with `Prefer: count=exact`, number after the slash of `Content-Range`) and `contentRange` on `AdminHttpResult` (default null, so every existing caller compiles unchanged). `get`, `patch`, `post`, `postPublic` unchanged.
2. NEW `data/admin/OverviewRepository.kt`: `load()` (10 requests in parallel after one login refresh), stats, feed.
3. NEW `ui/admin/OverviewScreen.kt`: all the blocks above as native cards. Charts are plain boxes.
4. EDIT `AdminModules.kt` (`overview` native) and `AdminConsoleHost.kt` (Overview branch).

## Deliberate differences from the website
- Totals that the website took from a capped list are exact counts: Registered Scholars, total unlocked grants, Inquiries total and new.
- Orders and scholar profiles are read up to 1000 rows (website 500). If the cap is reached the screen says so ("Order figures cover the newest N orders only", "Shares are based on the newest N scholars"). HYPOTHESIS: the server's own row cap is 1000 (Supabase default); if it is lower, the note will show a lower number.
- The 7-day bars and Today's Pulse use the PHONE's calendar days. The website mixed UTC days with local midnight, which shifts the bars by one day in Ethiopia.
- A failed read shows a dash or "could not be loaded" in that card only; the others still show. If nothing could be read at all: one error card with Try again.
- Not copied: the text "100% Ethiopian Rail" (a claim the database does not back), the pulsing dot, hover tooltips, the website's per-day revenue label on hover (the week's verified revenue is one line under the chart instead).
- The feed shows an "Inquiry" for the 8 newest inquiries and "Unlocked" for the 8 newest enrollments, like the website's recent lists.

## Evidence and limits
- `[~]` Written, NOT compiled, NOT run. Check the Actions runs of N1 to N5.
- Pitfall check by reading: every Modifier extension imported (`fillMaxHeight`, `fillMaxWidth`, `height`, `width`, `size`, `padding`, `alpha`, `clip`, `background`, `border`); `items` imported; no composable call in a LazyColumn builder body outside `item` or `items`; `CircleShape` imported; `ChartCard` takes its content lambda as the LAST parameter (trailing lambda is safe); smart casts on `s.orders` work because `OverviewStats.orders` is a `val` of a class in the same module (if the compiler disagrees, copy to a local val); `Math.round` of a Double gives Long, divided by 10.0 gives Double.
- HYPOTHESIS: the admin login may count rows of `question_explanations` and `academic_results` (the website does the same). A refusal shows a dash.
- HYPOTHESIS: HEAD with `Prefer: count=exact` returns `Content-Range` through OkHttp the same way as in `AdminCounts.kt` (same code path, already written in N1).
- HYPOTHESIS: the profiles columns `school_name` and `stream` exist (the website stats query uses them).
- Time shown assumes `created_at` is UTC (same as N2 to N4).
- Plan status line (plan file not resent): 2026-10-18 | Claude | N5 written `[~]`: Overview native. NEXT: N6 (Access Grants, Content Locks, Pricing and Catalog).

## What the owner should test
1. Admin Console > Overview opens natively. Compare with the website Overview: revenue, pending pipeline, order conversion and the order counts should match (unless more than 1000 orders exist).
2. Registered Scholars equals the number on the Scholars & Users tile of the home grid.
3. The 7 bars: today is the last bar and matches the number of orders you placed today.
4. Payment Rail Share and Top Packages list your real methods and packages.
5. The feed chips filter the list; each line shows a time such as 5m ago.
6. Airplane mode: "Could not load the overview" with Try again.

## NEXT
N6: Access Grants (10 KB), Content Locks (14 KB), Pricing and Catalog (13.7 KB). Audit each panel first. Pricing and Catalog may also replace the hand copy in `ADMIN_PACKAGES`.
