# GAMES NATIVE PLAN: Tower Climb + Tower Defense (ACTIVE, the ONLY live work)

> Owner order 2026-10-21. Repo is PUBLIC: no secrets, no student data.
> Written by Claude (chat) for Google AI Studio, which does the heavy coding. Claude (chat) fixes compile errors and reviews afterwards.

## 0. READ THIS FIRST (one minute)

1. **This is the ONLY live task.** Every other plan in this repo is PARKED (`docs/PARKED_2026-10-21.md`). Do not continue, fix, re-plan or "tidy" anything else. Do not start competition, notifications, admin, day mode, play store, motion or security slices.
2. **This file overrides**: the reading list and rule 3 of `HANDOFF.md` section 0, the "one slice per chat / do not chain stages" rule of `docs/AGENT_QUOTA_PROTOCOL.md` (AI Studio has a big quota: do MANY phases in one session), and the old "active plan" banners in `AGENTS.md` / `CLAUDE.md`.
3. **Everything else in `HANDOFF.md` section 0 still applies** (forbidden files rule 12, no new dependencies or hosts rule 13, never invent text or numbers rule 11h, public repo). Section 3 below repeats the parts that matter here.
4. **Do not ask the owner questions.** The owner is not a coder and works from a phone. Decide using this plan. If you are truly blocked, write one line starting `UNSURE:` in `docs/GAMES_HANDOFF_FOR_CLAUDE.md` and continue with the safest option.
5. **Role split (important):** AI Studio writes the bulk of the code (many complete files, big output is fine). AI Studio is weaker at polish and compile-cleanliness, so: write simple, explicit Kotlin, re-read every file after writing it, and follow the compile checklist in section 11. Claude (chat) then fixes whatever the CI build rejects.
6. **Commit after every phase** (G0, G1, ...) directly to `main`, one commit per phase, message ends with `not compiled`. Update the progress table (section 14) and `docs/GAMES_HANDOFF_FOR_CLAUDE.md` in the SAME commit. If you cannot push, output complete files and say which path each goes to.
7. Begin immediately with **Phase G0**, then go through the phases in order.

## 1. What the owner wants (his words, summarised)

- The website versions of the two games (Tower Defense, Tower Climb) were commented out because they were awkward: the UI, the game logic and the animation did not feel like a real game and were not engaging. They stay off. **The games are rebuilt NATIVELY in the Android app**, from scratch, with a real-game feel. Do NOT port the website UI or code style.
- **Tower Climb** (built from Question Banks): a tower with many floors, "very tall", built mostly by code (drawn, not photographed), scrollable far past the screen, strong visual design. It shows the player's position. It must show how many climbers are below the player ONLY with real data (see G10); never a made-up number. **The tower must never break or collapse when content is locked or unpurchased.**
- **Tower Defense** (built from Exams): survival game. Questions turn into enemies marching toward the Knowledge Citadel.
- Build them now, while the app is still free.
- Quality bar: it must feel like a mobile game (juice, motion, sound, haptics, clear feedback), not a quiz with decorations. Many students use cheap Android phones: it must stay smooth (aim 60 fps) and work offline for content already opened.

## 2. Facts about this repo (read on 2026-10-21; re-check anything you rely on)

