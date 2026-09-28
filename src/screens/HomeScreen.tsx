import React from 'react';
import { BookOpen, Package, Zap, HardDriveDownload, ArrowRight, ShieldCheck, Award, Sparkles, CheckCircle2 } from 'lucide-react';
import { ALL_BOOK_MODULES, COURSE_PACKAGES } from '../data/courses';
import { BookModule } from '../types';
import { offlineVault } from '../services/offlineVault';

interface HomeScreenProps {
  onNavigateToTab: (tab: 'learning' | 'packages' | 'account' | 'settings') => void;
  onOpenBook: (module: BookModule) => void;
}

export const HomeScreen: React.FC<HomeScreenProps> = ({ onNavigateToTab, onOpenBook }) => {
  const featuredModules = ALL_BOOK_MODULES.slice(0, 4);
  const featuredPackage = COURSE_PACKAGES[0];
  const vaultItems = offlineVault.getStoredItems();

  return (
    <div className="pb-24 pt-3 max-w-4xl mx-auto px-4 space-y-6">
      {/* Hero Banner */}
      <div className="relative rounded-3xl bg-gradient-to-br from-[#1E293B] via-[#0F172A] to-[#0A101D] p-6 sm:p-8 border border-cyan-500/25 shadow-xl overflow-hidden">
        {/* Ambient neon light */}
        <div className="absolute top-0 right-0 w-60 h-60 bg-cyan-400/10 rounded-full blur-3xl pointer-events-none" />

        <div className="relative z-10 max-w-lg">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-cyan-500/15 border border-cyan-500/30 text-[#00E5FF] text-xs font-bold uppercase tracking-wider mb-4">
            <Sparkles className="w-3.5 h-3.5" />
            <span>Ethiopian Higher Education Standard</span>
          </div>

          <h1 className="text-2xl sm:text-4xl font-extrabold text-white tracking-tight leading-tight">
            Your Complete Academic Companion for Ethiopia
          </h1>

          <p className="mt-3 text-sm sm:text-base text-slate-300 leading-relaxed">
            Master university freshman courses, remedial exams, and Grade 9–12 STEM subjects with zero margin for error.
          </p>

          <div className="mt-6 flex flex-wrap items-center gap-3">
            <button
              type="button"
              onClick={() => onNavigateToTab('learning')}
              className="px-5 py-3 rounded-2xl bg-[#00E5FF] text-[#0F172A] font-bold text-sm hover:bg-[#33ebff] active:scale-95 transition-all flex items-center gap-2 shadow-lg cursor-pointer"
            >
              <BookOpen className="w-4 h-4" />
              Explore 27 Modules
            </button>
            <button
              type="button"
              onClick={() => onNavigateToTab('packages')}
              className="px-5 py-3 rounded-2xl bg-slate-800/90 text-white font-semibold text-sm border border-slate-700 hover:bg-slate-700 active:scale-95 transition-all flex items-center gap-2 cursor-pointer"
            >
              <Package className="w-4 h-4" />
              View Packages
            </button>
          </div>
        </div>
      </div>

      {/* Offline Vault Status Card */}
      <div className="rounded-2xl bg-[#1E293B]/70 border border-slate-700/60 p-4.5 flex items-center justify-between">
        <div className="flex items-center gap-3.5">
          <div className="w-11 h-11 rounded-xl bg-cyan-500/15 text-[#00E5FF] flex items-center justify-center flex-shrink-0">
            <HardDriveDownload className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-white flex items-center gap-2">
              Offline PDF Vault
              <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500/20 text-emerald-400">
                Active
              </span>
            </h3>
            <p className="text-xs text-slate-400 mt-0.5">
              {vaultItems.length > 0
                ? `${vaultItems.length} modules saved (${offlineVault.formatBytes(offlineVault.getTotalVaultSize())}) • Ready offline`
                : 'Save course books to read anywhere with zero mobile data'}
            </p>
          </div>
        </div>
        <button
          type="button"
          onClick={() => onNavigateToTab('learning')}
          className="px-3.5 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-xs font-semibold text-cyan-300 border border-cyan-500/20 transition-all cursor-pointer flex-shrink-0"
        >
          {vaultItems.length > 0 ? 'Open Vault' : 'Browse'}
        </button>
      </div>

      {/* Quick Action Grid */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        {[
          { title: 'Freshman Modules', desc: 'Anthropology, Logic, Calc', icon: BookOpen, tab: 'learning' as const },
          { title: '3D Flashcards', desc: 'Instant exam recall', icon: Zap, tab: 'learning' as const },
          { title: 'Course Bundles', desc: 'Ethiopian Birr payment', icon: Package, tab: 'packages' as const },
          { title: 'Vault Storage', desc: 'Offline files manager', icon: HardDriveDownload, tab: 'settings' as const },
        ].map((item, idx) => {
          const Icon = item.icon;
          return (
            <button
              key={idx}
              type="button"
              onClick={() => onNavigateToTab(item.tab)}
              className="p-4 rounded-2xl bg-[#1E293B]/50 border border-slate-800 hover:border-cyan-500/30 hover:bg-[#1E293B] transition-all text-left group cursor-pointer"
            >
              <div className="w-9 h-9 rounded-xl bg-cyan-500/10 text-[#00E5FF] flex items-center justify-center mb-2.5 group-hover:scale-110 transition-transform">
                <Icon className="w-4 h-4" />
              </div>
              <h4 className="text-sm font-bold text-white group-hover:text-cyan-300 transition-colors">
                {item.title}
              </h4>
              <p className="text-[11px] text-slate-400 mt-0.5">{item.desc}</p>
            </button>
          );
        })}
      </div>

      {/* Featured Core Modules */}
      <div>
        <div className="flex items-center justify-between mb-3.5 px-1">
          <div>
            <h2 className="text-lg font-bold text-white">Popular Course Modules</h2>
            <p className="text-xs text-slate-400">Pre-seeded with instant offline availability</p>
          </div>
          <button
            type="button"
            onClick={() => onNavigateToTab('learning')}
            className="text-xs font-semibold text-[#00E5FF] hover:underline flex items-center gap-1 cursor-pointer"
          >
            See all 27
            <ArrowRight className="w-3.5 h-3.5" />
          </button>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          {featuredModules.map((m) => {
            const isCached = offlineVault.isPdfCached(m.fileId);
            return (
              <div
                key={m.id}
                className="p-4 rounded-2xl bg-[#1E293B] border border-slate-800 hover:border-cyan-500/40 transition-all flex flex-col justify-between"
              >
                <div>
                  <div className="flex items-center justify-between gap-2 mb-2">
                    <span className="text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-md bg-cyan-500/10 text-cyan-300 border border-cyan-500/20">
                      {m.category}
                    </span>
                    <span className="text-xs text-slate-400 font-mono">
                      {offlineVault.formatBytes(m.sizeBytes)}
                    </span>
                  </div>
                  <h3 className="text-base font-bold text-white">{m.title}</h3>
                  <p className="text-xs text-slate-400 mt-1 line-clamp-2">
                    {m.description}
                  </p>
                </div>

                <div className="mt-4 pt-3 border-t border-slate-800/80 flex items-center justify-between">
                  <span className="text-[11px] text-slate-500">
                    {m.pagesCount} pages • MoE Certified
                  </span>
                  <button
                    type="button"
                    onClick={() => onOpenBook(m)}
                    className="px-3 py-1.5 rounded-xl bg-cyan-500/15 hover:bg-cyan-500/25 text-[#00E5FF] text-xs font-bold transition-all flex items-center gap-1.5 cursor-pointer"
                  >
                    {isCached ? 'Open Cached' : 'Read & Download'}
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Featured Package Spotlight */}
      <div className="rounded-3xl bg-gradient-to-r from-cyan-950/40 to-slate-900 border border-cyan-500/30 p-6 sm:p-7 relative overflow-hidden">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div>
            <span className="px-2.5 py-1 rounded-md text-[10px] font-bold bg-[#00E5FF] text-[#0F172A] uppercase tracking-wider">
              {featuredPackage.badge || 'RECOMMENDED'}
            </span>
            <h3 className="text-xl font-bold text-white mt-2">{featuredPackage.title}</h3>
            <p className="text-xs sm:text-sm text-slate-300 mt-1 max-w-md">
              {featuredPackage.tagline}
            </p>
            <div className="mt-3 flex items-center gap-3 text-xs text-cyan-300 font-medium">
              <span className="flex items-center gap-1">
                <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
                All 27 Modules Included
              </span>
              <span>•</span>
              <span>1,200+ Flashcards</span>
            </div>
          </div>

          <div className="flex flex-col sm:items-end flex-shrink-0">
            <div className="text-2xl font-extrabold text-[#00E5FF]">
              {featuredPackage.priceETB} ETB
            </div>
            <div className="text-xs text-slate-500 line-through">
              {featuredPackage.originalPriceETB} ETB
            </div>
            <button
              type="button"
              onClick={() => onNavigateToTab('packages')}
              className="mt-3 px-5 py-2.5 rounded-xl bg-[#00E5FF] text-[#0F172A] font-bold text-xs sm:text-sm hover:bg-[#33ebff] transition-all cursor-pointer shadow-md"
            >
              Enroll Now
            </button>
          </div>
        </div>
      </div>

      {/* Platform trust pillars */}
      <div className="grid grid-cols-3 gap-3 text-center py-2 border-t border-slate-800">
        <div>
          <div className="text-base sm:text-xl font-extrabold text-white">14,000+</div>
          <div className="text-[11px] text-slate-400 mt-0.5">Ethiopian Students</div>
        </div>
        <div>
          <div className="text-base sm:text-xl font-extrabold text-[#00E5FF]">27 Modules</div>
          <div className="text-[11px] text-slate-400 mt-0.5">Pre-seeded & Cached</div>
        </div>
        <div>
          <div className="text-base sm:text-xl font-extrabold text-emerald-400">100% Offline</div>
          <div className="text-[11px] text-slate-400 mt-0.5">Private App Storage</div>
        </div>
      </div>
    </div>
  );
};
