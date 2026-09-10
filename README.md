# Wisdom Tower Academy (Android App)

Wisdom Tower Academy is an Ethiopian higher education and professional assessment platform built for Android (Kotlin, Jetpack Compose, Material 3, Android SDK 26–35).

---

## Architecture & Security Highlights

1. **Anti-Leak & Anti-Piracy Protection**:
   - **`FLAG_SECURE`**: Applied to `MainActivity` window to strictly block screenshots, screen mirroring, and screen recording apps.
   - **Private Sandbox Storage**: Downloaded PDFs and textbooks are stored exclusively in app-private storage (`context.filesDir/secure_academic_vault`). They are never exposed to external storage or public file intents.
   - **Hardware-Backed Encryption**: User authentication sessions and tokens are protected via Android `EncryptedSharedPreferences` backed by Android Keystore (AES-256 GCM).
   - **Anti-Leak Watermarking**: In-app PDF and notes viewers display dynamic scholar licensing watermarks.

2. **Native Zero-Permission PDF Reader**:
   - In-app rendering via Android's `PdfRenderer` for complete sandbox isolation. No external third-party viewer intents or public storage permissions required.

3. **Offline-First Room Vault**:
   - Downloaded modules, notes, reading progress percentages, flashcard mastery status, and mock exam attempts persist locally via Room DB (`wisdom_tower_academy.db`).
   - Toggle "Offline Mode" to study anywhere without internet connectivity.

4. **Curriculum Pathways**:
   - **Freshman Program**: Free for all signed-in users (General Physics, Mathematics for Natural Science, Communicative English, Critical Thinking & Logic, Emerging Technologies).
   - **Paid Packages**: MoE National Exit Exam, GAT Prep, COC Certification Hub, ECE & Computing Exams, Remedial University Program.

---

## Supabase Setup Instructions

The app connects to the official backend at `https://wisdom-tower-academy.live`. To configure your own Supabase project:

### 1. Environment Variables / Secrets
Add your Supabase credentials into `.env` (or via the AI Studio Secrets panel):
```properties
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```
*Never embed your `service_role` key into the mobile client.*

### 2. Database Tables & Schema

Run the following SQL in the Supabase SQL Editor:

```sql
-- 1. Profiles Table
CREATE TABLE public.profiles (
  id UUID REFERENCES auth.users ON DELETE CASCADE PRIMARY KEY,
  full_name TEXT,
  email TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users can view own profile" ON public.profiles
  FOR SELECT USING (auth.uid() = id);

-- 2. User Enrollments Table
CREATE TABLE public.user_enrollments (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id UUID REFERENCES auth.users ON DELETE CASCADE,
  package_id TEXT NOT NULL,
  package_name TEXT NOT NULL,
  is_verified BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE(user_id, package_id)
);

ALTER TABLE public.user_enrollments ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users can view their enrollments" ON public.user_enrollments
  FOR SELECT USING (auth.uid() = user_id);

-- 3. Academic Content Tables
CREATE TABLE public.packages (
  id TEXT PRIMARY KEY,
  title TEXT NOT NULL,
  tag TEXT,
  description TEXT,
  is_free BOOLEAN DEFAULT FALSE,
  price_etb INT DEFAULT 0
);

ALTER TABLE public.packages ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Public read for packages" ON public.packages
  FOR SELECT TO authenticated, anon USING (true);
```

### 3. Storage Buckets (for PDFs)

1. Create a storage bucket named `academic_vault`.
2. Configure RLS on `storage.objects`:
```sql
CREATE POLICY "Allow authenticated read for academic vault" ON storage.objects
  FOR SELECT TO authenticated
  USING (bucket_id = 'academic_vault');
```

---

## How to Run in Android Studio

1. **Prerequisites**:
   - Android Studio Ladybug (2024.2+) or newer.
   - Android SDK 35 (compileSdk 35, targetSdk 35, minSdk 26).
   - JDK 17 or JDK 21.

2. **Open Project**:
   - Open the project root folder in Android Studio.
   - Allow Gradle to sync dependencies.

3. **Run on Device or Emulator**:
   - Select an emulator or connected physical Android device (Android 8.0+).
   - Click **Run 'app'** (`Shift + F10`).

---

## Verification & Testing Guide

1. **Sign In / Demo Scholar**:
   - Tap **"Quick Demo Access"** to immediately sign in as a Freshman Scholar without configuring remote network keys, or enter your Supabase email & password.
2. **Freshman Curriculum (Free Tier)**:
   - Notice the **Freshman Program** is automatically unlocked (`ACTIVE & UNLOCKED`).
   - Tap into **General Physics (Phys 1011)** to access the 5 Hubs.
3. **In-App High-Yield Notes & Reading Progress**:
   - Open **"Mechanics & Newton's Laws Summary"**.
   - Scroll through the formatted markdown reader; observe the top linear progress bar updating without obstructing reading content.
4. **Offline Download Flow**:
   - Tap the download cloud icon in the top bar.
   - The module is saved directly to your private encrypted sandbox.
   - Return to Home, turn on the **"Offline Mode"** filter, or open the **"Encrypted Offline Vault"** to verify offline accessibility.
5. **In-App PDF Viewer**:
   - Open **"Official Freshman Physics Textbook"**.
   - Verify multi-page navigation (Next, Prev, Zoom In/Out), and observe the subtle license watermark.
6. **Active Recall Flashcards**:
   - Open the **Physics 1011 Flashcards**.
   - Tap to flip between Question and Answer in 3D, and mark cards as **"Mastered"** or **"Needs Review"**.
7. **Timed Mock Exam & Question Bank**:
   - Launch the **Timed Mock Midterm Examination**.
   - Complete the questions under the 10-minute live countdown timer and submit to view instant answer rationales.
8. **Verify Screenshot & Screen Recording Blocking**:
   - Attempt taking a screenshot using the device shortcut (`Power + Volume Down`).
   - Android OS will display: *"Can't take screenshot due to security policy"* (enforced by `FLAG_SECURE`).
