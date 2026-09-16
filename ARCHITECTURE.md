# Wisdom Tower Academy — Architecture (Website + Android App)

> **Read this first.** This document is the single source of truth for how the website and the Android app work together. Any AI / MCP / developer should start here.

---

## 1. Two repositories

| Repo | Purpose | Live URL / Role |
|------|---------|-----------------|
| [hiyabteklu/Wisdom-tower-academy](https://github.com/hiyabteklu/Wisdom-tower-academy) | **Website** (source of truth for all content, auth, packages, progress, flashcards, PDFs, questions, etc.) | https://wisdom-tower-academy.live |
| [hiyabteklu/Wisdom-tower-academy-app](https://github.com/hiyabteklu/Wisdom-tower-academy-app) | **Android app** — native shell that loads the website | Built via GitHub Actions → debug APK artifact |

There is **no separate backend** for the app. The app is a secure, offline-capable window around the live website.

---

## 2. How the app works (current reality — Sep 2026)

```
┌─────────────────────────────────────────────┐
│           Android App (this repo)           │
│  ┌───────────────────────────────────────┐  │
│  │  Compose UI                           │  │
│  │  • Top bar (logo + notifications)     │  │
│  │  • Bottom navigation (4 tabs)         │  │
│  │  • WebView (full content area)        │  │
│  └───────────────────────────────────────┘  │
│                      │                      │
│                      ▼                      │
│  Loads live website URLs:                   │
│  https://wisdom-tower-academy.live/...      │
│                                             │
│  + OfflineVault (private app storage)       │
│    – PDFs / books the user opens are saved  │
│    – Available when offline                 │
│    – Ownership still enforced by website    │
└─────────────────────────────────────────────┘
```

### Key behaviours

1. **Website is the source of truth**  
   All packages, ownership checks (`enrollments` / `orders`), progress, flashcards, question banks, results, AI tutor, GPA, leaderboards live on the website + Supabase. The app never invents or hard-codes curriculum.

2. **App loads the website**  
   The WebView opens the live site. Bottom tabs map to:
   - Home → `/`
   - Learning → `/learning`
   - Packages → `/packages`
   - Account → `/account`

3. **Offline & caching**  
   - Once a user visits a page **while online** (via the app), the browser cache + Service Worker (if present on the site) keep text, thumbnails, animations, flashcards, questions, progress UI available offline.
   - **PDFs / books** are additionally downloaded into the app’s private `filesDir/offline_vault` (not public Downloads). They stay available forever for that device as long as the user previously opened them while online **and** still owns the package.
   - Ownership is never bypassed: the website’s auth + RLS still decide what the user is allowed to see. Offline only re-plays content the user already had legitimate access to.

4. **Security of paid content**
   - Offline files live in the app’s **private internal storage** (not world-readable).
   - Screenshots / screen recording are blocked with `FLAG_SECURE`.
   - No easy way for a user to extract the vault and share paid PDFs without rooting + reverse-engineering.
   - Session cookies stay inside the WebView; the app does not expose service-role keys.

5. **Back button**  
   Hardware / gesture back first goes through WebView history. Only when the WebView has no history does the app finish (exit).

---

## 3. What is *not* the current architecture

- The pure native Jetpack Compose rewrite (talking only to Supabase with no WebView) is a **future direction**, not what ships today.  
  See the old notes in the website repo under `docs/NATIVE-ANDROID.md` for the long-term vision.  
  Until that lands, **this WebView shell is the production Android app**.

---

## 4. Developer / AI quick start

```bash
# Clone the app
git clone https://github.com/hiyabteklu/Wisdom-tower-academy-app.git
cd Wisdom-tower-academy-app

# Build debug APK (or use the GitHub Action artifact)
./gradlew assembleDebug
```

- Main code: `app/src/main/java/com/example/MainActivity.kt`
- Offline PDF vault: `app/src/main/java/com/example/OfflineVault.kt`
- Theme: `app/src/main/java/com/example/ui/theme/`
- CI: `.github/workflows/build-apk.yml` → uploads `Wisdom-Tower-Academy-debug.apk`

Website changes appear in the app automatically (it always loads the live URL).  
Only native shell changes (header, bottom nav, offline vault, FLAG_SECURE, etc.) require a new APK.

---

## 5. Package & ownership offline rule (critical)

> If a user has purchased a package **and** has opened the material at least once while online inside the app, they must be able to continue studying that material when the connection is gone.

The website already enforces ownership server-side. The app only caches what the website already allowed the user to see.  
Never invent a local “unlock” that bypasses Supabase `enrollments` / `orders`.

---

*Last updated: September 2026 — matches the live WebView shell in Wisdom-tower-academy-app.*
