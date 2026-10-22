# Instructions for Claude agents

> ## OWNER ORDER 2026-10-22 (newest): SECOND LIVE JOB = NATIVE "MY STATUS" V2 (real data per subject, chapter, exam; ring gauges; tap a small icon for a card)
>
> Plan: **`docs/MY_STATUS_V2_PLAN.md`** (read it first, it lists what already exists, the real data, the slices S1 to S6 and the progress table). One slice per chat, push straight to `main`, update the plan in the same commit. The games job below stays live too. Real data only: never invent a number.

---

> ## OWNER ORDER 2026-10-21: THE ONLY LIVE WORK IS THE NATIVE GAMES (Tower Climb + Tower Defense)
>
> Plan: **`docs/GAMES_NATIVE_PLAN.md`** (read it first). Handoff: `docs/GAMES_HANDOFF_FOR_CLAUDE.md`. Everything else is PARKED: `docs/PARKED_2026-10-21.md`.
> **Your role in this job (Claude chat):** Google AI Studio writes the bulk of the game code. You (1) fix the compile errors its code causes (the owner pastes the line starting with `e: file:///`; read `docs/GAMES_HANDOFF_FOR_CLAUDE.md` first, fix the smallest thing, log it in `docs/BUILD_PITFALLS.md` and in the handoff "CI errors seen"), (2) review a finished phase against the plan, (3) apply the optional G10 SQL only after the owner says go. Do not rewrite the games yourself unless a phase is broken beyond a small fix.
> The owner is on a free plan: small steps, push to `main` immediately, plain short words, lead with the answer.
> Do not continue the security, competition, notifications, admin, day mode, play store or motion plans: they are PARKED, whatever the old text below says.

---

# PARKED (kept for history only; do NOT follow unless the owner reactivates a named plan)

**READ FIRST (owner order 2026-10-20, newest, top priority): `docs/OWNER_RULE_PLAN_FIRST_PUSH_TO_MAIN.md`, then the ACTIVE PLAN `docs/SECURITY_PLAN_CONTENT_ACCESS.md` (server-enforced content locks, bulletproofing for the Telegram APK before Play Store). Continue that plan slice by slice. Push every slice directly to `main` (no feature branch), update the plan file in the same commit, never make the owner re-explain, never switch approach without writing why under "Plan changes" in the plan. Talk to the owner in plain words.**

**DAY MODE RETURNS (owner order 2026-10-19, newest decision; it overrides the "DAY / NIGHT MODE IS CANCELLED" paragraph below for DAY MODE ONLY): read `docs/DAY_MODE_PLAN.md` (premium "Ivory & Navy" day mode, rules, slices D1 to D9, status table in its section 6). Night stays exactly as it is. Do NOT use the old pale palette or slices L1 to L9 of `docs/DAY_NIGHT_PLAN.md`. Day cards must NOT be see-through (that was the reason for the cancel): the plan explains the frosted-ivory fix. One slice per chat, the owner says "do D<n>", do not start one on your own.**

**DAY / NIGHT MODE IS CANCELLED (owner order 2026-10-17, superseded for day mode by the 2026-10-19 paragraph above; it OVERRIDES `docs/DAY_NIGHT_PLAN.md`, ignore slices L1 to L9).** The app uses many transparent glass cards, so a light page does not work. The app is night-only: `AppThemeMode.isDark` is always true, and the light branches in Color.kt, WisdomGlass.kt, WisdomFieldTokens.kt, WisdomMessageCard.kt and Theme.kt are dead code (do not bring them back and do not build a day switch, EXCEPT as ordered by `docs/DAY_MODE_PLAN.md`). The student only picks a BACKGROUND (Settings > Display & Theme): Dark, BG 1, BG 2, BG 3 (the pictures `app/src/main/assets/backgrounds/bg_1.png` to `bg_3.png`), Blue. Code: `ui/theme/AppBackground.kt`. Still use `WisdomInk`, `WisdomOnInk`, `WisdomTextPrimary`, `WisdomMuted` instead of `Color.White` / `Color.Black` for text and tints.

**PLAY STORE PREPARATION (owner order 2026-10-17, the current priority): read `docs/PLAY_STORE_UI_PLAN.md` (release risks and UI slices S0 to S10, status table in its section 3). One slice per chat; the owner says "do S<n>", otherwise do the next open slice. First make sure the patch `scripts/patch_ux_consistency_fixes.py` was run and merged (see the plan, section 1).**

**FIRST, OWNER RULE 2026-10-16: read `docs/OWNER_RULE_WRITTEN_IS_DONE.md`. Written and pushed code counts as DONE (builds are green, the owner has confirmed it works). It overrides every `[~]`, "CI NOT seen" and "not seen on a device" label in the older docs. Report only what is genuinely not done.**

**UI MOTION (owner order 2026-10-16, third live task): read `docs/UI_MOTION_PLAN.md` (root causes, motion language, area inventory, slices M1 to M11, status table in its section 7). It overrides the "do not work on UI polishing" lines below for MOTION AND TRANSITIONS ONLY (bottom nav pill, tab and screen transitions, card entrance, Settings accordions and the rest of the plan). Everything else archived stays closed. One slice per chat; the owner says "do M<n>", otherwise do the next open slice.**

The agent rules live in ONE place: **`HANDOFF.md` section 0 (MANDATORY AGENT PROTOCOL)**. Read `HANDOFF.md` in full first, then **`docs/COMPETITION_STATUS.md`** (the live stage board), `docs/COMPETITION_DECISIONS.md` (owner answers, they override the plan), `docs/COMPETITION_PLAN.md`, then `docs/BUILD_PITFALLS.md` before writing any code, and the newest `docs/SESSION_*.md` files. Section 0, rule 10, plus `docs/EDITING_LARGE_FILES.md`, explain how to change files that are too big to resend: patch script plus the GitHub Action; NEVER answer "the file is too big".

**NOTIFICATIONS V2 (owner order 2026-10-15, live work, extends C8): read `docs/NOTIFICATIONS_V2_PLAN.md` (plan, verified facts, slices R0 to R8, progress table in its section 8) right after the status board. It spans this repo, the WEBSITE repo and the live Supabase project.**

**ADMIN CONSOLE (owner orders 2026-10-15 and 2026-10-16): `docs/ADMIN_MOBILE_PLAN.md` and `docs/ADMIN_UI_V2_PLAN.md`. Code is in the WEBSITE repo.**

**FREE-QUOTA RULE (owner is on a free plan and rotates between ~8 accounts; unpushed work is lost): read `docs/AGENT_QUOTA_PROTOCOL.md`. Do ONE small slice, push it in ONE commit with the stage board and a session log, then tell the owner what is next. Do not chain many stages in one chat.**

Current phase (2026-10-14): **the competition space is the ONLY live work** (trophy replaces Refresh, global timed exams, countdown, native runner, leaderboard). The owner ordered the stages C1 to C10 to be executed one at a time, in order. UI polishing T1 to T9 and everything older is archived (`docs/archive/README.md`): do not work on it. The global constraints G1 to G9 in `docs/UI_POLISH_TASKS.md` (forbidden files, no invented text, no new dependencies or hosts) stay in force. For every stage: inspect the code first, make focused changes, keep the approved design, and avoid regressions.

Before you stop (or when your quota is nearly out), write the session log first (`docs/COMPETITION_STATUS.md` session log and a `docs/SESSION_*.md` file).

The repo is public: never commit secrets or personal data. The owner is not a coder: explain in plain language, short, answer first; ask in plain words when you are stuck.
