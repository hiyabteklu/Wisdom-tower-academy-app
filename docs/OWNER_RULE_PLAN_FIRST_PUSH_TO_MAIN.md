# OWNER RULE (2026-10-20): plan first, push every slice to main, never make the owner re-explain

> Repo is PUBLIC: no secrets, no student data.

The owner is NOT a coder and is on a free tier, rotating accounts, so a new agent starts every 3-4 prompts. Past agents kept switching to a different approach because the plan was only in chat. Therefore:

1. **READ FIRST, in this order:** this file, then `docs/SECURITY_PLAN_CONTENT_ACCESS.md` (the active plan), then the newest `docs/SESSION_*` file. Continue the plan. Do NOT invent a new approach or restart. If you think the plan is wrong, write why in the plan file under "Plan changes" and keep going unless the owner decides.
2. **PLAN BEFORE CODE.** Any multi-slice job gets a plan file in `docs/` pushed to `main` BEFORE the first slice.
3. **PUSH EVERY SLICE DIRECTLY TO `main`, every time**, right after it is made (owner order, no feature branch, no waiting for a PR). One slice = one commit. Commit message ends with `not compiled` when the CI was not seen.
4. **UPDATE THE PLAN FILE IN THE SAME COMMIT** as each slice: tick the slice, note what is live in Supabase, note the next slice. The next agent must be able to continue with zero explanation from the owner.
5. **Record every live Supabase change** (migration name + one line of what it does) in the plan file. Database changes made through the Supabase connector are invisible in git otherwise (this already caused a stale doc on 2026-10-20).
6. **Talk to the owner in plain words.** No jargon, no walls of technical text. Say what changed, what he must test on the phone, nothing more.
7. Website repo is NOT a priority (see `docs/SESSION_2026-10-20_GRANULAR_LOCKS.md`). App only.
