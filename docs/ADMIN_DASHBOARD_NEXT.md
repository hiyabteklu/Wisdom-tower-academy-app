# ADMIN DASHBOARD: what is done and what comes next (2026-10-20)

Short working plan that sits under `docs/PLAN_2026-10-20_ADMIN_LOCKS_UX_THEME.md` (that file stays the big plan).
Owner rules: not a coder, plain steps, ONE small slice per chat, push every slice, nothing may weaken security.

## Where we are
- A1 DONE: database function `my_package_access()`.
- A2 DONE in code, CI #843 green: the app asks the server which packages a student owns (no more free mode).
  A2 follow-up (`warmUp()` on sign-in and resume) pushed.
- A3 PUSHED, `[~]` not compiled / not tested:
  - Locks screen (`LocksScreen.kt`): each of the four modes says WHO is affected; the open step shows a "what happens now" line
    and warns when Owners only is set while Paid mode is OFF (then nothing is enforced); "Reset to code default" became
    "Remove this lock (the level above decides)".
  - Grants screen (`AccessGrantsScreen.kt`): after a grant the message says "<email> now has access to <package>. The student sees
    it within about 30 seconds of opening the app."; a note appears while Paid mode is OFF (a grant changes nothing then);
    the fixed pink became the kit's `AdminRose`.
  - KEY FACT for testing grants: Paid mode (`system:enforce`) must be ON. With it OFF every signed-in student opens everything.
- A4 WRITTEN: `docs/A4_TEST_MATRIX.md` (expected results first). NOT run on the phone yet; results go to `docs/A4_RESULTS.md`.
- B1a PUSHED (2026-10-20), `[~]` not compiled / not tested on a phone:
  - `AdminKit.kt`: new `AdminDetailPanel(title, onBack, modifier, subtitle, actions, content)`. Full-screen panel drawn over the list
    (draw it as the LAST child of the Box that holds the list and keep the list composed): header with Back arrow, subtitle and title,
    content that starts at the top and scrolls on its own, optional bar fixed at the bottom for the main action. It registers its own
    `BackHandler`, so System Back closes the panel first. It swallows taps so nothing reaches the list underneath.
  - `LocksScreen.kt`: package shortcuts, chapters and materials are now plain rows (`LockRow`: name, current mode badge, arrow). A tap sets
    `detail = LockTarget(...)` and the panel opens: the lock key, a "what happens now" line, the four modes, and "Remove this lock"
    fixed at the bottom. Nothing expands below a row any more. The top step card (Academy > package > subject > hub) is unchanged.
    Lock rules, saving and the toast are unchanged (the toast sits above the fixed button while the panel is open).
  - To test on the phone (Paid mode can stay as it is): Content locks > tap a package shortcut far down the list = panel opens at the top; tap
    a mode = badge in the panel changes, toast shows; Back = back to the list at the SAME scroll position; open a hub, tap a chapter and a
    material = same panel; System Back closes the panel first, the second Back goes up one step.

## Next slices, in this order (one per chat)
1. A4 run on the phone: one device, one admin, one test student, rows in `docs/A4_TEST_MATRIX.md`; record in `docs/A4_RESULTS.md`.
2. B1b: Grants (`AccessGrantsScreen.kt`) on the same panel: the package picker stops opening inline inside the form; a grant row opens a
   detail panel with Revoke / Revoke all fixed at the bottom. Reuse `AdminDetailPanel`, do not write another one.
   Other modules (Users, Payments, Inquiries, Broadcasts, Competitions, Catalog, Free resources, Course content): NOT audited yet (that is B2).
3. B2 audit of all modules for "expands below" and long lists without search; result written to `docs/ADMIN_AUDIT.md`.
4. C1 audit of fixed colours and fills that hide the Day / Night / Transparent looks; `docs/ADMIN_LOOKS_AUDIT.md`.
   Already seen: `AccessGrantsScreen.kt` used a fixed pink (fixed in A3). Others unchecked. The new panel uses `WisdomBackground`,
   which is the page colour of the admin host, so it should follow Day / Night; check it in Transparent in C1.
5. B3 / C2 module by module: Orders (Payments), Users, Inquiries.

## Not in scope
Payments, prices, anything that weakens the server rules.
