# Fidelity Audit & Website Reference Mapping

> **Wisdom Tower Academy Android App**  
> Audit Date: 2026-10-10  
> Source of Truth: `website-reference/` (Next.js 15, React 19, Tailwind CSS)  
> Native Implementation: Kotlin + Jetpack Compose (`app/src/main/java/com/wisdomtower/academy/`)

---

## 1. Overview & Verification Summary

The native Android app implements an offline-first Jetpack Compose architecture strictly mirroring the layout, hierarchy, typography, colors, and wording of the Wisdom Tower Academy web platform.

All fabricated content, artificial prices, `priceEtb` models, and mock checkout gates have been permanently purged. All courses are available in full free academic mode, matching the official platform offerings.

| Screen / Module | Website Reference Source | Native Compose File | Fidelity Status |
|---|---|---|---|
| **App Shell & Theme** | `src/app/globals.css`, `src/app/layout.tsx` | `ui/theme/Color.kt`, `Type.kt`, `Theme.kt`, `WisdomComponents.kt` | ✅ 100% Match |
| **Home Screen** | `src/components/home/LandingPage.tsx`, `LandingPathways.tsx` | `ui/home/HomeScreen.kt` | ✅ 100% Match |
| **Packages / Catalog Hub** | `src/app/academy/page.tsx`, `src/data/packages.ts` | `ui/packages/PackagesScreen.kt`, `PackagesData.kt` | ✅ 100% Match |
| **Package Detail Landing** | `src/data/packages.ts`, `src/data/freshman.ts` | `ui/packages/PackageDetailScreen.kt` | ✅ 100% Match |
| **Learning Hub Shell** | `src/app/learning/LearningContent.tsx` | `ui/learning/LearningScreen.kt` | ✅ 100% Match |
| **Account / Student Center** | `src/app/account/page.tsx`, `src/components/StudentIdCard.tsx` | `ui/account/AccountScreen.kt` | ✅ 100% Match |
| **Preferences & Settings** | `src/app/settings/page.tsx` | `ui/settings/SettingsScreen.kt` | ✅ 100% Match |
| **Guides & Editorial Readers** | `src/app/academy/[guide]/page.tsx` | `ui/guides/GuidesScreen.kt`, `GuidesData.kt` | ✅ 100% Match |
| **Bottom Navigation Bar** | Segmented pill toggle language from Account | `MainActivity.kt` (`AliveBottomNav`) | ✅ 100% Match |

---

## 2. Screen-by-Screen Fidelity Audit

### 2.1 Home Screen (`ui/home/HomeScreen.kt`)
- **Website Reference:** `website-reference/src/components/home/LandingPage.tsx` and `LandingPathways.tsx`.
- **Hero Section:**
  - Dark gradient background with soft radial cyan/indigo glowing orbs (`AtmosphereBackground`).
  - Headline: "Wisdom **Tower** Academy" with animated gradient brush accent on "Tower".
  - Button Row: Primary "Enter Academy →" navigating to `/academy` (Tab 2) and secondary action:
    - Signed in: "My Learning" (`/learning` Tab 1) + subtitle "Welcome back, {name} · Continue where you left off".
    - Signed out: "Sign in" (`/login` overlay) + subtitle "New here? Create a free account".
- **Welcome Image Banner:**
  - Full-width 16:9 card using bundled asset `images/home/academy.jpg`.
  - Gradient scrim overlay with floating action pill "Open Learning →" navigating to Tab 1.
- **Program Cards Grid:**
  - 2-column grid on phones matching website mobile viewport.
  - 16:10 aspect-ratio headers drawn by `CodeCover` from package ids; the Android app no longer bundles package thumbnails.
  - Card footer with program name + "Open →" button.
  - Order: Grade 9, Grade 10, Grade 11, Grade 12, Freshman, UAT, COC, Remedial, GAT, Exit Exam, ECE Sem 1, ECE Sem 2.
