# Handoff (current status only)
Done: Slices 1-5 (website-reference removed, AGENTS.md entry point, docs moved to docs/, old handoff in docs/HISTORY.md, Slice 5 report). Owner installed the APK after these: it works. .env.example restored; backup and hard rule in docs/ENV_EXAMPLE_BACKUP.md.
Next: docs/AI_STUDIO_NEXT.md (A: verify web leftovers, report only; B: Tinos font dedupe).
Blocked: none.

## Slice 5 findings (UNVERIFIED, from AI Studio, report only)
- server.js, src/, index.html, vite.config.ts, package.json: said web-only (Vite/React preview shell, port 3000), not confirmed against Gradle/CI. Do NOT delete until task A confirms.
Note: other docs may still link old root paths; not scanned.
