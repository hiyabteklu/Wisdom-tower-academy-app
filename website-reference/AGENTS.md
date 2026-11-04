# AGENTS.md (website repo)

Read `docs/STATUS.md` first (newest entry on top), then the files it names.

## LIVE TASK: Admin Console redesign for the phone (owner order 2026-10-15)
The in-app Admin Console is this website's `/admin` page. The owner wants it rebuilt for the phone: a grid of modules, no horizontal scrolling, smooth scrolling, same look as the app.
- Website plan: `docs/ADMIN_MOBILE_PLAN.md` (this repo). MASTER plan with the owner order, hypotheses and acceptance checklist: `hiyabteklu/Wisdom-tower-academy-app-packed` -> `docs/ADMIN_MOBILE_PLAN.md`. Read both.
- Do one slice per chat. Do not change admin permissions, API routes or data behaviour: layout only.
- No invented text or numbers. This repo is PUBLIC: no secrets, emails or student names.
- The Claude sandbox cannot run npm; Vercel builds every push to `main` and a TypeScript error fails the deploy, so write type-safe code and say plainly that it was not compiled.
