# SESSION 2026-11-06 — U4: scholarship guide, one card per heading

Plan: `docs/PLAN_2026-11-06_URGENT_FIVE_FIXES.md` (owner: every line of the scholarship guide is its own "Good to know" card).
U3e is merged (PR #112). A first U4 attempt by the previous agent was never pushed (its file only existed in that chat), so U4 was redone from the real text.

## What was wrong (checked in the real data, 2026-11-06)
The guide text is `public.free_resource_pages.body_md` for slug `scholarships` (12.7 KB, written by the owner in the admin panel). It is an article:
6 headings (`##`), 9 sub-headings (`###`), 2 tips (`>`), 2 bullet lists (`-`), and **bold** / *italic* marks.
The old Tip landing (`parseTipBlocks` + `TipLanding` in `GuidesScreen.kt`) cut it at EVERY blank line: each paragraph became its own card, the heading marks were dropped
and everything after the fourth paragraph was labelled "Good to know". Only the labels for the first four positions were ever defined.

## What the student sees after U4
- ONE card per `##` heading, in the same glass look as before (number, round icon, heading), Night / Day / Transparent and text size as before.
- `###` lines are sub-headings inside their card (accent colour), e.g. the five scholarship categories under "The Real Categories".
- `-` lines are a tick checklist ("Where to Actually Look" and "Remember"), with the **bold** lead words shown bold.
- `>` lines are one amber tip box, no label (so "Good to know" no longer repeats at all).
- `**bold**` and `*italic*` are shown as real bold / italic (the Weak / Strong examples). THE WORDING IS NEVER CHANGED.
- Header card, the scholarship opportunity cards below and their order are untouched. If the intro has no `##` heading (the built-in default intro), the old Tip landing is used as before.
- Only the Scholarships tab changes. The Universities tab keeps the old Tip landing.

## What was written
- `scripts/u4/ScholarshipArticle.kt.txt` (text on main so main still builds): parser `parseSchArticle`, inline marks `schInline`, composables `SchArticleView`, `SchArticleCard`, `SchArticleBullet`, `SchArticleCallout`.
- `scripts/apply_u4_scholarship_article.py` (patch script): 4 imports, inserts the block before the "6. SCHOLARSHIPS" section of `GuidesScreen.kt` (it needs that file's private helpers), and switches `ScholarshipsFullSection` to the article.
  Tested on a mock copy with the real anchors: patches all, second run = "already patched", a wrong anchor writes nothing. The parsing logic was tested with a Python port on the real structure: 6 cards, 2 tips, 4+ sub-headings, 2 lists, every word kept.
  The Kotlin is NOT compiled here, so the first branch build is the real test.

## Owner steps
GitHub > Actions > **Apply pending patches** > Run workflow > script `apply_u4_scholarship_article.py`, branch `fix/u4-scholarship-article` > wait for the build > test > merge the pull request.
If the build fails, send the line that starts with `e: file:///`.

## Test on the phone
1. Other resources > Scholarship Info: the guide is 6 cards, each with its heading ("Why Most Applications Die...", "The Real Categories...", "The Application Itself...", "The Part Nobody Says Out Loud...", "Where to Actually Look...", "Remember").
2. No card says "Good to know". The two quoted tips show as amber boxes with a bulb.
3. Under "The Real Categories" the five numbered categories are sub-headings inside one card; "Weak:" and "Strong:" are bold, the sentences after them italic.
4. "Remember" is a tick list of ten lines. The scholarship cards and the closing note below the guide look as before.
5. Night, Day and Transparent; large text size; scroll is smooth.

## Not changed (on purpose, not asked)
- Universities tab and the other guides (Study techniques, Campus life, Departments) keep their layout; they do not share this text.
- The guide wording in Supabase was not touched (no live Supabase change in U4).
