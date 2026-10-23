# STATUS CARD PLAN: "Your week" insight + 9:16 picture the student saves to the gallery

Date: 2026-10-23. Owner request. Status: S1 and S2 pushed (CI result of S2 to be confirmed). Nothing seen on a device yet.

## Goal
1. Your status (Learning > Your status) gets a short, human "Your week" block: plain sentences, no AI jargon.
2. A "Save as picture" button turns the same numbers into ONE 1080 x 1920 (9:16) JPG with the logo, name, ID, gauges,
   this week's study time, subjects, the weak subject and ONE clear next step. Saved to the phone gallery.

## Hard rules (owner)
- Free tier: no big inventions. NO new backend, NO new table, NO new dependency, NO new permission (API 29+), NO new font files.
- REAL DATA ONLY (same rule as the rest of Your status): an unknown figure is a dash and a grey ring, never 0, never invented.
  This includes the student ID: use the real one, never make one up (no "WTA-XXXX").
- Plain words. Banned in the picture and in the new block: leverage, dwell, pace, cognitive, optimize, consolidation, retention
  index, "corrective action". Short sentences a Grade 9 student understands.
- Each slice = one small commit, CI green before the next. Push after every slice.

## What already exists (read on 2026-10-23, do not rebuild)
- `NativeStatusModel.kt`: `computeStatus(rows, track)` gives `StatusSummary` (hours, minutes, focusRatioPct, questionsAttempted/Correct,
  accuracyPct, streakDays, tier), `resolveStatusTrack` gives the track name and weekly target hours.
- `StatusBreakdown.kt`: `subjectStatus(...)` gives per subject `totals` (completionPct, itemsOpened/Total, accuracyPct, quiz counts,
  studySeconds), `chapters`, `exams` (averagePct, latestPct, `deltaVsPrevious` per attempt).
- `StatusDayPanel.kt`: `StudyDayLogReader.fetchRecent(7)` = real minutes per day for the last 7 days (this IS the week data;
  the old model comment saying a weekly figure is not possible is out of date for this reader).
- `StatusSubjectsPanel.kt`: `buildEntries(catalog, rows, attempts, picks)` builds the subject list (private today).
- `NativeCoverPageScreen.kt` + `CoverPageRenderer.kt`: the proven pattern for "draw a page with android.graphics.Canvas into a Bitmap".
  Same approach here. `minSdk = 24`, no `java.time` (use `Calendar` / `SimpleDateFormat`).
- Offline copies already exist for all readers (progress, day log, exam attempts). The card uses them and says "as of <date>".
- The other agent's plan used website fields (`weeklyProgressPct`, `readingSpeedWpm`, `toBlob`, `/images/brand/logo.png`).
  Those DO NOT exist in the app. This plan maps the card to the NATIVE fields above.

## Data map (card block -> source)
| Card block | Native source |
|---|---|
| Logo | the app's own logo asset (slice 3 finds the exact file used on the sign-in / splash screen) |
| Name | `studentName` (fallback "Scholar") |
| Student ID | the real ID the app already has for the student (slice 1 checks `UserProfile`); line omitted if none |
| Track | `resolveStatusTrack(...).name` |
| Standing chip | `summary.tier`, shown with plainer words (see Wording) |
| Gauge 1: Study this week | sum of last 7 days from `StudyDayLogReader` vs `track.weeklyTargetHours` (fill = min(done/target, 1)) |
| Gauge 2: Accuracy | `summary.accuracyPct` + "x of y right" |
| Gauge 3: Streak | `summary.streakDays` (fill = min(days/7, 1)) |
| Gauge 4: Focus | `summary.focusRatioPct` |
| 7 mini bars | minutes per day from the day log, today highlighted, total "5 h 20 min this week" |
| Subjects (max 5) | `SubjectBreakdown.totals`: name + thin bar + completion %, most studied first |
| Weak subject box | rule below |
| One next step | fixed templates below (no AI, no randomness) |
| Footer | site name + "saved <date>" |

## Weak subject rule (deterministic)
1. Among subjects with at least 5 quiz answers: the lowest `accuracyPct`.
2. Else among subjects with something opened: the lowest `completionPct`.
3. Else no weak box; show "Open a subject this week to see where you stand."
Only flag when the number is really low (accuracy under 60 or completion under 25). Otherwise show the "good week" tip.

## One next step (templates, first match wins)
1. Last exam of the weak subject went down: "Your last <Subject> exam dropped <n> points. Redo the questions you missed."
2. Accuracy low: "In <Subject> you got <c> of <a> right. Redo the missed ones before opening new chapters."
3. Barely opened: "You opened <o> of <t> items in <Subject>. Start with Chapter <k> short notes today."
4. Under the weekly hours: "You studied <h> of <target> hours this week. 20 more minutes today helps."
5. Nothing to flag: "Good week. Keep the streak alive, 20 minutes today is enough."

