# ECE Years 1, 2, 4, 5: status and slices (the ONE live file)

> Owner order 2026-11-23. Rules: `START_HERE.md`. You do not need to read `docs/ELECTRICAL_1ST_TO_5TH_YEAR_EXPANSION_PLAN.md` (6 KB): its essentials are below. **Year 3 is LOCKED** (data, ids, routes, screens).

## Owner decisions (chat, 2026-11-23: these answer every UNSURE from E1)
- **Package structure:** Years 1 and 2 are NEW separate top-level packages (`ece-y1-sem-1`, `ece-y1-sem-2`, `ece-y2-sem-1`, `ece-y2-sem-2`). They are NOT inside Year 3 and there is NO shared "Electrical" top-level package. Year 3 stays exactly as it is. Years 4 and 5 are also just their own cards on the home screen, in order (Year 1, Year 2, Year 3, Year 4, Year 5).
- **Freshman overlap:** the Year 1 courses overlap Freshman courses. Do not reuse the Freshman package's ownership: create the Year 1 courses again under the ECE packages. Same course content, DIFFERENT ownership. Someone who bought Freshman must not see those courses repeated inside ECE, and the other way round (same idea as Economics in Freshman and in Year 3 Semester 1).
- **Duplicate code:** agreed. Computer Programming = `eceg1052`, Applied Modern Physics = `eceg2052`.
- **Access:** separate packages, separate ownership. Buying Year 3 does not give Years 1 and 2 (owner said: different packages, different ownership). UNSURE: confirm only if a checkout rule needs it.

## Target navigation (owner-approved)
- Package > Year > Semester 1 / Semester 2. Every course opens the SAME six hubs: Books, Short Notes, Flashcards, Question Banks, Exams, Life Savers (reuse the existing hub routing; build no new hubs).
- Years 1 and 2: Semester > courses > six hubs.
- Year 4: Semester 1 > courses. Semester 2 > stream chooser > courses.
- Year 5: both semesters > stream chooser > courses.
- A stream is an optional navigation level, not a course and not a hub. Model it as year, semester, optional stream, course, with stable ids.
- Keep access/lock checks and progress tracking. Back must go up one level at every level.
- Course names: SHORT titles like Year 3 ("Applied Electronics II"). No credit hours, ECTS, prerequisites or other blueprint columns (owner order 2026-11-23).

## Facts already verified (do not re-read the source)
- Year 3 ECE lives in `PackageDetailScreen.kt`: course lists `ECE_SEM1_COURSES` / `ECE_SEM2_COURSES`, semester chooser `EceSemesterEntry` / `ECE_SEMESTER_ENTRIES`. Package ids `ece-y3-sem-1`, `ece-y3-sem-2`. Back: courses > semester cards > leave package.
- Website uses two levels: `special-packages/[slug]/page.tsx` (semester cards) and `special-packages/[slug]/[semester]/page.tsx` (course list). Semester cover images are in `assets/images/special-packages/`.
- Blueprint: the owner uploaded the PDF in chat on 2026-11-23 (Haramaya ECE Life Savers 1, May 2025). It is NOT in the repo; the names you need are in "Blueprint (short names)" below, so do not ask for it again.
- Home (`ui/home/HomeScreen.kt`, 25 KB, patch do not resend): `HOME_PROGRAMS` is a getter list of `ProgramItem(id, name, path, assetImage, webImageFallback, accentColor)`. The ECE card is the FIRST item (`id = "ece"`, name "ECE Engineering", path `.../electrical-computer-engineering/sem-1`, picture `ece.jpg`); a tap calls `onNavigateToUrl(program.path)`, so the router turns that path into the package. Year 1 and 2 cards go BEFORE it (owner order: Year 1, Year 2, Year 3).
- `ui/packages/PackagesData.kt` (30.6 KB, patch do not resend): `CATALOG_PACKAGES` holds `NativePackage(id, name, shortName, description, path, assetImage, enrolledLabel, group, accentColor, includes)`. The Year 3 entries are `ece-y3-sem-1` / `ece-y3-sem-2`, group `"special"`, paths `.../sem-1` / `.../sem-2`, at the end of that list.

