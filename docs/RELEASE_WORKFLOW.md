# How we work: app updates and releases (READ FIRST)

Applies to every agent/session touching the in-app update or releasing a build.
Detailed slice plan: `docs/IN_APP_UPDATE_PLAN.md`.

## Two repos, two jobs

| Repo | Visibility | Holds |
|---|---|---|
| `hiyabteklu/Wisdom-tower-academy-app` (this repo) | source code (make private when ready) | Kotlin app source, CI, docs. NEVER used as a download source by the app. |
| `hiyabteklu/wisdom-tower-releases` | PUBLIC | Only `version.json` and the APK files attached to GitHub Releases. No source, no secrets. |

Why: the app must download the APK without a token. A token inside the APK would leak, so downloads come from a public releases-only repo. Supabase free tier is NOT used for APK hosting (bandwidth limit).

## Endpoints the app uses

- Version check: `https://raw.githubusercontent.com/hiyabteklu/wisdom-tower-releases/main/version.json`
- APK: the `apkUrl` inside `version.json`, which points to a Release asset in the releases repo:
  `https://github.com/hiyabteklu/wisdom-tower-releases/releases/download/v<versionName>/<file>.apk`
- Fallback button: Telegram link from `telegramUrl` in `version.json`.

## version.json format

```json
{
  "versionCode": 12,
  "versionName": "1.1.2",
  "force": true,
  "minSupportedVersionCode": 11,
  "notes": ["Plain words, one line each"],
  "apkUrl": "https://github.com/hiyabteklu/wisdom-tower-releases/releases/download/v1.1.2/<file>.apk",
  "sizeBytes": 12600000,
  "sha256": "<sha256 of the APK>",
  "telegramUrl": "https://t.me/..."
}
```

Rules: app compares `versionCode` with its own. `force: true` or own versionCode < `minSupportedVersionCode` means blocking update screen; otherwise optional.

## Release steps (owner does these on phone/GitHub)

1. Bump versionCode/versionName in the app, let CI build the signed APK.
2. In `wisdom-tower-releases`: Releases, Draft new release, tag `v<versionName>`, attach the APK, publish.
3. Compute/get the APK sha256 and size.
4. Edit `version.json` LAST (so no client sees a version whose APK isn't uploaded yet).
5. Install-test on a device with the previous version.

Never publish `version.json` before the APK asset exists.

## App-side rules (what the code must do)

- Update check is silent: no "Checking for updates" UI. Runs during the splash, waits up to about 1.5 s, fails open when offline or on error.
- Download states: Idle, Downloading, Downloaded, Installing, Failed. Button disabled while busy. Auto-open the installer when the download reaches 100%. Reuse a cached APK if present. Verify sha256 before install.
- Update UI follows the app's glass design in Night/Day/Transparent. One filled pill for "Update now", quiet text link for Telegram.
- Expected, cannot be removed: "Install unknown apps" permission (once per device), Play Protect scan prompt, app restart after install.

## Agent working rules

- The owner is on a free tier with tight usage limits. Do NOT scan the whole repo. Open only the update-related files (update manager, update screen, splash, version check, release config).
- Push plans and code early in small slices, one commit each. Answer what was asked, no extras.
- Order of work: Slice 1 state machine, Slice 2 silent check + splash, Slice 3 update UI, Slice 4 point the app at the releases repo URLs above.
