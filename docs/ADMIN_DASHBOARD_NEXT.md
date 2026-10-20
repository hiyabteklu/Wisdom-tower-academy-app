# ADMIN DASHBOARD: what is done and what comes next (2026-10-20)

Short working plan that sits under `docs/PLAN_2026-10-20_ADMIN_LOCKS_UX_THEME.md` (that file stays the big plan).
Owner rules: not a coder, plain steps, ONE small slice per chat, push every slice, nothing may weaken security.

## Where we are
- A1 DONE: database function `my_package_access()`.
- A2 DONE in code, CI #843 green: the app asks the server which packages a student owns (no more free mode).
  A2 follow-up (`warmUp()` on sign-in and resume) pushed.
- A3 PUSHED (this slice), `[~]` not compiled / not tested:
  - Locks screen (`LocksScreen.kt`): each of the four modes says WHO is affected; the open step shows a "what happens now" line
    and warns when Owners only is set while Paid mode is OFF (then nothing is enforced); "Reset to code default" became
    "Remove this lock (the level above decides)".
  - Grants screen (`AccessGrantsScreen.kt`): after a grant the message says "<email> now has access to <package>. The student sees
    it within about 30 seconds of opening the app."; a note appears while Paid mode is OFF (a grant changes nothing then);
    the fixed pink became the kit's `AdminRose`.
  - KEY FACT for testing grants: Paid mode (`system:enforce`) must be ON. With it OFF every signed-in student opens everything.

## Next slices, in this order (one per chat)
1. A4 test matrix (written BEFORE testing; expected results first). One device, one admin, one test student.
   Modes open / require_purchase / coming_soon / locked at package, subject, hub, chapter, material level for: no purchase and no grant,
   with a grant, with an enrollment, signed out. Paid mode ON, then repeat the key rows with it OFF.
2. B1 shared "detail on top" pattern in `AdminKit.kt`: `AdminDetailPanel(title, onBack, actions) { content }`, a full-screen panel with a
   Back arrow, scrolling content that starts at the top, and the main action fixed at the bottom. The list underneath keeps its
   scroll position. System Back closes the panel first.
   Where it is needed (known from reading the code today):
   - Locks: package shortcuts, chapters and materials open their four modes INLINE below the tapped row (`ShortcutBlock`).
     They should open the panel instead.
   - Grants: the package picker opens inline inside the form; a grant row should open a detail panel with Revoke / Revoke all.
   - Other modules (Users, Payments, Inquiries, Broadcasts, Competitions, Catalog, Free resources, Course content): NOT audited yet.
3. B2 audit of all modules for "expands below" and long lists without search; result written to `docs/ADMIN_AUDIT.md`.
4. C1 audit of fixed colours and fills that hide the Day / Night / Transparent looks; `docs/ADMIN_LOOKS_AUDIT.md`.
   Already seen: `AccessGrantsScreen.kt` used a fixed pink (fixed in A3). Others unchecked.
5. B3 / C2 module by module: Locks and Grants first, then Orders (Payments), Users, Inquiries.

## Not in scope
Payments, prices, anything that weakens the server rules.
