# ADMIN DASHBOARD: what is done and what comes next (2026-10-20)

Short working plan that sits under `docs/PLAN_2026-10-20_ADMIN_LOCKS_UX_THEME.md` (that file stays the big plan).
Owner rules: not a coder, plain steps, ONE small slice per chat, push every slice, nothing may weaken security.

## Where we are
- A1 DONE: database function `my_package_access()`.
- A2 DONE in code, CI #843 green: the app asks the server which packages a student owns (no more free mode).
  A2 follow-up (`warmUp()` on sign-in and resume) pushed, CI #845 green.
- A3 PUSHED, CI #847 green (compiled), `[~]` not tested on a phone:
  - Locks screen (`LocksScreen.kt`): each of the four modes says WHO is affected; the open step shows a "what happens now" line
    and warns when Owners only is set while Paid mode is OFF (then nothing is enforced); "Reset to code default" became
    "Remove this lock (the level above decides)".
  - Grants screen (`AccessGrantsScreen.kt`): after a grant the message says "<email> now has access to <package>. The student sees
    it within about 30 seconds of opening the app."; a note appears while Paid mode is OFF (a grant changes nothing then);
    the fixed pink became the kit's `AdminRose`.
  - KEY FACT for testing grants: Paid mode (`system:enforce`) must be ON. With it OFF every signed-in student opens everything.
- A4 WRITTEN: `docs/A4_TEST_MATRIX.md` (expected results first). NOT run on the phone yet; results go to `docs/A4_RESULTS.md`.
- B1a PUSHED, CI #849 and #850 green (compiled), `[~]` not tested on a phone:
  - `AdminKit.kt`: new `AdminDetailPanel(title, onBack, modifier, subtitle, actions, content)`. Full-screen panel drawn over the list
    (draw it as the LAST child of the Box that holds the list and keep the list composed): header with Back arrow, subtitle and title,
    content that starts at the top and scrolls on its own, optional bar fixed at the bottom for the main action. It registers its own
    `BackHandler`, so System Back closes the panel first. It swallows taps so nothing reaches the list underneath.
  - `LocksScreen.kt`: package shortcuts, chapters and materials are plain rows (`LockRow`: name, current mode badge, arrow). A tap sets
    `detail = LockTarget(...)` and the panel opens: the lock key, a "what happens now" line, the four modes, and "Remove this lock"
    fixed at the bottom. Nothing expands below a row any more. The top step card (Academy > package > subject > hub) is unchanged.
  - To test on the phone: Content locks > tap a package shortcut far down the list = panel opens at the top; tap a mode = badge in the
    panel changes, toast shows; Back = back to the list at the SAME scroll position; open a hub, tap a chapter and a material = same
    panel; System Back closes the panel first, the second Back goes up one step.
- B1b PUSHED, CI #851 GREEN (compiled), `[~]` not tested on a phone:
  - `AccessGrantsScreen.kt` uses the same `AdminDetailPanel`. The Package field in the form is a button with an arrow; a tap opens a
    "Choose a package" panel (All packages + every package, tap one = it is selected and the panel closes). Nothing opens inline.
  - Each grant is a compact row (email, package, arrow). A tap opens the grant detail panel: package, package id, note, granted by,
    linked account or email only; `Revoke` and `Revoke all` are fixed at the bottom and STILL ask for confirmation; after a confirmed
    revoke the panel closes and the list reloads. Grant, revoke and the confirm dialogs are unchanged.
  - The form and list stay underneath, so typed e-mail / note and the scroll position are kept.
  - To test on the phone: Access grants > type an e-mail, tap Package = list opens on top, pick one = back to the form with the e-mail
    still typed; tap a grant row far down the list = detail opens at the top, Back = same scroll position; Revoke asks first.
- B2 DONE for 5 modules (read-only audit): `docs/ADMIN_AUDIT.md`. Worst: Competitions (create form, exam picker, reschedule and ALL entries open
  inline inside a card), then Inquiries (status buttons expand inside the card, no search), Payments (tall cards, Approve at the bottom of the card),
  Catalog (editor replaces the list but Save is at the end of a long form and the list scroll position is lost), Users (already replaces the list).
  NOT read yet: Broadcasts, Broadcast composer, Course content, Free resources, Overview, Database status, Admin home (B2b).
- B3a PUSHED, CI #856 GREEN (compiled), `[~]` not tested on a phone: `CompetitionsScreen.kt`, the Create card keeps only its intro and the
  `New competition` button. A tap opens the panel "New competition" (12 fields, `Create competition` fixed at the bottom). The exam field opens a
  second panel "Choose an exam" (search + rows); picking one closes only the picker. Back closes the picker first, then the form.
  Helper `CompFieldBlock`; toast lifted to 88 dp while a panel with a bottom bar is open. Phone test: `docs/SESSION_2026-10-20_ADMIN_B3A_COMPETITIONS.md`.
