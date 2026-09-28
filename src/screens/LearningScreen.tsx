import React, { useState } from 'react';
import { Search, Download, Check, BookOpen, Zap, HardDrive, Filter } from 'lucide-react';
import { ALL_BOOK_MODULES } from '../data/courses';
import { BookModule } from '../types';
import { offlineVault } from '../services/offlineVault';
import { FlashcardViewer } from '../components/FlashcardViewer';

interface LearningScreenProps {
  onOpenBook: (module: BookModule) => void;
  onDownloadToVault: (module: BookModule) => void;
  downloadingFileIds: Map<string, number>;
}

export const LearningScreen: React.FC<LearningScreenProps> = ({
  onOpenBook,
  onDownloadToVault,
  downloadingFileIds,
}) => {
  const [activeTab, setActiveTab] = useState<'modules' | 'flashcards'>('modules');
  const [selectedCategory, setSelectedCategory] = useState<string>('All');
  const [searchQuery, setSearchQuery] = useState('');

  const categories = ['All', 'Freshman', 'Natural Science', 'Social Science', 'Engineering', 'Grade 9-12'];

  const filteredModules = ALL_BOOK_MODULES.filter((m) => {
    const matchesCategory = selectedCategory === 'All' || m.category === selectedCategory;
    const matchesSearch =
      m.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      m.description.toLowerCase().includes(searchQuery.toLowerCase()) ||
      m.authorOrDept.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesCategory && matchesSearch;
  });

  return (
    <div className="pb-24 pt-3 max-w-4xl mx-auto px-4 space-y-5">
      {/* Top Toggle: Modules vs Flashcards */}
      <div className="flex bg-slate-900/90 p-1 rounded-2xl border border-slate-800 max-w-sm mx-auto">
        <button
          type="button"
          onClick={() => setActiveTab('modules')}
          className={`flex-1 flex items-center justify-center gap-2 py-2 rounded-xl text-xs sm:text-sm font-bold transition-all cursor-pointer ${
            activeTab === 'modules'
              ? 'bg-[#00E5FF] text-[#0F172A] shadow-md'
              : 'text-slate-400 hover:text-white'
          }`}
        >
          <BookOpen className="w-4 h-4" />
          <span>Modules & Vault ({ALL_BOOK_MODULES.length})</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('flashcards')}
          className={`flex-1 flex items-center justify-center gap-2 py-2 rounded-xl text-xs sm:text-sm font-bold transition-all cursor-pointer ${
            activeTab === 'flashcards'
              ? 'bg-[#00E5FF] text-[#0F172A] shadow-md'
              : 'text-slate-400 hover:text-white'
          }`}
        >
          <Zap className="w-4 h-4" />
          <span>3D Flashcards</span>
        </button>
      </div>

      {activeTab === 'flashcards' ? (
        <div className="pt-2">
          <FlashcardViewer />
        </div>
      ) : (
        <>
          {/* Search and Filters */}
          <div className="space-y-3">
            <div className="relative">
              <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search modules, subjects, or courses…"
                className="w-full pl-10 pr-4 py-2.5 rounded-2xl bg-[#1E293B] border border-slate-800 focus:border-cyan-400 text-sm text-white placeholder-slate-500 focus:outline-none transition-colors"
              />
            </div>

            {/* Horizontal Category pills */}
            <div className="flex items-center gap-2 overflow-x-auto pb-1 no-scrollbar text-xs">
              {categories.map((cat) => (
                <button
                  key={cat}
                  type="button"
                  onClick={() => setSelectedCategory(cat)}
                  className={`px-3.5 py-1.5 rounded-xl font-semibold whitespace-nowrap transition-all cursor-pointer ${
                    selectedCategory === cat
                      ? 'bg-cyan-500/20 text-[#00E5FF] border border-cyan-500/40 shadow-sm'
                      : 'bg-[#1E293B]/60 text-slate-400 hover:text-slate-200 border border-slate-800'
                  }`}
                >
                  {cat}
                </button>
              ))}
            </div>
          </div>

          {/* Module List Count Header */}
          <div className="flex items-center justify-between text-xs text-slate-400 px-1">
            <span>Showing {filteredModules.length} academic modules</span>
            <span className="flex items-center gap-1 text-cyan-400">
              <HardDrive className="w-3.5 h-3.5" />
              Offline Vault Enabled
            </span>
          </div>

          {/* Modules List Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
            {filteredModules.map((m) => {
              const isCached = offlineVault.isPdfCached(m.fileId);
              const downloadPct = downloadingFileIds.get(m.fileId) || downloadingFileIds.get(m.downloadUrl);
              const isDownloading = downloadPct !== undefined;

              return (
                <div
                  key={m.id}
                  className="rounded-2xl bg-[#1E293B] border border-slate-800 p-4 hover:border-cyan-500/40 transition-all flex flex-col justify-between"
                >
                  <div>
                    {/* Badges row */}
                    <div className="flex items-center justify-between gap-2 mb-2">
                      <span className="px-2.5 py-0.5 rounded-md text-[10px] font-bold uppercase tracking-wider bg-cyan-500/10 text-cyan-300 border border-cyan-500/20">
                        {m.category}
                      </span>
                      <span className="text-xs text-slate-400 font-mono tabular-nums">
                        {offlineVault.formatBytes(m.sizeBytes)}
                      </span>
                    </div>

                    <h3 className="font-bold text-white text-base leading-snug">{m.title}</h3>
                    <p className="text-xs text-slate-400 mt-1 line-clamp-2 leading-relaxed">
                      {m.description}
                    </p>
                  </div>

                  {/* Footer actions */}
                  <div className="mt-4 pt-3 border-t border-slate-800/80 flex items-center justify-between">
                    <span className="text-[11px] text-slate-500">
                      {m.pagesCount} pages • MoE
                    </span>

                    <div className="flex items-center gap-2">
                      {isCached ? (
                        <button
                          type="button"
                          onClick={() => onOpenBook(m)}
                          className="px-3 py-1.5 rounded-xl bg-emerald-500/15 hover:bg-emerald-500/25 text-emerald-400 text-xs font-bold transition-all flex items-center gap-1 cursor-pointer"
                        >
                          <Check className="w-3.5 h-3.5" />
                          <span>Open</span>
                        </button>
                      ) : isDownloading ? (
                        <div className="px-3 py-1.5 rounded-xl bg-cyan-500/20 text-cyan-300 text-xs font-mono font-bold flex items-center gap-1.5">
                          <div className="w-3 h-3 rounded-full border-2 border-cyan-400 border-t-transparent animate-spin" />
                          <span>{downloadPct}%</span>
                        </div>
                      ) : (
                        <button
                          type="button"
                          onClick={() => onDownloadToVault(m)}
                          className="px-3 py-1.5 rounded-xl bg-cyan-500/15 hover:bg-cyan-500/25 text-[#00E5FF] text-xs font-bold transition-all flex items-center gap-1 cursor-pointer"
                        >
                          <Download className="w-3.5 h-3.5" />
                          <span>Download</span>
                        </button>
                      )}

                      <button
                        type="button"
                        onClick={() => onOpenBook(m)}
                        className="px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-white text-xs font-medium transition-colors cursor-pointer"
                      >
                        Read
                      </button>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </>
      )}
    </div>
  );
};
