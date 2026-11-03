# Session 2026-11-03 V18 — Current board for IOS_MOTION_V2_PLAN (Claude, chat)

Append-only correction of the status table in `docs/IOS_MOTION_V2_PLAN.md` (that table is older than V13 to V17).

## Build
- CI green after V16 (owner report). V17 (tutor screen) not yet built when this was written.

## Board
| Slice | State |
|---|---|
| V1 | code pushed; phone test pending |
| V2 | code pushed; phone test pending |
| V3 | build fixed in V16 (removed the nonexistent `ExperimentalActivityApi`). Finger-tracking on Android 13 to 15 still needs `android:enableOnBackInvokedCallback="true"` in the manifest: OWNER DECISION PENDING |
| V4 | code pushed; V15 made the dissolve a real overlap; phone test pending, watch the Learning hub return |
| V5 | not started (needs `MainActivity.kt` patch script) |
| V6 | partial: Home, Settings, Account, Game Hub, Flashcards, Tutor, Competitions, Notifications, Status, Cover, Planner, drawer pages have the return entrance; notification list and tutor chats list animate rows (V17) |
| V7 | partial: Settings, Home sign-up, Game Hub, Account, Notifications, drawer links, package card Open, Learning screen (V14), Tutor controls (V17). Open: top bar icons and bottom tabs (inside `MainActivity.kt`), switches and checkboxes, hub list small pills (`ToolPill`, `ScreenBackButton`) |
| V8 | partial: dialogs already use a spring (`wisdomDialogEnter`), Status sheet springs up. Open: drag-to-dismiss and the tools overlay host (in `MainActivity.kt`) |
| V9 | partial: shared `#root` entrance in the pages. Open: keep the native cover until first paint (WebView host in `MainActivity.kt`) |
| V10 | not started, optional |
| V11 | done in code (reduced-motion suppression) |
| V12 | final sweep after the above |

## Read, no change needed (hypothesis, from code read)
`HubItemListScreen.kt` rows are `WisdomGlassCard(onClick = ...)`, which the plan says already presses in; not re-checked on a phone.

## Why the remaining work stops here
Everything still open sits in `MainActivity.kt` (about 245 KB). By HANDOFF rule 10 it is changed only through a patch script in `scripts/` that the owner runs from GitHub > Actions > Apply pending patches, after reading the live site first. That should be done one slice per chat.
