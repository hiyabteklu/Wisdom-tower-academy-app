# ECE status update 2026-11-23 (alias plan decided) - READ THIS FIRST, then `docs/ECE_STATUS.md`

`docs/ECE_STATUS.md` is long and partly stale. Where it disagrees with this file, THIS FILE WINS.

## Owner decision (final)
- Do NOT copy freshman content into ECE. ONE copy lives under `freshman/<slug>`; ECE Year 1/2 courses show that same content through an alias. When the admin adds material to a freshman subject, it appears in the matching ECE course automatically.
- Ownership stays separate: ECE Year 1/2 owners open the shared subjects; freshman buyers must not get ECE (the ECE hubs are gated by the ECE package ids from E2c-a).
- Owner fallback: if the alias proves too complex, copying is allowed. It is NOT needed now.

## What is DONE on the live database (all recorded; undo notes are in each SQL file)
- E2c-d (earlier today): one purchase opens both semesters of a year: `docs/ECE_E2C_D_SERVER_RULE.sql`. `docs/ECE_STATUS.md` still shows it as OPEN: STALE, it is DONE.
- E2c-e (now): ECE Year 1 owners open the shared freshman Year 1 subjects, Year 2 owners open inclusiveness and global-trends: `docs/ECE_E2C_E_SHARED_FRESHMAN.sql`. Tested in a rolled-back transaction first (results in the file header), then applied as migration `ece_y1_y2_owners_open_shared_freshman_rows`.
- Still true: 4 price rows (300) exist but are OFF (`active = false`). Database has 0 ECE Year 1/2 content rows and that is now correct (nothing to copy).

## NEXT (the only app work left for the alias): E2c-c-app, for AI Studio
One patch script `scripts/ece_e2c_alias.py` (anchors, safe to run twice, stops before writing if an anchor is missing). Do NOT resend `LearningContentRepository.kt` (40 KB) or `ContentAccess.kt` (35.7 KB).
1. In `LearningContentRepository.kt` add ONE helper that maps a scope `ece/y1-sem-N/<id>` or `ece/y2-sem-N/<id>` (N = 1 or 2, id = lowercase course code) to its freshman scope using the table in `docs/ECE_E2C_C_ALIAS_MAP.md`; any other scope returns null and keeps today's behaviour exactly (Year 3 and the `math-natural` alias untouched).
2. Use the helper in the SAME three places the existing aliases are used: `list`, `getHubCounts`, offline `catalogRows`, so Year 1/2 hubs query the freshman rows. No rows are copied. No Supabase change.
3. Read ONLY the function `inferPackageAndScope` in `ContentAccess.kt` to confirm the course path becomes scope `ece/y1-sem-N/<id>`; if it does not, say so in the log instead of guessing.
4. Constraints: do not touch `.env.example`, Year 3 files, prices, locks, or the 4 price rows. One commit, push to `main` as a patch script only; the app changes only when the Action runs it.

## Year 1/2 course to freshman subject (course ids are `EceTreeData.kt` ids)
Year 1: flen1011 english-1, loct1011 logic, math1011 math-natural, phys1011 physics, gees1011 geography, psch1011 psychology, spsc1011 physical-fitness, flen1012 english-2, anth1012 anthropology, math1042 applied-math-1, mgmt1012 entrepreneurship, emte1012 emerging-technology, mcie1012 civics, eceg1052 cpp-programming, hist1012 history.
Year 2: snie1012 inclusiveness, gltr1012 global-trends.
UNSURE (owner: please confirm, easy to change): loct1011 Critical Thinking = freshman logic; mcie1012 Moral and Civic Education = freshman civics; eceg1052 Computer Programming = freshman cpp-programming.
No content anywhere yet (hubs stay empty until the owner uploads): Year 2 engineering courses eceg2041, eceg2043, meng2061, ceng2051, math2061, eceg2061, math2062, meng2052, eceg2070, eceg2052, eceg2072, eceg2042.

## Order from here
1. E2b patch (Year 1/2 reachable) and E2c-a patch (access ids): owner runs the Action, build must pass.
2. E2c-c-app patch (above), build.
3. Phone test with a test account that owns only an ECE Year 1 package: Year 1 opens, a hub lists freshman content, a note opens, a PDF downloads. A freshman-only account must not get into ECE Year 1.
4. Go-live LAST: publish nothing (no copies exist), set the 4 prices `active = true` (last statement of `docs/ECE_E2C_B_PRICES.sql`).

## Log
| Date | What | Left / UNSURE |
|---|---|---|
| 2026-11-23 | Owner chose the alias plan. Read-only check of `effective_mode`, `open_resource_v2`, `learning_resources_public`, locks. Wrote and tested (rolled back) then applied the server rule E2c-e. Pushed this file and the SQL file to `main`. | The app alias patch is not written (next chat, task above). Testing note: free sample rows have item-level open locks, so test with rows whose mode is `require_purchase`. |
