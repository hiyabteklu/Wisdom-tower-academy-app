# OWNER DECISION 2026-10-19: publish with Night, Day and Transparent

The owner discarded the "ship Dark only first" gate (`docs/DAY_MODE_PLAN.md` section 7). The first Play Store build ships with all three looks.

- `DAY_MODE_OFFERED` in `ui/theme/AppBackground.kt` STAYS `true`. Do NOT set it to `false` and do not propose the gate again.
- Settings offers Night | Day | Transparent (`scripts/patch_d12_third_pill_transparent.py`).
- Consequence: the D9 audit (contrast and screenshots of every screen in all three looks, TalkBack, font Large) and the remaining day slices (D7e to D7g, pill tint fix) are now release blockers, not optional polish.
- Transparent is night colours over a photo with see-through glass: audit it as its own look.
