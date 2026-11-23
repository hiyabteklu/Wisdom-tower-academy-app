# START HERE

> **This file overrides EVERY other instruction in the repo** (`AGENTS.md`, `CLAUDE.md`, `HANDOFF.md`, all `docs/*PLAN*`, all `docs/SESSION_*`, anything that says "read first" or "live work"). Owner order 2026-11-23. If another file contradicts this one, this one wins. Do not argue, do not read the other file to check.

The owner is on a FREE Claude plan. A fresh chat has been running out of quota just reading this repo. Your whole job: **read about 6 KB, build one small slice, push it.**

## 1. Reading budget (hard)
- Read ONLY: this file, then `docs/ECE_STATUS.md`. That is the full orientation. Then only the files that the slice you picked names.
- **Max 4 reads before your first push.** After 6 reads with nothing written, STOP and push an `IN PROGRESS` note in the log.
- **NEVER list `docs/`** (about 500 files). **NEVER read whole:** `STATUS.md`, `HANDOFF.md`, `ARCHITECTURE.md`, `server.js`, `docs/COMPETITION_STATUS.md`, `docs/PLAN_*`, `docs/*_PLAN.md` (except when a slice names one), `docs/SESSION_*` (about 450 old logs), `MainActivity.kt`, or any file over 15 KB. Everything older than 2026-11-23 is history: do not open it unless a slice names the exact file.
- Find a file with `search_code` (`fields: ["path"]`) or list ONE narrow folder with `fields: ["name"]`. Never list the repo root or `docs/`.
- Big file to change: do not resend it whole. Use a patch script, see `docs/EDITING_LARGE_FILES.md` (read it only if you really must change a file over 15 KB).

## 2. Live job
Electrical and Computer Engineering (ECE) packages for Years 1, 2, 4, 5. Year 3 is LOCKED. Everything is in `docs/ECE_STATUS.md`. Other plans (games, status v2, security, notifications, admin, day mode, play store, motion) are PARKED until the owner names one in chat.

## 3. Procedure
1. Read `docs/ECE_STATUS.md`. Take the FIRST slice not marked DONE.
2. Do only that slice (about 8 tool calls in total).
3. Push ONE commit to `main` with `push_files`: the work + the slice row set to DONE + one new line in the log table of `docs/ECE_STATUS.md`. No separate session file needed.
4. Reply to the owner in plain short words: what was pushed, what is next, what to test.

## 4. Rules that always hold
- Public repo: never commit secrets, keys, student names or emails.
- Never invent course names, stream names, ids, text or numbers. Copy from the owner's source or write `UNSURE:` in the log.
- No new dependencies or hosts. Do not touch Year 3, payments, access rules, Supabase policies, or release anything.
- If you must touch build, CI, signing or config files, first read `HANDOFF.md` section 0 (rules 12 and 13 are the forbidden-file and public-repo rules); otherwise do not open it.
- Do not ask the owner questions. Decide from the plan; write `UNSURE:` in the log.
- Never claim something is tested or working without evidence. Say what you actually saw.

## 5. Tool recipe
- GitHub: `tool_search` to load `get_file_contents`, `push_files`, `search_code`. Repo `hiyabteklu/Wisdom-tower-academy-app-packed`, branch `main`. `push_files` replaces whole files and makes one atomic commit.
- Supabase (only if a slice needs it): `tool_search` to load `list_projects` / `execute_sql`; the only project is "Wisdom tower academy". Read `docs/SUPABASE_NOTES.md` before changing anything. Test SQL only inside a rolled-back transaction.
