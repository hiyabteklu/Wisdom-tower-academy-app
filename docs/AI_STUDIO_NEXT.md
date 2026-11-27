# Next tasks for AI Studio (A then B)

Owner order. This file is the WHOLE task. Follow it literally.

## FORBIDDEN (breaking any one = task failed)
1. NEVER open, edit, list, rename, delete or "tidy" `.env.example`, `.env*`, keystores, `*.properties`, `google-services.json` or any key file. If `.env.example` shows in your diff, restore it from `docs/ENV_EXAMPLE_BACKUP.md` before finishing.
2. NEVER scan the whole repo. NEVER list `docs/`. NEVER open `MainActivity.kt` in full (275 KB): only grep it for the exact strings named below.
3. Touch ONLY the paths named below. Revert anything else.
4. Do not reformat or edit any file not named. Do not ask questions; if unsure, do nothing for that item and write it under Blocked in HANDOFF.md.

## TASK A: verify web leftovers (REPORT ONLY, delete nothing)
Read ONLY: `settings.gradle*`, `app/build.gradle*`, `.github/workflows/*.yml`.
For each of `server.js`, root `src/`, `index.html`, `vite.config.ts`, `package.json`, `tsconfig.json`: is it used by Gradle or CI (named in a workflow, a Gradle task, or a build script)? Answer one line each: used by Android/CI, or web-only, or unsure.
Write at most 7 lines under `## Verification (A)` in HANDOFF.md. Keep HANDOFF.md under 1.5 KB.

## TASK B: Tinos font duplicate (two identical TTF sets, about 2.1 MB each)
Sets: `app/src/main/assets/fonts/` and `app/src/main/res/font/`. KEEP `res/font`.
1. List ONLY those two folders (name + size).
2. Grep ONLY inside `app/src/main` for the strings: `assets/fonts`, `android_asset/fonts`, `fonts/Tinos`, `Tinos`, `R.font`. Report each matching file path and the matching line only.
3. DECISION RULE:
   - If ANY match refers to the `assets/fonts` path (code, CSS, HTML, JS, Kotlin): delete NOTHING. Write the matches under `## Tinos (B)` in HANDOFF.md and stop.
   - If NO match refers to `assets/fonts`: delete the Tinos TTF files inside `app/src/main/assets/fonts/` only (and the folder if it becomes empty). Do not rename, move or edit anything else.
4. Commit message: `Tinos: remove duplicate assets/fonts set` (deletions only).

## FINAL SELF-CHECK (print it)
1. List every path changed; each must be named in this file.
2. State whether `.env.example` appears in the diff (it must not).
3. State anything you could not do.