- **No game code exists in the native app.** No Kotlin file mentions Tower Climb or Tower Defense and `ui/` has no games folder. Website versions exist only as reference in `website-reference/src/components/games/` and `website-reference/src/app/games/` (read-only snapshot). The website routes were commented out by the owner (`Header.tsx` drawer links, `exit-exam/page.tsx` link, `LearningContent.tsx` imports). Use the website code ONLY to learn the rules listed in sections 6 and 7; the rules are already copied here.
- **Stack:** Kotlin + Jetpack Compose (Material 3), package `com.wisdomtower.academy`, `minSdk 24`, `compileSdk 36`, Compose BOM as pinned in `gradle/libs.versions.toml` (`docs/BUILD_PITFALLS.md` says 2024.09.00: confirm). Available: coroutines, Room, OkHttp, Coil, `org.json`, Material icons core + extended. **Not available / not allowed to add:** DataStore, any game engine, any new library. No `java.time` (API 24 without desugaring): use `SimpleDateFormat` and `java.util.Calendar`.
- **Question content:** table `learning_resources`. Question banks = hub `question-banks`, `content_type = quiz`; Exams = hub `exams`, `content_type = exam`. `meta` JSON = `{"questions":[{"prompt","choices":[...],"correct":<0-based int>,"solution"}], "durationMin"?}` (20 to 97 questions per bank). `prompt`, `choices`, `solution` are RICH TEXT (inline math `$...$`, `[[term]]`, `==highlight==`, `**bold**`, backslashes may be doubled). Pass them to the page unchanged, exactly like `quiz.html` / `competition.html` do. Real shapes: `docs/CONTENT_SHAPES.md`.
- **Security door (do not bypass):** since slice S4 of the security plan the columns `body_md` and `meta` are NOT readable with a plain list query. Lists are "light" (`LearningResource.metaJson == "{}"`, `itemCount` gives the number of questions). Heavy content only through `LearningContentRepository.getResource(context, resourceId)` which returns `ContentItemResult.Found / NotFound / Failure(message)`; a locked item returns a Failure whose message says it is locked; it keeps an offline copy. Read `data/content/LearningContentRepository.kt`, `LearningResource.kt`, `ContentAccess.kt` (class `CourseLocks`, constants `COMING_SOON_*`, `PURCHASE_*`) and `ui/study/HubItemListScreen.kt` (how chapters and items are grouped and how locked ones are shown).
- **Navigation host:** `ui/study/StudyWorkspaceScreen.kt` has the private sealed interface `WorkspaceView` (HubList, Pdf, Note, Cards, Quiz) and opens `HubItemListScreen` per hub. `contentScopePath(basePath)` gives the course `scopePath`. The shell around it (top bar, floating bottom nav) belongs to `MainActivity.kt` (huge, FORBIDDEN): design the games for the area they are given, do not try to hide the app bars.
- **Reusable pattern for the question card:** `assets/notes/competition.html` + `ui/competition/CompetitionQuestionView.kt` render ONE question with KaTeX in a transparent WebView (`window.setQuestion(json)`, `WTBridge.onPick(index)`, no network, no answers inside the page). The games copy this pattern (section 5.3). Do NOT edit the competition files (parked).
- **Do not write game results into** `exam_attempts`, `academic_results`, `learning_progress` or any competition table: that would pollute the student tracker and the leaderboards. Game progress is stored locally (section 5.7); server sync only in G10.
- **Locks:** a locked chapter/item is still LISTED (the list shows it with a lock and a "Coming soon" or "Package required" dialog). The tower must be built from the whole list, including locked entries.
- **Local time:** the app uses Ethiopia time (EAT, `Africa/Addis_Ababa`) for anything date-based.
- **Build:** GitHub Actions builds every push to `main`. The owner cannot compile; Claude fixes errors from the CI log (the owner pastes the line starting with `e: file:///`).

## 3. Hard rules

1. **New code goes in NEW files** (layout in section 4). Edit existing files ONLY where section 8 says: `ui/study/StudyWorkspaceScreen.kt` (add two `WorkspaceView` cases + Back handling) and `ui/study/HubItemListScreen.kt` (one new parameter with a default value + a banner slot). Both are small/medium files: resend them whole, change only what is needed.
2. **FORBIDDEN (never edit, reformat, delete):** `MainActivity.kt`, `app/src/main/AndroidManifest.xml`, `.env.example`, `.github/**`, `gradle/**`, `gradlew*`, `gradle.properties`, every `*.gradle.kts`, ProGuard/R8/signing settings, `website-reference/**`, existing images/fonts/branding, `server.js`, `package.json`, `vite.config.ts`, `tsconfig.json`, `index.html`, `metadata.json`, the other repositories. Do not delete or rename existing files. Do not add dependencies or network hosts. No secrets anywhere.
3. **Never invent text, numbers or progress.** Game chrome words ("Floor 3", "Start", "Resume") are fine. Every statistic shown must be computed from real play or real data. NO fake leaderboard, fake player names, fake "climbers below" counts, fake XP.
4. **Icons:** draw with Canvas where possible. If you use a Material icon, grep `app/src` for an existing use or be sure it exists in `androidx.compose.material.icons.filled` / `automirrored.filled`. Never use web icon names (Lucide: BookOpen, Sparkles, Brain...).
5. **Text on Canvas:** draw shapes on `Canvas`, put text in normal Compose `Text` overlays. Do NOT use `drawText` / `TextMeasurer` (experimental API risk).
6. **No experimental APIs** (`@OptIn(Experimental...)`), no `rememberRipple`, no `RenderEffect`, no `Modifier.blur` (API 31 only), no new Material3 progress overloads. Use stable basics: `Canvas`, `drawPath`, `drawCircle`, `drawRoundRect`, `Brush`, `Path`, `Animatable`, `rememberInfiniteTransition`, `withFrameNanos`, `graphicsLayer`, `pointerInput` / `detectTapGestures`, `LazyColumn`, `BackHandler`.
7. **Haptics need no permission:** use `LocalView.current.performHapticFeedback(...)`. The manifest is forbidden, so no `VIBRATE` permission, no `Vibrator` service.
8. **Questions only through `getResource`** (the checked door). Never query `meta` directly.
9. **Files:** at most about 600 lines each, one concern per file, unique type names prefixed `Climb`, `Defense`, `Game` (avoid clashing with the 100+ existing files: grep before naming). Pure game logic (rules, state machines, simulation) lives in plain Kotlin files with NO Compose imports, so Claude can fix and test them cheaply.
10. **Unsure about an API?** Grep `app/src` for an existing usage and copy its imports and call shape. Do not guess.
11. The app is installed and approved: do not change the look of any existing screen.

