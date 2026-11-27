# AI Studio cleanup plan (slices 3 to 5)

Owner order. This file is the WHOLE task. Follow it literally. Do not improve it, extend it or reinterpret it.

## FORBIDDEN (breaking any one = task failed)
1. NEVER open, read, create, edit, rename, delete, reformat or print: `.env`, `.env.example`, `.env.*`, `*.properties`, `*.keystore`, `*.jks`, `google-services.json`, or any file holding keys, tokens or secrets. `.env.example` contains real values. Do not "tidy" it, do not list its names, do not fill it with empty names. It must stay byte-identical.
2. NEVER scan the repo. NEVER search for API keys. NEVER list directories (only `.github/workflows` may be listed). NEVER open files in `docs/` except those named below.
3. Touch ONLY the paths under "Allowed paths". If any other file changed, revert it before finishing.
4. Moved files are EXACT copies: no reformatting, no re-wrapping, no typo fixes, no summaries, no "improvements". If you cannot reproduce a file exactly, do NOT move it; write it under Blocked in HANDOFF.md.
5. NEVER touch fonts, `app/`, Gradle files, source code, `public/`, brand assets or website files.
6. Do not ask questions. If unsure, do nothing for that item and write it under Blocked.

## Allowed paths
READ: `.github/workflows/*.yml`, `settings.gradle*`, `app/build.gradle*`, and the files named below.
WRITE/DELETE ONLY: `AGENTS.md`, `README.md`, `HANDOFF.md`, `START_HERE.md` (delete), `PLAY_STORE_CHECKLIST.md`, `RELEASES.md`, `RELEASE_NOTES.txt`, `NATIVE_APP_BRIEF.md`, `ARCHITECTURE.md` (delete from root), and new `docs/PLAY_STORE_CHECKLIST.md`, `docs/RELEASES.md`, `docs/RELEASE_NOTES.txt`, `docs/NATIVE_APP_BRIEF.md`, `docs/ARCHITECTURE.md`, `docs/HISTORY.md`.

## SLICE 3 (one commit: "Slice 3: move docs, merge START_HERE into AGENTS")
A. Read `.github/workflows/*.yml` only. If a workflow names one of the 5 files, do NOT move that file; write it under Blocked.
B. For each remaining file: create it under `docs/` with identical content, delete the root copy.
C. `README.md`, exact edits only:
   - Replace the two top blockquote paragraphs ("OWNER ORDER 2026-10-21 ..." and "AI agents: read HANDOFF.md FIRST ...") with this one line: `> **AI agents: read [AGENTS.md](AGENTS.md) first.**`
   - Replace the line `Then read [NATIVE_APP_BRIEF.md](NATIVE_APP_BRIEF.md) and [ARCHITECTURE.md](ARCHITECTURE.md).` with `Then read [docs/NATIVE_APP_BRIEF.md](docs/NATIVE_APP_BRIEF.md) and [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) only if your task needs them.`
   - Change nothing else in README.md.
D. Replace `AGENTS.md` with EXACTLY the text between the markers below (use the old HANDOFF path only if Slice 4 fails).
E. Delete `START_HERE.md`.

<<<AGENTS
# Agents: start here

- Wisdom Tower Academy: native Android app (Kotlin, Compose), built by GitHub Actions. Backend: Supabase.
- The live website is a separate repo (hiyabteklu/Wisdom-tower-academy).
- Production app, public repo: never break the build; never commit secrets, keys or student data.
- Live plan: docs/ECE_STATUS.md (ECE Years 1, 2, 4, 5; Year 3 is LOCKED). Other plans are parked.
- Website snapshot removed. The live site is hiyabteklu/Wisdom-tower-academy; don't copy it here.

## Rules (owner is on a free tier)
- Read only the files the slice names (about 8 KB, max 4 reads before the first push). Never list the repo root, docs/ (~500 files) or scripts/. Never open MainActivity.kt (275 KB). search_code does not work here.
- Do ONE small slice. Commit and push after EACH slice.
- If budget may run out, stop and write 3 lines in HANDOFF.md: done / next / blocked.
- Files over 10 KB: don't resend, use a patch script (docs/EDITING_LARGE_FILES.md).
- Never touch .env* or any key file. Don't reformat beyond the slice. No new dependencies. Don't touch Year 3, payments, access rules, Supabase policies or releases.
- Never invent course names, ids or numbers; write UNSURE: in the log. Never claim something is tested without evidence.

## Index
- Live plan: docs/ECE_STATUS.md
- Supabase work: docs/SUPABASE_NOTES.md
- Build, CI or signing changes: docs/HISTORY.md section 0 (rules 12, 13) first
- Release and app docs (only if the slice names them): docs/RELEASES.md, docs/PLAY_STORE_CHECKLIST.md, docs/NATIVE_APP_BRIEF.md, docs/ARCHITECTURE.md
AGENTS>>>

## SLICE 4 (one commit: "Slice 4: split HANDOFF.md")
A. Create `docs/HISTORY.md` with the CURRENT `HANDOFF.md` content, character for character.
B. Replace `HANDOFF.md` with the short template below (max 1.5 KB).

<<<HANDOFF
# Handoff (current status only)
Last slice done: Slices 1-4 (website-reference removed, AGENTS.md entry point, docs moved to docs/, old handoff in docs/HISTORY.md).
Next: Tinos font dedupe (assets/fonts vs res/font), separate task, owner decides. Then docs/ECE_STATUS.md.
Blocked: <list items you could not do, or "none">
Note: other docs may still link the old root paths; not scanned.
HANDOFF>>>

## SLICE 5 (one commit: "Slice 5: web leftovers report"). REPORT ONLY, DELETE NOTHING
Check whether `server.js`, root `src/`, `index.html`, `vite.config.ts`, `package.json` are used by the Android app, Gradle or `.github/workflows`, or only by a leftover web setup. You may read ONLY: `settings.gradle*`, `app/build.gradle*`, `.github/workflows/*.yml`, and the first 40 lines of each of the five items. Append at most 8 lines of findings to `HANDOFF.md` under `## Slice 5 findings`, one line per item: used by Android / used by CI / web-only / unsure. Keep HANDOFF.md under 1.5 KB.

## FINAL SELF-CHECK (print this, do not skip)
1. List every path you changed. Each must be in "Allowed paths".
2. State: `.env.example` was not opened, not edited, not listed.
3. State anything you could not do, in plain words.
