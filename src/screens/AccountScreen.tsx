import React from 'react';
import { User, Award, Flame, BookOpen, HardDriveDownload, Send, ExternalLink, CheckCircle2 } from 'lucide-react';
import { COURSE_PACKAGES } from '../data/courses';
import { offlineVault } from '../services/offlineVault';

interface AccountScreenProps {
  enrolledPackageIds: Set<string>;
  onNavigateToTab: (tab: 'learning' | 'packages' | 'settings') => void;
}

export const AccountScreen: React.FC<AccountScreenProps> = ({
  enrolledPackageIds,
  onNavigateToTab,
}) => {
  const vaultItems = offlineVault.getStoredItems();
  const enrolledList = COURSE_PACKAGES.filter(p => enrolledPackageIds.has(p.id));

  return (
    <div className="pb-24 pt-3 max-w-4xl mx-auto px-4 space-y-6 select-none">
      {/* Student Profile Card */}
      <div className="rounded-3xl bg-gradient-to-br from-[#1E293B] to-[#0F172A] border border-cyan-500/25 p-6 shadow-xl flex flex-col sm:flex-row items-center sm:items-start gap-5 text-center sm:text-left">
        <div className="w-20 h-20 rounded-2xl bg-gradient-to-tr from-cyan-500 to-indigo-500 p-0.5 shadow-lg flex-shrink-0">
          <div className="w-full h-full rounded-[14px] bg-[#0F172A] flex items-center justify-center text-cyan-300">
            <User className="w-10 h-10" />
          </div>
        </div>

        <div className="flex-1">
          <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider bg-emerald-500/15 text-emerald-400 border border-emerald-500/30 mb-1">
            <CheckCircle2 className="w-3 h-3" />
            Verified Student
          </div>
          <h2 className="text-xl sm:text-2xl font-bold text-white">Dawit Mekonnen</h2>
          <p className="text-xs text-slate-400 mt-0.5">
            Student ID: <span className="font-mono text-cyan-300">WTA-2026-ET789</span> • Addis Ababa University
          </p>

          {/* Quick stats chips */}
          <div className="mt-4 flex flex-wrap items-center justify-center sm:justify-start gap-2.5">
            <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-900 border border-slate-800 text-xs font-semibold text-amber-400">
              <Flame className="w-4 h-4 fill-amber-400" />
              <span>14 Day Streak</span>
            </div>
            <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-900 border border-slate-800 text-xs font-semibold text-cyan-400">
              <BookOpen className="w-4 h-4" />
              <span>{enrolledList.length} Enrolled</span>
            </div>
            <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-900 border border-slate-800 text-xs font-semibold text-emerald-400">
              <HardDriveDownload className="w-4 h-4" />
              <span>{vaultItems.length} in Vault</span>
            </div>
          </div>
        </div>
      </div>

      {/* Enrolled Courses / Packages */}
      <div>
        <div className="flex items-center justify-between mb-3 px-1">
          <h3 className="text-base font-bold text-white">Active Enrollments</h3>
          <button
            type="button"
            onClick={() => onNavigateToTab('packages')}
            className="text-xs font-semibold text-[#00E5FF] hover:underline cursor-pointer"
          >
            Add More
          </button>
        </div>

        <div className="space-y-3">
          {enrolledList.length > 0 ? (
            enrolledList.map((pkg) => (
              <div
                key={pkg.id}
                className="p-4 rounded-2xl bg-[#1E293B] border border-slate-800 flex items-center justify-between"
              >
                <div>
                  <span className="text-[10px] font-bold text-cyan-400 uppercase tracking-wider">
                    {pkg.category} Package
                  </span>
                  <h4 className="text-sm font-bold text-white mt-0.5">{pkg.title}</h4>
                  <p className="text-xs text-slate-400 mt-0.5">Full access to 27 modules, exams & flashcards</p>
                </div>
                <button
                  type="button"
                  onClick={() => onNavigateToTab('learning')}
                  className="px-3 py-1.5 rounded-xl bg-cyan-500/15 text-[#00E5FF] hover:bg-cyan-500/25 text-xs font-bold transition-all cursor-pointer flex-shrink-0"
                >
                  Continue
                </button>
              </div>
            ))
          ) : (
            <div className="p-6 rounded-2xl bg-[#1E293B]/40 border border-dashed border-slate-800 text-center">
              <p className="text-sm text-slate-400">No active course packages yet.</p>
              <button
                type="button"
                onClick={() => onNavigateToTab('packages')}
                className="mt-3 px-4 py-2 rounded-xl bg-[#00E5FF] text-[#0F172A] font-bold text-xs hover:bg-[#33ebff] transition-all cursor-pointer"
              >
                Browse Ethiopian Curriculum Packages
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Official Telegram Community & Support */}
      <div className="rounded-2xl bg-[#1E293B] border border-slate-800 p-5">
        <div className="flex items-start justify-between gap-4">
          <div className="flex items-start gap-3">
            <div className="w-10 h-10 rounded-xl bg-sky-500/20 text-sky-400 flex items-center justify-center flex-shrink-0">
              <Send className="w-5 h-5" />
            </div>
            <div>
              <h4 className="text-sm font-bold text-white">Join Telegram Channel & Discussion</h4>
              <p className="text-xs text-slate-400 mt-1 leading-relaxed">
                Connect with thousands of fellow Ethiopian freshman & remedial students, receive daily exam solutions, and talk with tutors.
              </p>
            </div>
          </div>
          <a
            href="https://t.me/wisdomtower"
            target="_blank"
            rel="noopener noreferrer"
            className="px-3.5 py-2 rounded-xl bg-sky-500 text-white font-bold text-xs hover:bg-sky-400 transition-colors flex items-center gap-1.5 flex-shrink-0"
          >
            <span>@wisdomtower</span>
            <ExternalLink className="w-3.5 h-3.5" />
          </a>
        </div>
      </div>
    </div>
  );
};
