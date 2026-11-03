# Session 2026-11-03 V15 — Tab change: true cross-dissolve (Claude, chat)

**State:** Code pushed to main; compile and phone verification pending. Fixes the hypothesis 1 of `SESSION_2026-11-03_V13_AUDIT.md`.

## Cause (from code read)
In `WisdomTabLayer.kt` the incoming tab (zIndex 11) had the opaque `appBackdrop()` on its OUTER box. Only its content faded, so the outgoing tab was hidden at the first frame and the screen dipped to the background before the new tab appeared.

## Change
Only `WisdomTabLayer.kt`. The opaque backdrop is now on the OUTGOING (lower) layer only. The incoming layer puts its backdrop inside its own fading box (before the recede modifier, so the backdrop does not move). The outgoing tab therefore stays visible while the new one fades in. Outgoing and incoming swap roles in the same frame, so the backdrop never changes visually.

## Risk to watch (hypothesis)
When the Learning tab comes back from an open hub page (`active` false to true) there is no outgoing layer; the tab now fades in over whatever is underneath instead of over a solid backdrop. If a flash of the hub page shows there, revert the `.then(...)` lines to put `appBackdrop()` on the outer box when no other tab is leaving.

## Phone test
Tap the five tabs fast and slow: the old tab should dissolve into the new one with no dark dip. Open and close a hub in Learning and watch the return to the Learning tab.

## Not verified
No Gradle/CI run and no phone test.
