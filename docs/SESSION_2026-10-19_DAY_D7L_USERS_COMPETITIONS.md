# SESSION 2026-10-19 — D7l: Users and Competitions (admin) checked, nothing to change

> Repo is public: no secrets. Owner order: "Continue". One slice per chat. Written by Claude (chat). No code changed in this slice. Admin console is owner-only.

## Files read (`app/src/main/java/com/wisdomtower/academy/ui/admin/`)
- `UsersScreen.kt` (35 KB): every colour is a theme token (`WisdomCard`, `WisdomBorderWhite`, `WisdomTextPrimary`, `WisdomMuted`, `WisdomCyan`, `WisdomAccentAmber`, `WisdomAccentEmerald`). No `Color.White`, no literal. Initials avatar, student-id strip, package and score rows, the grant view and the package choices all follow the switch. The filter chips and badges use the kit, which since D7i leaves a theme token as it is in day. No change.
- `CompetitionsScreen.kt` (44 KB): tokens, plus ONE literal: `CompOrange` (FB923C), used only as the tone of the "Entries" button, which goes through `adminTone` and is darkened in day (a brown, readable). The disqualify dialog is a plain `AlertDialog` on `WisdomNavy` (solid ivory by day) with `AdminRose` and `WisdomMuted` buttons. The two switches ("package required", "show now") use the default Material 3 `Switch`, which takes its colours from the Material scheme set in `Theme.kt` in D1 (light scheme by day). I did not open `Theme.kt` again to check the switch colours. No change.

## Not verified
- No code, so nothing to compile. Not opened on a phone.
- The Material 3 switch in day (track and thumb colours) is the one thing to look at on a phone: it depends on the D1 light scheme.

## Owner: what to test (admin account, Day then Night)
Admin console > Users: card, student-id strip, History and Scores, Grant Package. Competitions: open "New competition", look at the two switches (on and off must be clearly different on ivory), the status badges, the Entries and Disqualify buttons and the dialog.

## Next
D7m: `FreeResourcesScreen` (42 KB) and `BroadcastComposerScreen` (49 KB; check first whether anything opens it: the host still opens the website composer). Then the D7 row is complete (except the Android system splash) and D9 (owner screenshots in both modes) is the gate. Optional tiny slice: `AccessGrantsScreen` and `CatalogScreen` `RoseTone` to `AdminRose`.
