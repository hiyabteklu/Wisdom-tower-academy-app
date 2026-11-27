# ECE Years 1, 2, 4, 5 + Year 3 consistency: the plan (Copilot: do ALL of it, in order, without stopping)

Owner orders 2026-11-23. The owner will not tell you each step. Push everything to `main`. Order: A, B, C first (they block phone testing), then 1 to 6.

## Rules (read first)
1. Read `docs/ECE_STATUS.md` once. **This plan overrides the reading budget and the "no search" rules in `START_HERE.md`** (those were for a small free chat plan). You may read and search as much as you need.
2. **Year 3 is LOCKED.** Its ids, routes, data and behavior must not change. The ONLY allowed Year 3 edit: which thumbnail file name it uses (Phase C), with a fallback to the old file so nothing breaks.
3. Live paying users exist. NEVER set `catalog_items.active = true`. NEVER run SQL on Supabase. Database changes = files in `docs/sql/` listed under "Needs owner".
4. Packages in scope: `ece-y1-sem-1/2`, `ece-y2-sem-1/2`, `ece-y4-sem-1/2`, `ece-y5-sem-1/2`.
5. Single source for courses, streams, paths: `app/src/main/java/com/wisdomtower/academy/ui/packages/EceTreeData.kt` (`ECE_TREE_YEARS`). Never retype course lists. Never invent names, prices, content.
6. Commit straight to `main`, one commit per phase. CI (APK build) must be green after each phase; if it fails, read the error, fix, continue.
7. Kotlin comments: never a slash followed by a star inside a comment.

## Phase A: Hubs open NATIVE, never the 404 web page (blocks everything)
Bug (phone, 2026-11-23): tapping a Year 1/2/4/5 course opens Study Workspace, and tapping a hub (Books, Notes ...) shows the website "404 Page Not Found".
Root cause (verified in code): `app/src/main/java/com/wisdomtower/academy/ui/study/StudyWorkspaceScreen.kt` has
`SCOPE_ECE = ^/academy/special-packages/[^/]+/(sem-[12])/([^/]+)$` and `TRACK_ECE` with the same `(sem-[12])`. The new paths are `.../y1-sem-1/<slug>` (see `EceTreeData.kt`), so they do not match, `contentScopePath` returns null, and a hub tap falls through to `onOpenHub(... safeHubUrl ...)` which loads a website page that does not exist.
Fix:
1. Change both regexes to accept `((?:y[1-5]-)?sem-[12])`. Scope becomes `ece/y1-sem-1/<slug>` (Year 3 stays `ece/sem-1/<slug>`, unchanged).
2. Find and fix EVERY other place that assumes `sem-[12]` or `ece-y3-sem-` for ECE paths. Search the whole app: `data/content/ContentAccess.kt` (`inferPackageAndScope`, `staticLocks`, `resolveLocks`, `unlockPackageIds`), `data/content/LearningContentRepository.kt` (the alias regex `^ece/(sem-[12])/([^/]+)$` must not break; new scopes match exactly), `ResourceScopeIndex.kt`, `PackageCheckout.kt`, `StatusSubjectBoard`, tracker ids, `MainActivity.kt` (`handleGlobalNav`, hub routing; use search, edit with a small patch, do not retype the file).
3. Safety net: for any path containing `/special-packages/electrical-computer-engineering/` the workspace must NEVER open the website. If a scope cannot be resolved, show a native empty state ("No content yet") instead of `onOpenHub`.
4. Add a unit test (Robolectric/JUnit, in `app/src/test`) that loops over every course in `ECE_TREE_YEARS` and asserts: `contentScopePath(course.path)` is `ece/<yN-sem-M>/<slug>`, and for each of the 6 hubs `contentScopePath(course.path + "/" + hub)` gives the same scope. Also assert Year 3 sample paths are unchanged.
Done when: the test passes in CI, and a Year 1/2/4/5 course opens the native hub grid and each hub opens the native item list (empty is fine where no content exists yet), same as Year 3. No web view anywhere in this flow.

## Phase B: Courses are narrow horizontal rows, not card grids (Years 1, 2, 4, 5 only)
Owner order: these 4 years have NO course thumbnails, so they must not reuse Year 3's two-column image card grid. Courses are listed as narrow horizontal rows with a book icon, the same look as the item rows inside hubs (`app/src/main/java/com/wisdomtower/academy/ui/study/HubItemListScreen.kt`: copy its row style, spacing, colors, shape).
- New composable `EceCourseRow(code, title, onClick)` in `ui/packages/`: full-width, one per line, book icon at the start (Material `MenuBook`), title (one or two lines), code in muted small text, chevron at the end, ripple, min touch height 56 dp.
- Use it in `PackageDetailScreen.kt` for the course lists of Years 1, 2, 4 (S1) and for the courses inside a stream of Years 4 (S2) and 5.
- The Year 4 S2 / Year 5 stream chooser (Communication, Computer, Control, Power, Electronics) is ALSO a list of the same narrow rows (stream name + "N courses"), not cards.
- The semester chooser (Semester 1 / Semester 2) stays as cards WITH the package cover (Phase C).
- Year 3 keeps its image card grid exactly as it is.
- Back goes up one level everywhere (year, semester, stream, course). Keep locks and progress.
Done when: Year 1, 2, 4, 5 course lists are rows with book icons; Year 3 looks the same as before.

