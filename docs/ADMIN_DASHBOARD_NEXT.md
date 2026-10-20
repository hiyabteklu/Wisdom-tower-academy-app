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
- B3b + B3c PUSHED, CI #857 GREEN and docs run #858 GREEN (compiled; owner's Actions screenshot, 3:12 pm), `[~]` not tested on a phone, same file:
  - Each contest is a compact row (`CompetitionRow`: state badge, visible/hidden badge, title, start + minutes, arrow). A tap opens the contest panel
    (`CompetitionDetailBody`: badges, when / ends / points, audience, prize, actions two to a row: Show/Hide, Pause/Resume, Start now, Reschedule,
    Cancel contest, Entries). The panel finds the contest by id in the reloaded list, so it shows the new state after an action.
  - Reschedule = small panel over it (date, time, duration, `Save new time` fixed at the bottom). Entries = panel over it (`EntriesBody`: count,
    search by name, entries; flag details stay inside the entry; Disqualify still asks for a reason).
  - Unchanged: every `CompetitionsRepository` call, Start now / Cancel / Show-to-students confirm dialogs, Ethiopia time, texts. Removed (replaced):
    `CompetitionCard`, `RescheduleEditor`, `EntriesPanel`.
  - Phone test (6 steps): `docs/SESSION_2026-10-20_ADMIN_B3B_B3C_COMPETITIONS.md`.
- B3d PUSHED (commit 64e215c, starts "Admin B3d (Inquiries)"), CI #859 GREEN (compiled; owner's Actions screenshot, 3:18 pm), `[~]` not tested on a phone: `InquiriesScreen.kt`.
  - Each inquiry is a compact row (`InquiryRow`: name, e-mail, status badge, two lines of the message, date, arrow). A tap opens the inquiry panel
    (`InquiryDetailBody`: status and date, e-mail, interest, the whole message; `StatusButtons`, the four statuses two to a row, fixed at the bottom).
    The panel stays open after a status change. New search box under the filter chips (name, e-mail, interest, message), combined with the status chips.
  - Unchanged: `InquiriesRepository` calls, status logic, toast and empty-state texts. Removed (replaced): `InquiryCard`, `expandedId`.
    `AdminToast` and `AdminStatusBadge` stay in `InquiriesScreen.kt` untouched (other screens use them); NOT moved to `AdminKit.kt`.
  - Phone test (5 steps): `docs/SESSION_2026-10-20_ADMIN_B3D_INQUIRIES.md`.
