# Electrical Engineering Package Expansion Plan (1st–5th Year)

**Status: PLAN ONLY — no application code, content rows, or third-year data will be changed in this planning commit.**  
Owner request: add the Electrical program from 1st year through 5th year. Keep the existing 3rd-year experience exactly as it is.

## Owner-approved navigation shape

### 1st, 2nd, and 3rd Year
- Package/year entry → Semester 1 and Semester 2.
- Open a semester → its course list.
- Open a course → the existing six learning hubs:
  1. Books
  2. Short Notes
  3. Flashcards
  4. Question Banks
  5. Exams
  6. Life Savers
- **3rd Year is explicitly protected:** preserve the current package, semester covers, course IDs/paths, hub routing, and content. Do not refactor it as part of this work. Its existing behavior may be used as a reference only.

### 4th Year
- Package/year entry → Semester 1 and Semester 2.
- Semester 1 → course list → course → six hubs.
- Semester 2 → stream selection → courses within selected stream → course → six hubs.

### 5th Year
- Package/year entry → Semester 1 and Semester 2.
- Each semester → stream selection → courses within selected stream → course → six hubs.

## Proposed hierarchy

```
Electrical Package
├── 1st Year
│   ├── Semester 1 → Courses → 6 Hubs
│   └── Semester 2 → Courses → 6 Hubs
├── 2nd Year
│   ├── Semester 1 → Courses → 6 Hubs
│   └── Semester 2 → Courses → 6 Hubs
├── 3rd Year [LOCKED / DO NOT TOUCH]
│   └── Existing navigation and content remain unchanged
├── 4th Year
│   ├── Semester 1 → Courses → 6 Hubs
│   └── Semester 2 → Streams → Courses → 6 Hubs
└── 5th Year
    ├── Semester 1 → Streams → Courses → 6 Hubs
    └── Semester 2 → Streams → Courses → 6 Hubs
```

## Implementation approach (recommended)

Use the existing catalog/package navigation and course-workspace routing patterns; avoid a separate bespoke screen for every year/semester/stream.

- Model the hierarchy explicitly (year, semester, optional stream, course), with stable IDs and paths.
- Keep course-to-resource/hub routing on the existing learning-resource system. Do not create new hub implementations.
- Treat stream as an optional navigation level, not a fake course or hub.
- Keep the current 3rd-year ECE data and routes out of the migration scope; new navigation should be isolated so the existing third-year entry cannot be accidentally re-keyed or redirected.
- Confirm the intended year/semester/stream/course data from the curriculum/source materials before creating course records or labels. Do not invent courses, stream names, or resource paths.
- Preserve access/lock checks, progress tracking, and the six-hub behavior for every course.

## Phased work plan

### Phase 0 — Repository and content audit (read-only)
- Confirm the active app repository and branch, current ECE/year-3 entry points, course catalog representation, and six-hub routing.
- Audit existing year/semester/course/stream records and content availability.
- List gaps without modifying records.
- **Gate:** produce a factual audit; identify what is already implemented and what is missing.

### Phase 1 — Navigation/catalog design
- Add the new hierarchy in the catalog model using stable identifiers.
- Define year 1, year 2, year 4 and year 5 navigation; year 3 remains untouched.
- Validate semester/stream placement against the provided official curriculum.
- **Gate:** review the structure before data import or screen implementation.

### Phase 2 — 1st Year
- Implement semester selector → courses → six hubs.
- Test both semesters, empty states, Back behavior, and course/hub navigation.

### Phase 3 — 2nd Year
- Implement semester selector → courses → six hubs.
- Test both semesters and verify no cross-year/course routing.

### Phase 4 — 4th Year
- Implement Semester 1 → courses → six hubs.
- Implement Semester 2 → streams → courses → six hubs.
- Test stream isolation, course routing, and Back behavior at every level.

### Phase 5 — 5th Year
- Implement both semesters → streams → courses → six hubs.
- Test semester/stream/course isolation, Back behavior, and resource access.

### Phase 6 — Verification and release readiness
- Run build and relevant automated checks.
- Verify each new course opens the correct six hubs and resources.
- Regression-check existing packages, especially **3rd Year (unchanged)**.
- Do not release/publish unless separately requested.

## Acceptance criteria
- [ ] Electrical years 1, 2, 3, 4 and 5 are represented in the requested order.
- [ ] Years 1–3 show Semester 1 and Semester 2 at the year level; years 1 and 2 are added, while year 3 remains unchanged.
- [ ] Year 4 Semester 1 is courses directly; Semester 2 is streams → courses.
- [ ] Year 5 both semesters are streams → courses.
- [ ] Every course opens the same six hubs: Books, Short Notes, Flashcards, Question Banks, Exams, Life Savers.
- [ ] No existing 3rd-year IDs, content, routes, screens, or hub logic are altered.
- [ ] Existing access rules, progress, and package behavior are preserved.
- [ ] All course/stream names and identifiers are sourced—not fabricated.
- [ ] Build/checks are green; phone QA is recorded separately from CI.

## Risks / items to resolve during audit
- Determine whether “Electrical” is a new top-level package that contains the academic years or a new package entry that replaces/extends the current ECE Year 3 entry. **Do not decide by changing existing IDs.**
- Identify how current data maps to package/year/semester/stream paths, including any course content already stored under year-3-only paths.
- The uploaded ECE curriculum blueprint is a source for course/semester/stream names; it must be cross-checked before content creation. This plan intentionally does not copy curriculum rows or infer missing streams.

## Scope boundary
This plan is for the navigation and package expansion only. It does not authorize editing third-year ECE content, creating resource content, changing Supabase policies/data, changing payment/access rules, redesigning hubs, or releasing an APK.
