# MY STATUS V2 (native): real data, per subject / chapter / exam, ring gauges, tap-a-small-icon cards

> Written 2026-10-22 by Claude (chat) from the owner's request. **S1 (data layer), S2 (ring + subject card), S3 (By subject panel in Your status) and S4 (chapter + exam cards) done 2026-10-22, all written but not compiled; S5 and S6 are NOT implemented.** Repo is PUBLIC: no secrets, no student data.
> Rules that apply to every slice: `docs/OWNER_RULE_PLAN_FIRST_PUSH_TO_MAIN.md` (one slice = one commit straight to `main`, tick this file in the same commit), `docs/AGENT_QUOTA_PROTOCOL.md` (free plan, small steps), `docs/UI_POLISH_TASKS.md` G1 to G9 (forbidden files, no invented text, no new dependencies or hosts), `docs/EDITING_LARGE_FILES.md` (big files: patch script, never "the file is too big"), `docs/BUILD_PITFALLS.md`.
> Owner is not a coder: talk in plain words, say what to test on the phone, nothing more. Website repo is NOT touched by this plan (owner rule 7).

## 1. Owner words (2026-10-22, as written)
"Look at my github projects wisdom tower academy app packed, and as u can see in the Website/supabase there are so many analytics gauges, circular gauges, reading time etc. But they are missing in the app. So I want to bring them into the 'my status' section in the tools kit / my learning section, inside my status. Make the status driven by that real data, show detailed status of each subject, chapter, or exam. Don't invent new status design, but we have status indicators in the Website, but they are scattered and ugly, now I want to bring a native status into the app too, user taps a small icon and sees the status like the Website as an overlay beautiful card. First push the plan, cuz I'm free tier user and limit is near."

## 2. What already exists (checked in code 2026-10-22)
- **The native "Your status" screen already exists:** `ui/study/NativeStatusScreen.kt` (34 KB) + `ui/study/NativeStatusModel.kt` (14 KB). Tile "Your status" in the Study Tools dock of `ui/learning/LearningScreen.kt` opens it. It is a port of the website's `StudentAnalyticsDashboard.tsx`, REAL DATA ONLY (owner rule 2026-10-11): header chip, Standing, track selector, summary, 4 HUD cards (Study Time, Focus, Accuracy, Streak), Retention & Exam Accuracy, Recommendations, Alerts. Day/night colours are the `St...` getters (`dayNight(...)`). T8 "My Subjects" filter is applied before `computeStatus` (`docs/T8_MY_SUBJECTS_PLAN.md`).
- **What it does NOT have (this plan):** ring (circular) gauges, reading time, anything per subject, per chapter or per exam, a weekday chart, a small icon that opens a card.
- **Website sources of the "scattered" indicators:** `StudentAnalyticsDashboard.tsx` (HUD, bars, weekday chart), `AcademicResultSaver.tsx` / `CollapsibleProgress.tsx` (progress tracker per scope), `CollapsibleGpa.tsx`, `QuizResultModal.tsx` (result gauge), `PdfReader.tsx` and `PomodoroTimer.tsx` (reading / focus time), `BranchLeaderboard.tsx`. The website computes per-subject numbers in `src/lib/student-knowledge-base.ts` (`courseBreakdown`) but with INVENTED values (random-looking accuracy delta per subject, time split by weights, 42 questions / 34 correct when empty). **Do not port those.**
- **Circular gauge drawing already exists natively:** `ui/study/DownloadGauge.kt` (9.8 KB). Reuse its ring look for every gauge (no new design). The agent must read it first and reuse the drawing, not write a different ring.

## 3. Real data available (verified live in Supabase, project ref `amieczmpqrvavzjikbdi`, read only, 2026-10-22)
All four tables are own-rows-only for the student (RLS `auth.uid() = user_id`); `learning_resources` is readable with the public key for published rows.

