# NATIVE_APP_BRIEF.md — Wisdom Tower Academy Android App

> Put this file in the root of `Wisdom-tower-academy-app-packed`. Any AI tool or agent that picks up this project must read this file and `ARCHITECTURE.md` FIRST, before touching code.
> Last updated: 2026-10-09 · Owner: Hiyab Teklu

---

## 1. What we are doing (one paragraph)

The **Wisdom Tower Academy website is the source of truth**. The Android app currently wraps the website in a WebView, so every tab switch looks like a page reload ("blinking"). We are turning the app into a **mostly native Android app (Kotlin + Jetpack Compose)** that **looks and is structured like the website** (same layouts, arrangement, images, colors, wording), uses the **same data** (same Supabase project, same authors, same content), and only hands off to the website for the heavy, server-based parts.

## 2. Repositories

| Repo | Role |
|---|---|
| `hiyabteklu/Wisdom-tower-academy` | The website (Next.js on Vercel). Source of truth. **Never edit it from this project.** |
| `hiyabteklu/Wisdom-tower-academy-app-packed` | This Android app. Branch `main`. |
| `website-reference/` (inside this repo) | Read-only snapshot of the website code, made by GitHub Action **"Sync website reference"** (Actions → Sync website reference → Run workflow). Re-run it when the website changes. It skips env files, Firebase config, keystores, lockfiles, PDFs, zips, videos and files over 1 MB. |

The coding agent (Google AI Studio or any other) can see only ONE repo, which is why the website snapshot lives here. `website-reference/` is excluded from the app build.

## 3. Website stack (do not blind yourself to these)

- **Next.js 15 + React 19 + Tailwind**, hosted on **Vercel**, code on GitHub
- **Supabase**: login/auth and the main database
- **Appwrite**: file storage only
- **Firebase**: push notifications (FCM)
- **Google Gemini**: the AI tutor
- **Groq**: the **"Explain with AI"** buttons on every question-bank and exam question
- All secret API keys live in **Vercel environment variables** and are NOT in the repos. **Never put an API key in the Android app.** Anything that needs a secret key must call the website's API routes (`website-reference/src/app/api/`).

## 4. What is native and what stays on the website

**Native (build these in Compose, mimic the website):**
- App shell: top bar, bottom navigation, menu, notifications list
- Home, Academy/Packages hubs, program/package landing pages, Learning landing, Account, Settings
- Static/guide pages (Success Stories, Study Techniques, Campus Life, Universities, Departments, Scholarships) when their data is not server-gated
- Cards, thumbnails, images, headers, empty/loading states

**Stays on the website (open from a native screen in the existing WebView host):**
- Books/PDF reading, flashcards, notes, exams, question banks, "Explain with AI"
- Progress tracker and analytics
- AI tutor and any other server-based feature
- Checkout, orders, admin

Rule of thumb: if the screen only arranges items and shows images/titles, build it native. If it needs the learning material itself, a server calculation or a secret key, open the website.

## 5. Hard rules