## 4. File layout (create exactly these; add more only if needed)

```
app/src/main/assets/notes/game_question.html            copy of competition.html + reveal state (5.3)
app/src/main/java/com/wisdomtower/academy/data/games/
  GameQuestion.kt          model, JSON parse, fair shuffle (5.1)
  GameQuestionLoader.kt    loads banks / exams through getResource, in-memory cache (5.2)
  GameStore.kt             local progress, SharedPreferences JSON per student (5.7)
  ClimbTower.kt            builds floors from the light list, zones, unlock rules (6.1)
  ClimbSession.kt          one floor run: state machine (6.2)
  ClimbRewards.kt          constants: XP, stars, thresholds (6.2)
  DefenseTuning.kt         ALL defense constants in one place (7.3)
  DefenseEngine.kt         pure simulation (7.3)
app/src/main/java/com/wisdomtower/academy/ui/games/
  common/GameQuestionCard.kt   WebView wrapper (5.3)
  common/GameClock.kt          frame loop + auto pause (5.4)
  common/GameAudio.kt          synthesized sounds (5.5)
  common/GameHaptics.kt        (5.6)
  common/GameParticles.kt      pooled particles
  common/GameTheme.kt          palettes, zone colours, low-end switch (5.8)
  common/GamePauseOverlay.kt   pause / quit / sound toggles
  common/GameButtons.kt        primary and secondary buttons, power-up chips
  climb/TowerClimbScreen.kt    host: loading -> tower -> floor -> result
  climb/TowerMapView.kt        exterior tower (6.3)
  climb/FloorSceneCanvas.kt    interior cutaway scene (6.4)
  climb/OwlPainter.kt          owl + lantern drawing
  climb/ClimbHud.kt
  climb/FloorResultSheet.kt
  climb/AtticScreen.kt         review of missed questions (6.5)
  defense/TowerDefenseScreen.kt  host: picker -> play -> review -> results
  defense/DefenseCanvas.kt       scene rendering
  defense/DefenseSprites.kt      painters: citadel, enemies, projectiles
  defense/DefenseHud.kt
  defense/ExamPickerScreen.kt
  defense/DefenseResultScreen.kt
docs/GAMES_HANDOFF_FOR_CLAUDE.md   you maintain it (section 13)
docs/sql/games_climb_progress.sql  G10 draft only, NOT applied
```

## 5. Shared kit (phases G1 to G3)

### 5.1 `GameQuestion.kt`
`data class GameQuestion(id: String, bankId: String, bankTitle: String, chapter: Int?, prompt: String, choices: List<String>, correctIndex: Int, solution: String, keepOrder: Boolean)`; `id = "<bankId>:<index>"`.
- Parse with `org.json` from `LearningResource.metaJson`. Skip (and count) entries with a blank prompt, fewer than 2 choices, or `correct` outside the range. Keep rich text raw.
- **Fair shuffle:** `fun shuffled(q, random): GameQuestion` permutes the choices and remaps `correctIndex`. Do NOT shuffle when `keepOrder` is true: any choice, lower-cased and trimmed, matches `(all|none|both|neither)\s+(of\s+)?(the\s+)?(above|these|them)` or `both\s+[a-d]\s+and\s+[a-d]`. Over many questions the correct letter must be spread evenly (the owner had a bug where about 80 percent of answers sat at A): add a short comment explaining the remap and test mentally with `correct = 0, 2, 3`.

### 5.2 `GameQuestionLoader.kt`
`suspend fun load(context, resource: LearningResource): LoadResult` with `Loaded(questions, skipped)`, `Locked(message)`, `NeedsInternet`, `Failed(message)`. Uses `LearningContentRepository.getResource`. In-memory cache of the last 12 parsed banks. A `Failure` whose message contains "locked" becomes `Locked`. Preload (in the background) the banks of the NEXT floor while the student plays.

