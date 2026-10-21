# GAMES HUB PLAN (owner decisions, 2026-10-21) — READ BEFORE TOUCHING THE GAMES

Status: STEP 1 (database) DONE on 2026-10-21. No app code for this plan is written yet. It replaces the earlier plan where games read Question Banks and Exams.

## 1. Two games (not four)
1. **Wisdom Defense** (redesign of Tower Defense)
   - Enemies drive toward the player's empire.
   - The player's "bullets" are the 4 answer choices of the question shown.
   - Correct answer: the bullet kills that enemy and clears the player's surroundings.
   - Wrong answer: the bullet bounces back, the enemy keeps getting closer, until the player gives a correct answer for it. If it arrives first, it attacks the empire.
2. **Match** (REPLACES Tower Climb)
   - 8 cards in a grid on one screen. Tap a card: it highlights. Tap the card that is its answer: the pair is matched. Wrong pair: reset the highlight.
   - (Proposal, not yet confirmed by the owner: wrong pair shakes; a matched pair disappears; the next pairs fill the grid until the set is finished.)
- The old Tower Climb code (`ui/games/climb`, `data/games/Climb*`) stays in the repo until Match is built; remove it only when the owner says so.
- A 3rd/4th game may come later (ideas given to the owner: Blitz 60-second round, Letter Lock, Duel). Not decided.

## 2. Flow (same for every game)
Bottom nav **Game** tab → pick a game → the student's own courses (the SAME list the Learning tab shows, from the academic level / package chosen at registration or in account settings; nothing else) → subject → chapter → game starts, FULL SCREEN (bottom bar and status bar hidden while playing).

## 3. Content (the big change)
- Games get their OWN dedicated, SHORT questions. Long questions are not used.
- The owner prepares these question sets separately, for every package, subject and chapter, one set per game.
- This content is NOT part of the 6 normal hubs and is NOT visible there. It is reachable only through the Game tab.
- CHAPTER is a new level in the app (today the app stops at subject).
- Question shapes: Wisdom Defense = short question + 4 choices. Match = pairs (term and meaning).
- A chapter with no set for a game shows "coming soon" for that game only.

## 4. Database (DONE 2026-10-21, Supabase project "Wisdom tower academy", migration `create_game_sets`)
Table `public.game_sets` (new, empty, does not touch `learning_resources`, so nothing changes on the website or in the normal hubs):
- `id` uuid, `package_id` text, `scope_path` text (SAME format as `learning_resources.scope_path`), `chapter` integer, `chapter_title` text, `game` text (`defense` or `match`), `title` text, `items` jsonb array, `published` boolean (default false), `sort_order`, `created_at`, `updated_at`.
- One set per (`scope_path`, `chapter`, `game`) (unique).
- `items` shape: defense = `[{"q":"...","choices":["a","b","c","d"],"correct":0,"hint":"..."}]`; match = `[{"a":"term","b":"meaning"}]`.
- Security (RLS on): everyone can read rows with `published = true`; admins (`private.is_admin()`) can do everything. Same rules as `learning_resources`.
- Locks (`content_locks`) are NOT applied yet; decide with the owner how game sets follow package access.

## 5. Build order (proposed)
1. ~~Database for chapters and game sets~~ DONE.
2. Game hub: pick game → learning-level courses → subject → chapter list (only the student's courses). Needs a way to list chapters (from `game_sets` rows of that scope).
3. Full-screen mode for games.
4. Match (new).
5. Wisdom Defense redesign (rewrite of the enemy/bullet loop).
6. Remove the old FABs on Question Banks / Exams and the old game question source.
7. Way for the owner to load the question sets (see below).

## 6. Still to ask the owner
- "Clears your surroundings": does a correct answer also remove nearby enemies, or only the targeted one?
- Students with no saved level: show all courses or a "choose your level" prompt?
- Content upload: how the owner wants to send the question sets (JSON files like the other hubs, or typed in the website admin).

## 7. Already built and live (Oct 21)
Game tab (`ui/games/GameTabScreen.kt`) with game → course → subject flow; games currently read Question Banks / Exams through StudyWorkspaceScreen. See `docs/GAMES_HANDOFF_FOR_CLAUDE.md`.