1. **Read before you build.** For every screen, open the website's page file, every component it imports, the CSS it uses and the data files behind it in `website-reference/`. Look at the images in `website-reference/public/images/`. Do not guess the design.
2. **Mimic the website**: same section order, same grid (e.g. 2 columns on mobile), same card structure, same colors, same fonts, same icons (lucide icons have Material equivalents; match as closely as possible), same wording.
3. **Respect `hide-on-app`.** The website marks sections with the CSS class `hide-on-app` when they should NOT appear in the app. Do not build those.
4. **Use the website's real assets and text.** Copy images from `website-reference/public/images/...` into the Android project. Never invent images, copy or numbers. If an asset is missing from the snapshot (e.g. over 1 MB), say so and ask; do not substitute.
5. **No duplicated content in the app.** Any content shown from data must come from the same Supabase/Appwrite source as the website (Phase B). Where Phase A needs placeholders, put them in ONE clearly marked seed file with a comment naming the website source file.
6. **No blinking.** Native screens must keep state between tab switches (`rememberSaveable`, a ViewModel per tab, `saveState/restoreState` on navigation). Any WebView host must stay alive between tabs and must not call `loadUrl` for a page it is already showing.
7. **Keep the app's existing rules** from `ARCHITECTURE.md`: navy status bar (no overlap with clock/battery), fixed top bar (menu · "Wisdom Tower Academy" · notifications), notification bell opens `/notifications` only, bottom nav Home / Learning / Packages / Account, offline PDF vault, `FLAG_SECURE`.
8. **Don't delete the WebView shell.** Build the native screens beside it and switch screen by screen, so the app always builds and can fall back.
9. **Never commit secrets** (env files, `google-services.json`, keystores, API keys).
10. **Never edit `website-reference/`.** If the website needs a change, write it in `WEBSITE_CHANGES.md` (file path, exact code, reason).
11. **Small commits**, one screen per commit, clear messages. After each commit, say what could affect tab-switch speed.
12. The **Build Production AAB & APK** GitHub Action must stay green.
13. **Read docs/BUILD_PITFALLS.md before writing code and never repeat a listed mistake.**
14. **Never put a secret or server key in the Android app.** Anything that needs a secret key (Explain with AI, SMS/email, push sending, admin) must call the website API routes in `website-reference/src/app/api/` or open the website.
15. **Only the 6 public keys above may exist in the app.** Only `NEXT_PUBLIC_SUPABASE_URL`, `NEXT_PUBLIC_SUPABASE_ANON_KEY`, `NEXT_PUBLIC_SITE_URL`, `NEXT_PUBLIC_APPWRITE_ENDPOINT`, `NEXT_PUBLIC_APPWRITE_PROJECT_ID`, and `NEXT_PUBLIC_APPWRITE_BUCKET_ID` may exist in `.env.example` and BuildConfig. Every key must have a non-empty value so no BuildConfig field is empty. Signing keys exist strictly as GitHub Actions secrets read via `System.getenv`.

## 6. Known facts about the website Home page (already analysed)

Source: `website-reference/src/app/page.tsx` → `src/components/home/LandingPage.tsx` + `LandingPathways.tsx`.

**Shown in the app (in this order):**
1. **Hero**: dark gradient background with soft glowing orbs; big heading "Wisdom **Tower** Academy" (middle word is an animated gradient accent); button row: primary "Enter Academy →" (`/academy`) and a second button that depends on login: signed in → "My Learning" (`/learning`), signed out → "Sign in" (`/login`, amber outline); under it a line: signed in → "Welcome back, {name} · Continue where you left off", signed out → "New here? Create a free account".
2. **Welcome image card** (`/images/home/academy.jpg`), full width, 16:9 on phones, rounded, gradient overlay, floating pill "Open Learning →" bottom-right, taps to `/learning`.
3. **Program cards grid**, 2 columns on mobile, 16:10 image on top, below it a single row: program name left + small "Open →" button right. Cards in order: ECE Engineering, Freshman, Grade 9–12, COC, UAT, GAT, Exit Exam, Remedial (images from `src/data/packages` and `/images/special-packages/ece.jpg`).
4. **"Other resources"** heading, then a 2-column grid of compact cards (icon tile + title + small arrow): Success Stories, Study Techniques, Campus Life, Universities Directory, Departments Guide, Scholarships Guide, each with its own accent color.

**Hidden in the app (`hide-on-app`):** stats slider, infinity card, "Want digital services instead?", final CTA, partnership path.

**Design tokens:** Tailwind colors `wisdom-dark`, `wisdom-navy`, `wisdom-card`, `wisdom-muted` come from CSS variables in `src/app/globals.css` (read the exact values there); `wisdom-cyan #22e0ff`, `cyan-dark #00c4e6`. Fonts come from `--font-display` and `--font-body` set in `src/app/layout.tsx`. Button and card styles (`btn-primary`, `btn-secondary`, `btn-open`, `card-modern`) are defined in `globals.css` / `ui-polish.css`. Read these files and port the exact values into a Compose theme.

## 7. Roadmap

**Step 0 — Safety.** Make sure the current app builds. Don't remove the WebView shell. (Optional: the old tab-reload regression was probably introduced around commit `9f6e59f` — cache v4 purge — see `ARCHITECTURE.md`; keeping each tab's WebView alive is the fallback fix.)

**Phase A — Native UI, no server data (current).**
- A0. Write `docs/WEBSITE_ANALYSIS.md`: for every website route that appears in the app, list the page file, components, CSS, data files, images, colors, what is `hide-on-app`, and whether it is native or website-handoff.
- A1. Compose theme (colors, typography, shapes, buttons, cards) ported from the website CSS.
- A2. Native **Home** screen exactly as in section 6.
- A3. Native **Academy / Packages** hub and package landing pages.
- A4. Native **Learning** landing (shell only; the learning material opens from the website).
- A5. Native **Account** and **Settings** shells.
- A6. Native static guide pages (only if not server-gated).
- Stop at every screen for the owner's review.

