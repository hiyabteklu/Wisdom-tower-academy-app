# Instructions for AI coding agents

> ## HARD RULE (owner order 2026-11-23): SPEND QUOTA LIKE IT IS ALMOST GONE
>
> The owner is on a free Claude plan. A previous agent used its whole limit just READING this repo and produced nothing. That must never happen again.
>
> 1. **Read the smallest possible set. Max 4 reads before your first push.** Allowed start list for the live work: this file, then `docs/ECE_EXPANSION_SLICES.md`, then `docs/ELECTRICAL_1ST_TO_5TH_YEAR_EXPANSION_PLAN.md`. Nothing else unless the slice you picked names it.
> 2. **NEVER list `docs/` (about 500 files, one listing costs ~15k tokens).** Never list the repo root twice. To find a file use `search_code` with `fields: ["path"]`, or a directory listing with a narrow path and `fields: ["name"]`.
> 3. **NEVER read these whole:** `STATUS.md` (39 KB), `HANDOFF.md` (22 KB), `docs/COMPETITION_STATUS.md` (52 KB), `docs/PLAN_APP_UPDATES.md` (48 KB), `docs/PLAN_*`, `docs/SESSION_*` (about 450 old logs), `ARCHITECTURE.md`, `server.js`, `MainActivity.kt` and any file over 15 KB. For a big file find the exact lines with `search_code` (`text_matches`) and change it with a patch script (`docs/EDITING_LARGE_FILES.md`).
> 4. **One slice per chat, finish it, push it to `main` in ONE commit** (work + one session-log file `docs/SESSION_<date>_<name>.md`). Target: at most ~8 tool calls in total.
> 5. **If you have done 6 reads and nothing is written yet: STOP reading.** Push a short `IN PROGRESS: <slice> / found so far / next step` note and tell the owner.
> 6. **Do not ask the owner questions.** Decide from the plan; write `UNSURE:` lines in the session log.
> 7. **Answer the owner in plain words, short:** what was pushed, what is left, what to test.
>
> The rules above beat every reading list further down this file and in `HANDOFF.md` section 0.

> ## OWNER ORDER 2026-11-23: LIVE WORK = ELECTRICAL (ECE) PACKAGES FOR YEARS 1, 2, 4, 5
>
> Follow `docs/ECE_EXPANSION_SLICES.md` (small slices, pick the first one not marked done). **Year 3 is LOCKED: do not touch its data, ids, routes or screens.** Never invent course names, stream names or ids: take them from the owner's curriculum blueprint or write `UNSURE:`.

---

> ## OWNER ORDER 2026-10-21: THE ONLY LIVE WORK IS THE NATIVE GAMES (Tower Climb + Tower Defense)
> *(Superseded for now by the 2026-11-23 order above. Games stay parked until the owner reactivates them by name.)*
>
> **Read `docs/GAMES_NATIVE_PLAN.md` NOW and follow it from Phase G0, committing each phase to `main`.** Then read `docs/BUILD_PITFALLS.md` before writing any Kotlin.
> **Every other plan, stage, slice and task in this repo is PARKED** (list: `docs/PARKED_2026-10-21.md`). Do not continue, fix or re-plan any of it, whatever the text below says.
> The games plan overrides the reading list and rule 3 of `HANDOFF.md` section 0 and the "one slice per chat" rule of `docs/AGENT_QUOTA_PROTOCOL.md`. The other rules of `HANDOFF.md` section 0 still hold (forbidden files, no new dependencies or hosts, never invent text or numbers, public repo).
> Do not ask the owner questions: decide from the plan, write `UNSURE:` lines in `docs/GAMES_HANDOFF_FOR_CLAUDE.md`.

---

# PARKED (kept for history only; do NOT follow unless the owner reactivates a named plan)

**READ FIRST (owner order 2026-10-20): `docs/OWNER_RULE_PLAN_FIRST_PUSH_TO_MAIN.md`, then the ACTIVE PLAN `docs/SECURITY_PLAN_CONTENT_ACCESS.md` (server-enforced content locks, app hardening). Continue that plan slice by slice. Push every slice directly to `main`, update the plan file in the same commit, never make the owner re-explain, never switch to a new approach without writing why in the plan under "Plan changes".**

The agent rules live in ONE place: **`HANDOFF.md` section 0 (MANDATORY AGENT PROTOCOL)**. Read `HANDOFF.md` in full first, then **`docs/COMPETITION_STATUS.md`** (the live stage board), `docs/COMPETITION_DECISIONS.md` (owner answers, they override the plan), `docs/COMPETITION_PLAN.md`, then `docs/BUILD_PITFALLS.md` before writing any code, and the newest `docs/SESSION_*.md` files. Section 0, rule 10, plus `docs/EDITING_LARGE_FILES.md`, explain how to change files that are too big to resend (patch script in `scripts/` + the GitHub Action); never stop at "the file is too big".

**NOTIFICATIONS V2 (owner order 2026-10-15, live work, extends C8): read `docs/NOTIFICATIONS_V2_PLAN.md` (plan, verified facts, slices R0 to R8, progress table in its section 8) right after the status board. It spans this repo, the WEBSITE repo and the live Supabase project.**

Current phase (2026-10-14): **the competition space is the ONLY live work.** The owner ordered stages C1 to C10 to be executed one at a time, in order. UI polishing T1 to T9 and everything older is archived (`docs/archive/README.md`): do not work on it. The global constraints G1 to G9 in `docs/UI_POLISH_TASKS.md` stay in force. Inspect the code first, make focused changes, keep the approved design, avoid regressions, and never claim something is verified or tested without evidence.

Before you stop (or when your quota is nearly out), write the session log first.

The repo is public: never commit secrets or personal data. The owner is not a coder: explain in plain language.
