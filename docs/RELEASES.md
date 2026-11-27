# Release ledger: Wisdom Tower Academy (Telegram APK)

One section per release. The build commit is the exact source of that APK. To see what a release contained, open its commit or tag, never `main`, because `main` keeps moving.

Times are UTC. Ethiopia time is UTC+3.

## All releases

| Version | Build (versionCode) | Tag | Build commit | Commit time | Published |
|---|---|---|---|---|---|
| 1.1.2 | 6 | `telegram-v1.1.2-6` | `8616777cc76da47ba3eb104f4b09002531fd1d99` | 2026-11-06 12:53 | 2026-11-06 12:59 |
| 1.1.1 | 5 | `telegram-v1.1.1-5` | `db02944e535c4c6b8f859c1e6e7c410610519fee` | 2026-11-05 13:51 | 2026-11-05 14:10 |
| 1.1.0 | 4 | `telegram-v1.1.0-4` | `1fd27711dee32b6d7fd67435d6546e4f83024b34` | 2026-11-05 10:17 | 2026-11-05 12:09 |
| 1.0.0 | 3 | `telegram-v1.0.0-3` | `dc2c646e67c0ed688a351efc35839e13f05c9e49` | 2026-11-03 12:25 | 2026-11-03 12:36 |

All four are marked "pre-release" on GitHub because the build workflow creates them with `--prerelease`. The APK download works the same.

`RELEASE_NOTES.txt` in the repo root only describes 1.0.0 (build 3). Use this file for releases from now on.

---

## 1.1.2 (build 6), latest

| Field | Value |
|---|---|
| Version name | 1.1.2 |
| Version code | 6 |
| Application ID | `com.wisdomtower.academy` |
| Release tag | `telegram-v1.1.2-6` |
| Build commit | `8616777cc76da47ba3eb104f4b09002531fd1d99` (short: `8616777`) |
| Commit message | Bump version to 1.1.2 (versionCode 6) for update-flow test |
| Commit time | 2026-11-06 12:53:07 UTC (15:53 Ethiopia) |
| Release published | 2026-11-06 12:59:11 UTC (15:59 Ethiopia), by github-actions[bot] |
| GitHub release | https://github.com/hiyabteklu/Wisdom-tower-academy-app/releases/tag/telegram-v1.1.2-6 |
| APK file name | `Wisdom-tower-academy-telegram.apk` |
| APK download | https://github.com/hiyabteklu/Wisdom-tower-academy-app/releases/download/telegram-v1.1.2-6/Wisdom-tower-academy-telegram.apk |
| Built by | Workflow "Build Telegram Release APK" (`.github/workflows/build-apk.yml`), started by hand |
| Build command | `./gradlew assembleRelease -PpaidUi=true` |
| Toolchain | JDK 21, Gradle 9.3.1, compileSdk 36, targetSdk 36, minSdk 24 |
| Signing | Upload keystore from repository secrets (the workflow stops if the secrets are missing) |
| Previous release | 1.1.1 (build 5) |

### What is in this release

Text entered in the in-app App Releases form:

- Advanced progress tracker
- AI tutor inside Notes
- Smarter Learning Experience
- In-App Updater
- Smoother Animations
- Live Games for ECE

### In-app updater record

Values in the App Releases form when this release was saved:

- Version code: 6
- Version name: 1.1.2
- Notes: the list above
- Download link: must point at the tag `telegram-v1.1.2-6` (link in the table above)
- Two more fields show `null` and `5`. The labels were not visible on the screen. UNSURE: what these two fields are.
- Publish this release: on. The form says a published newer version blocks the older app.

The download link visible in the form contains `/download/v1.1.2/`. No release with the tag `v1.1.2` exists. The real tag is `telegram-v1.1.2-6`. Check that the saved link uses the real tag, or the updater will get a 404.

### Not recorded yet

- APK file size
- APK SHA-256. To get it: `sha256sum Wisdom-tower-academy-telegram.apk`

### Note on this ledger

This file was added after the release, so `main` is already ahead of `8616777`. The release was built from `8616777`, which is the commit in the table.

---

## 1.1.1 (build 5)

- Tag: `telegram-v1.1.1-5`
- Build commit: `db02944e535c4c6b8f859c1e6e7c410610519fee`
- Commit message: release: version 5 (1.1.1) so the in-app update can be tested from version 4
- Commit time: 2026-11-05 13:51:38 UTC
- Published: 2026-11-05 14:10:58 UTC
- https://github.com/hiyabteklu/Wisdom-tower-academy-app/releases/tag/telegram-v1.1.1-5

## 1.1.0 (build 4)

- Tag: `telegram-v1.1.0-4`
- Build commit: `1fd27711dee32b6d7fd67435d6546e4f83024b34`
- Commit message: docs(content): describe online cache revalidation and offline fallback
- Commit time: 2026-11-05 10:17:00 UTC
- Published: 2026-11-05 12:09:22 UTC
- https://github.com/hiyabteklu/Wisdom-tower-academy-app/releases/tag/telegram-v1.1.0-4

## 1.0.0 (build 3)

- Tag: `telegram-v1.0.0-3`
- Build commit: `dc2c646e67c0ed688a351efc35839e13f05c9e49`
- Commit message: Document Telegram release tag fix
- Commit time: 2026-11-03 12:25:26 UTC
- Published: 2026-11-03 12:36:25 UTC
- https://github.com/hiyabteklu/Wisdom-tower-academy-app/releases/tag/telegram-v1.0.0-3

---

## Recording the next release

1. In `app/build.gradle.kts`, raise `versionCode` by 1 and set `versionName`. Right now they are `6` and `"1.1.2"`.
2. Commit to `main`. Write down the full commit SHA.
3. Run the workflow "Build Telegram Release APK". It creates the tag `telegram-v{versionName}-{versionCode}` and the GitHub release.
4. In the app's App Releases form, use the same version code and version name. The download link must contain the real tag from step 3.
5. Add a row to the table at the top and a section like the 1.1.2 one in this file.
6. Post to Telegram with the same version, build, date, commit and tag.