## Slices (take the first one not DONE; each = one chat, about 8 tool calls, one commit)
| Slice | What | Extra reads allowed | Done when | State |
|---|---|---|---|---|
| E0 | Read-only audit | - | Audit facts filled | DONE 2026-11-23 |
| E1 | Hierarchy model with Year 1 and Year 2 rows. No screen. | none | File pushed, ids listed in the log | DONE 2026-11-23: `ui/packages/EceTreeData.kt` |
| E2 | Year-driven ECE screen: semester chooser > course list > existing course workspace route. "Year 3" strings come from the year shown (Year 3 output unchanged). | `PackageDetailScreen.kt` | Back works at each level | DONE 2026-11-23 (code only; NOT built, NOT tested: CI + phone still to confirm). The screen picks the year from the package id (`ece-y1-` / `ece-y2-` use `ECE_TREE_YEARS`; anything else = Year 3 as before). It cannot be reached yet: no package or Home card exists for Year 1/2 (E2b). |
| E3 | Year 2 screens | - | - | FOLDED INTO E2 (the same code serves Year 2 because `ECE_TREE_YEARS` has it). Year 2 package, Home card and access are in E2b / E2c. |
| E2b | Make Year 1 and Year 2 reachable: add 4 `NativePackage` entries (ids above, group `"special"`, paths `.../y1-sem-1`, `.../y1-sem-2`, `.../y2-sem-1`, `.../y2-sem-2`, covers `ece-sem-1.jpg` / `ece-sem-2.jpg`) to `PackagesData.kt`; add 2 Home cards ("ECE Year 1", "ECE Year 2") before the existing ECE card in `HomeScreen.kt`, path `.../y1-sem-1` and `.../y2-sem-1`; find the router that turns a path into a package (list ONE narrow folder with `fields: ["name"]`, never read `MainActivity.kt` whole) and make it accept the `y1-` / `y2-` paths. Year 3 card, ids and routes untouched. | `PackagesData.kt` and `HomeScreen.kt` by patch script; router file | Tapping the Year 1 card shows the Year 1 semester cards; back works | OPEN (next) |
| E2c | Course workspace + access for Years 1 and 2: make the workspace route accept `y1-sem-N/<slug>` and `y2-sem-N/<slug>` paths and show the same course content as the matching published course (Freshman or other), but owned by the new ECE package ids (separate ownership, see owner decisions). Access ids in `AdminPackages.kt`, `PaymentMethods.kt`, `PackageCheckout.kt`, `ContentAccess.kt`; do NOT widen the `ece-y3-sem-` prefix check. | those four files, `search_code` snippets only | A Year 1 course opens the six hubs | OPEN |
| E4 | Year 4 data rows into `EceTreeData.kt` (S1 courses; S2 streams then courses), from "Blueprint (short names)". Package ids `ece-y4-sem-1` / `ece-y4-sem-2` (new package, own Home card). | `EceTreeData.kt` | Pushed | OPEN |
| E5 | Year 4 screens: S1 courses; S2 stream chooser then courses (the screen code from E2 needs a stream level). | screen file | Back works | OPEN |
| E6 | Year 5 data rows (both semesters have streams). Ids `ece-y5-sem-1` / `ece-y5-sem-2`. | `EceTreeData.kt` | Pushed | OPEN |
| E7 | Year 5 screens: stream chooser in both semesters. | screen file | Back works | OPEN |
| E8 | Regression note: Year 3 unchanged, locks/progress work, CI green. Phone QA recorded separately from CI. | CI result only | Written | OPEN |

If a slice is too big for one chat, split it, push the finished half, and add the rest as a new row (for example E2b, E2c).