| Table | Rows now | Columns that matter |
|---|---|---|
| `learning_progress` | 454 | `resource_id`, `progress_pct`, `last_page`, `total_seconds`, `focus_seconds`, `last_opened_at`, `meta` (`meta.quiz { attempted, correct, durationSeconds? }`, `meta.flashcards { know, learning, again, seen, total, accuracy }`) |
| `learning_resources` | 402 | `id`, `package_id`, `scope_path` (subject = LAST segment), `hub` (`books`, `short-notes`, `videos`, `flashcards`, `question-banks`, `exams`, `life-savers`), `title`, `chapter` (int, nullable), `content_type`, `item_count` (generated: cards or questions count), `published` |
| `exam_attempts` | 50 | `resource_id`, `started_at`, `submitted_at`, `score`, `meta.total` |
| `academic_results` | 51 | `scope_id`, `title`, `total`, `correct`, `missed`, `percent`, `created_at` (what the website tracker shows) |

Also found: `profiles.my_subject_keys` (text[], "chosen in the app Profile > My Subjects") and `profiles.study_subjects` already exist in the database; `profiles.daily_study_goal_minutes` (already passed to the status screen). The app reads progress with `StudentProgressReader.fetchAll()` (`ProgressRow` keeps only quiz attempted/correct) and the subject map with `ResourceScopeIndex` (id -> `package/slug` only, no hub, chapter or title).

