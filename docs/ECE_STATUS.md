# ECE Years 1, 2, 4, 5: status and slices (the ONE live file)

> Owner order 2026-11-23. Rules: `START_HERE.md`. **Year 3 is LOCKED** (data, ids, routes, screens). Year 4/5 course names are in `docs/ECE_BLUEPRINT_Y4_Y5.md` (read only for E4 to E7).

## Owner decisions (chat, 2026-11-23)
- **Structure:** Years 1 and 2 are NEW separate top-level packages (`ece-y1-sem-1`, `ece-y1-sem-2`, `ece-y2-sem-1`, `ece-y2-sem-2`). NOT inside Year 3, NO shared "Electrical" package. Years 4 and 5 are also their own cards on Home, in order: Year 1, 2, 3, 4, 5.
- **Freshman overlap:** create the Year 1 courses again under the ECE packages. Same content, DIFFERENT ownership. Freshman buyers must not see them repeated inside ECE and the other way round.
- **Duplicate code:** Computer Programming = `eceg1052`, Applied Modern Physics = `eceg2052`.
- **Access:** separate packages, separate ownership. Buying Year 3 does not give Years 1 and 2.
- **Navigation:** Package > Year > Semester 1 / 2. Every course opens the SAME six hubs (Books, Short Notes, Flashcards, Question Banks, Exams, Life Savers); build no new hubs. Year 4: S1 courses; S2 stream chooser > courses. Year 5: both semesters stream chooser > courses. A stream is an optional level (year, semester, optional stream, course; stable ids). Back goes up one level everywhere. Keep locks and progress.
- **Names:** SHORT titles like Year 3. No credit hours, ECTS, prerequisites.

## Verified facts (do not re-read the source)
- Year 3 ECE lives in `ui/packages/PackageDetailScreen.kt` (about 15 KB after E2). Year 3 lists `ECE_SEM1_COURSES` / `ECE_SEM2_COURSES` and type `NativeSpecialCourse` are in `ui/packages/PackagesData.kt` (30.6 KB: patch, do not resend). `CATALOG_PACKAGES` there holds `NativePackage(id, name, shortName, description, path, assetImage, enrolledLabel, group, accentColor, includes)`. Year 3 entries: `ece-y3-sem-1` / `ece-y3-sem-2`, group `"special"`, paths `.../sem-1` / `.../sem-2`, at the end of that list.
- Year 3 path shape: `/academy/special-packages/electrical-computer-engineering/sem-1/<code lowercase>`.
- Home: `ui/home/HomeScreen.kt` (25 KB: patch, do not resend). `HOME_PROGRAMS` is a getter list of `ProgramItem(id, name, path, assetImage, webImageFallback, accentColor)`. The ECE card is the FIRST item (`id = "ece"`, name "ECE Engineering", path `.../electrical-computer-engineering/sem-1`, picture `ece.jpg`); a tap calls `onNavigateToUrl(program.path)`. Year 1 and 2 cards go BEFORE it.
- Router: see `START_HERE.md` section 3 (no MainActivity edit expected).
- Access ids hard-code Year 3 in `data/admin/AdminPackages.kt`, `data/content/PaymentMethods.kt`, `data/content/PackageCheckout.kt` (routes `ece/sem-1`, `ece/sem-2`), `data/content/ContentAccess.kt` (prefix check `ece-y3-sem-`). New years need their own ids; do NOT widen that prefix check.
- Opening a course calls `onOpenSubjectWorkspace(course.title, subtitle, course.path)`; the workspace is wired in `MainActivity.kt` (do not read).
- Website: `special-packages/[slug]/page.tsx` (semester cards) and `[slug]/[semester]/page.tsx` (courses). Semester covers: `assets/images/special-packages/`.