### 5.3 Question card: `assets/notes/game_question.html` + `GameQuestionCard.kt`
Copy `competition.html` and extend it. Keep: transparent background, KaTeX + `richtext.css` / `richtext.js`, `blockNetworkLoads`, ignore every link, no answer key inside the page before the reveal.
- `window.setCard(jsonText)` with `{ id, prompt, choices, selected (-1 none), enabled, hidden: [indexes removed by 50/50], reveal: null | { correct: int, solution: string } }`.
- `reveal` state: the correct choice gets a green style, a wrong selected choice a red style plus a short shake (CSS only), the `solution` is rendered below with `WTRich.render` (same renderer, math works). Respect `prefers-reduced-motion`.
- Look: dark glass card like `qcard`, bigger tap targets (min 48dp), letter badges A to D, choice entrance animation 160 ms staggered 30 ms (CSS only).
- `WTBridge.onPick(index)` as in competition. Kotlin judges the answer, never the page.
- Compose side: `GameQuestionCard(card: GameCardState, onPick: (Int) -> Unit, modifier)`; copy the WebView set-up, `onRelease` clean-up and `rememberUpdatedState` handling of `CompetitionQuestionView.kt` (read it). One WebView per game session, updated with `setCard`, not recreated per question.

### 5.4 `GameClock.kt`
`@Composable fun GameFrameLoop(running: Boolean, onFrame: (dt: Float) -> Unit)` using `LaunchedEffect` + `withFrameNanos`; clamp `dt` to at most 0.05 s. Pauses automatically when the app leaves the foreground (use the `LocalLifecycleOwner` import that other files in `app/src` already use: grep it). Game time never advances while paused.

### 5.5 `GameAudio.kt`
No asset files. Synthesize short 16-bit mono PCM tones (22050 Hz) once, on a background thread, into static `AudioTrack`s (`USAGE_GAME`). Sounds: tap, correct (two rising notes), wrong (low buzz), combo, heart lost, floor clear / victory fanfare, defeat, timer tick, freeze, shot, hit. Replay with `stop()`, `reloadStaticData()`, `play()` in try/catch. Any failure = silent game, never a crash. A `muted` flag stored in `GameStore`.

### 5.6 `GameHaptics.kt`
`performHapticFeedback` only: `CONTEXT_CLICK` for taps and correct, `LONG_PRESS` for heart lost, `REJECT` on API 30+ (else `LONG_PRESS`) for wrong. A toggle in the pause overlay (stored in `GameStore`).

### 5.7 `GameStore.kt`
SharedPreferences `wt_games_v1`, one JSON string per student id (read the student id from `AuthStateManager`; fallback `guest`). Keys by scope:
- Climb per `scopePath`: cleared floors keyed by a STABLE floor key (`chapter:<n>` or `bank:<resourceId>`, never by position), stars (1 to 3), best accuracy, plays; `xp`; `bestStreak`; `lastDailyDate` (`yyyy-MM-dd`, EAT) and `dailyStreak`; `attic`: list of `{questionId, bankId, correctInRow}`.
- Defense per `scopePath` + examId: `bestScore`, `bestWave`, `bestCombo`, `runs`.
- Global: `soundOn`, `vibrationOn`.
All writes are small and synchronous-safe (`apply()`). Corrupt JSON = start fresh, never crash.

### 5.8 `GameTheme.kt` and the low-end switch
Brand colours come from `ui/theme/Color.kt` (`WisdomCyan`, `WisdomMuted`, gold/amber already used in the app: grep). Zone palettes (6.3) live here. `isLowEnd(context)`: `ActivityManager.isLowRamDevice` or `memoryClass <= 128` or `SDK_INT < 26`. Low-end: particle cap 24 (otherwise 80), no cloud/parallax layers, no glow gradients on every frame (draw them once into a cached `Brush`), 30 fps simulation allowed. Also read `Settings.Global.ANIMATOR_DURATION_SCALE`: if 0, skip decorative motion (keep gameplay).

### 5.9 Pause and Back
`GamePauseOverlay`: Resume, Sound on/off, Vibration on/off, Quit. The phone Back button (BackHandler) opens it during play instead of leaving. App going to background pauses automatically. Screen stays on during play (`keepScreenOn` on the host view, released on exit).

## 6. TOWER CLIMB (source: Question Banks ONLY, never Exams)

### 6.1 Building the tower (`ClimbTower.kt`, pure Kotlin)
Input: the LIGHT list of hub `question-banks` for one course `scopePath` (all rows, including locked ones) + the lock information the list screen already uses.
- **Floor = chapter.** Group rows by `chapter` ascending; rows with a null chapter each become their own floor (ordered by `sortOrder`) after the chaptered ones. Floor title comes from the data (the same "Chapter N" wording `HubItemListScreen` uses, or the bank title). Floor key = stable key of 5.7. Floor number = position (1-based). A floor's question pool = all its banks.
- **Boss floor:** every 5th floor (5, 10, 15 ...).
- **Zones** (five, drawn differently): Stone Foundation, Library Hall, Clockwork Gallery, Observatory, Sky Crown. With 41 or more floors use the fixed ranges 1 to 10, 11 to 20, 21 to 30, 31 to 40, 41+. With fewer floors spread the five zones proportionally over the tower, so even a 6-floor course climbs through all of them.
- **Sealed floors** (locked by the admin or not purchased): still drawn, with chains and a padlock; tapping shows the same message the list screen shows (reuse `HubGateDialog` and the `ContentAccess` wording constants, mirroring how `HubItemListScreen` chooses between Coming soon and Package required). **A sealed floor never blocks the climb:** the next playable floor is the lowest NOT-cleared OPEN floor. The tower is never shortened, hidden or reordered because of locks.
- Cleared floors can be replayed any time. Empty course (no banks) = friendly empty state, not a crash.
- Make the tower feel tall even when small: bottom Foundation block + top Sky Crown block + at least 1.5 screens of scroll.

