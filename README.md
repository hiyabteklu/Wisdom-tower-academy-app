# Wisdom Tower Academy — Android App

Production Android shell for [Wisdom Tower Academy](https://wisdom-tower-academy.live).

**Read [ARCHITECTURE.md](ARCHITECTURE.md) first.**

## What this app is

- Native Compose chrome + full-screen WebView
- Fixed top bar: menu · "Wisdom Tower Academy" · notifications
- Clean status-bar area (no overlap with clock/battery)
- Bottom nav: Home / Learning / Packages / Account
- Offline PDF vault + FLAG_SECURE
- Built by GitHub Actions → debug APK artifact

## Build

```bash
./gradlew assembleDebug
```

Website content updates appear automatically. Only native chrome changes need a new APK.
