# Handoff (current status only)
CLOSED: repo-cleanup / APK-size task (owner decision 2026-11-27). Nothing pending from it. Owner installed the APK after the cleanup: it works.
Next: new task from the owner. Live plan otherwise: docs/ECE_STATUS.md.
Blocked: none.

## What was done
- website-reference/ removed; AGENTS.md is the entry point; docs moved to docs/; old handoff in docs/HISTORY.md.
- .env.example restored; backup and hard rule in docs/ENV_EXAMPLE_BACKUP.md.

## Decisions (do not redo unless the owner asks)
- Tinos fonts: KEEP as is. Both sets are used (res/font by Compose Type.kt, assets/fonts by notes/richtext.css). Duplicate stays.
- Root server.js, src/, index.html, vite.config.ts, package.json, tsconfig.json: verified not used by Gradle or CI, but KEPT (owner chose to keep the current setup).
Note: other docs may still link old root paths; not scanned.