### 6.2 Rules (`ClimbSession.kt` + `ClimbRewards.kt`, pure Kotlin, injected `Random`)
- Normal floor: 8 questions drawn at random from the pool (all of them if fewer than 8; never invent), 3 hearts, 20 seconds per question. Each floor starts with one 50/50 and one Extra Time (+10 s) charge.
- Boss floor: 12 questions drawn from the pools of ALL open floors up to and including this one, 4 hearts, 20 s, NO power-ups, distinct milestone styling.
- Wrong answer or timeout: lose a heart, solution shown (card reveal) with a "Got it" button, then continue. Hearts reach 0: the floor is failed ("The lantern goes out": Retry or Back to tower).
- Pass: accuracy at least 50 percent (boss: 60) and hearts above 0. Stars: 3 stars at 90 percent or more, 2 stars at 70 percent or more, otherwise 1 star.
- XP (all constants in `ClimbRewards`, easy to tune): 10 per correct answer, +2 per answer in a streak beyond the first (cap +10), boss x2, +20 first clear of a floor.
- Streak = consecutive correct answers inside a run (shown as a flame).
- The same question never repeats inside one run. Prefer questions currently in the Attic.
- States: `Loading`, `Question(index, timeLeft)`, `Reveal(wasCorrect)`, `FloorComplete`, `Failed`. Events: `pick(i)`, `timeout()`, `usePowerUp(type)`, `acknowledge()`.

### 6.3 Exterior tower (`TowerMapView.kt`)
- A `LazyColumn(reverseLayout = true)`: one item per floor (about 200dp tall), each item draws its own wall segment on a `Canvas`. Never one giant canvas. Precompute `Path`s and `Brush`es once.
- Background sky behind the list changes smoothly with the scroll position (interpolate the zone palettes).
- **Zone looks (by code, original art):** Stone Foundation (brick courses, moss, torches), Library Hall (bookshelves of coloured rectangles, hanging lamps), Clockwork Gallery (gears that rotate slowly, driven by ONE shared infinite transition, only for visible rows), Observatory (dome, telescope, star field), Sky Crown (clouds, golden spires, soft aurora). Foundation block at the bottom, Sky Crown cap at the top.
- **Floor states:** Cleared (warm lit window, golden lantern, 1 to 3 stars), Current (pulsing glow, the owl sits here), Available, Sealed (chains + padlock, dimmed). Boss floors are wider, with banners and a crest.
- **Owl mascot with a golden lantern** (wise, scholarly, drawn in `OwlPainter.kt` from ovals/paths: body, wing flap, blinking eyes, glasses optional, lantern flicker). It idles with breathing and blinks, flaps when moving.
- Tap a floor: bottom sheet with title, number of questions (real count from `itemCount`, or the pool size once loaded), best stars, [Climb]. Sealed floor: the lock message.
- Header (Compose): back, course title, real XP, real streak flame, sound toggle. Chips: **Attic** (shows the real number of missed questions) and **Daily Climb**. Floating "Jump to me" button scrolls (`animateScrollToItem`) to the current floor. A thin progress rail on the right edge shows the whole tower as a mini-map with the owl dot (tap to jump).
- **Climbers below you:** only when G10 real data exists. Until then show nothing, not zero, not a guess.