## Wording clean-up in the live screen (slice 2b, labels only, needs owner OK of this list)
- "Focus Dwell Ratio" -> "Time you stayed focused"
- "Immediate Corrective Action:" -> "Do this today:"
- "Actionable Step:" -> "Next step:"
- "Passive Dwell Ratio" in the alert title -> "Too much time off focus"
- Standing words: "Elite Scholar Rank" -> "Top performer", "Honors Distinction" -> "Strong", "Solid Foundational" -> "On track",
  "Critical Review Required" -> "Needs work". (Check where `tier` is compared before renaming; if it is only displayed, rename in `computeStatus`.)

## Picture layout (1080 x 1920, one template first)
- Outer border 6 px at inset 24, thin inner border 2 px at inset 40, small corner brackets. Navy background with a soft gradient.
  Accents: cyan (main), amber (streak), emerald (good), rose (weak). Same colours as Your status at night.
- 0-230 Header: logo 120 px left, "WISDOM TOWER ACADEMY" (Tinos serif bold, already bundled), "Weekly status, 17-23 Oct" (from the phone dates).
- 250-470 Identity: name 64 px (shrinks to fit, max 2 lines), "ID ...", track, standing chip.
- 500-1130 Gauges 2 x 2: 300 degree arc, big number in the middle, label under it. Unknown = grey ring and a dash.
- 1150-1360 Week bars: 7 bars + weekday letters + weekly total.
- 1380-1650 Subjects: 5 rows (name, thin bar, %). The weak one gets a rose dot.
- 1670-1830 Weak box: rose border, "Needs work: <Subject>" + the one next step (max 2 lines).
- 1850-1890 Footer: wisdom-tower-academy.live and the saved date.
- Fonts: Tinos (bundled) for titles and name, system sans bold for numbers. No new font files.
- If any text is too long it shrinks, then cuts with "...". Never overflows the border.

## Slices (each one small, safe, pushed alone)
S1. DATA ONLY, no UI. New `StatusCardData.kt`: plain data class + `suspend fun buildStatusCardData(context, rows, summary, track, name, ...)`.
    Makes `buildEntries` / `SubjectEntry` in `StatusSubjectsPanel.kt` `internal` (the only edit to an existing file). Also reads the
    day log and picks the weak subject + tip. Pure functions, easy to review. Check where the student ID lives (`UserProfile`).
S2. "Your week" block on Your status (new composable in its own file + ONE line in `NativeStatusScreen.kt`): 4 to 5 plain sentences
    from `StatusCardData`: hours this week vs target, best day, streak, weak subject + next step. Slice 2b: the wording list above.
S3. Renderer: new `StatusCardRenderer.kt`, `renderStatusCardBitmap(data, logo): Bitmap` (android.graphics.Canvas, same style as
    `CoverPageRenderer.kt`). No UI yet. Gauges, bars, borders, text fitting.
S4. Button "Save as picture" at the end of Your status: builds the bitmap off the main thread, small preview dialog, then saves JPG
    (quality 92) to the gallery: API 29+ via MediaStore into Pictures/Wisdom Tower (no permission); API 24-28 via the system "Save as"
    screen (`CreateDocument("image/jpeg")`, like the Cover Page). Toast on success or failure. Button hidden when signed out or no data.
S5. Polish after the owner has seen it on the phone: colour choice (3 accents like the Cover Page), a second border style, share button.

## Out of scope (free tier)
PDF version, parent report, weekly push notification with the picture, reading speed, any new table or API, any image library.

## Open questions (answer when convenient, none blocks S1)
1. Which logo file should the picture use (the sign-in logo or the round app icon)?
2. OK to rename the standing words and the labels in "Wording clean-up"?
3. Week = last 7 days ending today (as Your status already does) or Monday to Sunday? Default: last 7 days.

## Progress
| Slice | State |
|---|---|
| Plan | pushed (this file) |
| S1 StatusCardData.kt | pushed (#1086, commit 0a2f901). Read-checked 2026-10-23 against StatusBreakdown, NativeStatusModel, MySubjectsStore: field names and types match. CI result not seen by this session |
| S2 part 1 StatusWeekBlock.kt | pushed (#1087, commit ce55f2f). CI FAILED: the same commit overwrote NativeStatusScreen.kt with a broken file ("Expecting a top level declaration" at line 1, plus 4 "Unresolved reference NativeStatusScreen") |
| S2 part 2 restore NativeStatusScreen.kt + one StatusWeekBlock line | pushed (#1088, commit cafdc63). File read in full 2026-10-23: complete, the one call matches the StatusWeekBlock signature. CI result to be confirmed |
| S2b wording clean-up | not started, waits for owner OK (open question 2) |
| S3 to S5 | not started. Do not start S3 until #1088 is green |

Lesson: never push a whole existing file over NativeStatusScreen.kt from a partial copy. Read it first, change one line, push it back.