- B3b + B3c PUSHED (commit starts "Admin B3b/B3c (Competitions)"), `[~]` CI result not read yet / not tested on a phone, same file:
  - CI: the owner's Actions screenshot (3:06 pm) showed #857 (this commit) and #858 (docs) still In progress. Result not seen. A code read in the
    B3d chat found no problem in the three risk spots below (code read only, not a build): `EntryItem.name` is used as a plain String in
    `Text(text = entry.name)`; `EntriesBody` gets `result` as a parameter, so its smart casts are stable; the multi-child composables sit inside the
    panel's composable Column lambda.
  - Each contest is a compact row (`CompetitionRow`: state badge, visible/hidden badge, title, start + minutes, arrow). A tap opens the contest panel
    (`CompetitionDetailBody`: badges, when / ends / points, audience, prize, actions two to a row: Show/Hide, Pause/Resume, Start now, Reschedule,
    Cancel contest, Entries). The panel finds the contest by id in the reloaded list, so it shows the new state after an action.
  - Reschedule = small panel over it (date, time, duration, `Save new time` fixed at the bottom). Entries = panel over it (`EntriesBody`: count,
    search by name, entries; flag details stay inside the entry; Disqualify still asks for a reason).
  - Unchanged: every `CompetitionsRepository` call, Start now / Cancel / Show-to-students confirm dialogs, Ethiopia time, texts. Removed (replaced):
    `CompetitionCard`, `RescheduleEditor`, `EntriesPanel`.
  - Phone test (6 steps): `docs/SESSION_2026-10-20_ADMIN_B3B_B3C_COMPETITIONS.md`.
  - Compile risks to check first if red: `EntriesBody` uses `it.name.contains(...)` (assumes `EntryItem.name` is a non-null String); the new
    `KeyboardArrowRight` import; the plain composables that emit several children into the panel Column.
- B3d PUSHED (commit starts "Admin B3d (Inquiries)"), `[~]` CI not seen / not tested on a phone: `InquiriesScreen.kt`.
  - Each inquiry is a compact row (`InquiryRow`: name, e-mail, status badge, two lines of the message, date, arrow). A tap opens the inquiry panel
    (`InquiryDetailBody`: status and date, e-mail, interest, the whole message; `StatusButtons`, the four statuses two to a row, fixed at the bottom).
    The panel stays open after a status change. New search box under the filter chips (name, e-mail, interest, message), combined with the status chips.
  - Unchanged: `InquiriesRepository` calls, status logic, toast and empty-state texts. Removed (replaced): `InquiryCard`, `expandedId`.
    `AdminToast` and `AdminStatusBadge` stay in `InquiriesScreen.kt` untouched (other screens use them); NOT moved to `AdminKit.kt`.
  - Phone test (5 steps): `docs/SESSION_2026-10-20_ADMIN_B3D_INQUIRIES.md`.
  - Compile risks to check first if red: the new `KeyboardArrowRight` import; `InquiriesRepository.STATUSES.chunked(2)` inside `StatusButtons`;
    the local `val service = it.service` in the search filter.

## Next slices, in this order (one per chat)
1. First: read the CI result of B3b/B3c (#857) and B3d (ask the owner for the Actions list); if red, fix it before anything else and log it in `docs/BUILD_PITFALLS.md`.
2. A4 run on the phone: one device, one admin, one test student, rows in `docs/A4_TEST_MATRIX.md`; record in `docs/A4_RESULTS.md`.
3. B3e Payments (Approve / Reject fixed at the bottom, logic unchanged), B3f Catalog (editor in the panel,
   fixed pink to `AdminRose`), B3g Users (order in `docs/ADMIN_AUDIT.md`). Reuse `AdminDetailPanel`, never write another one.
   `AdminToast` and `AdminStatusBadge` live in `InquiriesScreen.kt`: never delete them (other screens use them).
4. B2b audit of the 7 files not read yet (add them to `docs/ADMIN_AUDIT.md`).
5. C1 audit of fixed colours and fills that hide the Day / Night / Transparent looks; `docs/ADMIN_LOOKS_AUDIT.md`.
   Already seen: `AccessGrantsScreen.kt` used a fixed pink (fixed in A3); `CatalogScreen.kt` RoseTone FB7185; `CompetitionsScreen.kt` CompOrange FB923C.
   The panel uses `WisdomBackground`, which is the page colour of the admin host, so it should follow Day / Night; check it in Transparent in C1.

## Not in scope
Payments, prices, anything that weakens the server rules.
