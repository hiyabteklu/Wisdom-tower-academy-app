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
- Never touch .env* or any key file. HARD RULE: if .env.example is ever changed, blanked or removed, restore it from docs/ENV_EXAMPLE_BACKUP.md first, no matter what.
- Don't reformat beyond the slice. No new dependencies. Don't touch Year 3, payments, access rules, Supabase policies or releases.
- Never invent course names, ids or numbers; write UNSURE: in the log. Never claim something is tested without evidence.

## Index
- Live plan: docs/ECE_STATUS.md
- Supabase work: docs/SUPABASE_NOTES.md
- Build, CI or signing changes: docs/HISTORY.md section 0 (rules 12, 13) first
- Release and app docs (only if the slice names them): docs/RELEASES.md, docs/PLAY_STORE_CHECKLIST.md, docs/NATIVE_APP_BRIEF.md, docs/ARCHITECTURE.md