### 6.4 Floor play (`FloorSceneCanvas.kt`, `ClimbHud.kt`)
Split screen: top about 40 percent is the floor scene (cutaway room with a ladder or spiral stair of N rungs where N = number of questions, torches, dust motes), bottom about 60 percent is `GameQuestionCard`.
- Correct: the owl hops to the next rung (arc jump about 420 ms, squash 0.85 then stretch 1.15 on landing), lantern flares, sparkles, small camera ease, ascending sound, light haptic.
- Wrong or timeout: the owl loses its grip (shake and a half-rung slide that returns), a heart cracks and shatters into particles, screen shake 6dp decaying over 180 ms, red vignette flash (alpha 0.35, 140 ms), dull buzz, strong haptic, then the solution reveal.
- Timer: a ring around the lantern; the last 5 seconds the flame shrinks and flickers faster and a tick plays. Timeout = wrong.
- Streak: from 3 in a row the lantern burns brighter and a flame icon with the number appears.
- Progress pips (one per question: gold filled, red wrong, empty pending). Hearts drawn as heart paths (not text). Power-up chips bottom corners of the scene.
- Floor complete: the door at the top opens with a light burst, `FloorResultSheet`: stars pop in one after another (scale overshoot 1.3 to 1.0 over 300 ms, 150 ms apart) with particles, XP counts up over 700 ms, accuracy, buttons [Next floor] [Replay] [Back to tower]. Next floor = the tower view scrolls up and the owl hops to the next floor. First clear of a floor gets confetti.
- Game feel numbers (tune, but keep them): hit-stop 70 ms on a hit, camera lerp 0.12 per frame, lantern flicker from two summed sine waves.

### 6.5 Review Attic and Daily Climb
- **Attic:** every missed question is stored (`questionId`, `bankId`). The Attic screen runs them through the same card. A question is removed after 2 correct answers in a row (mastered). Show the real count only.
- **Daily Climb:** 8 mixed questions from all OPEN banks of the course, bonus XP the first time each EAT day; `dailyStreak` +1 when played on consecutive EAT days, otherwise reset to 1. Replays give no bonus. If fewer than 4 questions are available, hide the button.

## 7. TOWER DEFENSE OF KNOWLEDGE (source: Exams ONLY, never Question Banks)

### 7.1 Source and modes
Hub `exams` of the course `scopePath` (an Entrance Questions year page is such a scope too). `ExamPickerScreen` lists the exams (title, real `itemCount`; locked ones shown sealed with the same lock message). Two modes:
- **Exam Run:** one chosen exam; each of its questions appears exactly once; victory when all are answered with the citadel standing; waves = groups of 5 questions.
- **Endless:** all OPEN exams of the course merged and shuffled (reshuffle when exhausted); survive as long as possible.
Ignore `durationMin` (the march is the timer).

### 7.2 The scene (`DefenseCanvas.kt`, `DefenseSprites.kt`)
Side-view battlefield. The Knowledge Citadel (drawn tower, glowing core) stands on the left; enemies march in from the right along a ground lane. 3 parallax layers, torches, drifting embers; the sky shifts dusk to night as waves rise. Citadel health = glowing crystal shards on the citadel (plus a hearts HUD). Original enemy designs, drawn from paths: **Scout** (small, fast, jagged spark creature), **Standard** (rounded ink golem with glowing eyes), **Armored Boss** (iron tome knight with a cracked shield). Pre-build every sprite `Path` once; zero allocation in the draw loop.

### 7.3 Rules and engine (`DefenseEngine.kt` + `DefenseTuning.kt`, pure Kotlin)
Fixed-step simulation at 1/60 s driven by the frame loop; the engine emits events (`Spawned`, `Hit`, `Killed`, `CitadelDamaged`, `PowerUpUsed`, `WaveCleared`, `Victory`, `Defeat`) that the UI turns into sound, haptics and particles. All numbers live in `DefenseTuning`:
- Citadel health 5. Base time per question 25 s at wave 1, shrinking by about 1.2 s per wave down to a floor of 10 s (later waves are tighter). Scouts march about 25 percent faster, Armored about 25 percent slower.
- **The march IS the timer.** Only the lead enemy is the target; the question card shows its question. An enemy is spawned when it becomes the target and walks from the right edge so that it needs exactly the question's time to reach the citadel. The enemies queued behind it are cosmetic silhouettes (their number equals the real remaining questions in the wave, shown up to 6), so timing is always fair.
- Correct: a bolt of light flies from the citadel in an arc, impact burst, the enemy dissolves, score popup, combo grows. Armored Boss needs 2 correct answers (two different questions): the first cracks the armor and knocks it back 35 percent of the lane.
- Wrong or timeout: the enemy reaches the citadel, the citadel shakes, loses 1 health (Armored: 2), hit-stop, then the solution reveal; the march stays paused while the student reads and resumes with [Next].
- Combo multipliers on score: x2 at 3 in a row, x3 at 5, x4 at 8. Score per kill = 100 x multiplier + up to 50 for time left.
- Waves: every 5 questions. Wave 1 Standard only; Scouts from wave 2; every 3rd wave ends with an Armored Boss.
- **Power-ups** (chips on the HUD, each with a charge count): *Chronos Stasis* freezes the march for 12 s (ring turns ice blue), *Logic Filter* removes two wrong choices, *Fortify Core* +1 citadel health (cap start + 2), *Tactical Deflection* skips the current enemy with no damage and no score (listed as skipped in the review). Start with 1 Chronos and 1 Logic; earn Fortify at a streak of 6 and Deflection at a streak of 10 (max 2 of each).
- Choice shuffle: the fair shuffle of 5.1 at runtime.
- Pause, Back, background pause: section 5.9.