## Audit facts (E0, 2026-11-23)
- Storage: Kotlin lists, not a Supabase table. Package/course structure is compiled into the app.
- Screen: `app/src/main/java/com/wisdomtower/academy/ui/packages/PackageDetailScreen.kt` (about 15 KB after E2, the one screen file for E5/E7). ECE branch is `isEce = pkg.id.startsWith("ece")`; semester drill-down state is the local `eceSemesterId`; `goBack` and `BackHandler` already go courses > semester cards > leave package.
- Year 3 course type `NativeSpecialCourse(code, title, slug, semester, assetImage, path)` and the lists `ECE_SEM1_COURSES` / `ECE_SEM2_COURSES` are in `ui/packages/PackagesData.kt`. Year 3 path shape: `/academy/special-packages/electrical-computer-engineering/sem-1/<code lowercase>`, image `file:///android_asset/images/special-packages/courses/<code lowercase>.jpg`. Other users of the lists: `ui/games/GameEnrollment.kt`, `ui/games/GameHubScreen.kt`, `data/content/MySubjectsStore.kt`.
- Opening a course: the screen calls `onOpenSubjectWorkspace(course.title, subtitle, course.path)`. The six-hub workspace is behind that callback (wired in `MainActivity.kt`, not read).
- Package ids and access, each hard-codes Year 3: `data/admin/AdminPackages.kt`, `data/content/PaymentMethods.kt`, `data/content/PackageCheckout.kt` (route `ece/sem-1` and `ece/sem-2` map to the two ids; anything else returns null), `data/content/ContentAccess.kt` (a prefix check on `ece-y3-sem-`: owning one semester unlocks the check for the other). New years need their own ids; do not widen that prefix check silently.
- `ui/packages/EceTrackCover.kt` (0.6 KB) is the Home "ECE Engineering" card picture and title for the whole ECE track (Year 3).

## E1 result (2026-11-23)
- `ui/packages/EceTreeData.kt`: types `EceTreeCourse(id, code, title, path)`, `EceTreeStream`, `EceTreeSemester(packageId, id, label, courses, streams)`, `EceTreeYear`; values `ECE_TREE_YEAR_1`, `ECE_TREE_YEAR_2`, `ECE_TREE_YEARS`. Year 3 is not in it.
- Ids: packages `ece-y1-sem-1`, `ece-y1-sem-2`, `ece-y2-sem-1`, `ece-y2-sem-2`. Course id = lowercase code. Paths: `/academy/special-packages/electrical-computer-engineering/y1-sem-1/<slug>` (same depth as Year 3; `y1-sem-1` replaces `sem-1`).
- No course images exist for the new courses; the E2 screen passes `null` to `OpenCard` (Entrance does the same). Year 1/2 semester covers: `ece-sem-1.jpg` / `ece-sem-2.jpg` are reused until the owner supplies new ones.
- Blueprint typo ECEg2052 (printed for two courses): RESOLVED by the owner, see decisions above.
- UNSURE (E2c): the workspace route may not accept the `y1-sem-1` path segment yet (not read).

## E2 result (2026-11-23)
- `PackageDetailScreen.kt` only. New private `EceCourseCard(title, image, path)`; `EceSemesterEntry.courses` is now `List<EceCourseCard>`. Helpers `eceTreeYearFor(pkgId)`, `eceYearLabelFor(pkgId)`, `eceSemesterEntriesFor(pkgId)`. Year 3 data is mapped through `toEceCards()` (same title, picture and path), so the Year 3 cards, page title (`Semester 1 · Year 3`) and workspace subtitle (`ECE Year 3 · Semester 1`) are written the same as before. Year 1/2: title `Semester 1 · Year 1`, subtitle `ECE Year 1 · Semester 1`.
- Not compiled here. Things CI could flag: the `private` data classes used by private functions in the same file (should be fine), and `OpenCard`'s image parameter accepting `String?` (Entrance already passes `null`).

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
| 2026-11-23 | owner answers + E2 | Owner decisions recorded above (separate top-level packages for Years 1 and 2, separate ownership from Freshman, duplicate code agreed). `PackageDetailScreen.kt` made year-driven (Year 1 and 2 from `ECE_TREE_YEARS`, Year 3 mapped to identical output). Year 2 screens folded into this. Added Home / PackagesData facts and slices E2b, E2c. | E2b next. Not compiled or built here: CI must confirm. Phone test: nothing visible yet (no package or Home card for Year 1/2 exists until E2b). UNSURE: router path match for `y1-`/`y2-`, workspace route for those paths (E2c). |
