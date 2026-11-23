# START HERE

> **EXCEPTION (owner order 2026-11-23): if you are GitHub Copilot, or any agent assigned `docs/ADMIN_ECE_Y1_Y5_PLAN.md`, that plan overrides everything below it: no reading budget, no "one small slice", you may read, search and edit any file, and you must finish ALL its phases in order. The rules about Year 3, live users, secrets and Supabase still apply.**

> Otherwise this file overrides EVERY other instruction in the repo (owner order 2026-11-23). Do not read other files to check.

The owner is on a FREE Claude plan. Your job: **read about 8 KB, build ONE small slice, push it.** Production app: docs and small new files only unless the slice says otherwise. Never break the build.

## 1. Reading budget (hard)
- Read ONLY: this file, then `docs/ECE_STATUS.md` (about 5 KB). Then only the files the slice names.
- **Max 4 reads before your first push.** After 6 reads with nothing written, STOP and push an `IN PROGRESS` line in the log.
- **NEVER list `docs/`, `scripts/` or the repo root.** Anything named SESSION_*, PLAN_*, STATUS.md, PENDING_PATCHES.md or `scripts/patch_*.py` is history: do not open it.
- **NEVER open:** `HANDOFF.md`, `ARCHITECTURE.md`, `server.js`, `MainActivity.kt` (275 KB), `docs/BUILD_PITFALLS.md`, `docs/ECE_BLUEPRINT_Y4_Y5.md` (only for slices E4 to E7).
- `search_code` does NOT work on this repo. Do not use it. To find a file, list ONE narrow folder with `fields: ["name","size"]`.
- File over 10 KB to change: NEVER resend it. Use a patch script, see `docs/EDITING_LARGE_FILES.md`.

## 2. Live job
Electrical and Computer Engineering (ECE) packages for Years 1, 2, 4, 5. Year 3 is LOCKED. Everything is in `docs/ECE_STATUS.md`. All other plans are PARKED.

## 3. Router: already known, do NOT hunt for it
The code that turns a path into a package is `handleGlobalNav` in `MainActivity.kt`: it does `catalogPackages.find { it.path.equals(pathPart, ignoreCase = true) || ... }`. So new `NativePackage` entries with their own `path` should open with NO `MainActivity.kt` edit. Never fetch `MainActivity.kt`. If after the build the Year 1 card does not open, write that in the log and stop; the owner decides.

## 4. Procedure
1. Read `docs/ECE_STATUS.md`. Take the FIRST slice not marked DONE.
2. Do only that slice (about 8 tool calls).
3. Push ONE commit to `main` with `push_files`: the work (new small files or patch scripts) + a log line. Patch scripts do not change the app until the owner runs the Action, which makes a branch to check first.
4. Reply to the owner in plain short words: what was pushed, what is next, what to test.

## 5. Rules that always hold
- Public repo: never commit secrets, keys, student names or emails.
- Never invent course names, ids, text or numbers. Copy from the owner's source or write `UNSURE:` in the log.
- No new dependencies or hosts. Do not touch Year 3, payments, access rules, Supabase policies, or release anything.
- If you must touch build, CI, signing or config files, first read `HANDOFF.md` section 0 (rules 12 and 13); otherwise do not open it.
- Do not ask the owner questions. Decide from the plan; write `UNSURE:` in the log.
- Never claim something is tested or working without evidence.

## 6. Tool recipe
- GitHub: `tool_search` to load `get_file_contents`, `push_files`. Repo `hiyabteklu/Wisdom-tower-academy-app`, branch `main`. `push_files` replaces whole files and makes one atomic commit.
- Supabase (only if a slice needs it): `tool_search` for `list_projects` / `execute_sql`; project "Wisdom tower academy". Read `docs/SUPABASE_NOTES.md` first. Test SQL only in a rolled-back transaction.
