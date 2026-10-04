import React, { useState, useEffect, useRef } from 'react';
import {
  Menu,
  Bell,
  RotateCw,
  Home,
  BookOpen,
  Package,
  User,
  Settings,
  X,
  ShieldCheck,
  ExternalLink,
  Wifi,
  BatteryCharging,
  Smartphone,
  Info,
  CheckCircle2,
  Clock,
  ArrowLeft,
  Lock,
  Layers,
  Sparkles,
  Play,
  Pause
} from 'lucide-react';

interface TabItem {
  id: string;
  label: string;
  url: string;
  icon: React.ComponentType<{ className?: string }>;
}

const TABS: TabItem[] = [
  { id: 'home', label: 'Home', url: 'https://www.wisdom-tower-academy.live/', icon: Home },
  { id: 'learning', label: 'Learning', url: 'https://www.wisdom-tower-academy.live/learning', icon: BookOpen },
  { id: 'packages', label: 'Packages', url: 'https://www.wisdom-tower-academy.live/packages', icon: Package },
  { id: 'account', label: 'Account', url: 'https://www.wisdom-tower-academy.live/account', icon: User },
  { id: 'settings', label: 'Settings', url: 'https://www.wisdom-tower-academy.live/settings', icon: Settings },
];

export default function App() {
  const [activeTab, setActiveTab] = useState<string>('home');
  const [currentUrl, setCurrentUrl] = useState<string>('https://www.wisdom-tower-academy.live/');
  const [isMenuOpen, setIsMenuOpen] = useState<boolean>(false);
  const [showExitDialog, setShowExitDialog] = useState<boolean>(false);
  const [currentTime, setCurrentTime] = useState<string>('09:41');
  const [studySeconds, setStudySeconds] = useState<number>(1420);
  const [isTimerRunning, setIsTimerRunning] = useState<boolean>(true);
  const [viewMode, setViewMode] = useState<'device' | 'architecture' | 'checklist'>('device');
  const [iframeKey, setIframeKey] = useState<number>(0);
  const iframeRef = useRef<HTMLIFrameElement>(null);

  // Status bar live clock
  useEffect(() => {
    const updateTime = () => {
      const now = new Date();
      setCurrentTime(
        now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false })
      );
    };
    updateTime();
    const interval = setInterval(updateTime, 30000);
    return () => clearInterval(interval);
  }, []);

  // Study timer ticker
  useEffect(() => {
    if (!isTimerRunning) return;
    const timer = setInterval(() => {
      setStudySeconds((prev) => prev + 1);
    }, 1000);
    return () => clearInterval(timer);
  }, [isTimerRunning]);

  const formatTimer = (totalSec: number) => {
    const m = Math.floor(totalSec / 60);
    const s = totalSec % 60;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  const handleNavClick = (tab: TabItem) => {
    setActiveTab(tab.id);
    setCurrentUrl(tab.url);
    setIsMenuOpen(false);
  };

  const handleNotificationClick = () => {
    // Per ARCHITECTURE.md: notification bell navigates to /notifications only, bottom nav remains current
    setCurrentUrl('https://www.wisdom-tower-academy.live/notifications');
  };

  const handleReload = () => {
    setIframeKey((prev) => prev + 1);
  };

  return (
    <div className="min-h-screen bg-[#060911] text-slate-100 flex flex-col items-center justify-between p-2 sm:p-4 select-none font-sans">
      {/* Top Header / Environment Ribbon */}
      <header className="w-full max-w-5xl flex flex-wrap items-center justify-between gap-3 mb-2 px-3 py-2 bg-[#0F172A]/80 backdrop-blur-md border border-slate-800 rounded-2xl shadow-xl">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-cyan-500 to-blue-600 flex items-center justify-center shadow-lg shadow-cyan-500/20">
            <span className="font-extrabold text-white text-base tracking-tighter">WT</span>
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-sm sm:text-base font-bold text-white tracking-tight">Wisdom Tower Academy</h1>
              <span className="text-[10px] font-semibold uppercase px-2 py-0.5 rounded-full bg-cyan-500/10 text-cyan-400 border border-cyan-500/30">
                Android App Shell
              </span>
            </div>
            <p className="text-[11px] text-slate-400 flex items-center gap-1.5">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
              Dev server active · Port 3000 · Live WebView Bridge
            </p>
          </div>
        </div>

        {/* View Switcher */}
        <div className="flex items-center gap-1 bg-[#1E293B] p-1 rounded-xl border border-slate-700/60 text-xs">
          <button
            onClick={() => setViewMode('device')}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg font-medium transition-all ${
              viewMode === 'device'
                ? 'bg-cyan-500 text-slate-950 font-bold shadow-md shadow-cyan-500/20'
                : 'text-slate-300 hover:text-white hover:bg-slate-800'
            }`}
          >
            <Smartphone className="w-3.5 h-3.5" />
            <span>Interactive Device</span>
          </button>
          <button
            onClick={() => setViewMode('architecture')}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg font-medium transition-all ${
              viewMode === 'architecture'
                ? 'bg-cyan-500 text-slate-950 font-bold shadow-md shadow-cyan-500/20'
                : 'text-slate-300 hover:text-white hover:bg-slate-800'
            }`}
          >
            <Layers className="w-3.5 h-3.5" />
            <span>Architecture & CI</span>
          </button>
          <button
            onClick={() => setViewMode('checklist')}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg font-medium transition-all ${
              viewMode === 'checklist'
                ? 'bg-cyan-500 text-slate-950 font-bold shadow-md shadow-cyan-500/20'
                : 'text-slate-300 hover:text-white hover:bg-slate-800'
            }`}
          >
            <ShieldCheck className="w-3.5 h-3.5" />
            <span>Play Store Readiness</span>
          </button>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="w-full max-w-5xl flex-1 flex items-center justify-center">
        {viewMode === 'device' && (
          <div className="relative w-full max-w-[420px] h-[830px] max-h-[calc(100vh-100px)] bg-[#060B15] rounded-[42px] border-[4px] border-slate-800 shadow-[0_25px_70px_rgba(0,0,0,0.85),0_0_0_1px_rgba(34,224,255,0.2)] flex flex-col overflow-hidden">
            {/* Top speaker grill & camera notch */}
            <div className="absolute top-2 left-1/2 -translate-x-1/2 w-28 h-4 bg-slate-900 rounded-full z-[60] flex items-center justify-center">
              <div className="w-12 h-1 bg-slate-700/60 rounded-full" />
              <div className="w-2.5 h-2.5 rounded-full bg-slate-800 border border-slate-700/40 ml-2" />
            </div>

            {/* Android System Status Bar (solid deep navy background #060B15) */}
            <div className="h-8 bg-[#060B15] flex items-center justify-between px-6 text-[11px] font-semibold text-slate-300 pt-1.5 z-50 select-none">
              <span>{currentTime}</span>
              <div className="flex items-center gap-2">
                <Wifi className="w-3.5 h-3.5 text-slate-300" />
                <span className="text-[10px] font-bold text-[#22E0FF]">5G</span>
                <div className="flex items-center gap-0.5">
                  <span className="text-[10px]">98%</span>
                  <BatteryCharging className="w-3.5 h-3.5 text-emerald-400" />
                </div>
              </div>
            </div>

            {/* Native Fixed Top Bar (#060B15) */}
            <div className="h-14 bg-[#060B15] border-b border-[#22E0FF]/15 flex items-center justify-between px-3 z-50">
              <button
                onClick={() => setIsMenuOpen(!isMenuOpen)}
                className="w-10 h-10 rounded-xl flex items-center justify-center text-[#22E0FF] hover:bg-[#22E0FF]/10 active:scale-95 transition-all"
                title="Open menu"
                aria-label="Open menu"
              >
                <Menu className="w-5 h-5" />
              </button>

              <div className="flex items-center gap-2">
                <div className="w-2 h-2 rounded-full bg-[#22E0FF] shadow-[0_0_8px_#22E0FF]" />
                <span className="font-bold text-white text-base tracking-tight">Wisdom Tower Academy</span>
              </div>

              <div className="flex items-center gap-1">
                <button
                  onClick={handleReload}
                  className="w-9 h-9 rounded-xl flex items-center justify-center text-slate-400 hover:text-[#22E0FF] hover:bg-[#22E0FF]/10 active:scale-95 transition-all"
                  title="Reload web view"
                >
                  <RotateCw className="w-4 h-4" />
                </button>
                <button
                  onClick={handleNotificationClick}
                  className="w-9 h-9 rounded-xl flex items-center justify-center text-[#22E0FF] hover:bg-[#22E0FF]/10 active:scale-95 transition-all relative"
                  title="View notifications"
                >
                  <Bell className="w-4 h-4" />
                  <span className="absolute top-2 right-2 w-2 h-2 rounded-full bg-[#22E0FF] ring-2 ring-[#060B15]" />
                </button>
              </div>
            </div>

            {/* Study Floating Timer Pill */}
            <div className="absolute top-24 right-4 z-40 flex items-center gap-2 bg-[#0C1424]/90 backdrop-blur-md border border-[#22E0FF]/40 rounded-full px-3 py-1 shadow-lg text-xs font-bold text-[#22E0FF]">
              <button
                onClick={() => setIsTimerRunning(!isTimerRunning)}
                className="text-emerald-400 hover:text-emerald-300"
                title={isTimerRunning ? 'Pause timer' : 'Resume timer'}
              >
                {isTimerRunning ? <Pause className="w-3 h-3" /> : <Play className="w-3 h-3" />}
              </button>
              <Clock className="w-3 h-3 text-[#22E0FF]" />
              <span className="tabular-nums font-mono text-[11px]">{formatTimer(studySeconds)}</span>
            </div>

            {/* Compact Branded Card Menu Dialog */}
            {isMenuOpen && (
              <div
                className="absolute inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-start justify-start p-4 pt-16"
                onClick={() => setIsMenuOpen(false)}
              >
                <div
                  className="w-72 bg-[#0C1424] border border-[#22E0FF]/25 rounded-3xl p-5 shadow-2xl flex flex-col gap-4 animate-in fade-in zoom-in-95 duration-150"
                  onClick={(e) => e.stopPropagation()}
                >
                  <div className="flex items-center justify-between pb-3 border-b border-[#22E0FF]/15">
                    <div className="flex items-center gap-2.5">
                      <div className="w-8 h-8 rounded-lg bg-[#22E0FF]/15 border border-[#22E0FF]/30 flex items-center justify-center font-bold text-[#22E0FF] text-xs">
                        WT
                      </div>
                      <div>
                        <p className="text-sm font-bold text-white">Wisdom Tower</p>
                        <p className="text-[10.5px] text-[#22E0FF]">Academy Services</p>
                      </div>
                    </div>
                    <button
                      onClick={() => setIsMenuOpen(false)}
                      className="text-slate-400 hover:text-white p-1 rounded-lg"
                    >
                      <X className="w-5 h-5" />
                    </button>
                  </div>

                  <div className="space-y-1 text-sm">
                    <button
                      onClick={() => {
                        setCurrentUrl('https://www.wisdom-tower-academy.live/about');
                        setIsMenuOpen(false);
                      }}
                      className="w-full text-left px-3 py-2 rounded-xl hover:bg-[#22E0FF]/10 text-slate-200 hover:text-[#22E0FF] font-medium transition-all text-xs"
                    >
                      About Academy
                    </button>
                    <button
                      onClick={() => {
                        setCurrentUrl('https://www.wisdom-tower-academy.live/contact');
                        setIsMenuOpen(false);
                      }}
                      className="w-full text-left px-3 py-2 rounded-xl hover:bg-[#22E0FF]/10 text-slate-200 hover:text-[#22E0FF] font-medium transition-all text-xs"
                    >
                      Contact & Support
                    </button>
                    <button
                      onClick={() => {
                        setCurrentUrl('https://www.wisdom-tower-academy.live/faq');
                        setIsMenuOpen(false);
                      }}
                      className="w-full text-left px-3 py-2 rounded-xl hover:bg-[#22E0FF]/10 text-slate-200 hover:text-[#22E0FF] font-medium transition-all text-xs"
                    >
                      Frequently Asked Questions
                    </button>
                    <button
                      onClick={() => {
                        setCurrentUrl('https://www.wisdom-tower-academy.live/privacy');
                        setIsMenuOpen(false);
                      }}
                      className="w-full text-left px-3 py-2 rounded-xl hover:bg-[#22E0FF]/10 text-slate-200 hover:text-[#22E0FF] font-medium transition-all text-xs"
                    >
                      Privacy Policy
                    </button>
                    <button
                      onClick={() => {
                        setCurrentUrl('https://www.wisdom-tower-academy.live/terms');
                        setIsMenuOpen(false);
                      }}
                      className="w-full text-left px-3 py-2 rounded-xl hover:bg-[#22E0FF]/10 text-slate-200 hover:text-[#22E0FF] font-medium transition-all text-xs"
                    >
                      Terms of Service
                    </button>
                  </div>

                  <div className="pt-2 border-t border-[#22E0FF]/10 text-center">
                    <p className="text-[10px] text-slate-500">Wisdom Tower Academy • 2026</p>
                  </div>
                </div>
              </div>
            )}

            {/* Main WebView Window */}
            <div className="flex-1 relative bg-[#060B15] overflow-hidden">
              <iframe
                key={iframeKey}
                ref={iframeRef}
                src={currentUrl}
                title="Wisdom Tower Academy"
                className="w-full h-full border-none bg-[#060B15]"
                sandbox="allow-scripts allow-same-origin allow-forms allow-popups allow-downloads"
              />
            </div>

            {/* Native 5-Tab Bottom Navigation Bar (#060B15) */}
            <div className="h-16 bg-[#060B15] border-t border-[#22E0FF]/15 flex items-center justify-around px-2 z-50">
              {TABS.map((tab) => {
                const IconComponent = tab.icon;
                const isActive = activeTab === tab.id;
                return (
                  <button
                    key={tab.id}
                    onClick={() => handleNavClick(tab)}
                    className={`flex-1 flex flex-col items-center justify-center gap-1 h-12 rounded-2xl mx-0.5 transition-all duration-200 ${
                      isActive
                        ? 'text-[#22E0FF] bg-[#22E0FF]/15 border border-[#22E0FF]/35 font-bold shadow-sm'
                        : 'text-slate-400 hover:text-slate-200'
                    }`}
                  >
                    <IconComponent className={`w-4.5 h-4.5 ${isActive ? 'stroke-[2.5]' : 'stroke-[1.8]'}`} />
                    <span className="text-[10.5px] tracking-tight">{tab.label}</span>
                  </button>
                );
              })}
            </div>

            {/* Android Navigation Gesture Pill */}
            <div className="h-3.5 bg-[#060B15] flex items-center justify-center z-50 pb-1">
              <div className="w-32 h-1 bg-slate-600/80 rounded-full cursor-pointer hover:bg-slate-400 transition-colors" />
            </div>
          </div>
        )}

        {/* Architecture & CI View */}
        {viewMode === 'architecture' && (
          <div className="w-full max-w-3xl bg-[#0F172A] border border-slate-800 rounded-3xl p-6 shadow-2xl space-y-6">
            <div className="flex items-center justify-between border-b border-slate-800 pb-4">
              <div>
                <h2 className="text-xl font-bold text-white flex items-center gap-2">
                  <Layers className="w-5 h-5 text-cyan-400" />
                  System Architecture & Cloud Flow
                </h2>
                <p className="text-xs text-slate-400 mt-1">
                  How the live web portal, Android APK, and automated CI/CD work together seamlessly.
                </p>
              </div>
              <span className="px-3 py-1 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
                100% Intact
              </span>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {/* Web Portal */}
              <div className="bg-[#1E293B]/70 border border-slate-700/60 rounded-2xl p-4 space-y-2">
                <div className="flex items-center justify-between">
                  <h3 className="font-bold text-white text-sm">Website (Source of Truth)</h3>
                  <a
                    href="https://wisdom-tower-academy.live"
                    target="_blank"
                    rel="noreferrer"
                    className="text-cyan-400 hover:underline text-xs flex items-center gap-1"
                  >
                    Visit <ExternalLink className="w-3 h-3" />
                  </a>
                </div>
                <p className="text-xs text-slate-300">
                  Hosted on Vercel. Controls all authentication, course packages, user accounts, Telegram payment verifications, and lecture materials.
                </p>
                <div className="pt-2 text-[11px] text-slate-400 space-y-1">
                  <p>✓ All changes on the web portal reflect automatically in the Android app.</p>
                  <p>✓ Environment variables managed securely in your Vercel Dashboard.</p>
                </div>
              </div>

              {/* Android Native Shell */}
              <div className="bg-[#1E293B]/70 border border-slate-700/60 rounded-2xl p-4 space-y-2">
                <h3 className="font-bold text-white text-sm">Native Android Shell (`com.wisdomtower.academy`)</h3>
                <p className="text-xs text-slate-300">
                  Native Jetpack Compose shell providing hardware integration, status bar padding, local PDF caching, and FLAG_SECURE enforcement.
                </p>
                <div className="pt-2 text-[11px] text-slate-400 space-y-1">
                  <p>✓ Private sandboxed PDF cache via `OfflineVault.kt`</p>
                  <p>✓ Screenshot & video capture prevention active</p>
                </div>
              </div>
            </div>

            {/* FCM Notifications Note */}
            <div className="bg-cyan-950/30 border border-cyan-800/40 rounded-2xl p-4 space-y-2">
              <div className="flex items-center gap-2 text-cyan-400 font-bold text-sm">
                <Bell className="w-4 h-4" />
                <span>Push Notifications & GitHub Secrets</span>
              </div>
              <p className="text-xs text-slate-300 leading-relaxed">
                Your push notification setup with Vercel and GitHub Secrets (`GOOGLE_SERVICES_JSON_BASE64`, `KEYSTORE_BASE64`) is preserved in your repository workflow (`.github/workflows/build-apk.yml`). During GitHub Actions builds, it automatically injects your real secrets and signs the APK with your release key.
              </p>
            </div>
          </div>
        )}

        {/* Play Store Checklist View */}
        {viewMode === 'checklist' && (
          <div className="w-full max-w-3xl bg-[#0F172A] border border-slate-800 rounded-3xl p-6 shadow-2xl space-y-6">
            <div className="flex items-center justify-between border-b border-slate-800 pb-4">
              <div>
                <h2 className="text-xl font-bold text-white flex items-center gap-2">
                  <ShieldCheck className="w-5 h-5 text-cyan-400" />
                  Google Play Store Production Checklist
                </h2>
                <p className="text-xs text-slate-400 mt-1">
                  Summary from `PLAY_STORE_CHECKLIST.md` for releasing to Google Play Console.
                </p>
              </div>
              <span className="text-xs text-cyan-400 font-mono">v1.0.0 (Code 3)</span>
            </div>

            <div className="space-y-3">
              <div className="p-3.5 bg-[#1E293B]/70 border border-slate-700/60 rounded-xl flex items-start gap-3">
                <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0 mt-0.5" />
                <div className="space-y-1">
                  <p className="text-sm font-semibold text-white">Target SDK 36 (Android 16 Readiness)</p>
                  <p className="text-xs text-slate-300">
                    Compiled against SDK 36 with minimum SDK 24, fully compliant with 2026 Google Play target API rules.
                  </p>
                </div>
              </div>

              <div className="p-3.5 bg-[#1E293B]/70 border border-slate-700/60 rounded-xl flex items-start gap-3">
                <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0 mt-0.5" />
                <div className="space-y-1">
                  <p className="text-sm font-semibold text-white">12-Tester Closed Test Requirement</p>
                  <p className="text-xs text-slate-300">
                    For personal accounts created after Nov 2023, Google requires 12 opt-in testers continuously active for 14 days before applying for production release.
                  </p>
                </div>
              </div>

              <div className="p-3.5 bg-[#1E293B]/70 border border-slate-700/60 rounded-xl flex items-start gap-3">
                <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0 mt-0.5" />
                <div className="space-y-1">
                  <p className="text-sm font-semibold text-white">Data Safety & Privacy Policy</p>
                  <p className="text-xs text-slate-300">
                    Declare no location data collected. Offline PDF vault uses private sandbox (`getFilesDir()`). Privacy policy linked at `/privacy`.
                  </p>
                </div>
              </div>
            </div>
          </div>
        )}
      </main>

      {/* Footer */}
      <footer className="w-full max-w-5xl mt-2 px-3 py-2 flex flex-wrap items-center justify-between gap-2 text-xs text-slate-400 border-t border-slate-800/80">
        <div className="flex items-center gap-2">
          <span className="w-2 h-2 rounded-full bg-cyan-400 shadow-[0_0_6px_#00E5FF]" />
          <span>Wisdom Tower Academy App</span>
          <span className="text-slate-600">·</span>
          <span>Package: <code className="text-cyan-400 font-mono text-[11px]">com.wisdomtower.academy</code></span>
        </div>
        <div className="flex items-center gap-3">
          <span>GitHub CI/CD: <strong className="text-slate-200">Preserved</strong></span>
          <span className="text-slate-600">·</span>
          <span>Vercel Integration: <strong className="text-slate-200">Active</strong></span>
        </div>
      </footer>
    </div>
  );
}
