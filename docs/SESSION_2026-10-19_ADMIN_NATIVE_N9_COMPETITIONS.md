# SESSION 2026-10-19 — ADMIN-native N9 (Competitions)

> Repo is PUBLIC: no secrets, no student data. Plan: `docs/ADMIN_NATIVE_PLAN.md` (its slice numbers shifted by one after N6: Course Content was N7 and Push Broadcasts N8, so Competitions is N9). Previous: `docs/SESSION_2026-10-19_ADMIN_NATIVE_N8A_BROADCASTS.md` and the commits N8b part 1 and 2 (native notification composer). Owner order: "continue making such admin dashboard native". Operating rules read: `CLAUDE.md`, `docs/ADMIN_NATIVE_PLAN.md`, `docs/AGENT_QUOTA_PROTOCOL.md`, `docs/BUILD_PITFALLS.md`, `docs/COMPETITION_ADMIN_CHECKLIST.md`, `docs/COMPETITION_API_CONTRACT.md`.

## Audit of the website panel (read, not guessed)
Files read: `website-reference/src/components/admin/CompetitionsAdminPanel.tsx` and `EntryFlagsInfo.tsx`. Everything is PLAIN Supabase with the admin's own login. No website API route, no secret, so the whole module could be made native in one slice.
- **List**: table `competitions` (newest 100, columns id, title, audience, package_id, requires_package, published, countdown_state, starts_at, duration_min, ends_at, points, prize_title, prize_details). Exam picker: table `learning_resources` where hub = exams and content_type = exam (id, title, package_id, meta durationMin).
- **Create**: function `admin_publish_competition_from_exam` (10 parameters, see `docs/COMPETITION_API_CONTRACT.md`).
- **Show or hide**: `admin_set_competition_published`. **Pause, resume, cancel**: `admin_set_countdown` with `paused`, `running`, `cancelled`. **Start now**: `admin_start_competition_now`. **Reschedule**: `admin_reschedule_competition`.
- **Entries**: table `competition_entries` (ranked only, best score first, 200), names from `profiles` (id, full_name), flags from `competition_flags` (entry_id, kind, detail, created_at). **Disqualify**: `admin_disqualify_entry`.

## What was pushed (one commit)
1. NEW `data/admin/CompetitionsRepository.kt`: `load()` (contests and exams in parallel), `loadEntries(id)` (entries, names and flags), `create`, `setPublished`, `setCountdown`, `startNow`, `reschedule`, `disqualify`; the website's error wording (`friendlyError`, including the `bad_question_at_position_N` sentence); Ethiopia-time helpers (`eatToIso`, `phaseOf`, flag summary and details, same rule as `EntryFlagsInfo`).
2. NEW `ui/admin/CompetitionsScreen.kt`: header with Refresh, a collapsible Create form (exam picker with search, title, date and time as two text fields, duration, audience, points, prize title and details, two switches), the contest list with status and visible/hidden badges, action buttons two per row (Hide or Show, Pause or Resume, Start now, Reschedule, Cancel contest, Entries), an inline reschedule editor, an entries panel with flags (tap the amber line for details) and Disqualify, confirm dialogs and a toast.
3. EDIT `AdminModules.kt` (`competitions` is native) and `AdminConsoleHost.kt` (Competitions branch; the deep link `/admin?tab=competitions` already maps to this module).

## Deliberate differences from the website
- The date and time are two text fields (`2026-10-25` and `14:30`, 24-hour) because the app has no date-time picker. They are read as Ethiopia time, like the website, and checked as real calendar values.
- The Create form is collapsed until the owner taps New competition (one scroller, the list stays visible). The exam picker is a search list instead of a dropdown.
- Ticking Show to students now asks for confirmation first (the contest reaches real students at once). The website does not ask.
- Pause and Resume buttons are shortened from Pause countdown and Resume countdown so two fit on a row; the toast sentences are the website's.
- The disqualify reason is typed in an app dialog instead of a browser prompt.
- Toasts stay 4.5 seconds because some error sentences are long.

## Evidence and limits
- `[~]` Written, NOT compiled, NOT run. Check the Actions run of this commit.
- Pitfall check by reading: every Modifier extension used is imported (`background`, `border`, `clickable`, `padding`, `fillMaxSize`, `fillMaxWidth`, `height`, `alpha`, `clip`); `items` imported; no composable call in a LazyColumn builder body outside `item` or `items`; named arguments for every lambda that is not last; smart casts use local vals (`current`, `startItem`, `cancelItem`, `dqItem`, `endsAt`, `prize`, `reason`, `score`, `total`, `taken`); only core icon `Refresh`; no slash-star inside any comment; no secret.
- HYPOTHESIS: the aliased select `durationMin:meta->>durationMin` on `learning_resources` works through the REST address the same as in the website client (the app encodes the `>` characters). If the exam list is empty or fails, the picker says so and the Create form cannot be used until it loads.
- HYPOTHESIS: the six admin functions accept a call through `rpc/<name>` with the JSON names taken from the website calls and the contract (`p_resource_id`, `p_starts_at`, ..., `p_id`, `p_state`, `p_entry_id`, `p_reason`). Unset text parameters are sent as JSON null, like the website client.
- HYPOTHESIS: the order `score.desc.nullslast` is accepted as written (it is the same order the website asks for).
- The phase (upcoming, live, ended) is computed from the phone's clock, exactly as the website uses the computer's clock.
- Test contests: use the audience `owner-test` (see `docs/COMPETITION_ADMIN_CHECKLIST.md`) so no student sees them.

## What the owner should test
1. Admin Console > Competitions opens natively under the section bar; Back returns to the grid. The deep link `/admin?tab=competitions` (for example from a contest notification) opens it directly.
2. The list matches the website: status badge, visible or hidden, start time in Ethiopia time, duration, points, prize.
3. New competition: choose an exam, type a date and time at least a minute ahead, audience `owner-test`, leave Show off, Create competition. Expected toast: Competition created as hidden. Use Show to publish it when ready. A wrong date shows the website's sentence.
4. On the TEST contest: Show then Hide; Pause then Resume; Reschedule (new date and time, Save new time); Start now (asks first); Cancel contest (asks first).
5. Entries on a contest with entries: names, score, time; the amber flag line opens the details; Disqualify asks for a reason and the row shows Disqualified.
6. Airplane mode: a card with Could not reach the server and Try again.
7. Afterwards ask an agent with the Supabase connector to delete the test contests and their entries.

## NEXT
Remaining on the website: Free Resources (file uploads need a file chooser), Database and SQL Hub (probably stays web: arbitrary SQL needs a server route; decide with the owner). Then the regression pass (plan slice N11) and the owner's phone test. Plan status line (plan file not resent): 2026-10-19 | Claude | N9 written `[~]`: Competitions native (all actions of the website panel). NEXT: Free Resources, or the owner picks.
