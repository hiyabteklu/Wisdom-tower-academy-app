# AI Studio task: ECE Year 1/2 alias, app side only (E2c-c-app)

You have no database access. Do everything in the app code. The database rule (E2c-e) is already applied and tested, so do not write SQL and do not ask for any Supabase change.

Read first: `docs/ECE_STATUS_UPDATE_2026-11-23_ALIAS.md` and `docs/ECE_E2C_C_ALIAS_MAP.md`. Do not read anything else unless you must.

## Do
1. In `LearningContentRepository.kt`, add ONE helper that maps scope `ece/y1-sem-N/<id>` or `ece/y2-sem-N/<id>` (N = 1 or 2, id = lowercase course code) to its freshman scope, using the table in `ECE_E2C_C_ALIAS_MAP.md`. Any other scope returns null and keeps today's behaviour exactly. Year 3 and the `math-natural` alias stay untouched.
2. Use the helper in the same three places the existing aliases are used: `list`, `getHubCounts`, and offline `catalogRows`. No rows are copied.
3. Read ONLY the function `inferPackageAndScope` in `ContentAccess.kt`. Confirm the course path becomes scope `ece/y1-sem-N/<id>`. If it does not, say so in the log instead of guessing.
4. Deliver it as ONE patch script `scripts/ece_e2c_alias.py`: uses anchors, is safe to run twice, and stops before writing if an anchor is missing. Do NOT resend `LearningContentRepository.kt` or `ContentAccess.kt` whole.

## Hard rules
- **Do NOT create, edit, or add any line to `.env.example`.** Leave it exactly as it is. Do not add empty values, placeholders, or new keys anywhere. If the build fails, report the error in the log and do not change `.env.example` to fix it.
- Do not touch Year 3 files, prices, locks, or the 4 price rows.
- One commit, push to `main`. The app changes only when the GitHub Action runs the script.
- Add one log row to `docs/ECE_STATUS_UPDATE_2026-11-23_ALIAS.md` saying what was done and what is unsure.

## Still unconfirmed by the owner (easy to change in the alias map)
- loct1011 Critical Thinking = freshman logic
- mcie1012 Moral and Civic Education = freshman civics
- eceg1052 Computer Programming = freshman cpp-programming
