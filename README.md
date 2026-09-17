# Wisdom Tower Academy — Android App

Production Android shell for [Wisdom Tower Academy](https://wisdom-tower-academy.live).

**Read [ARCHITECTURE.md](ARCHITECTURE.md) first.**

## What this app is

- Native Compose chrome + full-screen WebView of the live site
- Fixed top bar: menu · "Wisdom Tower Academy" · notifications
- Notification bell opens **`/notifications` only** (not Settings / Account)
- Clean status-bar area (no overlap with clock/battery)
- Bottom nav: Home / Learning / Packages / Account
- Offline PDF vault + FLAG_SECURE
- Built by GitHub Actions → debug APK artifact

## Build locally

```bash
./gradlew assembleDebug
```

## Build APK via GitHub Actions

1. Push to `main` **or** run **Actions → Build Debug APK → Run workflow**.
2. Open the successful run → **Artifacts** → download **Wisdom-Tower-Academy-debug**.
3. Unzip and install the APK on a device.

Recent successful runs:  
https://github.com/hiyabteklu/Wisdom-tower-academy-app/actions/workflows/build-apk.yml

Website content updates appear in the app automatically. Only native chrome / offline vault changes need a new APK.