## Phase C: Package thumbnails with consistent names (Years 1 to 5) and the build draws them
Naming (the owner uploads these to the WEBSITE repo; the app build copies them):
- Year cards (Home and Learning > Curriculum): `ece-y1.jpg`, `ece-y2.jpg`, `ece-y3.jpg`, `ece-y4.jpg`, `ece-y5.jpg`
- Semester / package covers: `ece-y1-sem-1.jpg`, `ece-y1-sem-2.jpg`, `ece-y2-sem-1.jpg`, `ece-y2-sem-2.jpg`, `ece-y3-sem-1.jpg`, `ece-y3-sem-2.jpg`, `ece-y4-sem-1.jpg`, `ece-y4-sem-2.jpg`, `ece-y5-sem-1.jpg`, `ece-y5-sem-2.jpg`
- Folder in the WEBSITE repo: `public/images/special-packages/` (in this repo the synced copy is `website-reference/public/images/special-packages/`).
Superseded 2026-11-27: ECE cards and semester covers are generated by `CodeCover`; the Android app no longer copies or loads these image files. The website copies under `website-reference/public/images/special-packages/` remain website-only.

## Phase 1: Admin content tree (makes "add content" and "lock" work)
File: `app/src/main/java/com/wisdomtower/academy/data/admin/AdminContentTree.kt`. `specialNode()` only lists Year 3. Add Years 1, 2, 4, 5 under "Special packages":
- One node per semester (label like "Electrical & Computer Engineering · Y1 S1"), `packageId` = the semester package id.
- Under it one `subjectNode(packageId, "ece/<yN-sem-M>/<slug>", slug, "CODE · Title")` per course (six hubs each), generated from `ECE_TREE_YEARS`.
- Years 4 S2 and 5: courses repeat across streams but the path is the same (`y4-sem-2/<slug>`), so list each course ONCE per semester (union of streams) so ids stay unique; put the stream names in the label.
- Year 3 entries stay exactly as they are.
Done when: admin Content and Locks screens show all 5 ECE years; lock keys (`package:<id>`, `scope:<path>`, `hub:<path>:<hub>`) are the same ones the student app checks (Phase A).

## Phase 2: Add content (upload) for the new packages
Files: `data/admin/AdminContentRepository.kt`, `ui/admin/AdminModules.kt`, `PaidPdfPicker.kt`, `PaidPdfUploader.kt`.
- Wherever Year 3 (`ece-y3-sem-`) is hard-coded or special-cased, give the new packages the same treatment. A row added under a new scope is saved with the right `package_id` (Year 1 S1 course gets `ece-y1-sem-1`, and so on) and the exact `scope_path` the student hub reads (`ece/<yN-sem-M>/<slug>`, exact match).
- Check Books, Short Notes, Flashcards, Question Banks, Exams, Life Savers all save and list for a new course.
Done when: adding an item under any new course creates a `learning_resources` row that the matching student hub lists.

## Phase 3: Price
Files: `data/admin/AdminPackages.kt`, `ui/admin/CatalogScreen.kt`, `data/admin/AdminSupabase.kt`.
- `AdminPackages.kt` already lists all 8 ids; verify names.
- Prices live only in `public.catalog_items` (rows for Years 1, 2, 4, 5 exist at 300, `active = false`). Verify `CatalogScreen.kt` lists ALL rows including inactive, lets the admin edit `price_etb` and switch `active`, and does not hide the ECE rows. Fix if it does. Do NOT change any price or flag.

## Phase 4: Game
- Find every place Year 3 appears in game code: `website-reference/src/lib/games/tower-defense/curated-exam-tracks.ts`, `website-reference/src/data/content-availability.ts`, app code for games/competitions, and the admin game or competition screen (`ui/admin`).
- Give the new packages the same wiring using only real content. A package with no questions shows a clean "no questions yet" state, never fake questions.

## Phase 5: Website admin mirror (only if it builds)
`website-reference/src/data/admin-nav.ts`, `components/admin/LocksPanel.tsx` (`PACKAGE_SHORTCUTS`), `data/packages.ts`, `data/special-packages.ts`, `NotificationAudiencePicker.tsx`. Same data, same pattern. Skip a file if the web build is not part of CI and say so in the log. Note: `website-reference` is a snapshot of the website repo; list the same edits in `docs/WEBSITE_ECE_IMAGES.md` under "Website repo changes" for the owner.

## Phase 6: Docs and final check
- `docs/ECE_STATUS.md`: one log row per phase (pushed, UNSURE). Do not delete rows.
- Confirm CI green on `main`. List the Year 3 files changed (should be only the thumbnail helper usage).
- End with: DONE, NOT DONE, NEEDS OWNER.

## Needs owner / Claude (do not do these)
- Supabase: `content_guard.resource_visible` and `open_resource_v2` must accept Year 1/2/4/5 package ids and the "one purchase opens both semesters" rule, plus the freshman alias decision (E2c-c, E2c-d). Claude does this on Supabase after review.
- Updating `catalog_items.image` and website references to the new thumbnail names.
- Setting prices `active = true` (go-live), only after the phone test.
