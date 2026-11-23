# ECE Years 1, 2, 4, 5: status and slices (the ONE live file)

> Owner order 2026-11-23. Rules: `START_HERE.md`. You do not need to read `docs/ELECTRICAL_1ST_TO_5TH_YEAR_EXPANSION_PLAN.md` (6 KB): its essentials are below. **Year 3 is LOCKED** (data, ids, routes, screens).

## Target navigation (owner-approved)
- Package > Year > Semester 1 / Semester 2. Every course opens the SAME six hubs: Books, Short Notes, Flashcards, Question Banks, Exams, Life Savers (reuse the existing hub routing; build no new hubs).
- Years 1 and 2: Semester > courses > six hubs.
- Year 4: Semester 1 > courses. Semester 2 > stream chooser > courses.
- Year 5: both semesters > stream chooser > courses.
- A stream is an optional navigation level, not a course and not a hub. Model it as year, semester, optional stream, course, with stable ids.
- Keep access/lock checks and progress tracking. Back must go up one level at every level.
- Open question: is "Electrical" a new top-level package holding the years, or an extension of the current Year 3 entry? Decide without re-keying any existing id; write the decision in the log.

## Facts already verified (do not re-read the source)
- Year 3 ECE lives in `PackageDetailScreen.kt`: course lists `ECE_SEM1_COURSES` / `ECE_SEM2_COURSES`, semester chooser `EceSemesterEntry` / `ECE_SEMESTER_ENTRIES`. Package ids `ece-y3-sem-1`, `ece-y3-sem-2`. Back: courses > semester cards > leave package.
- Website uses two levels: `special-packages/[slug]/page.tsx` (semester cards) and `special-packages/[slug]/[semester]/page.tsx` (course list). Semester cover images are in `assets/images/special-packages/`.
- Source of course/stream names: the owner's ECE curriculum blueprint. NOT yet located in the repo. If you cannot find it with one `search_code` for a path, write `UNSURE: blueprint missing` and do only slices that need no names.

## Slices (take the first one not DONE; each = one chat, about 8 tool calls, one commit)
| Slice | What | Extra reads allowed | Done when | State |
|---|---|---|---|---|
| E0 | Read-only audit. `search_code` (paths only) for `ece-y3-sem-1`, `ECE_SEM1_COURSES`, `PackageDetailScreen`, and one for the blueprint (e.g. `curriculum`, `blueprint`). Fill "Audit facts" below: where packages/courses are stored (Kotlin lists or Supabase table), which file opens a course's six hubs, where the blueprint is. No code change. | search results; at most 2 files under 15 KB | Audit facts filled with real paths | DONE 2026-11-23 (blueprint still missing) |
| E1 | NEW small Kotlin or data file with the hierarchy model (year, semester, optional stream, course, stable ids) holding Year 1 and Year 2 rows copied from the blueprint. No screen. | blueprint, E0 facts | File pushed, ids listed in the log | OPEN, BLOCKED until the owner gives the blueprint |
| E2 | Year 1 screens: semester chooser > course list > existing course workspace route. | the one screen file named in E0 (patch script if over 15 KB) | Back works at each level | OPEN |
| E3 | Year 2, same as E2, reusing its code. | same | same | OPEN |
| E4 | Year 4 data rows (S1 courses; S2 streams then courses). | blueprint | Pushed | OPEN |
| E5 | Year 4 screens: S1 courses; S2 stream chooser then courses. | screen file | Back works | OPEN |
| E6 | Year 5 data rows (both semesters have streams). | blueprint | Pushed | OPEN |
| E7 | Year 5 screens: stream chooser in both semesters. | screen file | Back works | OPEN |
| E8 | Regression note: Year 3 unchanged, locks/progress work, CI green. Phone QA recorded separately from CI. | CI result only | Written | OPEN |

If a slice is too big for one chat, split it, push the finished half, and add the rest as a new row (for example E2a, E2b).

## Audit facts (E0, 2026-11-23)
- Storage: Kotlin lists, not a Supabase table. Package/course structure is compiled into the app.
- Screen: `app/src/main/java/com/wisdomtower/academy/ui/packages/PackageDetailScreen.kt` (14.4 KB, the one screen file for E2/E3/E5/E7). ECE branch is `isEce = pkg.id.startsWith("ece")`; semester drill-down state is the local `eceSemesterId`; `goBack` and `BackHandler` already go courses > semester cards > leave package. Semester cards use `ECE_SEMESTER_ENTRIES` (`sem-1`, `sem-2`, covers `file:///android_asset/images/special-packages/ece-sem-1.jpg` and `ece-sem-2.jpg`).
- HARD-CODED YEAR 3 in that screen: `ECE_YEAR_LABEL = "Year 3"`, the page title `"<semester> · Year 3"`, and the workspace subtitle `"ECE Year 3 · <semester>"`. E2 must make these come from the year being shown, without changing Year 3 output.
- Course lists `ECE_SEM1_COURSES` / `ECE_SEM2_COURSES` (type `NativeSpecialCourse`: `title`, `assetImage`, `path`) are used from `ui/packages/PackagesData.kt` (30.6 KB, NOT read; too big, use `search_code` snippets or a patch script). Other users of the lists: `ui/games/GameEnrollment.kt`, `ui/games/GameHubScreen.kt`, `data/content/MySubjectsStore.kt`. Changing the list shapes affects them.
- Opening a course: the screen calls `onOpenSubjectWorkspace(course.title, subtitle, course.path)`. The six-hub workspace is behind that callback (wired in `MainActivity.kt`, not read). New years should reuse it by giving each course a `path` like the Year 3 ones.
- Package ids and access, each hard-codes Year 3: `data/admin/AdminPackages.kt` (`ece-y3-sem-1`, `ece-y3-sem-2`), `data/content/PaymentMethods.kt` (names `ECE Year 3: Semester N`), `data/content/PackageCheckout.kt` (route `ece/sem-1` and `ece/sem-2` map to the two ids), `data/content/ContentAccess.kt` (a prefix check on `ece-y3-sem-` means owning one semester unlocks the check for the other). New years need their own ids (for example `ece-y1-sem-1`) and an explicit access decision from the owner; do not widen that prefix check silently.
- `ui/packages/EceTrackCover.kt` (0.6 KB) is the Home "ECE Engineering" card picture and title for the whole ECE track.
- Blueprint: one `search_code` (`curriculum OR blueprint`, md/txt/json) found only `docs/ELECTRICAL_1ST_TO_5TH_YEAR_EXPANSION_PLAN.md`. UNSURE: blueprint missing. The owner must paste it or give its path before E1.

## Log (append-only, newest at the bottom; one line per chat)
| Date | Slice | What was pushed | Left / UNSURE |
|---|---|---|---|
| 2026-11-23 | setup | `START_HERE.md` override, stubbed `CLAUDE.md`, `AGENTS.md`, `docs/AGENT_QUOTA_PROTOCOL.md`, merged slices into this file | E0 next. UNSURE: blueprint location, whether Electrical is a new top-level package |
| 2026-11-23 | E0 | Audit facts above (docs only, no code changed) | E1 blocked on the blueprint. UNSURE: new top-level package vs extending Year 3 (not decided; Year 3 ids and the `ece-y3-sem-` access prefix stay untouched either way). Phone test: nothing yet. |
