# GAMES HUB PLAN (owner decisions, 2026-10-21) — READ BEFORE TOUCHING THE GAMES

> Repo is PUBLIC: no secrets, no student data. This is the ONLY live plan (see `docs/PARKED_2026-10-21.md`). Follow `docs/OWNER_RULE_PLAN_FIRST_PUSH_TO_MAIN.md`: one slice = one commit straight to `main`, update this file in the same commit, say "not compiled" when CI was not seen.

Status: STEP 1 (database) DONE 2026-10-21. Owner answered all open questions the same day (section 5). STEP 2a (data layer) written, not compiled. Next: STEP 2b.

## 1. Two games (not four)

### 1a. Wisdom Defense (redesign of Tower Defense)
Owner words 2026-10-21, turned into a spec:
- **Cockpit view.** The player looks out from inside a cockpit.
- **Enemies** drive toward the player's empire, each starting at a DIFFERENT distance.
- **Question** is shown at the TOP of the screen.
- **Bullets = the 4 answer choices.** They sit in an ARC around the player (cockpit side).
- **Tap one choice** and it flies STRAIGHT to the NEAREST enemy.
- **Correct:** kills ONLY the targeted enemy (decided: no area clearing).
- **Wrong:** the bullet bounces back; the enemy keeps getting closer until the player answers correctly for it. If it arrives first, it attacks the empire.
- **Background:** the same app background as everywhere else, in all 3 looks (Dark, Day, and the third look) if possible. Use the app's existing background/theme system; do not invent a game-only background.

### 1b. Match (REPLACES Tower Climb)
- 8 cards in a grid on one screen. Tap a card: it highlights. Tap the card that is its answer: the pair is matched. Wrong pair: reset the highlight.
- (Proposal, NOT confirmed by the owner: wrong pair shakes; a matched pair disappears; the next pairs fill the grid until the set is finished.)
- Same background rule as Defense (app background, 3 looks).
- The old Tower Climb code (`ui/games/climb`, `data/games/Climb*`) stays until Match is built; remove it only when the owner says so.

A 3rd/4th game may come later (ideas given: Blitz 60-second round, Letter Lock, Duel). Not decided.

## 2. Flow (same for every game)
Bottom nav **Game** tab → pick a game → the student's own courses (the SAME list the Learning tab shows, from the academic level / package chosen at registration or in account settings) → subject → chapter → game starts FULL SCREEN (bottom bar and status bar hidden while playing).

## 3. Content
- Games use a TOTALLY NEW set of dedicated, SHORT questions. The previous agent's work of wiring the existing Question Banks / Exams into the games is NOT wanted and is replaced by this plan.
- The owner prepares these sets separately for every package, subject and chapter, one set per game.
- Not part of the 6 normal hubs and not visible there. Reachable only through the Game tab.
- CHAPTER is a new level in the app (today it stops at subject).
- Shapes: Wisdom Defense = short question + 4 choices. Match = pairs (term and meaning).
- A chapter with no set for a game shows "coming soon" for that game only. Never invent questions or text (HANDOFF rule 11h).
- Math/markup in game strings (`$..$`, `[[term]]`, `==hl==`) must go through the shared rich-text renderer the other hubs use (see `docs/CONTENT_SHAPES.md`), if the owner's sets use it.

## 4. Database (DONE 2026-10-21, Supabase "Wisdom tower academy", migration `create_game_sets`)
Table `public.game_sets` (new, empty; does not touch `learning_resources`):
- `id` uuid, `package_id` text, `scope_path` text (SAME format as `learning_resources.scope_path`), `chapter` integer, `chapter_title` text, `game` text (`defense` or `match`), `title` text, `items` jsonb array, `published` boolean (default false), `sort_order`, `created_at`, `updated_at`.
- One set per (`scope_path`, `chapter`, `game`) (unique).
- `items`: defense = `[{"q":"...","choices":["a","b","c","d"],"correct":0,"hint":"..."}]` (`correct` is a 0-based index); match = `[{"a":"term","b":"meaning"}]`.
- RLS on: everyone reads rows with `published = true`; admins (`private.is_admin()`) do everything.
- Remember the ECE quirk in `docs/CONTENT_SHAPES.md`: ECE rows use `scope_path = ece/sem-1/<slug>` even for semester 2 courses. The app query already handles it.

### Live Supabase change log (add a line for every change)
- 2026-10-21: migration `create_game_sets` (table + RLS above).

## 5. Owner decisions (2026-10-21, in his words, summarised; never drop)
1. Correct answer kills ONLY the targeted enemy.
2. Enemies come at different distances. Question on top. Both games share the app background in the 3 looks.
3. Defense = cockpit view, 4 answer bullets in an arc around the player, tap one and it goes straight to the nearest enemy.
4. Games use a totally NEW question set (not the existing banks/exams).
5. No student lacks a saved level (enforced at registration): no "choose your level" prompt.
6. Access follows the package. Grade 9 package = Grade 9 games. No extra lock logic, `content_locks` is NOT used for game sets.
7. Upload: the owner pastes JSON in bulk through the admin dashboard into the database, like the other hubs.

## 6. Build order and status
| Step | What | Status |
|---|---|---|
| 1 | Database `game_sets` | DONE |
| 2a | Data layer `data/games/GameSetsRepository.kt` (chapters of a subject, one set per chapter and game, offline cache, skips malformed items). New file, used by no screen yet | WRITTEN, NOT COMPILED |
| 2b | Game hub UI: Game tab → pick game (Wisdom Defense / Match) → student's own courses → subject → chapter list from `GameSetsRepository.chapters`, "coming soon" per game when a chapter has no set. Open points: how a subject maps to the `scope_path` string (check how StudyWorkspaceScreen builds it), and how MainActivity passes the chosen course | NEXT |
| 3 | Full-screen mode while playing | not started |
| 4 | Match (new) | not started |
| 5 | Wisdom Defense rewrite (cockpit, arc, nearest-enemy shot) | not started |
| 6 | Remove the old FABs on Question Banks / Exams and the old game question source | not started |
| 7 | Admin: a bulk JSON paste form for `game_sets` (the hub paste flow only writes `learning_resources`). Needed before the owner can load sets | not started |

Order note: step 7 may be moved earlier if the owner wants to load content while the games are being built.

## 7. Plan changes
- 2026-10-21: the earlier plan (games read Question Banks / Exams through StudyWorkspaceScreen) is dropped. Its built files (G0–G9) stay as code to reuse where useful (frame loop, audio, haptics, particles, pause overlay, question card), see `docs/GAMES_HANDOFF_FOR_CLAUDE.md`.

## 8. Already built and live (Oct 21)
Game tab (`ui/games/GameTabScreen.kt`) with game → course → subject flow; the games currently read Question Banks / Exams through StudyWorkspaceScreen (to be replaced). Rules for compile safety: always draw `AtmosphereBackground(modifier = Modifier.fillMaxSize())` as the first child of a `Box`, never with a content lambda (`docs/GAMES_HANDOFF_FOR_CLAUDE.md`, CI errors).
