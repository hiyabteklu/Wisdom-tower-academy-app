# SESSION 2026-10-23 PAID S3 (admin Locks screen as a tree)

Append-only note for `docs/PLAN_TELEGRAM_PAID_NATIVE_V2.md` (read that first). Previous agent stopped after the plan commit (S2 ticked, facts on orders RLS).

## Done (written `[~]`, NOT compiled, NOT seen on a phone)
- `ui/admin/LocksScreen.kt` rewritten (whole file). Tree: Academy > package > subject or ECE course > hub > chapters > materials, same drill-down as the student navigation. Only 3 actions everywhere: Owned only (`require_purchase`), Free for signed-in (`open`), Coming soon (`coming_soon`). An old row with mode `locked` is shown as Coming soon.
- Package step: card THIS PACKAGE with the 3 actions. Choosing one first removes every chapter and material row of that package, then writes `package:<id>` (the admin gets a confirm dialog when rows would be removed; the owner may correct this rule).
- Hub step: list of chapters (from the materials plus `chapter:` rows with no materials) and materials. Tag colour: coloured = set on that row, grey = same as the package. Tap opens a panel with the 3 actions and a button Same as the package (deletes the row). Choosing the same action as the package deletes the row instead of writing one.
- Add chapter: button in every hub, panel with number and optional title, saves `chapter:<scope>:<hub>:<n>` = Coming soon (label = title or Chapter N). The student list (S1) shows it as a row.
- New `data/admin/LocksBulk.kt`: `clearMany` (delete many lock rows) and `itemIdsInPrefix` (which material ids live under a package scope prefix, so the item rows of one package can be found).
- Removed from the screen on purpose: PAID MODE card (`system:enforce`; the Telegram build decides by the rows), the package shortcut block, the all-overrides list, the Hard locked action. The `system:enforce` row in the database is untouched. Owner may ask for any of them back.
- Not changed: `LocksRepository.kt`, the keys, `ContentAccess.kt`.

## Unsure / to check on the next green run
- Compile result of this commit (new Compose code, `BasicTextField` with `KeyboardOptions`, local functions inside the screen).
- ECE: the package row uses the sold id `ece-y3-sem-1` / `-2` from the tree, same as S1 reads. Chapter rows use the scope of the semester that is open.
- `itemIdsInPrefix` uses `scope_path=like.<prefix>*`; if a package has item locks stored under both ECE semesters, only the open semester is cleaned.

## Left
- S4 admin approval, S5 cleanup (see the plan).
- Owner actions still open: run Actions > Apply pending patches for `scripts/patch_bank_logos.py` and `scripts/patch_package_tab_highlight.py`.
