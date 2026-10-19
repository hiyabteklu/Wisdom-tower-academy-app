# SESSION 2026-10-19 — ADMIN-native N6b (Content Locks)

> Repo is PUBLIC: no secrets, no student data. Plan: `docs/ADMIN_NATIVE_PLAN.md` (N6 row, second of three panels). Previous: `docs/SESSION_2026-10-18_ADMIN_NATIVE_N6A_ACCESS_GRANTS.md`. Owner order: "Continue".

## Owner question answered in this chat: who can see the admin dashboard?
Checked in code, not assumed: the "Admin Console" button on the Account screen (`ui/account/AdminEntryButton.kt`) draws NOTHING unless `AdminAccess.isAdmin()` is true. That call asks the database (RPC `am_i_admin`, which returns `private.is_admin()`); no e-mail list is kept in the app, and any failure (offline, signed out, function missing) answers false. Every native screen reads and writes with the signed-in account's own token, so the database's row-level security still decides what an account may touch: a non-admin who somehow opened the screen would get empty lists or refusals, never admin data. NOT verified by reading: whether MainActivity also blocks a direct `/admin` link for a non-admin (the screen itself would show errors then, but I did not read that code). If you want it closed there too, say so and I add the check.

## Audit of the website panel (read, not guessed)
Files read: `LocksPanel.tsx`, `lib/content-locks.ts`, `data/admin-nav.ts` and its source lists (`academy.ts`, `grade-subjects.ts`, `freshman.ts`, `special-packages.ts`, `entrance-questions.ts`, `packages.ts`).
- Table `content_locks` (lock_key, mode, label, note, updated_at, updated_by). Keys: `package:ID`, `scope:SCOPEPATH`, `hub:SCOPEPATH:HUB`. Most specific wins (hub, then subject, then package); no row means the code default.
- Modes: open, require_purchase, coming_soon, locked. Missing table message: run `docs/content-locks-setup.sql`.
- Set = upsert on `lock_key` (note and updated_by written as null). Clear = delete the row.
- Plain Supabase REST with the admin's own login. No API route, no secret.

## What was pushed (one commit)
1. NEW `data/admin/AdminContentTree.kt`: `ADMIN_CONTENT_TREE` (Grades 9–12 > grade > subject > 6 hubs; Entrance Questions > stream > subject > year > Exams; Freshman > 21 courses > hubs; Special packages > ECE S1 and S2 > 7 courses each > hubs), the three lock-key functions, `ADMIN_HUB_IDS`. Ids and names only. Reused by N7 (Course Content).
2. NEW `data/admin/LocksRepository.kt`: `list()`, `set()`, `clear()`.
3. NEW `ui/admin/LocksScreen.kt`: header and refresh, package shortcuts (Freshman, ECE S1, ECE S2: tap to open the four modes), path line with an up arrow, controls for the selected step (four mode rows with the website's hints, Reset to code default, the lock key in monospace), folder rows with the current lock badge, hub note, and the list of all active overrides at the top level.
4. EDIT `AdminModules.kt` (`locks` native) and `AdminConsoleHost.kt` (Locks branch).

## Deliberate differences from the website
- Modes are full-width rows with the hint text visible (the website shows the hint only as a hover tooltip, which a phone has not).
- Package shortcuts are collapsed until tapped (three shortcuts times four modes would fill the screen).
- Clear reports an error if no row was deleted (silent refusal); the website does not.
- Up one level: system Back and the arrow. Jumping straight to a parent step is not offered (the path line is text, not links).
- `updated_by` stays null like the website (an e-mail in a uuid column would be refused).

## Evidence and limits
- `[~]` Written, NOT compiled, NOT run. Check the Actions runs of N1 to N6b.
- Pitfall check by reading: Modifier extensions imported (`clickable`, `defaultMinSize`, `height`, `size`, `width`, `padding`, `alpha`, `clip`, `background`, `border`); `items` imported; no composable call in a LazyColumn builder body outside `item` or `items`; `Folder`, `Lock`, `LockOpen`, `KeyboardArrowDown`, `KeyboardArrowUp` are core Material icons, `KeyboardArrowRight` and `ArrowBack` come from the automirrored set; `AdminToneBadge(text, tone)` is used like in N5; smart casts use local vals (`current`, `node`, `activeKey`, `activeLock`).
- HYPOTHESIS: `lock_key` is the primary key or has a unique constraint (the website's upsert relies on it). If not, the save fails with the server's message in the toast.
- HYPOTHESIS: the admin login may insert, update and delete in `content_locks`. A refusal shows in the toast.
- HYPOTHESIS: the tree copy matches the website today (read from the repo snapshot `website-reference`). It does not follow later website changes by itself.
- Plan status line (plan file not resent): 2026-10-19 | Claude | N6b written `[~]`: Content Locks native. NEXT: N6c Pricing and Catalog (CatalogPanel, 13.7 KB).

## What the owner should test
1. Admin Console > Content Locks opens natively; "All active overrides" matches the website list.
2. Open Freshman package under shortcuts, choose a mode, see the toast "Saved ..." and the badge change; Reset to code default clears it.
3. Walk Grades 9–12 > Grade 9 > Chemistry > Books; set a mode on each step and check the app's student side follows (hub beats subject beats package).
4. System Back goes up one step, then leaves to the admin home.
5. Airplane mode: "Could not reach the server" with Try again.

## NEXT
N6c Pricing and Catalog (audit `CatalogPanel.tsx` first; it may also replace the hand copies of `ADMIN_PACKAGES` and the tree).
