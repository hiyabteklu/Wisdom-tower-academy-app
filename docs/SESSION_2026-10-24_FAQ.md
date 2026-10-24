# Session 2026-10-24: native FAQ

Owner order: replace the "Coming soon" FAQ in the drawer with the owner's own 10-question FAQ, native, cool design.

## What changed
- `app/src/main/java/com/wisdomtower/academy/ui/drawer/DrawerScreens.kt` only. `FaqScreen(onBack, modifier)` keeps its name and signature, so `MainActivity.kt` is untouched.
- Section 3 of that file: the "Coming soon" card is gone. New: hero card, then 10 tap-to-open question cards (`FAQ_ENTRIES`, `FaqCard`, `FaqPartView`). One card open at a time, the first starts open (`rememberSaveable`). Chevron turns with `animateFloatAsState`, the answer slides with `AnimatedVisibility`. Cards reuse `WisdomGlassCard` and `wisdomReveal`, so they follow day / night.
- The last answer shows two tappable rows (email, phone) through the existing `ContactChannelRow`.
- The FAQ text is the owner's own text from chat, not from the website. A comment at the top of the file records this as an owner decision (the old rule "no invented text" had the website FAQ as its source, and the website FAQ is still "Coming soon").

## Not verified
- Not compiled by the agent. CI (Build Production AAB & APK) is the first compile. If it fails, read the error, fix it, and add an entry to `docs/BUILD_PITFALLS.md`.
- Imports added to DrawerScreens.kt: AnimatedVisibility, expandVertically, shrinkVertically, fadeIn, fadeOut, animateFloatAsState, Icons.Filled.Info, Icons.Filled.KeyboardArrowDown, rememberSaveable, rotate, AnnotatedString.

## Owner check on device
1. Drawer > FAQ opens with card 01 open.
2. Tap another question: the first closes, the new one opens, the chevron turns.
3. Question 10: tap the email and phone rows (opens mail app and dialer).
4. Day mode and night mode both readable.
5. Back from FAQ returns to where the drawer was opened.

## STATUS.md
Not edited (the file is ~39 KB and this agent could not patch it safely). Next session: add one line under the log, "2026-10-24 native FAQ in DrawerScreens.kt, see docs/SESSION_2026-10-24_FAQ.md".
