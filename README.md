# Wisdom Tower Academy — Android App

Production Android shell for [Wisdom Tower Academy](https://wisdom-tower-academy.live).

**Read [ARCHITECTURE.md](ARCHITECTURE.md) first.** It explains exactly how the website and this app work together.

## What this app is

- Native Android UI (Jetpack Compose) + full-screen WebView
- Loads the live website: https://wisdom-tower-academy.live
- Bottom navigation (Home / Learning / Packages / Account)
- Top header with logo + notifications
- Offline PDF vault (private app storage) for books the user has already opened
- Hardware back button uses WebView history first
- `FLAG_SECURE` enabled (blocks screenshots / screen recording of paid content)
- Built automatically by GitHub Actions → download the debug APK from the Actions artifact

## Build

```bash
./gradlew assembleDebug
```

Or just push to `main` — the workflow uploads `Wisdom-Tower-Academy-debug.apk`.

## Important

The website is the source of truth for all content, ownership, progress, flashcards, questions, etc.  
This app never invents curriculum. Offline only re-plays what the user already had legitimate access to while online.