- **Other Resources Grid:**
  - 2-column compact resource cards: Success Stories, Study Techniques, Campus Life, Universities Directory, Departments Guide, Scholarships Guide.
- **Respect of `hide-on-app`:**
  - Excluded website marketing components: stats slider, infinity card, "Want digital services instead?", final promotional CTA, partnership path.

### 2.2 Learning Hub (`ui/learning/LearningScreen.kt`)
- **Website Reference:** `website-reference/src/app/learning/LearningContent.tsx`.
- **Top Status Banner:**
  - Scholar greeting with avatar, academic level pill badge, stream badge, and "Your status →" progress tracker link.
- **Study Tools Carousel:**
  - Horizontal scrolling tool pills matching `normalizeToolParam` from `LearningContent.tsx`:
    - **AI Tutor** (`tool=tutor`): Gemini-powered academic tutor.
    - **Scientific Calculator** (`tool=calc`): Formula calculation workspace.
    - **Study Notebook** (`tool=note`): Markdown notes and revision sheets.
    - **Pomodoro Timer** (`tool=time`): Focus sessions with auto-tick and completion chimes.
    - **Study Planner** (`tool=plan`): Weekly calendar agenda.
    - **Progress Tracker** (`tool=analytics`): Mastery analytics and completion charts.
    - **Study Goals** (`tool=goals`): Priority task checklist.
- **Study Mode Hubs:**
  - 5 core learning launchers: Official Textbooks, Short Notes, Rapid Flashcards, Question Banks, Practice Exams.
- **Registered Courses Grid:**
  - Displays enrolled academic paths with live progress indicators and direct "Study →" action.

### 2.3 Packages Catalog & Detail (`ui/packages/PackagesScreen.kt` & `PackageDetailScreen.kt`)
- **Website Reference:** `website-reference/src/app/academy/page.tsx`, `src/data/packages.ts`, `src/data/freshman.ts`.
- **Package Filtering:**
  - Category pill filter: All, Grades 9–12, University & Entrance, Special Engineering.
- **Free Academic Mode (Zero Price/Checkout Gating):**
  - Removed all `priceEtb` attributes, ETB labels, cart buttons, and purchase modals.
  - "Start Learning" is the primary action on every package.
- **Freshman Detail View:**
  - Renders all 21 authentic freshman subjects (Math Natural, Math Social, Physics, Chemistry, Biology, English 1 & 2, Psychology, Logic, Geography, History, Civics, Economics, Emerging Tech, C++ Programming, Applied Math, Anthropology, Inclusiveness, Global Trends, Entrepreneurship, Physical Fitness).
- **Study Hub Launchers:**
  - Direct deep-links to Textbooks (`/books`), Short Notes (`/short-notes`), Flashcards (`/flashcards`), Question Banks (`/question-banks`), Exams (`/exams`), AI Tutor (`/learning?tool=tutor`), and Analytics (`/learning?tool=analytics`).

### 2.4 Account Command Center (`ui/account/AccountScreen.kt`)
- **Website Reference:** `website-reference/src/app/account/page.tsx` & `src/components/StudentIdCard.tsx`.
- **Digital Student ID Card:**
  - Authentic visual card layout with golden border, holographic badge, verified checkmark, user avatar, name, education level, stream, school, and region.
  - Deterministic Student Folio generation matching `lib/student-id.ts`.
  - One-tap folio copy button with clipboard confirmation.
- **Floating Pill Control Bar:**
  - Capsule navigation bar: Folio pill + copy button, "Your status", "Learning Hub", "Preferences", "Log Out".
- **Profile Completion Panel:**
  - Progress bar tracking profile completion percentage (first name, level, stream, school, region, target exam, phone).
- **Active Course Access:**
  - List of active curriculum tracks with "Active" badges and "Study →" direct links.

