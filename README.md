# Wisdom Tower Academy — Android App

> **AI agents: read [AGENTS.md](AGENTS.md) first.**

Android app for [Wisdom Tower Academy](https://www.wisdom-tower-academy.live): a premium, native-feeling Compose app using the same Supabase / Appwrite backend as the website.

Then read [docs/NATIVE_APP_BRIEF.md](docs/NATIVE_APP_BRIEF.md) and [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) only if your task needs them.

## What this app is

- Native Compose screens (Home, Packages, Learning, Account, Settings, Guides) with native learning hubs (books, notes, flashcards, question banks, exams); a WebView shell is kept as fallback (see `docs/NATIVE_CONTENT_PLAN.md`)
- Fixed top bar: menu · "Wisdom Tower Academy" · notifications
- Notification bell opens the notifications page (not Settings / Account)
- Clean status-bar area (no overlap with clock/battery)
- Bottom tabs as currently installed and approved by the owner
- Offline PDF vault + FLAG_SECURE
- Built by GitHub Actions
- Current phase: UI polishing (planned, not implemented yet): see `STATUS.md` section B1

## Build locally

```bash
./gradlew assembleDebug
```

## Build via GitHub Actions

Push to `main` **or** run the workflow from the **Actions** tab (see `.github/workflows/`), open the successful run, and download the artifact.

Website content updates appear in the app automatically. Only native chrome / offline vault changes need a new APK.
