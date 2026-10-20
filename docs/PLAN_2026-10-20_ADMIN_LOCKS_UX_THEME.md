# PLAN 2026-10-20: lock ownership and grants, admin dashboard ease of use, day / night / transparent modes

Owner report (2026-10-20, 12:xx): (1) the lock control only works as a hard lock; ownership and grant access do not take
effect, (2) the admin dashboard is hard to use: when an item is tapped its details appear far down the list and the admin
has to scroll, (3) the admin screens misbehave against the three looks (day, night, transparent). Make all three
"perfect to use". Owner rules still apply: not a coder, plain steps, one small slice per chat, push every slice,
nothing may weaken security.

## PROGRESS SO FAR (all on main unless noted)
- S5 / S5b / S5c: paid PDFs live in the PRIVATE Appwrite bucket; students get a 10-minute address from the Edge Function
  `book-url` after the database check (`open_book`). VERIFIED ON DEVICE 2026-10-20: the S5 test PDF opened in the app.
- Database: `learning_resources_hub_check` now allows `life-savers` (was missing; saving a Life Savers row failed).
- S5d phone upload: Edge Function `paid-pdf-upload` is DEPLOYED (admin only, PDF only, 5 MB chunks, 50 MB max, private
  bucket only; see docs/S5d-phone-upload.md). App side (PaidPdfUploader, PaidPdfPicker, editor button) is pushed,
  NOT compiled, NOT tested. Next for S5d: read the CI result, then one real upload.
- The test row "S5 test" (Life Savers, ece/sem-1/eceg3071) is still published; switch it to Draft or delete it.
- A1 DONE (2026-10-20, database): `public.my_package_access()` exists. Checked: signed out gives `not_signed_in`; the anon key
  cannot run it; a signed-in user can. Not yet used by the app (that is A2).

## FINDINGS (read from the code and the live database today, not guesses)
1. The DATABASE already knows four lock modes: `open`, `locked` (shown as coming soon), `coming_soon`, `require_purchase`.
   `content_guard.effective_mode` picks the most specific row (item, chapter, hub, scope, package). `content_guard.resource_visible`
   lets a student into a `require_purchase` item only if (a) `system:enforce` is `locked` (it IS, today) and the student has an
   `enrollments` row or an `access_grants` row for the package (`__all__` and the two ECE semesters count as one), or (b) enforcement
   is off. `open_resource` and `open_book` use it, so DOWNLOADS are already enforced correctly by the server.
2. The APP's own screens decide with `data/content/ContentAccess.kt`, and there `IS_FREE_MODE = true`: every signed-in student
   "owns" everything, and enrollments / access grants are never read (the file itself says "N9b, NOT finished"). So
   `require_purchase` never blocks anyone in the app lists, and a grant can never matter. Only a hard lock (`locked` /
   `coming_soon`) changes what the app shows. THIS is why only the hard lock seems to work.
3. A second mismatch: the app's paid-mode fallback lists "free for registered" packages (ECE, Freshman, Grades, Entrance), but
   the database rule has NO such list: with enforcement on, a registered student without an enrollment or grant is refused.
   Rule chosen for the fix: THE SERVER IS THE ONLY SOURCE OF TRUTH; the app mirrors its answer and never invents ownership.
4. Admin dashboard: the Content library already replaces the list with the editor (good). Other modules (Locks, Grants, Orders,
   Inquiries, Users, Competitions, Broadcasts) must be audited for "expand below the tapped row" patterns.
5. Looks: `ui/theme/Theme.kt` defines Night (dark scheme) and Day (light scheme); the transparent / background look is drawn by
   `AppBackdrop` / `AppBackground`. The admin screens use `AdminKit.kt` and some fixed colour constants (for example `AdminRose`,
   `WisdomMuted`, `WisdomCard`). UNVERIFIED: which of them ignore Day or Transparent. The audit below finds out.

## THE PLAN (each slice small, pushed alone, tested alone)

### A. Ownership and grants really work (highest value, security related)
- A1 DONE (server, additive, safe): database function `public.my_package_access()` for signed-in users. Answer:
  `{ status: "ok", enforced: boolean, packages: text[] }` (or `{ status: "not_signed_in" }`), using the SAME rule as
  `resource_visible` (enrollments + access grants, by user id or e-mail). No new table; nobody can read anybody else's rows.
  Call it with the student's own token: POST `/rest/v1/rpc/my_package_access` with body `{}`.
  The app must treat a package id `__all__` as owning everything, and `ece-y3-sem-1` / `ece-y3-sem-2` as one package
  (same as the server).
- A2 (app, NEXT): `ContentAccess.ownsAny()` stops using `IS_FREE_MODE`. It uses the A1 answer (cached, refreshed at sign-in and when the
  app comes to the front). Offline: reuse the last answer; never invent ownership (a paid package stays closed rather than opening).
  `enforced = false` means everything open, exactly like the server.
- A3 (app, admin): Locks screen explains each mode in one plain line ("Open", "Coming soon / locked", "Package required: only students
  who bought or were granted it"), and shows who is affected. Grants screen: after granting, show "Student now has access to <package>".
- A4 test matrix (one device, admin plus one test student): each mode (open, locked, coming soon, require_purchase) at package, course,
  hub, chapter, item level, for a student with nothing, with an enrollment, with a grant, and signed out. Expected results written
  in the test file before testing.

### B. Admin dashboard easy to use (one shared pattern, then each module)
- B1: shared pattern in `AdminKit.kt`: tapping a row OPENS THE DETAIL ON TOP (full-screen panel with a Back arrow, like the Content
  editor), never below the list. The list keeps its scroll position when the admin comes back. Panels start at the top.
- B2: audit all modules; list every "expands below" place and the long lists that need a search box or filters
  (users, orders, inquiries, locks). Bigger touch targets, short plain labels, the main action fixed at the bottom of the panel.
- B3: apply the pattern module by module, Locks and Grants first (the owner uses them most), one module per chat.

### C. Day / Night / Transparent in every admin screen
- C1: audit `AdminKit.kt` and each admin screen for fixed colours and for fills that hide the transparent background;
  list them in docs/ADMIN_LOOKS_AUDIT.md.
- C2: replace fixed colours with the theme palette (`WisdomDarkPalette` / `WisdomLightPalette` through the existing helpers);
  in Transparent mode cards use the same semi-transparent fill as the rest of the app.
- C3: screenshot check of Locks, Grants, Content, Orders in all three looks.

## ORDER
A1 (done), A2, A3, A4 first (security and correctness), then B1, then C1, then B3 / C2 module by module. One slice per chat.

## OPEN POINTS (decisions the owner may change)
- Default chosen: server rules win (see finding 3). If the owner wants "free for registered students" for some packages, that has to
  be done in the DATABASE (a lock row set to `open`), never by a hidden list in the app.
- Nothing in this plan touches payments or prices.