### 7.4 Flow and results
Picker -> short countdown (3, 2, 1) -> play -> end. `DefenseResultScreen`: score, accuracy, best combo, waves cleared, time, new best badge (only if truly better), then **Missed questions review**: every missed or skipped question shown with the card in reveal mode and its solution, paged with next/previous. [Play again] [Back]. Save bests in `GameStore`.

## 8. Entry points and wiring (small edits to two existing files)

- `HubItemListScreen`: add `onOpenGame: (() -> Unit)? = null` (default null, so no other caller breaks: grep its callers). When `hubId == "question-banks"` and the list has at least one row, show a banner card ABOVE the list: "Tower Climb" with a small mini-tower drawn on Canvas. When `hubId == "exams"`, the same banner for "Tower Defense". The banner calls `onOpenGame`. No banner when the list is empty or still loading.
- `StudyWorkspaceScreen`: add `WorkspaceView.Climb(hubId)` and `WorkspaceView.Defense(hubId)` (or one `Game` case), render `TowerClimbScreen(scopePath, courseTitle, locks, onBack)` / `TowerDefenseScreen(...)` inside the existing `when`, and make Back return to `WorkspaceView.HubList(hubId)`. The games own their inner Back handling through the pause overlay (like the quiz screen does with its own BackHandler).
- Do not touch the bottom navigation, the drawer or `MainActivity.kt`. A global Games entry on Home is a later owner decision.

## 9. Locked content (summary)
Locks change what is playable, never what is drawn. Sealed floors and sealed exams stay visible with a padlock and the standard message. The tower height, floor order and zones are computed from the full list. If a bank is opened and the door says locked at play time (the owner locked it meanwhile), show the lock message and return to the tower; never crash.

## 10. Phases (do them in order; one commit each)

| Phase | Deliverable | Done when |
|---|---|---|
| **G0** Recon (no code) | Read the files named in sections 2 and 3. Fill section 15 "Verified signatures" with the REAL signatures copied from the code: `LearningContentRepository.list` and `getResource`, `CourseLocks` lock checks for hub/chapter/item, `HubItemListScreen` parameters and callers, how `AuthStateManager` exposes the student id, theme colour names, the `LocalLifecycleOwner` import in use. Create the skeleton of `docs/GAMES_HANDOFF_FOR_CLAUDE.md` if missing. | Section 15 has real signatures. |
| **G1** Data kit | `GameQuestion.kt`, `GameQuestionLoader.kt`, `GameStore.kt` (no Compose imports) | Parse, skip rules, fair shuffle, cache, store compile-clean by reading. |
| **G2** Question card | `game_question.html`, `GameQuestionCard.kt` | setCard, hidden choices, reveal with solution, no network, no answer before reveal. |
| **G3** Feel kit | `GameTheme`, `GameClock`, `GameAudio`, `GameHaptics`, `GameParticles`, `GamePauseOverlay`, `GameButtons` | Each file stands alone and is used by later phases. |
| **G4** Climb logic | `ClimbTower`, `ClimbSession`, `ClimbRewards` (pure Kotlin) | Rules of 6.1 and 6.2 complete; sealed floors never block. |
| **G5** Climb tower view | `TowerClimbScreen` (loading, empty, error, tower), `TowerMapView`, `OwlPainter`, floor sheet, sealed handling | Tall scrollable tower, five zone looks, owl, jump to me, mini-map rail. |
| **G6** Climb play + wire-up | `FloorSceneCanvas`, `ClimbHud`, play flow, `FloorResultSheet`, power-ups, fail state, `AtticScreen`, Daily Climb, entry banner and `WorkspaceView.Climb` (section 8) | **OWNER TEST POINT 1:** Tower Climb playable end to end. |
| **G7** Defense engine + scene | `DefenseTuning`, `DefenseEngine`, `DefenseSprites`, `DefenseCanvas` | Simulation and drawing complete, zero per-frame allocation. |
| **G8** Defense flow + wire-up | `ExamPickerScreen`, `TowerDefenseScreen`, `DefenseHud`, power-ups, reveal, `DefenseResultScreen`, entry banner and `WorkspaceView.Defense` | **OWNER TEST POINT 2:** Tower Defense playable end to end. |
| **G9** Polish and safety pass | Low-end mode, reduce-motion, memory check (cache sizes, WebView release), re-read EVERY new file against section 11, update docs and the owner test list | Handoff lists every file and every UNSURE. |
| **G10** Real climbers-below (only if the owner says go) | `docs/sql/games_climb_progress.sql` draft (table of own progress per student and scope with row-level security for own rows; an aggregate function returning only COUNTS of climbers below/above, never names), a Kotlin repository behind a constant `GAMES_SERVER_ENABLED = false`, UI that shows the number ONLY when the server returned real data. Do NOT apply SQL (Claude applies it after review through the Supabase connector; read `docs/SUPABASE_NOTES.md`). | Flag off = the app behaves exactly as before. |

