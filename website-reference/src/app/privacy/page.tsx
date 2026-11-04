import type { Metadata } from "next";
import Link from "next/link";
import {
  Lock,
  Shield,
  Eye,
  Database,
  FileCheck,
  Server,
  UserCheck,
  CheckCircle2,
} from "lucide-react";

export const metadata: Metadata = {
  title: "Privacy Policy · Wisdom Tower Academy",
  description:
    "Institutional privacy policy governing student data protection, academic telemetry, encryption standards, and digital records at Wisdom Tower Academy.",
};

const LAST_REVISED = "12 October 2026";
const EFFECTIVE_PERIOD = "Academic Year 2026/2027";

export default function PrivacyPage() {
  return (
    <div className="relative min-h-[85vh] py-14 md:py-24">
      {/* Background ambient glow */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        <div className="absolute top-12 left-1/2 -translate-x-1/2 w-[42rem] h-96 bg-cyan-500/5 rounded-full blur-3xl" />
        <div className="absolute top-96 right-1/4 w-72 h-72 bg-amber-500/5 rounded-full blur-3xl" />
      </div>

      <div className="relative max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Document Header */}
        <header className="border-b border-white/10 pb-8 mb-10">
          <div className="flex flex-wrap items-center gap-2 mb-3">
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider bg-cyan-500/10 text-cyan-300 border border-cyan-400/20">
              <Shield className="w-3.5 h-3.5" />
              Student Data Protection
            </span>
            <span className="text-xs text-wisdom-muted">
              Effective: {EFFECTIVE_PERIOD}
            </span>
          </div>

          <h1 className="font-display text-3xl sm:text-4xl md:text-5xl font-extrabold text-white tracking-tight mb-4">
            Institutional Privacy Policy
          </h1>

          <p className="text-base sm:text-lg text-slate-300 leading-relaxed max-w-3xl">
            Wisdom Tower Academy is dedicated to safeguarding the privacy, personal records, and academic integrity
            of every enrolled learner. This charter articulates our rigorous protocols for data stewardship,
            encryption safeguards, and the protection of student telemetry.
          </p>

          <div className="mt-4 flex flex-wrap items-center gap-4 text-xs text-wisdom-muted">
            <span>Policy version: 3.2</span>
            <span>•</span>
            <span>Last revised: {LAST_REVISED}</span>
            <span>•</span>
            <Link href="/terms" className="text-cyan-300 hover:underline">
              Read Companion Terms of Service →
            </Link>
          </div>
        </header>

        {/* Key Guarantees Box */}
        <div className="mb-12 rounded-2xl border border-cyan-400/20 bg-cyan-500/[0.04] p-5 sm:p-6">
          <h2 className="text-sm font-bold uppercase tracking-wider text-cyan-300 flex items-center gap-2 mb-3">
            <Lock className="w-4 h-4" />
            Our Academic Privacy Guarantees
          </h2>
          <div className="grid sm:grid-cols-2 gap-4 text-xs sm:text-sm text-slate-300">
            <div className="flex items-start gap-2.5">
              <CheckCircle2 className="w-4 h-4 text-cyan-400 shrink-0 mt-0.5" />
              <span><strong>Zero Data Monetization:</strong> We never sell, lease, or broker student personal or academic data to third-party ad networks.</span>
            </div>
            <div className="flex items-start gap-2.5">
              <CheckCircle2 className="w-4 h-4 text-cyan-400 shrink-0 mt-0.5" />
              <span><strong>End-to-End Encryption:</strong> TLS 1.3 protocol protects all transit data; cryptographic vaults secure student profiles at rest.</span>
            </div>
            <div className="flex items-start gap-2.5">
              <CheckCircle2 className="w-4 h-4 text-cyan-400 shrink-0 mt-0.5" />
              <span><strong>Student-Centric Ownership:</strong> Your progress records, bookmarks, and quiz histories belong to your academic journey.</span>
            </div>
            <div className="flex items-start gap-2.5">
              <CheckCircle2 className="w-4 h-4 text-cyan-400 shrink-0 mt-0.5" />
              <span><strong>Underage Scholar Protections:</strong> Dedicated safety guardrails for secondary school learners in Grades 9–12.</span>
            </div>
          </div>
        </div>

        {/* Detailed Sections */}
        <div className="space-y-12 text-slate-300 text-sm sm:text-[15px] leading-relaxed">
          {/* Section 1 */}
          <section id="section-1" className="scroll-mt-20">
            <h2 className="text-xl sm:text-2xl font-display font-bold text-white mb-3 flex items-center gap-2.5">
              <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-white/10 text-xs font-mono text-cyan-300">
                01
              </span>
              Institutional Scope & Controller Identity
            </h2>
            <div className="space-y-3 text-slate-300/90 pl-9">
              <p>
                Wisdom Tower Academy (“Wisdom Tower”, “we”, “our”, or “us”) operates the primary educational platform accessible
                via wisdomtower.et and its sub-domains. For the purposes of applicable data protection legislation and institutional
                standards, Wisdom Tower Academy is the official Data Controller responsible for the processing and protection
                of your personal and academic information.
              </p>
              <p>
                If you have questions regarding this charter or wish to exercise statutory data subject rights, you may reach our Data
                Protection Desk directly at:{" "}
                <a href="mailto:support@wisdomtower.tech" className="text-cyan-300 hover:underline">
                  support@wisdomtower.tech
                </a>
              </p>
            </div>
          </section>

          {/* Section 2 */}
          <section id="section-2" className="scroll-mt-20">
            <h2 className="text-xl sm:text-2xl font-display font-bold text-white mb-3 flex items-center gap-2.5">
              <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-white/10 text-xs font-mono text-cyan-300">
                02
              </span>
              Categories of Information We Collect
            </h2>
            <div className="space-y-4 text-slate-300/90 pl-9">
              <p>
                We collect only the essential data necessary to deliver personalized, uninterrupted educational services across our hubs:
              </p>

              <div className="space-y-3">
                <div className="rounded-xl border border-white/8 bg-white/[0.02] p-4">
                  <h3 className="font-semibold text-white text-sm mb-1">A. Student Identity & Authentication Records</h3>
                  <p className="text-xs sm:text-sm text-slate-300/90">
                    Full name, email address, password hash (one-way salted via bcrypt), and unique profile identifiers. When registering
                    via Google OAuth, we receive authorized profile tokens (name, primary email, profile picture) without accessing personal
                    passwords or external Google account details.
                  </p>
                </div>

                <div className="rounded-xl border border-white/8 bg-white/[0.02] p-4">
                  <h3 className="font-semibold text-white text-sm mb-1">B. Academic Telemetry & Performance Data</h3>
                  <p className="text-xs sm:text-sm text-slate-300/90">
                    Chapter completion timestamps, question-bank solution logs, mock exam scores, streak frequencies, study planner
                    schedules, flashcard confidence ratings, and voluntary submissions to department or field leaderboards.
                  </p>
                </div>

                <div className="rounded-xl border border-white/8 bg-white/[0.02] p-4">
                  <h3 className="font-semibold text-white text-sm mb-1">C. Enrollment & Financial Verification Data</h3>
                  <p className="text-xs sm:text-sm text-slate-300/90">
                    Package identifiers, payment methods selected (Telebirr, CBE, Abyssinia, Awash), transaction reference codes,
                    student contact phone numbers submitted for payment reconciliation, and optional payment receipt image uploads.
                    <span className="text-amber-200"> Note: We never handle, store, or solicit private banking PINs or mobile wallet passwords.</span>
                  </p>
                </div>

                <div className="rounded-xl border border-white/8 bg-white/[0.02] p-4">
                  <h3 className="font-semibold text-white text-sm mb-1">D. Device, Cache & Network Telemetry</h3>
                  <p className="text-xs sm:text-sm text-slate-300/90">
                    IP address (coarse geographic region), browser user agent, operating system, and localized IndexedDB cache storage
                    used strictly to preserve offline study capability during internet interruptions in Ethiopia.
                  </p>
                </div>
              </div>
            </div>
          </section>

          {/* Section 3 */}
          <section id="section-3" className="scroll-mt-20">
            <h2 className="text-xl sm:text-2xl font-display font-bold text-white mb-3 flex items-center gap-2.5">
              <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-white/10 text-xs font-mono text-cyan-300">
                03
              </span>
              Lawful Grounds for Processing
            </h2>
            <div className="space-y-3 text-slate-300/90 pl-9">
              <p>
                In alignment with international privacy standards and Ethiopian legal frameworks, our processing activities rest upon well-defined legal bases:
              </p>
              <ul className="list-disc pl-5 space-y-2">
                <li>
                  <strong>Contractual Necessity:</strong> Processing required to authenticate your enrollment, unlock purchased packages,
                  record completed chapters, and calculate exam scores.
                </li>
                <li>
                  <strong>Legitimate Institutional Interests:</strong> Enhancing pedagogical curriculum quality, diagnosing platform errors,
                  safeguarding against fraudulent bank reference submissions, and preventing illegal scraping of copyrighted material.
                </li>
                <li>
                  <strong>Explicit Student Consent:</strong> Optional participation in field leaderboards, scholarship notification updates,
                  or voluntary academic feedback surveys.
                </li>
              </ul>
            </div>
          </section>

          {/* Section 4 */}
          <section id="section-4" className="scroll-mt-20">
            <h2 className="text-xl sm:text-2xl font-display font-bold text-white mb-3 flex items-center gap-2.5">
              <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-white/10 text-xs font-mono text-cyan-300">
                04
              </span>
              Infrastructure Safeguards & Cryptographic Protocols
            </h2>
            <div className="space-y-3 text-slate-300/90 pl-9">
              <p>
                Wisdom Tower Academy enforces rigorous technological barriers to shield student records from unauthorized access, alteration, or disclosure:
              </p>
              <ul className="list-disc pl-5 space-y-1.5">
                <li>
                  <strong>Transit Encryption:</strong> All HTTP traffic is forcefully upgraded to Transport Layer Security (TLS 1.3 / HTTPS),
                  ensuring eavesdropping protection even on public Wi-Fi or cellular networks.
                </li>
                <li>
                  <strong>Database Vault:</strong> Production database clusters are maintained via Supabase with row-level security (RLS)
                  policies ensuring students can only access their own private records.
                </li>
                <li>
                  <strong>Restricted Administrative Access:</strong> Academic registrars and staff operate under strict Role-Based Access
                  Controls (RBAC) governed by multi-factor authentication and detailed audit logging.
                </li>
              </ul>
            </div>
          </section>

          {/* Section 5 */}
          <section id="section-5" className="scroll-mt-20">
            <h2 className="text-xl sm:text-2xl font-display font-bold text-white mb-3 flex items-center gap-2.5">
              <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-white/10 text-xs font-mono text-cyan-300">
                05
              </span>
              Third-Party Sub-processors & Service Partners
            </h2>
            <div className="space-y-3 text-slate-300/90 pl-9">
              <p>
                We partner only with enterprise-grade sub-processors bound by strict confidentiality and data-processing covenants:
              </p>
              <ul className="list-disc pl-5 space-y-1.5">
                <li><strong>Supabase:</strong> Cloud database infrastructure, user authentication, and encrypted receipt asset storage.</li>
                <li><strong>Google Cloud Platform & Vercel:</strong> Edge computing servers, static asset delivery, and DDoS mitigation.</li>
                <li><strong>Payment Verification Gateways:</strong> Integration services (such as Verify.et) for automated bank reference validation.</li>
                <li><strong>Google Gemini AI:</strong> Server-side AI processing for &ldquo;Explain with AI&rdquo; question walkthroughs (prompts contain only academic question context; no personal student identity is transferred).</li>
              </ul>
            </div>
          </section>

          {/* Section 6 */}
          <section id="section-6" className="scroll-mt-20">
            <h2 className="text-xl sm:text-2xl font-display font-bold text-white mb-3 flex items-center gap-2.5">
              <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-white/10 text-xs font-mono text-cyan-300">
                06
              </span>
              Offline Persistence & Low-Bandwidth Caching
            </h2>
            <div className="space-y-3 text-slate-300/90 pl-9">
              <p>
                Because Ethiopian students frequently face network volatility or high data costs, our platform uses client-side
                Service Workers and browser IndexedDB caches to store downloaded chapter summaries, flashcards, and reading progress locally.
              </p>
              <p>
                This localized data resides entirely on your device and can be cleared at any time through your browser settings or
                via the platform Preferences panel.
              </p>
            </div>
          </section>

          {/* Section 7 */}
          <section id="section-7" className="scroll-mt-20">
            <h2 className="text-xl sm:text-2xl font-display font-bold text-white mb-3 flex items-center gap-2.5">
              <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-white/10 text-xs font-mono text-cyan-300">
                07
              </span>
              Student Rights & Autonomous Data Control
            </h2>
            <div className="space-y-3 text-slate-300/90 pl-9">
              <p>
                Every enrolled student possesses comprehensive rights regarding their personal data:
              </p>
              <ul className="list-disc pl-5 space-y-2">
                <li><strong>Right to Inspect:</strong> View all saved profile details, active package enrollments, and quiz histories inside My Learning and Account Settings.</li>
                <li><strong>Right to Rectify:</strong> Update your name, phone number, and study preferences at any time.</li>
                <li>
                  <strong>Right to Erasure (Right to Be Forgotten):</strong> Delete your account and personal data yourself at any time from the{" "}
                  <Link href="/delete-account" className="text-cyan-300 hover:underline">account deletion page</Link>{" "}
                  or, in the Android app, from Settings &gt; Security. Records of verified payments are kept as described in section 9.
                </li>
                <li><strong>Right to Opt-Out:</strong> Toggle off optional leaderboard visibility to keep your score progress completely private.</li>
              </ul>
            </div>
          </section>

          {/* Section 8 */}
          <section id="section-8" className="scroll-mt-20">
            <h2 className="text-xl sm:text-2xl font-display font-bold text-white mb-3 flex items-center gap-2.5">
              <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-white/10 text-xs font-mono text-cyan-300">
                08
              </span>
              Protection of Secondary School Minors
            </h2>
            <div className="space-y-3 text-slate-300/90 pl-9">
              <p>
                For learners enrolled in our secondary curriculum tracks (Grades 9 through 12) who may be under the age of 18,
                Wisdom Tower Academy enforces protective parameters:
              </p>
              <ul className="list-disc pl-5 space-y-1.5">
                <li>We do not display public contact details or personal social media handles on secondary leaderboards.</li>
                <li>Parents and legal guardians may inspect or request modification of their dependent student&apos;s records by contacting our support desk.</li>
                <li>We provide an entirely educational, ad-free environment without behavioural profiling or commercial solicitations.</li>
              </ul>
            </div>
          </section>

          {/* Section 9 */}
          <section id="section-9" className="scroll-mt-20">
            <h2 className="text-xl sm:text-2xl font-display font-bold text-white mb-3 flex items-center gap-2.5">
              <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-white/10 text-xs font-mono text-cyan-300">
                09
              </span>
              Data Retention Schedules
            </h2>
            <div className="space-y-3 text-slate-300/90 pl-9">
              <p>
                Active student accounts and their associated learning histories are retained throughout the active enrollment period
                to maintain continuous academic records across academic semesters. Transaction records are maintained for a period of
                five (5) years in compliance with Ethiopian tax and commercial bookkeeping statutes. Dormant accounts with no package
                enrollments may be purged after two (2) years of persistent inactivity following prior email notification.
              </p>
            </div>
          </section>

          {/* Section 10 */}
          <section id="section-10" className="scroll-mt-20">
            <h2 className="text-xl sm:text-2xl font-display font-bold text-white mb-3 flex items-center gap-2.5">
              <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-white/10 text-xs font-mono text-cyan-300">
                10
              </span>
              Inquiries, Grievances & Data Protection Officer
            </h2>
            <div className="space-y-3 text-slate-300/90 pl-9">
              <p>
                If you have inquiries concerning this policy or wish to exercise your data subject prerogatives, please submit a formal request:
              </p>
              <div className="rounded-xl border border-white/10 bg-wisdom-card p-4 space-y-1 font-mono text-xs text-slate-300">
                <p className="text-white font-semibold font-sans text-sm mb-1">Wisdom Tower Academy · Data Protection Officer</p>
                <p>Addis Ababa, Ethiopia</p>
                <p>Email: <a href="mailto:support@wisdomtower.tech" className="text-cyan-300 hover:underline">support@wisdomtower.tech</a></p>
                <p>Official Channel: <a href="https://t.me/wisdom_tower2" target="_blank" rel="noopener noreferrer" className="text-amber-300 hover:underline">@wisdom_tower2</a></p>
              </div>
            </div>
          </section>
        </div>

        {/* Footer Navigation */}
        <div className="mt-16 pt-8 border-t border-white/10 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-wisdom-muted">
          <p>© {new Date().getFullYear()} Wisdom Tower Academy. All rights reserved.</p>
          <div className="flex items-center gap-4">
            <Link href="/terms" className="text-cyan-300 hover:underline">
              Terms of Service
            </Link>
            <span>•</span>
            <Link href="/about" className="text-slate-300 hover:underline">
              About the Academy
            </Link>
            <span>•</span>
            <Link href="/contact" className="text-slate-300 hover:underline">
              Contact Support
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
