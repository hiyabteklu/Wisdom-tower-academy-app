# Google Play Store Production Readiness Checklist (2026)

Wisdom Tower Academy (`com.wisdomtower.academy`)

> 2026-10-11: section 3C (Data Safety) was rewritten because the earlier text was a guess, and section 5 (blockers) was added. Read section 5 first.
> 2026-10-17: account deletion (section 3C Deletion line and section 5 item 1) checked and corrected, see `docs/SESSION_2026-10-17_S2_ACCOUNT_DELETION.md`.

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

### C. Privacy Policy & Data Safety (rewritten 2026-10-11)
1. **Privacy Policy Link:** `https://www.wisdom-tower-academy.live/privacy`. Before submitting, make sure that page lists everything below and mentions account deletion (it does since 2026-10-12: the Right to Erasure item links to `/delete-account`).
2. **Data Safety form.** What the app really handles (verified by reading the app code and manifest on 2026-10-11, except where marked "confirm"):
   - **Permissions in the manifest:** internet, network state, notifications (plus `RECEIVE_BOOT_COMPLETED`, a normal permission, nothing to declare). NO location, NO storage, NO camera, NO microphone, NO exact-alarm permission (removed in S1). So: location data = none.
   - **Personal info:** full name, e-mail address, phone number (sign-in and profile).
   - **Profile details:** academic level, stream, school or university, region, short bio, target exam and score, daily study goal, avatar choice. Saved in the Supabase `profiles` table.
   - **App activity (confirm):** study progress and results saved by the website pages shown inside the app (Supabase `learning_progress`, `academic_results`).
   - **Device or other IDs:** Firebase push-notification token (the app contains Firebase Cloud Messaging).
   - **Text sent to AI (confirm):** questions typed into the AI tutor and "Explain with AI" go to the website server and from there to Gemini and Groq (third parties). Declare this as data shared with third-party services.
   - **Security:** all traffic is HTTPS (cleartext is switched off in the manifest). Sign-in tokens are stored encrypted on the phone (Android Keystore). Say "data is encrypted in transit".
   - **Deletion:** BUILT and checked 2026-10-17 (S2). In the app: Settings > Security > Delete my account (type DELETE). On the web: `https://www.wisdom-tower-academy.live/delete-account` (enter this as the account deletion URL in Play Console). Answer "yes, users can request deletion" and "yes, data is deleted"; mention that verified payment records are kept five years and no longer linked to the account.
   - **Ads:** none known. Confirm there is no ad SDK in `app/build.gradle.kts` before answering "no ads".
   - **Payments:** the app is in free mode (`IS_FREE_MODE = true` on the website) and collects no payment data. If paid packages come back, redo this form.
   - **Local storage:** PDFs and cached pages stay in the app's private storage.

### D. Content Rating & Target Audience
1. **Content Rating Questionnaire:**
   - Educational app: Complete the IARC rating questionnaire (typically PEGI 3 / Everyone).
2. **Target Audience:**
   - Educational material designed for high school (Grade 9–12) and university students. Select **13 and older** (avoids strict Designed for Families COPPA restrictions).

### E. Store Listing Assets
Prepare and upload under **Store presence → Main store listing**:
- **App icon:** 512 × 512 px, 32-bit PNG with alpha.
- **Feature graphic:** 1024 × 500 px, JPG or 24-bit PNG (no alpha).
- **Screenshots:**
  - Phone: At least 2 screenshots (16:9 or 18:9, e.g. 1080 × 2400 px) showing Home, Learning hub, Grade 12 / Freshman course books, and Settings.
  - Tablet (optional but recommended): 7-inch and 10-inch screenshots.
- **Short description:** Up to 80 characters (e.g. *Premier online learning academy for high school & university freshman scholars.*).
- **Full description:** Detailed description highlighting courses, books, flashcards, exams, offline reading vault, and study tools. Write only features that exist in the app (no invented claims).

---

## 4. Releasing to Production
1. In Play Console, go to **Release → Production**.
2. Click **Create new release**.
3. Upload `Wisdom-tower-academy.aab`.
4. Review release notes (`v1.0.0 (3)`).
5. Click **Save** and **Start rollout to Production** (or submit for review).

---

## 5. Blockers found 2026-10-11 (do these before step 4)

1. **Account deletion: DONE in code (checked 2026-10-17, S2).** Website route `/api/account/delete`, public page `/delete-account`, privacy link, and the in-app button all exist; the live database has every table the route uses and no foreign key can block the delete. **Owner still to do:** one real test with a TEST account on a phone (delete, then sign in again must fail), and enter the `/delete-account` URL in Play Console. Details: `docs/SESSION_2026-10-17_S2_ACCOUNT_DELETION.md`.
2. **Owner decision on the website login fix** (`WEBSITE_CHANGES.md` section 1) and the Supabase "Confirm email" setting. Until then website users can still get a fake local-only login.
3. **Privacy page must match the data above** (section 3C). Still open: Groq, Appwrite and Firebase are not named (`WEBSITE_CHANGES.md` section 5).
4. **12 testers for 14 days** (section 3B). Start this as early as possible, it is the longest wait.
5. **Store pictures and texts** (section 3E) are not made yet.
6. **Release signing secrets** (section 1) must exist in GitHub before a signed `.aab` can be built. Not verified whether they are set.
