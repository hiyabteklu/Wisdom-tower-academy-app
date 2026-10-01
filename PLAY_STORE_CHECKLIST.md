# Google Play Store Production Readiness Checklist (2026)

Wisdom Tower Academy (`com.wisdomtower.academy`)

---

## 1. Keystore & GitHub Secrets Setup

Google Play requires all release builds to be signed with a dedicated, non-debug upload key.

### A. Generate Upload Keystore Locally
Run the following command on your terminal:

```bash
keytool -genkeypair -v \
  -keystore my-upload-key.jks \
  -alias upload \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storetype JKS
```

*Remember the passwords and alias you specify.*

### B. Convert Keystore to Base64
Encode the generated `.jks` file so it can be stored as a GitHub Actions secret:

```bash
# macOS
base64 -i my-upload-key.jks | tr -d '\n' > keystore_base64.txt

# Linux
base64 -w 0 my-upload-key.jks > keystore_base64.txt
```

### C. Add Secrets to GitHub Repository
Go to your GitHub repository: **Settings → Secrets and variables → Actions → New repository secret** and add:

| Secret Name | Value | Required |
|-------------|-------|----------|
| `KEYSTORE_BASE64` | The entire base64 string from `keystore_base64.txt` | Yes |
| `STORE_PASSWORD` | The password you entered for the keystore | Yes |
| `KEY_PASSWORD` | The key password (if different from store password) | Optional (defaults to `STORE_PASSWORD`) |
| `KEY_ALIAS` | Key alias (e.g. `upload`) | Optional (defaults to `upload`) |

> **Security Note:** Never commit `my-upload-key.jks` or `keystore_base64.txt` into git. Keep an offline backup in a secure vault (1Password / Bitwarden / encrypted USB). If you lose this key before Google Play App Signing is activated, you will not be able to update your app.

---

## 2. Building the Production App Bundle (AAB)

Play Store requires an Android App Bundle (`.aab`) for production publishing.

### Local Build (with local keystore configured):
```bash
export KEYSTORE_PATH="$PWD/my-upload-key.jks"
export STORE_PASSWORD="your-keystore-password"
export KEY_PASSWORD="your-key-password"
export KEY_ALIAS="upload"

./gradlew bundleRelease
```
Output location:
`app/build/outputs/bundle/release/app-release.aab`

### GitHub Actions CI:
1. Push to `main` or trigger the **Build Production AAB & APK** workflow manually under the **Actions** tab.
2. The workflow will build both the signed `.aab` and release `.apk` (with R8 code shrinking and resource optimization enabled).
3. Download `Wisdom-tower-academy-AAB` from the workflow run **Artifacts** section.

---

## 3. Google Play Console Manual Setup

### A. Create App in Play Console
1. Open [Google Play Console](https://play.google.com/console).
2. Click **Create app**:
   - **App name:** Wisdom Tower Academy
   - **Default language:** English (or Amharic if preferred)
   - **App or game:** App
   - **Free or paid:** Free
3. Accept the Developer Program Policies and US export laws.

### B. Personal Account Requirement (12-Tester Closed Test)
*Official Google requirement for personal developer accounts created after Nov 13, 2023:*
1. Create a **Closed testing** track.
2. Recruit at least **12 testers** who opt into your closed test on Android for at least **14 continuous days**.
3. Once 14 consecutive days have completed with active tester engagement and opt-ins, apply for production access in Play Console.

### C. Privacy Policy & Data Safety
1. **Privacy Policy Link:**
   - Link: `https://www.wisdom-tower-academy.live/privacy` (or your official hosted privacy policy URL).
   - In Play Console: **Policy and programs → App content → Privacy policy**.
2. **Data Safety Form:**
   - Location data: None collected.
   - User account data: Handled via the secure web portal (login credentials encrypted over HTTPS).
   - Local device storage: App stores local PDF study materials and offline web cache in private app sandbox (`getFilesDir()`). No external storage access requested.
   - Ads: App does not contain third-party advertising SDKs.
   - Financial info: Course package transactions are processed externally through authorized Ethiopian payment gateways on the web portal; no card numbers or banking secrets are collected or stored in the Android app.

### D. Content Rating & Target Audience
1. **Content Rating Questionnaire:**
   - Educational app: Complete the IARC rating questionnaire (typically PEGI 3 / Everyone).
2. **Target Audience:**
   - Educational material designed for high school (Grade 11–12) and university freshman students. Select **13 and older** (avoids strict Designed for Families COPPA restrictions).

### E. Store Listing Assets
Prepare and upload under **Store presence → Main store listing**:
- **App icon:** 512 × 512 px, 32-bit PNG with alpha.
- **Feature graphic:** 1024 × 500 px, JPG or 24-bit PNG (no alpha).
- **Screenshots:**
  - Phone: At least 2 screenshots (16:9 or 18:9, e.g. 1080 × 2400 px) showing Home, Learning hub, Grade 12 / Freshman course books, and Settings.
  - Tablet (optional but recommended): 7-inch and 10-inch screenshots.
- **Short description:** Up to 80 characters (e.g. *Premier online learning academy for high school & university freshman scholars.*).
- **Full description:** Detailed description highlighting courses, books, flashcards, exams, offline reading vault, and study tools.

---

## 4. Releasing to Production
1. In Play Console, go to **Release → Production**.
2. Click **Create new release**.
3. Upload `Wisdom-tower-academy.aab`.
4. Review release notes (`v1.0.0 (3)`).
5. Click **Save** and **Start rollout to Production** (or submit for review).
