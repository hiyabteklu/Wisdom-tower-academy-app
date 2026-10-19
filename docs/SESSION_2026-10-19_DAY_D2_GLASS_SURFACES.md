# SESSION 2026-10-19 - DAY MODE D2 (glass and surface tokens)

Owner request (chat): "Continue", after D1. Next open slice in `docs/DAY_MODE_PLAN.md` is D2.

## What was done (written and pushed to `main`; NOT compiled by the agent, CI decides)
1. `ui/theme/WisdomGlass.kt`: the day glass is no longer see-through white. Panel / row fill = frosted ivory F8F5EE at 82 percent, pill / button fill = 90 percent, quiet (done) fill = 65 percent, edge = deep navy 12 percent. New helper `Modifier.wisdomCardShadow(shape)`: a soft navy shadow (3 dp) in DAY ONLY, returns the modifier unchanged at night. `WisdomGlassCard` uses it.
2. `ui/theme/WisdomComponents.kt`: `WisdomModernCard` (the shared card, both the plain and the tappable version) uses the same shadow helper. Nothing else changed in the file (buttons already use the tokens).
3. `ui/theme/WisdomMessageCard.kt`: day error card is a solid pale rose (FBE9EA at 95 percent) instead of the 13 percent red tint; the card gets the day shadow.
4. `ui/theme/WisdomFieldTokens.kt`: day field surface is solid ivory FBF9F4 (was a cool white F8FAFC).

Night: every night value is untouched; the shadow helper returns `this` at night.

## Scope note
The pop-ups, menus, sheets and dialogs that use Material defaults already got their ivory 92 percent fill in D1 (`Theme.kt`). Custom pop-up surfaces and cards drawn inside a screen with fixed colours are swept per screen in D3 to D7, because they live in those files. I could not find them with the code search tool (it returned nothing for exact colour searches), so D2 does NOT claim every shared pop-up is covered.

## Hypotheses / not verified
- The 3 dp shadow on translucent frosted cards: expected to be drawn only outside the card (platform rule), but not seen on a device. If it looks heavy or dirty, lower the elevation or the two alpha values in `wisdomCardShadow` (one place).
- Many shadows in long lists could cost scroll smoothness on weak phones. Owner to watch Learning and Packages lists.
- Ivory glass values are the plan's starting point; the owner judges on the phone.

## What the owner should test
1. Night: Settings, a hub list and a message card (for example signed out Learning) look exactly as before.
2. Day: cards are ivory and clearly visible on the page, with a thin edge and a soft shadow; the Settings fields are light ivory with dark text; a failed-load message card is pale rose with dark rose text.
3. Scroll a long list in day for smoothness.

## Next
D3: Home, Learning, Packages, Package detail, Settings, My Subjects, GPA card, Study workspace. The owner says "do D3".
