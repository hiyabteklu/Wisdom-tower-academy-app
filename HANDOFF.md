# Handoff (current status only)
Last slice done: Slices 1-4 (website-reference removed, AGENTS.md entry point, docs moved to docs/, old handoff in docs/HISTORY.md).
Next: Tinos font dedupe (assets/fonts vs res/font), separate task, owner decides. Then docs/ECE_STATUS.md.
Blocked: none
Note: other docs may still link the old root paths; not scanned.

## Slice 5 findings
- `server.js`: web-only (Node HTTP preview server on port 3000; not used by Android, Gradle or CI)
- `src/`: web-only (React/Vite preview shell; not used by Android, Gradle or CI)
- `index.html`: web-only (root Vite HTML entry point; not used by Android, Gradle or CI)
- `vite.config.ts`: web-only (Vite dev/build config on port 3000; not used by Android, Gradle or CI)
- `package.json`: web-only (Vite/React npm config; not used by Android, Gradle or CI)
