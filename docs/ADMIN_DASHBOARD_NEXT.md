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
- B3a through B3g: five modules converted to compact rows + detail panels (see earlier entries). B3 DONE in code for Competitions, Inquiries, Payments, Catalog, Users.
- **C1 PUSHED** (2026-10-20, commit on main): Night and Transparent now use the shared app backdrop in admin.
  - Cause: `AdminConsoleHost` and `AdminDetailPanel` painted solid `WisdomBackground`, which covered the photo (Transparent) and flattened Night.
  - Fix: both use `appBackdrop()` like the rest of the app. Day unchanged in intent (ivory backdrop).
  - Session log: `docs/SESSION_2026-10-20_ADMIN_C1_LOOKS.md`.
  - `[~]` not compiled by agent; phone test in Night, Day, Transparent required.
  - Leftover (not this slice): fixed CompOrange in Competitions; WebView solid bg in `AdminWebSection`; dialog `WisdomNavy` container.

## Next slices, in this order (one per chat)
1. Phone-test C1 in all three looks (see session log). Confirm CI for this push is green.
2. First: read the CI result of B3f (#861) and of B3g (ask the owner for the Actions list); if red, fix it before anything else and log it in `docs/BUILD_PITFALLS.md`.
3. A4 run on the phone: one device, one admin, one test student, rows in `docs/A4_TEST_MATRIX.md`; record in `docs/A4_RESULTS.md`.
4. B2b audit of the 7 files not read yet (Broadcasts, Broadcast composer, Course content, Free resources, Overview, Database status, Admin home; add them to `docs/ADMIN_AUDIT.md`), then a B3 slice for each one the audit flags.
   Reuse `AdminDetailPanel`, never write another one. `AdminToast` and `AdminStatusBadge` live in `InquiriesScreen.kt`: never delete them (other screens use them).
5. Optional C1 follow-up: remaining fixed accents (CompOrange) and dialog/WebView solid fills if still noticeable after the backdrop fix.

## Not in scope
Payment logic, prices, anything that weakens the server rules (B3e changed the Payments LAYOUT only; B3f changed the Catalog LAYOUT only; B3g changed the Users LAYOUT only).
