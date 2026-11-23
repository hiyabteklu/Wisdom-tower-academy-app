# ECE expansion: small slices for free-tier agents (Years 1, 2, 4, 5)

> Owner order 2026-11-23. Source plan: `docs/ELECTRICAL_1ST_TO_5TH_YEAR_EXPANSION_PLAN.md` (6 KB, read it once). Quota rules: `AGENTS.md` HARD RULE. **Year 3 is LOCKED.**

## What is already known (verified from `docs/SESSION_2026-10-11_ece_semesters.md`, do not re-read it)
- Year 3 ECE is built into `PackageDetailScreen.kt`: it holds the course lists `ECE_SEM1_COURSES` / `ECE_SEM2_COURSES` and a chooser of two cover cards (`EceSemesterEntry`, `ECE_SEMESTER_ENTRIES`). Package ids: `ece-y3-sem-1`, `ece-y3-sem-2`. Back goes courses, then semester cards, then leaves the package.
- The website uses two levels: `special-packages/[slug]/page.tsx` (semester cards) and `special-packages/[slug]/[semester]/page.tsx` (course list). Semester cover images exist in `assets/images/special-packages/`.
- Not yet checked (Phase 0 still open): how the catalog records packages/courses (Supabase tables vs Kotlin lists), where the six hubs are opened from a course, and where the owner's curriculum blueprint is.

## Slice rules
Each slice = one chat, about 8 tool calls, one commit to `main`, plus `docs/SESSION_<date>_ECE_<slice>.md`. Mark a slice done in the table in the same commit. Never invent course names, stream names or ids: copy from the owner's blueprint or write `UNSURE:`.

| Slice | What | Reads allowed (besides the start list) | Done when | State |
|---|---|---|---|---|
| E0 | Phase 0 audit, read only: use `search_code` (paths only) for `ece-y3-sem-1`, `ECE_SEM1_COURSES`, and the catalog/package table name. Write facts into the section "Audit facts" below: where packages and courses live, how a course opens the six hubs, where the blueprint is. No code change. | search results only, plus at most 2 files under 15 KB | Audit facts filled with file paths | open |
| E1 | Phase 1 data: one NEW small file holding the hierarchy model (year, semester, optional stream, course) with stable ids, containing Year 1 and Year 2 rows copied from the blueprint. No screen. | the blueprint file, E0 audit facts | File pushed, ids listed in the log | open |
| E2 | Year 1 screens: semester chooser then course list, using the existing course workspace route. | the one screen file named in E0 (patch script if over 15 KB) | Phone path Year 1 > Semester > Course > six hubs works; Back at each level | open |
| E3 | Year 2, same as E2 reusing the E2 code. | same | same for Year 2 | open |
| E4 | Year 4 data rows (Semester 1 courses; Semester 2 streams then courses). | blueprint | Pushed | open |
| E5 | Year 4 screens: Semester 1 courses; Semester 2 stream chooser then courses. | screen file | Back works at every level | open |
| E6 | Year 5 data rows (both semesters have streams). | blueprint | Pushed | open |
| E7 | Year 5 screens: stream chooser in both semesters. | screen file | Back works | open |
| E8 | Regression note: Year 3 unchanged, locks and progress still work, CI green. Phone QA is written separately from CI. | CI result only | Log written | open |

## Audit facts (E0 fills this in)
- (empty)

## Do NOT
- Touch Year 3 ids, routes, screens or content.
- Change Supabase policies or data, payments, access rules or hubs, or release an APK.
- Create resource content (books, notes, flashcards, questions, exams, life savers). Navigation and package expansion only.
