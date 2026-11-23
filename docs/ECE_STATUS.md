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
- Course names: SHORT titles like Year 3 ("Applied Electronics II"). No credit hours, ECTS, prerequisites or other blueprint columns (owner order 2026-11-23).

## Facts already verified (do not re-read the source)
- Year 3 ECE lives in `PackageDetailScreen.kt`: course lists `ECE_SEM1_COURSES` / `ECE_SEM2_COURSES`, semester chooser `EceSemesterEntry` / `ECE_SEMESTER_ENTRIES`. Package ids `ece-y3-sem-1`, `ece-y3-sem-2`. Back: courses > semester cards > leave package.
- Website uses two levels: `special-packages/[slug]/page.tsx` (semester cards) and `special-packages/[slug]/[semester]/page.tsx` (course list). Semester cover images are in `assets/images/special-packages/`.
- Blueprint: the owner uploaded the PDF in chat on 2026-11-23 (Haramaya ECE Life Savers 1, May 2025). It is NOT in the repo; the names you need are in "Blueprint (short names)" below, so do not ask for it again.

## Slices (take the first one not DONE; each = one chat, about 8 tool calls, one commit)
| Slice | What | Extra reads allowed | Done when | State |
|---|---|---|---|---|
| E0 | Read-only audit | - | Audit facts filled | DONE 2026-11-23 |
| E1 | Hierarchy model with Year 1 and Year 2 rows. No screen. | none | File pushed, ids listed in the log | DONE 2026-11-23: `ui/packages/EceTreeData.kt` |
| E2 | Year 1 screens: semester chooser > course list > existing course workspace route. Make the "Year 3" strings in `PackageDetailScreen.kt` come from the year shown (Year 3 output must not change). | `PackageDetailScreen.kt` (14 KB; patch script if it grows over 15 KB) | Back works at each level | OPEN (next) |
| E3 | Year 2, same as E2, reusing its code. | same | same | OPEN |
| E4 | Year 4 data rows into `EceTreeData.kt` (S1 courses; S2 streams then courses), from "Blueprint (short names)". | `EceTreeData.kt` | Pushed | OPEN |
| E5 | Year 4 screens: S1 courses; S2 stream chooser then courses. | screen file | Back works | OPEN |
| E6 | Year 5 data rows (both semesters have streams). | `EceTreeData.kt` | Pushed | OPEN |
| E7 | Year 5 screens: stream chooser in both semesters. | screen file | Back works | OPEN |
| E8 | Regression note: Year 3 unchanged, locks/progress work, CI green. Phone QA recorded separately from CI. | CI result only | Written | OPEN |

If a slice is too big for one chat, split it, push the finished half, and add the rest as a new row (for example E2a, E2b).

## Audit facts (E0, 2026-11-23)
- Storage: Kotlin lists, not a Supabase table. Package/course structure is compiled into the app.
- Screen: `app/src/main/java/com/wisdomtower/academy/ui/packages/PackageDetailScreen.kt` (14.4 KB, the one screen file for E2/E3/E5/E7). ECE branch is `isEce = pkg.id.startsWith("ece")`; semester drill-down state is the local `eceSemesterId`; `goBack` and `BackHandler` already go courses > semester cards > leave package. Semester cards use `ECE_SEMESTER_ENTRIES` (`sem-1`, `sem-2`, covers `file:///android_asset/images/special-packages/ece-sem-1.jpg` and `ece-sem-2.jpg`).
- HARD-CODED YEAR 3 in that screen: `ECE_YEAR_LABEL = "Year 3"`, the page title `"<semester> · Year 3"`, and the workspace subtitle `"ECE Year 3 · <semester>"`. E2 must make these come from the year being shown, without changing Year 3 output.
- Year 3 course type `NativeSpecialCourse(code, title, slug, semester, assetImage, path)` and the lists `ECE_SEM1_COURSES` / `ECE_SEM2_COURSES` are in `ui/packages/PackagesData.kt` (30.6 KB, do NOT read it whole; use `search_code` snippets). Year 3 path shape: `/academy/special-packages/electrical-computer-engineering/sem-1/<code lowercase>`, image `file:///android_asset/images/special-packages/courses/<code lowercase>.jpg`. Other users of the lists: `ui/games/GameEnrollment.kt`, `ui/games/GameHubScreen.kt`, `data/content/MySubjectsStore.kt`.
- Opening a course: the screen calls `onOpenSubjectWorkspace(course.title, subtitle, course.path)`. The six-hub workspace is behind that callback (wired in `MainActivity.kt`, not read).
- Package ids and access, each hard-codes Year 3: `data/admin/AdminPackages.kt`, `data/content/PaymentMethods.kt`, `data/content/PackageCheckout.kt` (route `ece/sem-1` and `ece/sem-2` map to the two ids; anything else returns null), `data/content/ContentAccess.kt` (a prefix check on `ece-y3-sem-`: owning one semester unlocks the check for the other). New years need their own ids and an explicit owner decision on access; do not widen that prefix check silently.
- `ui/packages/EceTrackCover.kt` (0.6 KB) is the Home "ECE Engineering" card picture and title for the whole ECE track.

