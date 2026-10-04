# Wisdom Tower Academy — Architecture (Website + Android App)

> **Read this first.** Single source of truth for how the website and the Android app work together.
>
> **Last updated:** 2026-09-17

---

## 1. Two repositories

| Repo | Purpose | Live URL / Role |
|------|---------|-----------------|
| [hiyabteklu/Wisdom-tower-academy](https://github.com/hiyabteklu/Wisdom-tower-academy) | **Website** (source of truth for content, auth, packages, progress, flashcards, PDFs, free resources) | https://wisdom-tower-academy.live |
| [hiyabteklu/Wisdom-tower-academy-app](https://github.com/hiyabteklu/Wisdom-tower-academy-app) | **Android app** — native shell that loads the website | Built via GitHub Actions → debug APK artifact |

There is **no separate backend** for the app. The app is a secure, offline-capable window around the live website.

---

## 2. How the app works (current reality — Sep 2026)

```
┌─────────────────────────────────────────────────────────────┐
│                 Android App (this repo)                     │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  System status bar (clock / battery / network)        │  │
│  │  ── solid navy background, icons light ────────────── │  │
│  ├───────────────────────────────────────────────────────┴  │
│  │  FIXED native top bar (never scrolls)                 │  │
│  │  [☰ Menu]  Logo + "Wisdom Tower Academy"  [🔔 Notif]  │  │
│  ├───────────────────────────────────────────────────────┴  │
│  │                                                       │  │
│  │              WebView (full content area)              │  │
│  │         loads https://wisdom-tower-academy.live       │  │
│  │                                                       │  │
│  ├───────────────────────────────────────────────────────┴  │
│  │  Bottom nav: Home / Learning / Packages / Account / Settings │
│  └───────────────────────────────────────────────────────┘  │
│                                                             │
│  + OfflineVault (private app storage for PDFs)              │
│  + FLAG_SECURE (blocks screenshots / screen recording)      │
└─────────────────────────────────────────────────────────────┘
```

### Key behaviours

1. **Website is the source of truth**
2. **Native chrome owns the header + bottom nav** — fixed; website header/footer hidden via injected CSS
3. **Status bar is clean** — solid navy behind system icons; content padded with WindowInsets.statusBars
4. **Top-left menu** — Compact native Card with About, Contact us, FAQ, Privacy, Terms
5. **Notification bell** opens `https://wisdom-tower-academy.live/notifications` only (package / order status list — **not** Settings)
6. Visiting `/notifications` must **not** force bottom-nav tab to Home or Account
7. **5-tab bottom navigation** — Home, Learning, Packages, Account, Settings
8. **Offline PDF vault** in private app storage
9. **FLAG_SECURE** on
10. **Back button** uses WebView history first
11. **Offline cache** — Static pages, assets, and PDFs remain vaulted; dynamic list APIs are not persisted

---

## 3. What is *not* the current architecture

Pure native Jetpack Compose rewrite is future only. This WebView shell is production.

---

## 4. Developer quick start

```bash
git clone https://github.com/hiyabteklu/Wisdom-tower-academy-app.git
cd Wisdom-tower-academy-app
./gradlew assembleDebug
```

- Main: `app/src/main/java/com/wisdomtower/academy/MainActivity.kt`
- Offline: `OfflineVault.kt`
- CI: `.github/workflows/build-apk.yml` uploads **Wisdom-Tower-Academy-debug** artifact

### APK from CI

Actions → **Build Debug APK** → open a green run → Artifacts → **Wisdom-Tower-Academy-debug**

---

## 5. UI rules (do not regress)

| Rule | Why |
|------|-----|
| Status-bar area solid navy | Clean mobile look |
| Native top bar fixed | Must never disappear on scroll |
| Hamburger menu top-left | About / Contact / FAQ / etc. |
| Branding "Wisdom Tower Academy" | Correct name |
| Notification icon → `/notifications` only | Not Settings / Account |
| Bottom nav unaffected by `/notifications` | Keep current tab |
| Unified chrome palette | Status & top/bottom bars use deep navy `#060B15`, accent `#22E0FF`, card surface `#0C1424`, perfectly matching website surfaces and pill button language |

---

## 6. Flashcards

Live in website: `FlashcardViewer.tsx` + ui-polish `.fc-*` classes — 3D flip, distinct back colour, swipe animations.

---

## 7. Study Timer (Pomodoro Focus Timer)

- **Website is the primary control:** The web Focus timer (`PomodoroTimer.tsx` + `focus-timer` library) is the single source of truth and full control interface (presets, start/pause/reset, sound/nudges).
- **No separate floating player in content area:** The app must NEVER show a secondary permanent or floating music-player style widget over the WebView.
- **Top Bar Indicator:** While the timer is actively running (`isRunning && timerRemainingSeconds > 0`), the native top bar displays a compact, non-intrusive status pill cleanly positioned directly below the notification bell.
- **Reliable Stop & Clean Dismiss:** The top bar pill includes an inline close `(X)` button that immediately stops the timer in both the web layer (`wt-focus-timer` / `localStorage`) and native state, hiding the pill completely.
- **Process Death & Restart:** On app launch or after process death, if the timer is not actively running, no timer UI is shown.

---

## 8. Change log

| Date | Change |
|------|--------|
| 2026-09-29 | Aggressive caching, lifecycle recovery & navigation fix: (1) Upgraded WebCacheVault to 6-thread concurrent worker pool, expanded CORE_HUBS with Grade 12 & Freshman hubs, enabled parallel pre-caching of all srcset and Next.js thumbnails on launch so internet bandwidth actively caches the site, and served all cached resources from disk online for 0ms loads. (2) Added onRenderProcessGone recovery in WebViewClient to prevent white/dead frozen state when switching back from heavy apps like TikTok. (3) Added lifecycle onResume/onPause webView timers handling and unconditional loadUrl in navigateTo so bottom navigation tabs reliably transition. (4) Scoped header hiding strictly to fixed site top bar and removed dragstart touch interceptors so hub cards open instantly. |
| 2026-09-29 | Fixed Study Timer: removed duplicate floating music-player style widget from WebView content area. Added compact status pill in the native top bar cleanly under the notification bell that only displays while actively running. Added reliable inline (X) stop control that updates web localStorage + events, and ensured timer indicator never awkwardly persists or reappears after app restart when stopped. |
| 2026-09-27 | Streamlined futuristic loader: purely circular design with a subtle translucent circular backing matching the spinner dimensions (removed card/box and text clutter), debounced auto-duration display that never triggers on text boxes, email/password inputs, flashcards, or fast-loaded pages, and immediately dismisses the instant content is ready |
| 2026-09-27 | Redesigned hamburger menu into a compact native Card Dialog (removed duplicated/bloated items: Settings, Account, Logout, Telegram; kept 5 essential links: About, Contact us, FAQ, Privacy, Terms), added Settings as 5th tab on bottom navigation (Home — Learning — Packages — Account — Settings), added anti-copy DOM protection, and clean in-place download progress update |
| 2026-09-26 | Fixed PDF size check & silent download: strictly prohibited probes from falling through to full GETs (unresolved probes return fabricated response without Content-Length), ensured vault writes occur ONLY on explicit user download actions, eliminated 'Ready' fallback in favor of 'Size unknown', and added single-flight cached UI state ('Already downloaded — opening…') |
| 2026-09-25 | Replaced bottom navigation with native Apple-quality tab bar (removed tacky gradients, capsule boxes, and dots; crisp icon/typography tinting, subtle ripple and haptic feedback) and fixed tab highlight accuracy across all /academy, /packages, /account routes and on app resume after inactivity |
| 2026-09-25 | Fixed book size check and silent download: pre-seeded book catalog sizes, hooked fetch probes with empty body + CORS expose headers, prevented premature vault writes on probes, and eliminated 'Ready' fallback |
| 2026-09-21 | Pinned Refresh + Notifications in end Row; added getPdfSize to AndroidOfflineVault and enhanced BOOK_PAGE_HELPERS_JS |
| 2026-09-17 | Notification bell → `/notifications` only; MainActivity shell restored; docs updated |
| 2026-09-16 | Fixed status-bar overlap; fixed top bar; hamburger menu; branding; notifications |
| 2026-09-16 | Website flashcard flip + swipe restored |
