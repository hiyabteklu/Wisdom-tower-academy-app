# ADMIN for ECE Years 1, 2, 4, 5: the plan (Copilot: do ALL of it, in order, without stopping)

Owner order 2026-11-23: the admin dashboard must manage the new ECE packages exactly like Year 3: **add content, lock, set price, game**. Everything functional. The owner will not tell you each step. Push everything to `main`.

## Rules (read first)
1. Read `START_HERE.md` and `docs/ECE_STATUS.md` once. Do not re-read what ECE_STATUS "Verified facts" already says.
2. **Year 3 is LOCKED.** Its ids, routes, screens and behavior must not change. Copy its pattern; never edit its data.
3. Live paying users exist. NEVER set `catalog_items.active = true` for any new row. NEVER run SQL against Supabase. If a database change is needed, write it as a file in `docs/sql/` and list it under "Needs owner" at the end.
4. Packages in scope (ids already exist in `AdminPackages.kt`): `ece-y1-sem-1`, `ece-y1-sem-2`, `ece-y2-sem-1`, `ece-y2-sem-2`, `ece-y4-sem-1`, `ece-y4-sem-2`, `ece-y5-sem-1`, `ece-y5-sem-2`.
5. Single source of truth for courses, streams and paths: `app/src/main/java/com/wisdomtower/academy/ui/packages/EceTreeData.kt` (`ECE_TREE_YEARS`). Do NOT retype course lists by hand; generate admin nodes from it so they cannot drift. Never invent course names, prices or content.
6. Commit straight to `main`, one commit per phase, clear messages. CI (GitHub Action building the APK) must be green after the last commit. If it fails, read the error and fix it yourself; keep going until green.
7. Short Kotlin comments only, and never put a slash followed by a star inside a comment (Kotlin nests comments).

## Phase 1: Content tree (this makes "add content" and "lock" work)
File: `app/src/main/java/com/wisdomtower/academy/data/admin/AdminContentTree.kt`.
Today `specialNode()` only lists Year 3 (`ECE_SEMESTERS`). Add Years 1, 2, 4, 5 under "Special packages":
- One node per semester, label like "Electrical & Computer Engineering · Y1 S1", `packageId` = the semester package id.
- Under it, one `subjectNode(packageId, scopePath, slug, "CODE · Title")` per course (six hubs each), built from `ECE_TREE_YEARS`.
- Years 4 and 5 have an optional stream level (year, semester, stream, course). Add a stream node between semester and course where `EceTreeData.kt` has streams. Scope path must be exactly the path the student screens use for that course (see the `path` field in `EceTreeData.kt`, turned into a scope the same way `inferPackageAndScope` in `data/content/ContentAccess.kt` does it). Read that function once to match.
- Year 3 entries stay exactly as they are.
Done when: the admin Content and Locks screens show all 5 ECE years, every course has the six hubs, and lock keys (`package:<id>`, `scope:<path>`, `hub:<path>:<hub>`) are the same ones the student app checks.

## Phase 2: Add content (upload) works for the new packages
Files: `data/admin/AdminContentRepository.kt`, `ui/admin/AdminModules.kt` (and `PaidPdfPicker.kt`, `PaidPdfUploader.kt` if they hold package lists).
- Wherever Year 3 ids (`ece-y3-sem-`) are hard-coded or special-cased, give the new packages the same treatment. A row added under a new scope must be saved with the correct `package_id` for that course's semester (Year 1 S1 course gets `ece-y1-sem-1`, and so on) and the exact `scope_path` the student hub lookup uses (exact match, see ECE_STATUS "Hub content lookup").
- Check Books, Short Notes, Flashcards, Question Banks, Exams, Life Savers all save and list for a new course.
Done when: adding an item under any Year 1/2/4/5 course creates a `learning_resources` row that the matching student hub would list. Paid/free flag works like Year 3.

## Phase 3: Price
Files: `data/admin/AdminPackages.kt`, `ui/admin/CatalogScreen.kt`, `data/admin/AdminSupabase.kt`.
- `AdminPackages.kt` already lists all 8 ids: verify names match the catalog.
- Prices live only in `public.catalog_items` (already has the rows for Years 1, 2, 4, 5 at 300, `active = false`). Verify `CatalogScreen.kt` lists ALL rows including inactive ones, lets the admin edit `price_etb` and flip `active`, and does not filter out the ECE rows. Fix it if it does.
- Do NOT change any price or active flag in the database.
Done when: the admin can see, price and (manually) switch on each new package.

## Phase 4: Game
- Find every place Year 3 appears in game code: `website-reference/src/lib/games/tower-defense/curated-exam-tracks.ts`, `website-reference/src/data/content-availability.ts`, and search the app for tower-defense / game / competition code that lists packages (`rg "ece-y3"` and `rg -i "tower"`).
- Give the new packages the same game wiring Year 3 has, using only real content. A package with no questions must show a clean "no questions yet" state, never fake or placeholder questions.
- If the admin dashboard has a game or competition screen (`ui/admin`, `AdminModules.kt`), make sure the new packages appear in its package list.
Done when: every package list that includes Year 3 also includes the new ones, without breaking Year 3.

## Phase 5: Website admin mirror (same data, only if it builds)
`website-reference/src/data/admin-nav.ts` (the original of the hand copy in AdminContentTree.kt), `website-reference/src/components/admin/LocksPanel.tsx` (`PACKAGE_SHORTCUTS`), `website-reference/src/data/packages.ts`, `website-reference/src/data/special-packages.ts`, `NotificationAudiencePicker.tsx`. Add the new years the same way. Skip a file if the web build is not part of CI, and say so in the log.

## Phase 6: Docs and check
- `docs/ECE_STATUS.md`: add one log row per phase (what was pushed, what is UNSURE). Do not delete rows.
- Confirm CI is green on `main`. Confirm Year 3 files changed only where the plan said (list them).
- End with a plain list: DONE, NOT DONE, NEEDS OWNER.

## Needs owner / Claude (do not do these)
- Supabase: `content_guard.resource_visible` and `open_resource_v2` must accept Year 1/2/4/5 package ids and the "one purchase opens both semesters" rule, and the freshman alias decision (E2c-c, E2c-d). Claude handles this on Supabase after review.
- Setting prices `active = true` (go-live), only after the phone test.