## 4. What can be shown honestly, and what cannot
**Can (real numbers):** per subject / chapter: items opened of items published, completion % (mean `progress_pct` over ALL published items, unopened = 0), study time, focus %, reading time (hubs books + short-notes), watch time (videos), question accuracy (`meta.quiz`), flashcards known % (`know / seen`), exams taken, best / latest / average score %, time taken per exam (`submitted_at - started_at`), last active day, last PDF page.
**Cannot, today:** (a) the weekday chart and "minutes today vs daily goal" ring: `learning_progress` stores LIFETIME seconds per item, there is no per-day log; (b) reading speed (WPM) and the reading-method diagnostic: nothing records words per second; (c) solve pace per question (only `durationSeconds` when a quiz saved it). These stay a dash or are left out. **Never estimate** (owner rule 2026-10-11, G4). (a) can be added with a small new table, see slice S6 (needs the owner's OK).

## 5. Design rule: no new design
- Reuse the existing look of `NativeStatusScreen.kt`: `StPanel`, `StChip`, `StHudCard`, `StInfoCard`, the `St...` day/night colour getters, same texts style. Gauge colours use the website's own HUD thresholds: >= 75 emerald, >= 50 amber, below rose, no data = muted ring with a dash.
- Ring = the `DownloadGauge.kt` drawing, parameterised (value 0..1, colour, centre text). One small composable `StatusRing` in a NEW file.
- The "overlay beautiful card" = a bottom sheet / overlay card made of the SAME panels (not a new style), opened by a small gauge icon.
- Keep the T8 filter: only the subjects the student picked are listed (no picks = everything that has data, with the existing one-line note).
- Add new files; keep edits to the two big status files to the smallest possible insert (call one new composable). Check G1 forbidden files first.

## 6. Slices (one per chat; owner says "do S<n>", otherwise the next open one)
| Slice | What | Files |
|---|---|---|
| **S1 data layer, no UI** | (a) `ResourceCatalogIndex` (NEW, disk cache like `ResourceScopeIndex`, which stays untouched): `id, package_id, scope_path, hub, title, chapter, content_type, item_count` of published rows. (b) `ExamAttemptsReader` (NEW): own `exam_attempts` (`resource_id, started_at, submitted_at, score, meta`). (c) `ProgressRow`: add `lastPage` and `flashcardsKnow/Seen` WITH default values (the only constructor is in `StudentProgressReader`). (d) `ui/study/StatusBreakdown.kt` (NEW, pure functions, no UI): `subjectStatus(...)`, `chapterStatus(...)`, `examStatus(...)` with the definitions of section 4. | `data/content/*`, `ui/study/StatusBreakdown.kt` |
| **S2 ring + subject card** | `StatusRing` (reuse `DownloadGauge.kt` drawing) and `StatusDetailCard` (bottom sheet / overlay): hero ring (completion), small rings (accuracy, focus, flashcards), rows for study / reading / watch time, items opened X of Y, exams, last active. Dash where unknown. | `ui/study/StatusRing.kt`, `ui/study/StatusDetailCard.kt` (NEW) |
| **S3 "By subject" in My status** | New panel in `NativeStatusScreen` (one inserted call): one row per picked subject (name from `PackagesData.kt`, mini ring, completion %), a small gauge icon on every row; tap the row or the icon -> subject card (S2). Skeleton + retry in the existing message-card style. | smallest insert in `NativeStatusScreen.kt` + new `StatusSubjectsPanel.kt` |
| **S4 chapter and exam cards** | Inside the subject card: chapter list (group by `chapter`, null = "General"), tap -> chapter card (one line per hub: opened X of Y, time, accuracy). Exams: list of the student's attempts (date, score/total, %, time taken, up/down vs previous, best), tap -> exam card. | `StatusDetailCard.kt`, `StatusBreakdown.kt` |
| **S5 small icon outside Status** | The same gauge icon on subject cards / hub item rows in the course lists (`HubItemListScreen.kt`, package subject list) opening the same card in place. Big files: patch protocol. | tiny inserts, patch script if needed |
| **S6 (needs owner OK first)** | New table `study_day_log (user_id, day, seconds, focus_seconds)` with own-rows RLS; `LearningProgressRepository.saveProgress` also adds today's seconds; then the weekday chart and the "today vs daily goal" ring become REAL. Record the migration name in section 9. Do not start without "yes". | Supabase migration + `LearningProgressRepository.kt` + chart panel |
| **Parked** | Reading speed (WPM), method diagnostic, solve pace: not measurable. | - |

Every slice: log `docs/SESSION_<date>_<letter>.md` with `IN PROGRESS: S<n>` first, commit message ends with `not compiled` unless CI was seen, tick the table in section 8, then tell the owner in plain words what to test.

## 7. Open questions for the owner (defaults let the agent continue; the owner can change them any time)
A. **Where is the small icon?** Default: on every subject row inside "Your status" (S3) and later on subject cards in the course lists (S5). Tell me if you want it somewhere else (for example next to the "Your status" tile).
B. **Weekday chart and daily-goal ring (S6):** OK to add one small new table in Supabase so these become real? Default: NO until you say yes; the status shows no chart rather than a fake one.
C. **Reading speed (WPM):** the app cannot measure it, so it is left out for good unless you say otherwise. Default: left out.

## 8. Progress
| Slice | Status |
|---|---|
| S0 plan (this file + pointer in CLAUDE.md) | done 2026-10-22 |
| S1 data layer | **done 2026-10-22, written, NOT compiled** (log `docs/SESSION_2026-10-22_STATUS_S1.md`) |
| S2 ring + subject card | **done 2026-10-22, written, NOT compiled** (log `docs/SESSION_2026-10-22_STATUS_S2.md`) |
| S3 By subject panel | **done 2026-10-22, written, NOT compiled** (log `docs/SESSION_2026-10-22_STATUS_S3.md`; opens the S2 card) |
| S4 chapter + exam cards | **done 2026-10-22, written, NOT compiled** (log `docs/SESSION_2026-10-22_STATUS_S4.md`) |
| S5 icon in course lists | open (next) |
| S6 day log (owner OK needed) | blocked on question B |

## 9. Live Supabase changes made for this plan
None. (Only read-only table inspection on 2026-10-22.)

## 10. Plan changes
- 2026-10-22 (S1): `ProgressRow.flashcardsKnow/Seen` default to 0 (not null) because 0 seen already means "no flashcard data" and the UI shows a dash then; `lastPage` defaults to null. Names in `StatusBreakdown.kt` carry the suffix `Breakdown` (`SubjectBreakdown`, `ChapterBreakdown`, ...) to avoid clashes with `NativeStatusModel.kt`.
- 2026-10-22 (S2): `StatusDetailCard` takes the subject name as a parameter (`title`); the caller (S3) resolves it from `PackagesData.kt`. The card is a `Dialog` (scrim + bottom card), no new dependency.
- 2026-10-22 (S4): chapter and exam views live inside the same `StatusDetailCard` dialog (internal view state, back arrow returns), so no new file and no change to `StatusBreakdown.kt`; chapters are labelled "Chapter N" / "General" because the data has no chapter titles.
