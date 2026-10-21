# Wisdom Tower Academy — Android App

> **OWNER ORDER 2026-10-21 (AI agents, read this first): the ONLY live work is the native games, Tower Climb and Tower Defense. Read [docs/GAMES_NATIVE_PLAN.md](docs/GAMES_NATIVE_PLAN.md) and start at Phase G0. Every other plan is PARKED: see [docs/PARKED_2026-10-21.md](docs/PARKED_2026-10-21.md).** The games plan overrides the "read HANDOFF.md first / competition is the only live work" lines below.

> **AI agents: read [HANDOFF.md](HANDOFF.md) FIRST.** It has the rules, the goal, and the document map; live status and the active roadmap are in [STATUS.md](STATUS.md). Update it before you stop.

Android app for [Wisdom Tower Academy](https://www.wisdom-tower-academy.live): a premium, native-feeling Compose app using the same Supabase / Appwrite backend as the website.

Then read [NATIVE_APP_BRIEF.md](NATIVE_APP_BRIEF.md) and [ARCHITECTURE.md](ARCHITECTURE.md).

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
