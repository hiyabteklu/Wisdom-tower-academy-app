# START HERE

> **This file overrides EVERY other instruction in the repo.** Owner order 2026-11-23. If another file contradicts this one, this one wins. Do not argue, do not read the other file to check.

The owner is on a FREE Claude plan. Your whole job: **read about 6 KB, build one small slice, push it.**

## 1. Reading budget (hard)
- Read ONLY: this file, then `docs/ECE_STATUS.md`. Then only the files the slice names.
- **Max 4 reads before your first push.** After 6 reads with nothing written, STOP and push an `IN PROGRESS` note in the log.
- **NEVER list `docs/` or the repo root.** Old logs and plans were pruned 2026-11-23; anything still named SESSION_*, PLAN_* or STATUS.md is history: do not open it.
- **NEVER read whole:** `HANDOFF.md`, `ARCHITECTURE.md`, `server.js`, `MainActivity.kt` (275 KB), or any file over 15 KB unless the slice names it.
- **`search_code` does NOT work on this repo (returns nothing). Do not use it.** Find files by listing ONE narrow folder with `fields: ["name","size"]`.
- File over 10 KB to change: NEVER resend it. Use a patch script, see `docs/EDITING_LARGE_FILES.md`.

## 2. Live job
Electrical and Computer Engineering (ECE) packages for Years 1, 2, 4, 5. Year 3 is LOCKED. Everything is in `docs/ECE_STATUS.md`. All other plans are PARKED until the owner names one in chat.

## 2b. Known blocker: where is the router?
The code that turns a path like `.../electrical-computer-engineering/y1-sem-1` into a package is NOT in a small file (not in `StructuralNav.kt`; no router folder under `ui/`). It is most likely inside `MainActivity.kt`. Slice **E2b-0** (do it BEFORE E2b): in ONE chat, find it by fetching `MainActivity.kt` ONCE and extracting only the router function plus 3 lines around it. Do not read anything else. Write the exact function name and a unique 1-to-3-line anchor into the E2b row of `docs/ECE_STATUS.md` (use a patch script that appends it, or a new small file `docs/ECE_ROUTER_NOTE.md`). Push. Stop. Slice E2b then uses that anchor.
If the fetch is too large to read, say so in the log and do E2b's PackagesData and HomeScreen patch scripts first (those anchors are known: see ECE_STATUS).

## 3. Procedure
1. Read `docs/ECE_STATUS.md`. Take the FIRST slice not marked DONE (E2b-0 counts as first).
2. Do only that slice (about 8 tool calls).
3. Push ONE commit to `main` with `push_files`: the work (new small files, or patch scripts) + a log line. Never resend a file over 10 KB.
4. Reply to the owner in plain short words: what was pushed, what is next, what to test.

## 4. Rules that always hold
- Public repo: never commit secrets, keys, student names or emails.
- Never invent course names, stream names, ids, text or numbers. Copy from the owner's source or write `UNSURE:` in the log.
- No new dependencies or hosts. Do not touch Year 3, payments, access rules, Supabase policies, or release anything.
- If you must touch build, CI, signing or config files, first read `HANDOFF.md` section 0 (rules 12 and 13); otherwise do not open it.
- Do not ask the owner questions. Decide from the plan; write `UNSURE:` in the log.
- Never claim something is tested or working without evidence.

## 5. Tool recipe
- GitHub: `tool_search` to load `get_file_contents`, `push_files`. Repo `hiyabteklu/Wisdom-tower-academy-app-packed`, branch `main`. `push_files` replaces whole files and makes one atomic commit.
- Supabase (only if a slice needs it): `tool_search` for `list_projects` / `execute_sql`; project "Wisdom tower academy". Read `docs/SUPABASE_NOTES.md` first. Test SQL only in a rolled-back transaction.