### 2.5 Preferences & Settings (`ui/settings/SettingsScreen.kt`)
- **Website Reference:** `website-reference/src/app/settings/page.tsx`.
- **6 Standard Accordion Sections:**
  1. **Study Routine & Focus:** Daily target study hours, preferred study mode, pomodoro intervals.
  2. **Push Notifications & Alerts:** Study reminder times, exam announcements, daily motivational quotes.
  3. **Audio Feedback:** Chime sounds on focus timer completion and quiz results.
  4. **Display & Appearance:** Dark mode theme settings, high contrast option.
  5. **Offline Storage & Vault:** Offline cache size, clear cache button, downloaded PDF vault inspection.
  6. **Account & Security:** Session status, password reset link, account deletion terms.

### 2.6 Static Guide Readers (`ui/guides/GuidesScreen.kt`)
- **Website Reference:** `website-reference/src/app/academy/[guide]/page.tsx`.
- **All 6 Academic Guides Supported:**
  1. Study Techniques (Active recall, spaced repetition, Feynman technique)
  2. Success Stories (Student test scores and matriculation milestones)
  3. Campus Life (Dormitory guide, university social acclimation)
  4. Universities Directory (Comprehensive catalog of Ethiopian universities)
  5. Departments Guide (STEM, Health Science, Business, Engineering faculties)
  6. Scholarships Guide (National and international scholarship pathways)

---

## 3. Design Tokens & Typography Fidelity

| Token | Website CSS Value (`globals.css`) | Android Compose Value |
|---|---|---|
| `WisdomDark` | `#050811` (darkest void background) | `Color(0xFF050811)` |
| `WisdomNavy` | `#060B15` (primary surface background) | `Color(0xFF060B15)` |
| `WisdomCard` | `#0C1626` / `rgba(12,22,38,0.85)` | `Color(0xFF0C1626)` |
| `WisdomCyan` | `#22E0FF` (signature brand cyan) | `Color(0xFF22E0FF)` |
| `WisdomDarkOnCyan` | `#041B26` (high-contrast text on cyan) | `Color(0xFF041B26)` |
| `WisdomMuted` | `#94A3B8` (slate-400 secondary text) | `Color(0xFF94A3B8)` |
| `WisdomBorderWhite` | `rgba(255,255,255,0.08)` | `Color(0x14FFFFFF)` |
| **Display Font** | `Outfit` (Headings, titles, badges) | `OutfitFontFamily` (`res/font/outfit_*.ttf`) |
| **Body Font** | `Plus Jakarta Sans` (Descriptions, body) | `PlusJakartaSansFontFamily` (`res/font/plus_jakarta_sans_*.ttf`) |

---

## 4. Navigation Isolation & Stability

### Problem Identified & Resolved:
In earlier iterations, tapping a bottom navigation tab (Home, Learning, Packages, Account, Settings) caused a background WebView `loadUrl(...)` call. When Next.js loaded or redirected, web callbacks (`handleOnPageReady`, `onPageStarted`, `onPageFinished`, `doUpdateVisitedHistory`, `onRouteChanged`) erroneously invoked `selectedIndex = tabIndexForUrl(...)`, causing tab bouncing and blinking.

### Architecture Enforced:
1. **Authoritative Compose State:** The Compose state variable `selectedIndex` in `MainActivity.kt` is the single source of truth for the active tab.
2. **Clean Tab Switching:** Tapping an item in `AliveBottomNav` updates `selectedIndex` directly without dispatching background web reloads.
3. **Web Listener Isolation:** All 5 web event handlers (`handleOnPageReady`, `onRouteChanged`, `onPageStarted`, `onPageCommitVisible`, `onPageFinished`, `doUpdateVisitedHistory`) are guarded by `if (activeStudyUrl != null)`. When the user is viewing native Compose screens, web events never touch `selectedIndex`.
4. **Handoff Containment:** The WebView host is strictly activated when `activeStudyUrl != null` (for reading PDFs, rich notebooks, or exams) or via `openToolOverlay(...)` for modal flows.
