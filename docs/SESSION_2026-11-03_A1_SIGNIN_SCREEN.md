# SESSION 2026-11-03 A1: Sign in / Create Account screen scales to the phone, pill switch, richer look

Plan: `docs/READER_EXAM_FLASHCARD_FIX_PLAN.md` (slice A1, owner problem 10). Read first: `NativeSignInScreen.kt`, `SESSION_2026-10-17_S5_ONE_SIGNIN_LOOK.md`, `SESSION_2026-10-17_S8_WIDTH_CAP.md`.

## What the owner saw
Fine on his phone (a mini / desktop-like display size), too big and confusing on other phones. The Sign in / Create Account switch is a rounded square, it must be a pill. Wanted: a richer look and the brand above the form.

## Cause (read from the code)
Every size on the screen was a fixed dp value (logo 64 dp, title 26 sp, card paddings 24/28 dp, 16 dp gaps, tab corners 16/13 dp). On a phone with a narrower dp width the same fixed sizes fill much more of the screen. The switch track used 16 dp corners and the selected part 13 dp, on a bar about 42 dp high, so it was a rounded rectangle, not a pill.

## What changed (1 code file: `ui/auth/NativeSignInScreen.kt`)
- ONE size factor `fit`, from the room the screen gets (`BoxWithConstraints`): the smaller of width / 400 dp and height / 780 dp, kept between 0.80 and 1.0. Roomy screen = exactly the old look (1.0). Narrow or short screen = logo, title, gaps, card paddings and the main button height shrink up to 20 percent. Text stays in sp (the system font size still works), the text fields keep Material's own height, the screen still scrolls, so a large font size / display size never hides a field.
- Switch: track and sliding selected part are `CircleShape` (full pill). The selected part is cyan tinted with a cyan edge. Each tab is at least 44 dp high; the main button at least 48 dp.
- Look: a soft cyan glow at the top of the screen, a stronger glow behind the logo, a faint cyan tint at the top of the card in the night look. Logo + app name stay above the card.
- Not changed: texts, fields, validation, sign in / sign up logic, the 448 dp card cap (S8), the one-sign-in-look (S5), the day look colours, TalkBack roles.

## Unsure (not run, read from the code)
- Not compiled, not tested on a device. `BoxWithConstraints` now wraps the whole screen; the close button inside still uses `align(TopEnd)` which is valid there.
- The 400 x 780 dp reference is a guess. If it still looks big on a small phone, lower the two numbers; if it looks small on a normal phone, raise them (one line: `val fit = ...`).
- Day look not checked on a device: the cyan glow is light (14 percent) on the ivory background.
- No endless animation was added on purpose (the app has a Remove animations setting and this screen does not read it); motion is the existing entrance, the sliding switch and the button press.

## Owner test on the phone (new build)
1. Sign in screen: the switch looks like a pill, the cyan part slides smoothly to Create Account and back.
2. Look at it on your phone and on a friend's phone (or change Settings > Display size to Small and Large): same proportions, nothing cut off.
3. Set the system font size to large: everything still readable, the page scrolls.
4. Create Account: all fields open, the button is still reachable; sign in still works.
5. Day and night looks: the card and the glow are readable in both.

## Next slice
P1 (fast when online). Its first commit writes the sub-slices P1a, P1b... in the plan before any code.
