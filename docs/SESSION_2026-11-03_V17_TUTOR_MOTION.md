# Session 2026-11-03 V17 — Tutor screen motion (Claude, chat)

**State:** Code pushed to main; CI was green before this commit (owner report after V16), this commit is not yet built. Phone test pending.

## Changes (`ui/study/NativeTutorScreen.kt` only)
- V6: chat rows in the "Study Chats" side panel use `animateItem()` (Compose foundation 1.7, BOM 2024.09), so adding, deleting or reordering a chat slides the rows instead of jumping.
- V7: the menu bar icons (Back, New, Clear), the Chats toggle, Send, Stop, the panel's Close, "+ Start New Chat" and each chat row press in with the shared tick. The press modifier is placed first in each chain so the whole control scales.
- Not changed on purpose: the small delete icon in a chat row and "Clear All Past Chats" (still plain clickable), and all chat logic, streaming and the WebView page.

## Phone test
Open the AI Tutor: tap the menu icons and Send, they squeeze slightly. Open Chats, add a new chat and delete one: the rows should glide.

## Not verified
Compile of this commit and phone behaviour.