## Done so far
- **E1:** `ui/packages/EceTreeData.kt`: types `EceTreeCourse(id, code, title, path)`, `EceTreeStream`, `EceTreeSemester(packageId, id, label, courses, streams)`, `EceTreeYear`; values `ECE_TREE_YEAR_1`, `ECE_TREE_YEAR_2`, `ECE_TREE_YEARS`. Course id = lowercase code. Paths `.../electrical-computer-engineering/y1-sem-1/<slug>`. No course images yet (screen passes `null`). Semester covers `ece-sem-1.jpg` / `ece-sem-2.jpg` reused.
- **E2:** `PackageDetailScreen.kt` is year-driven: year picked from the package id (`ece-y1-` / `ece-y2-` use `ECE_TREE_YEARS`; anything else = Year 3 as before). Helpers `eceTreeYearFor`, `eceYearLabelFor`, `eceSemesterEntriesFor`; private `EceCourseCard(title, image, path)`. Year 3 output unchanged. Not reachable yet (no package or Home card for Year 1/2).

## Slices (take the FIRST one not DONE; one chat, about 8 tool calls, one commit)
| Slice | What | Extra reads allowed | State |
|---|---|---|---|
| E0, E1, E2, E3 | audit, model, year-driven screen, Year 2 | - | DONE 2026-11-23 (code not yet built or phone-tested) |
| E2b-0 | Find the router | - | SKIPPED: router known (START_HERE section 3) |
| E2b | Make Year 1 and 2 reachable: patch script that adds 4 `NativePackage` entries to `PackagesData.kt` (ids above, group `"special"`, paths `.../y1-sem-1`, `.../y1-sem-2`, `.../y2-sem-1`, `.../y2-sem-2`, covers `ece-sem-1.jpg` / `ece-sem-2.jpg`) and a second patch script adding 2 Home cards ("ECE Year 1", "ECE Year 2", paths `.../y1-sem-1`, `.../y2-sem-1`) BEFORE the ECE card in `HomeScreen.kt`. No MainActivity edit. Done when: tapping the Year 1 card shows Year 1 semester cards, back works. | `PackagesData.kt`, `HomeScreen.kt` (read once each, only to copy an anchor) | PATCH PUSHED 2026-11-23: `scripts/ece_e2b_reachable.py` (ONE script does both files). NOT applied yet: owner runs the Action, then phone test. Mark DONE only after the phone test. |
| E2c | Course workspace + access for Years 1 and 2 (same course content, owned by the new ECE ids; see owner decisions) | the four access files, snippets only | OPEN (next) |
| E4 | Year 4 data rows into `EceTreeData.kt`; ids `ece-y4-sem-1` / `ece-y4-sem-2`, own Home card | blueprint file, `EceTreeData.kt` | OPEN |
| E5 | Year 4 screens (stream level for S2) | `PackageDetailScreen.kt` | OPEN |
| E6 | Year 5 data rows; ids `ece-y5-sem-1` / `ece-y5-sem-2` | blueprint file, `EceTreeData.kt` | OPEN |
| E7 | Year 5 screens (stream chooser both semesters) | `PackageDetailScreen.kt` | OPEN |
| E8 | Regression note: Year 3 unchanged, locks/progress work, CI green; phone QA recorded separately from CI | CI result only | OPEN |

If a slice is too big for one chat, split it, push the finished half, and add the rest as a new row.

## Log (append-only, newest at the bottom; one line per chat)
| Date | Slice | What was pushed | Left / UNSURE |
|---|---|---|---|
| 2026-11-23 | setup to E2 | START_HERE override, E0 audit, `EceTreeData.kt`, year-driven `PackageDetailScreen.kt`, owner decisions recorded | Not compiled: CI must confirm. Phone test: nothing visible yet. |
| 2026-11-23 | cleanup | Docs only: START_HERE shortened (read budget about 8 KB), blueprint moved to `docs/ECE_BLUEPRINT_Y4_Y5.md`, E2b-0 skipped (router known), EDITING_LARGE_FILES step 5 no longer sends agents to PENDING_PATCHES.md | E2b next. UNSURE: router must match the new `y1-`/`y2-` paths via `it.path`; check on phone after the build. |
| 2026-11-23 | E2b | `scripts/ece_e2b_reachable.py` (one script, both files; tested in the chat sandbox on copies of the real anchors, run twice: second run says already patched). Patch only, app unchanged until the Action runs. Suggested branch: `ece-e2b-reachable` | UNSURE: Home cards Year 1 and Year 2 reuse `ece.jpg` (no own picture yet). UNSURE: ECE Year 3 Home card is still named "ECE Engineering" (not renamed, Year 3 locked). Not built: CI on the patch branch is the real test. |