**Phase B — Real data.** Sign in with the same Supabase project (shared session with the WebView so users log in once), read packages, courses, authors and notifications from the same tables (check row-level security), load thumbnails from Appwrite storage. No new tables, no copied content.

**Phase C — Handoffs.** Open books/PDFs, flashcards, notes, exams, question banks, progress tracker and AI tutor on the website from native screens with the logged-in session.

## 8. Progress log (update this after every working session)

| 2026-10-09 | Claude (planning) | Brief written, website Home analysed, sync workflow works | Run agent prompt for Phase A0–A2 |
| 2026-10-09 | AI Studio Agent | Phase A0 (WEBSITE_ANALYSIS.md), Phase A1 (Compose theme tokens & WisdomComponents), Phase A2 (Native HomeScreen & assets copied) completed | Owner review of Home screen, then Phase A3 (Academy/Packages Hub) |
| 2026-10-09 | AI Studio Agent | Phase A3 (Native PackagesScreen catalog, PackageDetailScreen landing pages, Freshman subjects grid & assets) completed | Owner review, then Phase A4 (Native Learning Landing Shell) |
| 2026-10-09 | AI Studio Agent | Phase A4 (Native LearningScreen shell, study tools carousel, study mode hubs, smooth webview study reader handoff) completed | Owner review, then Phase A5 (Native Account & Settings Shells) |
| 2026-10-09 | AI Studio Agent | Phase A5 (Native AccountScreen with digital Student ID card, active course passes, and SettingsScreen with push alerts, vault controls & theme) completed | Owner review, then Phase A6 (Native Static Guide Pages) |
| 2026-10-09 | AI Studio Agent | Phase A6 (Native GuidesScreen with all 6 guide readers: Study Techniques, Success Stories, Campus Life, Universities, Departments, Scholarships) completed | All Phase A screens completed! Ready for Phase B (Real Supabase / Appwrite data integration) |
| 2026-10-09 | AI Studio Agent | Fixed Run #5 build failure: replaced BookOpen with MenuBook, replaced rememberRipple with ripple across all screens, created docs/BUILD_PITFALLS.md | Verify GitHub Actions CI green before Phase B |
| 2026-10-09 | AI Studio Agent | Phase B (Real Supabase / Appwrite data integration): AuthStateManager with bidirectional JS bridge session sync, UserProfile and deterministic StudentIdData models, live NotificationRepository with unread badge counter, dynamic AcademyRepository catalog and Appwrite storage resolver, wired seamlessly across HomeScreen, LearningScreen, PackagesScreen, PackageDetailScreen, and AccountScreen | Owner review of Phase B, then proceed to Phase C (Interactive Handoffs) |
| 2026-10-09 | AI Studio Agent | Phase C (Interactive Handoffs): All 7 specified study handoffs implemented with retained logged-in session (Books/PDFs with offline vault caching, Flashcard decks, Short Notes & rich Notebook, Practice Exams, Question Banks, Academic Progress Tracker & Analytics, and AI Tutor). Added clean web handoff overlay with auto-dismiss for auth (Sign In/Sign Up) & checkout, live Study Workspace top bar navigation with back arrow, and direct study resource launcher chips across LearningScreen and PackageDetailScreen. | Ready for QA testing |
| 2026-10-09 | AI Studio Agent | Fix AccountScreen unresolved references: imported WisdomOpenButton from WisdomComponents, replaced unresolved packagePath on EnrolledPackageRecord with targetPath derived from CATALOG_PACKAGES matching website-reference packages.ts data, hardened .env.example with non-empty public defaults, and documented in docs/BUILD_PITFALLS.md | Verify GitHub Actions CI build |
| 2026-10-10 | AI Studio Agent | Phase D (Offline & Fidelity Hardening): Decoupled bottom navigation tab state from background WebView URL listeners to eliminate tab bouncing/blinking, purged all obsolete priceEtb references from AcademyRepository for 100% free curriculum access, verified non-empty public keys in .env.example, generated comprehensive docs/FIDELITY_AUDIT.md mapping all native screens to website-reference, and logged fixes in docs/BUILD_PITFALLS.md | Ready to sync to GitHub and trigger GitHub Actions build |
