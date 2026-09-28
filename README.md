# Wisdom Tower Academy — React Web Application

Complete React web application rewritten from the original Android applet for **Wisdom Tower Academy** (https://wisdom-tower-academy.live), Ethiopia's premier higher education learning platform.

## Features Preserved & Ported

- **Native Chrome & Layout Shell:**
  - Fixed top bar with hamburger menu, official brand logo, title, pinned refresh action, and notification bell.
  - Dedicated `/notifications` route that preserves the active bottom tab (Architecture Rule #6).
  - Apple-quality 5-tab bottom navigation: **Home**, **Learning**, **Packages**, **Account**, **Settings**.
- **Onboarding Experience:**
  - 5-step interactive onboarding flow with Ethiopian curriculum introduction, accuracy-first philosophy, AI features, and notification opt-in.
  - Uses `animation.gif` brand asset and persistent completion state in local storage.
- **Offline Vault & Catalog Engine:**
  - Complete pre-seeded catalog with all 27 Ethiopian university modules & grade books and exact byte sizes (Anthropology, Logic, Calculus, Physics, Chemistry, C++, History, Economics, etc.).
  - Zero-data offline caching in private storage with real-time download progress tracking (`tabular-nums` formatting).
  - In-app Book Reader with chapter navigation, text scaling, bookmarking, and offline indicators.
- **Interactive 3D Flashcards:**
  - Authentic 3D card flip with realistic physics, question prompts, key concepts, explanations, and mastery tracking.
- **Curriculum Packages & Ethiopian Payment Flow:**
  - Undergraduate Freshman Suite, Natural Sciences & Pre-Engineering, Remedial, and Grade 9–12 STEM packages with Ethiopian Birr (ETB) pricing.
  - Support for Telebirr, CBE Birr, and Chapa checkout simulation with instant course unlocking.
- **Account & Community:**
  - Student profile, 14-day study streak, enrolled course manager, and official Telegram discussion link (`https://t.me/wisdomtower`).
- **Settings & Vault Storage Manager:**
  - View consumed storage, manage cached modules, delete individual files or clear the entire vault.
  - Data saving preferences (Download on Wi-Fi only, auto-cache).
- **Exit Confirmation Dialog:**
  - Clean exit prompt matching the native applet specifications.

## Development & Build

```bash
# Install dependencies
npm install

# Start Vite dev server on port 3000
npm run dev

# Build production bundle with TypeScript check
npm run build
```
