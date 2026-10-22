# GAMES COOL PLAN (owner order 2026-10-22): level selector, result saver, and a much cooler Wisdom Defense

> Written 2026-10-22 by Claude (chat). Plan only, NO code in this commit. Repo is PUBLIC: no secrets, no student data. Follows `docs/GAMES_FEEL_PLAN.md` (D1-D5, M1-M3, P1 are written, phone test of the result is the owner's input) and `docs/GAMES_HUB_PLAN.md`. Rules as every slice: `docs/OWNER_RULE_PLAN_FIRST_PUSH_TO_MAIN.md`, `docs/AGENT_QUOTA_PROTOCOL.md`, `docs/EDITING_LARGE_FILES.md`. Owner is not a coder and on the free plan: ONE small slice per chat, push to `main`, tell the owner exact taps only, no re-reading of files the slice does not touch.

Owner words: add a level selector (easy, medium, hard), a result saver, and make the games look cool, especially Wisdom Defense.

Defaults used below (the owner can change any of them by saying so; until then they stand):
1. All three levels are open from the start (no unlock chain, matches the earlier "no extra lock complexity" decision).
2. Level changes how the game PLAYS (speed, lives, count, help). Questions are not rewritten. An item MAY carry an optional `"level"` tag later (section 3); untagged items work everywhere.
3. Results are the student's OWN rows, best per chapter + game + level (not a public leaderboard in this plan).
4. Every number is a proposal in the style of the feel plan; change any of them after a phone test.

## 1. What exists today (read from the code, 2026-10-22)
- Data: `GameSetsRepository` loads ONE published `game_sets` row per (scope_path, chapter, game) with `items`. Defense item `{q, choices[4], correct 0..3, hint?}`, Match item `{a, b}`. Unknown keys in an item are ignored, so a `level` key is safe to add. A saved copy is used offline.
- Flow: Game tab > course > subject > chapter row (Play + download icon) > the game starts at once. There is NO level step and NOTHING is saved after the result screen.
- Defense (D1-D5, P1): engine with distance model, enemy kinds (slime, bat, golem), shots, shield, wall, hearts, 2x2 answer cards, result screen with stars. Match (M1-M3, P1): stable slots, pulse/shake, progress bar, streak, result with stars.
- Owner decision (2026-10-21, chat) not yet in the code: Defense should be COCKPIT style (the 4 answers as bullets in an arc around the player, tap one and it flies to the nearest enemy), enemies at different distances, question at the top, same background as the app. D4 built a 2x2 card layout instead; slice W3 below closes that gap.

## 2. Level selector (G = game levels)

### 2.1 Flow
Chapter row > Play > full-screen "Choose level" > game. Three big pill cards: **Easy** (green), **Medium** (amber), **Hard** (rose). Each shows an icon, one line of what changes, the student's best stars for that level and "Played N times". The last level used for that game is pre-selected (kept on the phone, per game). Back from the picker = chapter list. Same glass look and motion as the rest of the app, day and night colours via the existing tokens, `reduceMotion` respected.

### 2.2 What each level changes
| | Easy | Medium (= today) | Hard |
|---|---|---|---|
| **Defense** walk time, far edge to wall | about 30 s | about 22 s | about 15 s |
| Hearts | 5 | 3 | 3 |
| Enemies on the lane at once | up to 2 | up to 3 | up to 4 |
| Wrong answer | lock + small lunge 0.05, hint line shown after the first wrong (item `hint`) | as today (lock, lunge 0.08) | lock, lunge 0.12, no hint |
| Armored golem | normal | normal | needs 2 correct hits (armor cracks after the first) |
| Last enemy | normal | normal | boss: bigger, own health bar, final wave banner |
| Score multiplier | x1 | x1.5 | x2 |
| **Match** pairs on the board at once | 3 (6 cards) | 4 (8 cards, today) | 6 (12 cards) |
| Extra | no timer, hint: a wrong pair briefly shows which side was right | no timer | par timer shown, extra star only if finished under par |
| Stars | 3 stars at moves <= pairs x 1.5 | as M3 (pairs x 1.25 / x 2) | 3 stars needs moves <= pairs x 1.25 AND under par time |

### 2.3 Code shape (small and safe)
- NEW `data/games/GameLevel.kt`: `enum GameLevel { EASY, MEDIUM, HARD }` with `fromId`, plus two plain data classes `DefenseTuning` and `MatchTuning` (the numbers of the table) and `tuningFor(level)`. No UI, no engine change, compiles alone.
- `WisdomDefenseEngine` and `MatchEngine` take the tuning as ONE new constructor parameter with MEDIUM as default, so every existing call keeps compiling and MEDIUM behaves exactly as today.
- NEW `ui/games/LevelPickerScreen.kt` and the hub passes the chosen level into the game screens.

## 3. Optional question tags (no new table)
An item may add `"level": "easy" | "medium" | "hard"`. Rule when a set has tags: the pool for a level = items tagged for it; if that gives fewer than 8 items (Defense) or 6 pairs (Match), top up from the neighbouring level (Medium first); untagged = medium. A set with NO tags uses every item on every level (only the tuning changes). `GameSetsRepository.parseSet` reads the optional key; the admin Game Sets editor only gets one hint line about the key (the admin pastes JSON, so no new admin UI). Tags are optional and can come later; nothing waits for them.

## 4. Result saver (R = results)

### 4.1 Table and function (live Supabase change, applied by Claude with the owner's "yes", exactly like S6a; SQL record in `docs/sql/`)
`public.game_bests`, one row per student per (game, scope_path, chapter, level):
`user_id uuid` (on delete cascade from `auth.users`), `game text` ('defense' | 'match'), `scope_path text`, `chapter int`, `level text` ('easy' | 'medium' | 'hard'), `best_stars int 0..3`, `best_score int`, `plays int`, `wins int`, `last_stars int`, `last_score int`, `last_played timestamptz`. Primary key `(user_id, game, scope_path, chapter, level)`. RLS on: select / insert / update OWN rows only, role `authenticated`.
Function `public.save_game_result(p_game, p_scope_path, p_chapter, p_level, p_stars, p_score, p_won)`: atomic upsert for `auth.uid()` (plays + 1, wins + 1 when won, best = greatest of old and new, last = new), every value clamped (stars 0..3, score 0..100000), `security invoker`, execute for `authenticated` only (not `anon`). Checked after applying like S6a (RLS true, policies, anon cannot execute).
No guest rows: a signed-out student plays normally and the result screen says "Sign in to save your results" (same sign-in pill as the other screens).

### 4.2 App side
- NEW `data/games/GameResultsRepository.kt` (same pattern as `StudyDayLogReader` and the progress repository: own token, refresh, one retry after a 401, only HTTP codes logged, never throws): `save(...)` calls the RPC; `bestsFor(scopePath)` reads the student's rows for one subject (`select=...&scope_path=eq...`).
- Local first: the result is also kept in a small phone-side list. If the RPC fails (offline) the result waits in a pending list and is sent next time a game screen opens. The student sees his best stars at once from the local copy.
- Where it shows: (1) result screen: "New best!" chip, level name, "Saved" tick or "Will save when online"; (2) level picker cards: best stars + plays; (3) chapter rows in the Game tab: small best-stars line for the last used level.
- Not in this plan (parked, say so if wanted): leaderboards, game minutes inside "Your status", badges.

## 5. Wisdom Defense, make it cool (W = wow)
Goal: it should feel like a real tower-defense shooter, readable on a small phone and still light on a cheap one. No new dependencies, Canvas only, `GameTheme.reduceMotion` and `isLowEnd` respected (fewer particles, no shake, no slow-mo).

| Slice | What | Notes |
|---|---|---|
| W1 Atmosphere | Layered background behind the lane: slow parallax stars / dust, soft nebula glow tinted by level (Easy teal, Medium violet, Hard ember red), glowing lane edges, light pulse on the wall crystal. Uses the app background mode (Dark, BG 2, Day colours via tokens). | `DefenseArt.kt` only |
| W2 Enemies | Idle life (slime squish, bat wing blur, golem stomp dust), portal flash when an enemy spawns at the far edge, level skins (Easy friendly green, Medium purple, Hard ember), golem armor crack, Hard boss with health bar and entrance roar flash. | `DefenseArt.kt`, small engine hook for armor hits |
| W3 Cockpit | Player turret / ship at the bottom centre with recoil and muzzle flash. The 4 answers become bullet buttons in an ARC around it (owner decision 2026-10-21): tap one, that bullet flies straight to the nearest enemy; letters A-D kept for accessibility; long answers show a full-text pop-up on press-and-hold so nothing is cut. Question stays at the top. | `WisdomDefenseScreen.kt` (+ a new `DefenseCockpit.kt`); the 2x2 cards stay as a fallback when the width is too small |
| W4 Juice | Combo meter with glow, kill banners ("Double!", "Triple!", "Unstoppable!") from streak 2, screen-edge pulse on a streak, shockwave ring on a wall hit, floating score numbers, short slow-motion (0.4 s at 0.4x) on the LAST enemy, wave banner ("WAVE 2 / 3", "BOSS"). Haptics and sounds reuse P1 events plus one new chime for banners. | `DefenseEffects.kt` |
| W5 Power-ups (OPTIONAL, ask owner first) | Earned by streak, never bought: streak 3 = Freeze (enemies stop 3 s), streak 5 = Shield (absorbs one wall hit). One icon above the cockpit, tap to use. Off on Easy (already forgiving). | engine + `DefenseCockpit.kt` |
| W6 Result v2 | Level name chip, stars that pop with a confetti burst on 3 stars, score line with the level multiplier, "New best!", saved tick, buttons Retry / Next level (when not Hard) / Done. | `DefenseResult.kt` |
| W7 Polish | `isLowEnd` pass (particle counts, no slow-mo / shake), reduce-motion check of every new effect, owner phone-test fixes. | all of the above |

Match gets a lighter lift after its levels (slice MW below): card back pattern + flip on select, confetti on set complete, level chip in the HUD.

## 6. Slices (one per chat; owner says "Continue"; order is the order of the table)
| Slice | Area | What | Status |
|---|---|---|---|
| G1 | Levels, data | `GameLevel.kt` (enum + tuning for both games), engines take tuning (default MEDIUM = today), optional `level` tag read in `GameSetsRepository.parseSet`, pool rule of section 3 | OPEN (next) |
| G2 | Levels, UI | `LevelPickerScreen.kt` + hub wiring (chapter Play > picker > game), last level remembered per game, Defense uses the tuning (walk time, hearts, enemies at once, wrong behaviour, hint line) | OPEN |
| G3 | Levels, Match | Match uses its tuning (pairs on board, hint, par timer, stars rule) | OPEN |
| R1 | Results, DB | `game_bests` + `save_game_result` applied live with the owner's yes, SQL record `docs/sql/<date>_game_bests.sql`, checks | OPEN |
| R2 | Results, app | `GameResultsRepository.kt` (save, bestsFor, pending list), result screens call it, "Sign in to save" for guests | OPEN |
| R3 | Results, show | Best stars + plays on the level picker and chapter rows | OPEN |
| W1 | Defense | Atmosphere (section 5) | OPEN |
| W2 | Defense | Enemies | OPEN |
| W3 | Defense | Cockpit and arc answers | OPEN |
| W4 | Defense | Juice | OPEN |
| W5 | Defense | Power-ups (only if the owner says yes) | OPEN, needs owner yes |
| W6 | Defense | Result v2 | OPEN |
| MW | Match | Lighter look lift (flip, confetti, level chip) | OPEN |
| W7 | Both | Polish and low-end pass | OPEN |

Why this order: levels first (they change the engines, so the art slices are built on the final engine), results next (the result screens need the level), art last. G1 to G3 and R1 to R3 can be done by a quota-limited agent in about six short chats; every W slice touches one or two files.

## 7. Owner phone test (after each slice, short)
- G2: Play on a chapter opens "Choose level"; Easy feels slower with more hearts and a hint after a wrong answer; Hard feels fast with up to four enemies and a boss at the end; the last level you picked is pre-selected next time.
- G3: Easy Match has 6 cards, Hard has 12 cards and a timer.
- R2/R3: finish a game signed in: the result says Saved; reopen the picker: the stars and "Played 1 time" are there; kill the internet, finish a game: "Will save when online", then it syncs later. Signed out: the result asks to sign in.
- W1 to W4: background moves softly; monsters visibly alive; the answers sit in an arc around your ship and fly to the nearest monster; a streak shows banners and a glow; the last kill goes slow-motion.

## 8. Honest limits and risks
- Nothing here is compiled or seen on a phone yet; every slice says so in its commit and its log (`docs/SESSION_<date>_GAMES_<slice>.md`).
- The tuning numbers are guesses; they will need a phone test (walk time and hearts first).
- W3 changes the main play layout; it is the riskiest slice, so it keeps the 2x2 cards as a fallback and is done alone in its own chat.
- Cheap phones: W1, W2 and W4 add drawing work; W7 is where it is measured and cut.
- Offline results can be lost if the app data is cleared before the next sync (same limit as the study day log).

## 9. Progress
All slices OPEN. Next: **G1**.
