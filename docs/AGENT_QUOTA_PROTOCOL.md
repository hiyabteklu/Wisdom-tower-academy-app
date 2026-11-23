# AGENT_QUOTA_PROTOCOL.md — how to work when the owner has a tiny free quota

> Written 2026-10-14. Applies to EVERY agent (Claude chat, AI Studio, Codex, anyone). It adds to `HANDOFF.md` section 0; it does not replace it.

## HARD RULE (owner order 2026-11-23, beats everything below)
A previous agent burned its whole free limit only on reading. So:
- **Max 4 reads before the first push.** Start list for the live work: `AGENTS.md`, `docs/ECE_EXPANSION_SLICES.md`, `docs/ELECTRICAL_1ST_TO_5TH_YEAR_EXPANSION_PLAN.md`.
- **Never list `docs/`** (about 500 files, ~15k tokens). Find files with `search_code` + `fields: ["path"]` or a narrow directory path + `fields: ["name"]`.
- **Never read whole:** `STATUS.md`, `HANDOFF.md`, `docs/COMPETITION_STATUS.md`, `docs/PLAN_APP_UPDATES.md`, any `docs/SESSION_*`, `ARCHITECTURE.md`, `server.js`, or any file over 15 KB. Use `search_code` `text_matches` to see the needed lines, patch scripts for big files.
- **6 reads and nothing written: STOP**, push an `IN PROGRESS` note.
- **One slice per chat, ~8 tool calls, one commit.** No questions to the owner (use `UNSURE:` lines).

## Why this exists
The owner is a free-plan user and rotates between about 8 Claude accounts. Each chat hits its limit after a few steps and is cut off without warning. In the past, work done in a chat was LOST because it was never pushed. So: **small slices, pushed immediately, logged in the same commit.**

## The rules
1. **Orient cheaply.** *(SUPERSEDED 2026-11-23 by the HARD RULE above: the old list of `HANDOFF.md` + `COMPETITION_STATUS.md` + two session files is far too expensive. Use the HARD RULE start list.)*
2. **Pick ONE slice you can finish AND push in at most ~8 tool calls.** A slice is a unit that is useful alone: one SQL file, one small new Kotlin file, one test run with its result written down, one doc. If the next stage is too big, cut it into a slice and say which part is left.
3. **Push as soon as the slice exists, in ONE commit** (`push_files`, branch `main`): the work + the updated stage board row + one session-log line + a `docs/SESSION_<date>_<letter>.md` file. Never leave work only in the chat.
4. **Log first, polish after.** If quota looks nearly gone, push the log (what is done, what is left) before anything else. If you start something big, push a line `IN PROGRESS: <slice>` first.
5. **Do not start a slice you cannot push in this chat.** Do not run long multi-stage plans in one chat; stop after the first pushed slice and tell the owner what comes next.
6. **Never claim more than the evidence.** Mark code `[~]` until CI is green AND the owner tested it. Say what you actually saw.
7. **Big files (`MainActivity.kt`)**: patch script + Action, see `docs/EDITING_LARGE_FILES.md`. Never resend a huge file whole.
8. **Forbidden files and the public-repo rule** (HANDOFF rules 12 and 13) still apply: no secrets, no student names/emails.
9. **Session logs are append-only.** Correct a mistake with a new correction line.
10. **Final reply to the owner**: plain words, short: what was pushed (commit), what is left, what to test. No long explanations.

## Tool recipe (chat agents with connectors)
- GitHub: `tool_search` to load `get_file_contents` / `push_files` / `search_code`. Repo `hiyabteklu/Wisdom-tower-academy-app-packed`, branch `main`. `push_files` replaces whole files, creates folders, and makes one atomic commit.
- Supabase: `tool_search` to load `list_projects` / `execute_sql`. The only project is "Wisdom tower academy" (eu-central-1). Applied migrations can be read back with `select version, name, array_to_string(statements, E'\n') from supabase_migrations.schema_migrations order by version desc;`. Follow `docs/SUPABASE_NOTES.md` before changing anything. DDL goes through a migration and then a copy is pushed to `docs/sql/`.
- Test SQL against live only inside a transaction that is rolled back, or on rows you delete afterwards; log the result you really saw.

## What a good next slice looks like (competition work)
- C1: run the functional SQL tests (rolled back) for `start_competition`, `submit_competition`, `competition_leaderboard`, `admin_publish_competition_from_exam`; write the outputs to the session log.
- C3: one new small Kotlin file under `data/competition/` per slice (models, then repository, then phase logic) from `docs/COMPETITION_API_CONTRACT.md`; read `docs/BUILD_PITFALLS.md` first.
- C2: the website-dashboard contract/checklist doc for the owner.
