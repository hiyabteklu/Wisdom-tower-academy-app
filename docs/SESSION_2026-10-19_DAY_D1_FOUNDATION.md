# SESSION 2026-10-19 - DAY MODE D1 (foundation)

Owner request (chat): "continue the open tasks, UI day mode". The only open work in `docs/DAY_MODE_PLAN.md` was D1 to D9, so this chat did D1 (one slice per chat, `docs/AGENT_QUOTA_PROTOCOL.md`).

## What was done (written and pushed to `main`, two commits; NOT compiled by the agent, CI decides)
1. `ui/theme/AppThemeMode.kt`: `isDark` is no longer forced true. It is `!AppBackground.selected.isLight`, so the saved background choice drives day / night. Default stays Dark.
2. `ui/theme/AppBackground.kt`: new option `Day` (label "Day", base E9E5DC, two soft glows: pale sky at the top, champagne at the bottom) and a new flag `isLight` (true only for Day). The picker offers Dark and Day (Day replaces BG 2, the plan's assumption). BG 2 and the other pictures stay in the enum and the assets (nothing deleted) but are not offered; a saved BG 2 loads as Dark. New constant `DAY_MODE_OFFERED` (publishing gate, plan section 7).
3. `ui/theme/Color.kt`: `WisdomLightPalette` replaced by the Ivory & Navy values from the plan (page E9E5DC, card F8F5EE at 82 percent, navy 12 percent hairline border, ink 0B1B33, muted 4A5A70, teal 0B7285). Day hero, card and primary-button gradients updated. Night values and token names are unchanged.
4. `ui/theme/Theme.kt`: real light Material scheme on the new palette, with ivory overlay roles at about 92 percent for pop-ups, menus and sheets. Status and navigation bars use the solid ivory page colour with dark icons in day (the sync code already existed).
5. `ui/settings/BackgroundSection.kt`: sun icon for Day, moon for Dark. `Color.White` replaced by `WisdomInk` (white at night, so night is identical).

## Decisions and deviations (so the owner can overrule)
- Day category accents are darker than the old pale palette for amber (92400E), emerald (065F46) and orange (9A3412): my hand arithmetic gave about 4.0 to 4.4 for the old values on ivory, below the plan's 4.5 rule. Hypothesis, D9 measures.
- The plan's accent 0B7285 was kept as written. By my hand estimate it is about 4.4 on the bare page and about 5.1 on a card. If D9 measures it under 4.5 on the page, darken it slightly.
- No gold token yet (plan: premium moments only); it is added in the slice that first uses it.
- Plan section 8 (replace BG 2 or add a third icon) is still unanswered; Day replaces BG 2 for now.

## Known to look wrong in day until later slices (expected, not bugs of D1)
- Anything still written with `Color.White` or a fixed dark colour in screens (swept in D3 to D7).
- `WisdomGlass.kt`, `WisdomFieldTokens.kt`, `WisdomMessageCard.kt` still hold their old light branches (D2 replaces them with the frosted-ivory rule).
- Top bar, bottom nav and drawer (D4).
- All local WebView pages (notes, quiz, flashcards, tutor, math): the page is transparent and the glass script assumes light text on a dark page, so in day the text can be unreadable (D8).
- A value read once at class load or in a `remember { }` without keys will not follow the switch until the app is restarted.
- THEREFORE: do not ship day to students yet. For a Play Store build set `DAY_MODE_OFFERED = false` in `AppBackground.kt` (plan section 7).

## What the owner should test (phone)
1. Night first: Settings > Background shows a moon and a sun, Dark is selected, the whole app looks exactly as before.
2. Tap the sun: the page turns ivory with soft sky and champagne glows, the status bar and navigation bar turn ivory with dark icons. Screens not yet swept (see above) may look wrong: send screenshots, they decide D2 to D8.
3. Tap the moon: back to the exact night look. Close and reopen the app in each mode: the choice is kept.
4. A phone that had BG 2 saved: it opens in Dark.

## Not verified
Nothing was compiled or run. Material 3 icon `Icons.Filled.LightMode` and the `outlineVariant` / surface-container roles are expected to exist (icons-extended is a dependency, and the container roles were already used for night), but the first CI run is the proof.

## Next
D2: glass and surface tokens for day (`WisdomGlass.kt`, card gradient use, field surface, message card), shared cards, overlays and pop-ups. The owner says "do D2".