- B3e PUSHED (commit c485bd6, starts "Admin B3e (Payments)"), CI #860 GREEN (compiled; owner's Actions screenshot, 3:24 pm), `[~]` not tested on a phone: `PaymentsScreen.kt`. LAYOUT ONLY, money logic untouched.
  - Each order is a compact row (`OrderRow`: package, student, amount, date, status badge, arrow; pending rows keep the amber border). A tap opens the
    order panel (`OrderDetailBody`: status, placed date, order id, package, amount and method, student, phone, e-mail, Ref and note, `View Receipt`).
    For a PENDING order `Approve & Unlock` and `Reject` are the bar fixed at the bottom (`OrderActionButtons`); verified / rejected orders have no bottom bar.
  - Unchanged: `doApprove`, `doReject`, both confirm dialogs (still ask first), every `OrdersRepository` call, totals, search and chips, texts.
    After a SUCCESSFUL approve / reject the panel closes and the toast shows the result; after an error the panel stays open. Removed (replaced): `OrderCard`.
  - Phone test (5 steps; use a test order, do not approve a real student's payment): `docs/SESSION_2026-10-20_ADMIN_B3E_PAYMENTS.md`.
- B3f PUSHED (commit starts "Admin B3f (Catalog)", 564b342), CI #861 In progress at 3:24 pm (result not seen), `[~]` not tested on a phone: `CatalogScreen.kt`. LAYOUT ONLY, save / delete logic untouched.
  - Each package is a compact row (`CatalogRow`: name, id, price, Inactive badge, arrow). A tap opens the editor panel (`CatalogEditor` = `AdminDetailPanel`,
    same 12 fields and labels, helper `CatalogField`); `Cancel` and `Save package` are the bar fixed at the bottom; `Delete package` is the last item in the
    panel and still asks first. The list stays composed underneath, so its scroll position is kept. `RoseTone` became `AdminRose`.
  - Unchanged: every `CatalogRepository` call, `newItem()`, `isBuiltIn()`, the built-in note, texts. After a successful save or delete the panel closes and the list
    reloads; after an error the panel stays open and the toast shows it. Removed (replaced): `CatalogCard`, the old full-screen editor.
  - Phone test (5 steps; use a test package, do not delete a real one): `docs/SESSION_2026-10-20_ADMIN_B3F_CATALOG.md`.
  - Compile risks to check first if red: `AdminDetailPanel` named arguments plus trailing content lambda; smart cast of `editTarget` in the `onDelete` lambda; the new imports.
- B3g PUSHED (commit starts "Admin B3g (Users)"), `[~]` not compiled / not tested on a phone: `UsersScreen.kt`. LAYOUT ONLY, `UsersRepository` calls, search, filters and grant logic untouched.
  - Each scholar is a compact row (`ScholarRow`: initials, name, e-mail, student id and level, arrow). A tap opens the profile panel (`ProfilePanel` = `AdminDetailPanel`,
    subtitle `Student Scholar Profile`): student id (tap to copy), level, joined, contact and school details, unlocked packages, recent scores. `Close` and
    `Grant New Package` are the bar fixed at the bottom.
  - `Grant New Package` opens a SECOND panel over the profile (`GrantPanel`: recipient, note, package choices; `Cancel` and `Confirm & Unlock Package` fixed at the bottom).
    `All Packages` still asks first. After a successful grant the grant panel closes and the profile reloads its package list; after an error the grant panel stays open.
  - Changed on purpose: the quick `Grant Package` button on each row is gone (open the scholar first). Removed (replaced): `ScholarCard`, `InspectView`, `GrantView`, `ViewHeader`,
    the screen-level `BackHandler`s (the panels have their own).
  - Phone test (5 steps; use a test student, do not grant a real student a package): `docs/SESSION_2026-10-20_ADMIN_B3G_USERS.md`.
  - Compile risks to check first if red: `AdminDetailPanel` named arguments plus trailing content lambda; `ProfileBody` and the content lambdas emitting several children; smart casts of `inspectTarget` / `grantTarget` in lambdas.
- B3 (the five audited modules: Competitions, Inquiries, Payments, Catalog, Users) is now DONE in code.

## Next slices, in this order (one per chat)
1. First: read the CI result of B3f (#861) and of B3g (ask the owner for the Actions list); if red, fix it before anything else and log it in `docs/BUILD_PITFALLS.md`.
2. A4 run on the phone: one device, one admin, one test student, rows in `docs/A4_TEST_MATRIX.md`; record in `docs/A4_RESULTS.md`.
3. B2b audit of the 7 files not read yet (Broadcasts, Broadcast composer, Course content, Free resources, Overview, Database status, Admin home; add them to `docs/ADMIN_AUDIT.md`), then a B3 slice for each one the audit flags.
   Reuse `AdminDetailPanel`, never write another one. `AdminToast` and `AdminStatusBadge` live in `InquiriesScreen.kt`: never delete them (other screens use them).
4. C1 audit of fixed colours and fills that hide the Day / Night / Transparent looks; `docs/ADMIN_LOOKS_AUDIT.md`.
   Already seen: `AccessGrantsScreen.kt` used a fixed pink (fixed in A3); `CatalogScreen.kt` RoseTone (fixed in B3f); `CompetitionsScreen.kt` CompOrange FB923C.
   The panel uses `WisdomBackground`, which is the page colour of the admin host, so it should follow Day / Night; check it in Transparent in C1.

## Not in scope
Payment logic, prices, anything that weakens the server rules (B3e changed the Payments LAYOUT only; B3f changed the Catalog LAYOUT only; B3g changed the Users LAYOUT only).