## E1 result (2026-11-23)
- `ui/packages/EceTreeData.kt`: types `EceTreeCourse(id, code, title, path)`, `EceTreeStream`, `EceTreeSemester(packageId, id, label, courses, streams)`, `EceTreeYear`; values `ECE_TREE_YEAR_1`, `ECE_TREE_YEAR_2`, `ECE_TREE_YEARS`. Year 3 is not in it.
- Ids: packages `ece-y1-sem-1`, `ece-y1-sem-2`, `ece-y2-sem-1`, `ece-y2-sem-2`. Course id = lowercase code. Paths: `/academy/special-packages/electrical-computer-engineering/y1-sem-1/<slug>` (same depth as Year 3; `y1-sem-1` replaces `sem-1`).
- No course images exist for the new courses; `OpenCard(title, null)` is allowed (Entrance uses it). Year 1/2 semester covers: reuse `ece-sem-1.jpg` / `ece-sem-2.jpg` unless the owner supplies new ones.
- Blueprint typo: ECEg2052 is printed for both Computer Programming (Year 1 S2) and Applied Modern Physics (Year 2 S2). Computer Programming uses `eceg1052` (the id of its published content); Applied Modern Physics uses `eceg2052`. UNSURE: confirm with the owner.
- Freshman overlap: Year 1 courses are the same as the Freshman package courses (English, Math, Physics, Geography, Psychology, History, Entrepreneurship, Computer Programming...). UNSURE: whether the six hubs of a Year 1 course should reuse the Freshman content (same content, new route) or be separate. E2 needs this answer before wiring paths.
- UNSURE: the workspace route may not accept the `y1-sem-1` path segment yet (not read).

## Blueprint (short names; code - title)
A course that appears in several streams is ONE course (same id, same content). Path must not include the stream. "Exit Exam" rows are not courses: skip. "Elective" rows have no code (`ECEgxxxx`).

**Year 4 S1 (common, 7):** ECEg4101 Computer Architecture and Organization; ECEg4111 Introduction to Communication Systems; ECEg4113 Digital Signal Processing; ECEg4115 Introduction to Control Systems; ECEg4117 Introduction to Power Systems; ECEg4045 Electrical Engineering Lab V; ECEg4119 Introduction to Instrumentation.

**Year 4 S2 by stream (stream ids: communication, computer, control, power, electronics):**
- Communication: ECEg4270 Research Methods and Presentation; ECEg4120 Communication Systems (Digital); ECEg4180 Data Communication and Computer Networks; ECEg4154 Microcomputers and Interfacing; ECEg4140 EM Waves and Guide Structures; ECEg4250 Microelectronic Devices and Circuits; ECEg4142 Antennas and Radio Wave Propagation.
- Computer: ECEg4154; ECEg4160 Data Structures; ECEg4170 Software Engineering; ECEg4182 Computer and Network Security; ECEg4270; ECEg4180; ECEg4150 Embedded Systems.
- Control: ECEg4190 Modern Control Systems; ECEg4154; ECEg4150; ECEg4220 Electrical Machines; ECEg4222 Electrical Installation; ECEg4270; ECEg4260 Power Electronics and Electrical Drives.
- Power: ECEg4220; ECEg4260; ECEg4222; ECEg4210 Power Systems; ECEg4270; ECEg4154; ECEg4212 Power System Operation and Control.
- Electronics: ECEg4240 Principles of Electronic Design; ECEg4250; ECEg4262 Integrated Circuit Technology; ECEg4154; ECEg4270; ECEg4140; ECEg4122 Optoelectronics.

**Year 5 S1 by stream** (every stream also has ECEg5271 Industry Internship and MEng5402 Industrial Management and Engineering Economy; the Power table prints MEng5401, probably a typo, use MEng5402):
- Communication: ECEg5141 Telecommunication and Switching Networks.
- Computer: Elective 1 (choose from ECEg5162 Database Systems, ECEg5242 VLSI Design, ECEg5171 Introduction to Compiler).
- Control: ECEg5191 Neural Network and Fuzzy Logic Control.
- Power: ECEg5231 Power System Planning and Management.
- Electronics: ECEg5241 Analog System Design.

**Year 5 S2 by stream** (every stream also has ECEg5272 B.Sc. Project):
- Communication: ECEg5132 Microwave Devices and Systems; ECEg5130 Wireless and Mobile Communication; ECEg5122 Optics and Optical Communication.
- Computer: ECEg5170 Operating Systems; ECEg5172 Algorithm Analysis and Design; ECEg5202 Introduction to Robotics; Elective 2 (same elective list).
- Control: ECEg5192 Instrumentation Engineering; ECEg5194 Digital Control Systems; ECEg5204 Industrial Process Control and Automation; ECEg5202 Introduction to Robotics.
- Power: ECEg5220 Energy Conversion and Rural Electrification; ECEg5214 Power System Protection; ECEg4190 Modern Control Systems; ECEg5232 Introduction to Power System Automation.
- Electronics: ECEg5242 VLSI Design; ECEg5252 High Frequency Electronic Devices and Systems; ECEg5244 Digital System Design; ECEg5264 Power Electronics Technology and Applications; ECEg5262 PCB Design.

## Log (append-only, newest at the bottom; one line per chat)
| Date | Slice | What was pushed | Left / UNSURE |
|---|---|---|---|
| 2026-11-23 | setup | `START_HERE.md` override, stubbed `CLAUDE.md`, `AGENTS.md`, `docs/AGENT_QUOTA_PROTOCOL.md`, merged slices into this file | E0 next. UNSURE: blueprint location, whether Electrical is a new top-level package |
| 2026-11-23 | E0 | Audit facts above (docs only, no code changed) | E1 blocked on the blueprint. UNSURE: new top-level package vs extending Year 3 (not decided; Year 3 ids and the `ece-y3-sem-` access prefix stay untouched either way). |
| 2026-11-23 | E1 | `ui/packages/EceTreeData.kt` (Year 1 and 2 model, short names, data only) and the blueprint names for Years 4 and 5 in this file | E2 next. Not compiled or built here: CI must confirm it builds. UNSURE: Freshman overlap, ECEg2052 duplicate code, `y1-sem-1` route support, new top-level package vs Year 3 extension, access ids for Years 1 and 2. Phone test: nothing visible yet. |
