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
│  │  Bottom nav: Home / Learning / Packages / Account     │  │
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
4. **Top-left menu** — About, Contact us, FAQ, Privacy, Terms, My account
5. **Notification bell** opens `https://wisdom-tower-academy.live/notifications` only (package / order status list — **not** Settings)
6. Visiting `/notifications` must **not** force bottom-nav tab to Home or Account
7. **Offline PDF vault** in private app storage
8. **FLAG_SECURE** on
9. **Back button** uses WebView history first

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

- Main: `app/src/main/java/com/example/MainActivity.kt`
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

---

## 6. Flashcards

Live in website: `FlashcardViewer.tsx` + ui-polish `.fc-*` classes — 3D flip, distinct back colour, swipe animations.

---

## 7. Change log

| Date | Change |
|------|--------|
| 2026-09-17 | Notification bell → `/notifications` only; MainActivity shell restored; docs updated |
| 2026-09-16 | Fixed status-bar overlap; fixed top bar; hamburger menu; branding; notifications |
| 2026-09-16 | Website flashcard flip + swipe restored |