If quota runs out mid-phase: commit what exists, mark the phase `IN PROGRESS` in section 14 and list what is missing in the handoff.

## 11. Compile checklist (run it on every file before committing)

The owner cannot compile; every error costs Claude quota. Past errors are in `docs/BUILD_PITFALLS.md`: read it fully before writing code. In particular:
- Every `Modifier.xxx` extension and every `Icons.*` use has its own import (`clickable`, `background`, `border`, `padding`, `size`, `clip`, `fillMaxWidth`, `weight`...).
- `items(list)` in a `LazyColumn` needs `import androidx.compose.foundation.lazy.items`.
- Never call a `@Composable` (including `LaunchedEffect`) directly inside `LazyColumn { }` builder scope; only inside `item { }` / `items { }`.
- Trailing-lambda trap: if the lambda is not the LAST parameter, pass `onClick = { ... }` by name.
- No `rememberRipple`; no deprecated APIs; no `coerceAtMost` on `Dp` (use if/else).
- `AnimatedVisibility` needs `import androidx.compose.animation.AnimatedVisibility`.
- Do not write a slash followed by a star inside a comment, Kotlin nests block comments.
- Plain `String` / `Int` / `Float` math: write explicit types, avoid clever generics and extension chains. Prefer `if` over `when` with subject smart-casts on `var`s.
- No name clashes: grep `app/src` for every new top-level name before using it.
- When you change a data model field, grep `app/src` for every use.
- Drawing: no allocation inside `DrawScope` lambdas that run every frame (no `Path()`, `Offset` lists, `Brush` creation per frame; reuse).
- Kotlin `private` top-level declarations in different files with the same name are fine, `internal`/public ones clash.

## 12. Quality bar: what must be true before you call a phase done

- It reads like a GAME: instant feedback on every tap (visual + sound + haptic), motion with easing (never linear snaps), particles on every reward, a camera that follows, parallax, idle ambient movement, juicy result screens.
- No dead taps, no unexplained states, no text the owner did not approve invented as facts.
- Every long operation (loading banks) shows an animated owl or lantern, never a blank screen. Offline: banks never opened and not cached show a clear "needs internet once" message with Retry.
- Rotating or leaving the screen mid-run never crashes; leaving cancels timers and releases the WebView and AudioTracks.
- Performance: Canvas work per frame stays small; low-end mode works; no object churn in the frame loop.
- Night look is the default; draw your own full-bleed scenes (do not depend on the app background picture).

## 13. Handoff file for Claude (`docs/GAMES_HANDOFF_FOR_CLAUDE.md`)
Keep it current in every commit: (a) every file created or changed, one line each; (b) per phase: done / in progress; (c) `UNSURE:` lines (APIs you were not certain about, with file and line); (d) known risks; (e) a section "CI errors seen" that Claude fills. This file is what lets Claude fix a red build in two or three tool calls.

## 14. Progress table (update in every commit)

| Phase | Status | Commit | Notes |
|---|---|---|---|
| G0 Recon | open | | |
| G1 Data kit | open | | |
| G2 Question card | open | | |
| G3 Feel kit | open | | |
| G4 Climb logic | open | | |
| G5 Climb tower view | open | | |
| G6 Climb play + wire-up | open | | owner test point 1 |
| G7 Defense engine + scene | open | | |
| G8 Defense flow + wire-up | open | | owner test point 2 |
| G9 Polish and safety | open | | |
| G10 Climbers below (optional) | not started | | needs owner OK |

## 15. Verified signatures (G0 fills this, copied from the code, with file names)

(empty: to be filled in G0)

## 16. Owner phone test (plain words; give this to the owner after G6 and G8)

1. Learning > a course > Question Banks: a Tower Climb banner appears on top. Tap it: a tall tower opens, scroll it, tap Jump to me.
2. Open the current floor, answer 8 questions: the owl climbs on right answers, loses a heart on wrong ones and the solution shows.
3. A chapter you locked in Admin shows chains but the tower is still whole and the other floors still work.
4. Learning > a course > Exams: a Tower Defense banner appears. Pick an exam: enemies march, answer before they arrive; power-ups work; the end screen lists the questions you missed.
5. Turn the phone sound off and on in the pause menu; press Back during play: the pause menu opens.
6. Airplane mode after opening a bank once: it still plays.

## 17. Plan changes
(append here if you must change the approach: date, what, why. Never switch approach silently.)
