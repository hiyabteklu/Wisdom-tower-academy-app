# SESSION 2026-10-19 — D7i: Database status, Inquiries, Push broadcasts in day mode

> Repo is public: no secrets. Owner order: "continue the open tasks around d7i". One slice per chat. Written by Claude (chat). NOT compiled by the agent, NOT seen on a phone. Admin console is owner-only: students never see it.

## Files read (`app/src/main/java/com/wisdomtower/academy/ui/admin/`)
- `DatabaseStatusScreen.kt`: every colour is a theme token (`WisdomCard`, `WisdomBorderWhite`, `WisdomTextPrimary`, `WisdomMuted`, `WisdomCyan`, `WisdomAccentEmerald`, `WisdomAccentAmber`, `AdminRose`). No literal colour. No change.
- `InquiriesScreen.kt`: tokens only. It has its own private `StatusButton`, `FilterChip`, `MessageCard`, and the shared `AdminStatusBadge` and `AdminToast`; none goes through `adminTone`, and the tokens they use already have the darker day value. `AdminToast` sits on `WisdomNavy` (solid ivory by day). No change.
- `BroadcastsScreen.kt`: tokens, plus ONE literal: `BroadcastOrange` (FB923C, the "contest" history badge). It is only passed to `AdminToneBadge`, which darkens a literal in day, so it reads. Left as it is (night must stay identical; FB923C differs from the token F97316). No change.

## The one code change: `AdminKit.kt` (commit `3f21495`)
FINDING (code read): D7g made `adminTone` mix EVERY tone 45 percent toward the navy ink in day. It assumed callers pass bright literal colours. The three screens above pass theme TOKENS (`WisdomAccentAmber`, `WisdomAccentEmerald`, `WisdomCyan`, `AdminRose`), and those already have a darker day value (amber 92400E, emerald 065F46, cyan 0B7285, rose BE123C). So in day they were darkened twice: by my hand calculation amber became about 552F1F (brown) and emerald about 08403D, and the badges and buttons lose their colour difference. Hypothesis until seen on a phone.
FIX: new `isDayToken(tone)` (compares with `WisdomCyan`, `WisdomMuted` and the nine `WisdomAccent*` tokens and `AdminRose`). `adminTone` now returns the tone unchanged when night OR when the tone is a theme token; only a literal colour is still mixed toward the ink (the default chip tone F59E0B, `BroadcastOrange`). Night: `adminTone` already returned the tone, so night is identical. Nine new imports of existing tokens (all exist in `Color.kt`), no new dependency.
Effect on the other 15 admin screens: any that pass a token get the plain day token now (the intended D1 colour); any that pass a literal behave as in D7g.

## Not verified
- Not compiled. Possible compile point: `Color` equality (`==`) between a getter value and the parameter; this is ordinary value equality, should be fine.
- I did NOT read the other 15 admin screens, so I do not know how many pass literals versus tokens.
- Not opened on a phone. The 45 percent mix and the day colours are still hand estimates for D9.
- `docs/DAY_MODE_PLAN.md` board was NOT updated in this commit: D7g, D7h and D7i are logged only in their session files (the board's D7 row still says "D7e onward open"). Resend of that whole file was avoided to not risk a transcription error.

## Owner: what to test (admin account only, Day then Night)
Admin console > Database status, Inquiries (open one card, tap the status buttons), Push notifications (Scheduled and History tabs, open the cancel and delete dialogs): amber, green, red and teal badges and buttons are clearly different colours on ivory, and the text is readable. Night: exactly as before.

## Next (D7 proposal, unchanged)
D7j: `AccessGrantsScreen`, `LocksScreen`, `PaymentsScreen`. D7k: `CatalogScreen`, `CourseContentScreen`, `OverviewScreen`. D7l: `UsersScreen`, `CompetitionsScreen`. D7m: `FreeResourcesScreen`, `BroadcastComposerScreen` (check first whether it is used: the host still opens the website composer).
