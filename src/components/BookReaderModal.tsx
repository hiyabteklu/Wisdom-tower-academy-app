import React, { useState } from 'react';
import { X, Download, Check, BookOpen, ChevronLeft, ChevronRight, Bookmark } from 'lucide-react';
import { BookModule } from '../types';
import { offlineVault } from '../services/offlineVault';

interface BookReaderModalProps {
  module: BookModule | null;
  onClose: () => void;
  isCached: boolean;
  onDownloadToVault: (module: BookModule) => void;
  isDownloading: boolean;
  downloadPercent?: number;
}

export const BookReaderModal: React.FC<BookReaderModalProps> = ({
  module,
  onClose,
  isCached,
  onDownloadToVault,
  isDownloading,
  downloadPercent = 0,
}) => {
  const [activeChapterIndex, setActiveChapterIndex] = useState(0);
  const [fontSize, setFontSize] = useState<'sm' | 'md' | 'lg'>('md');
  const [bookmarked, setBookmarked] = useState(false);

  if (!module) return null;

  const currentChapter = module.sampleContent[activeChapterIndex] || module.sampleContent[0];
  const totalChapters = module.sampleContent.length;

  return (
    <div className="fixed inset-0 z-50 flex flex-col bg-[#0F172A] text-slate-100 select-text overflow-hidden animate-fade-in">
      {/* Header bar */}
      <div className="flex items-center justify-between px-4 py-3 bg-[#1E293B] border-b border-slate-700 select-none">
        <div className="flex items-center gap-2 overflow-hidden pr-2">
          <BookOpen className="w-5 h-5 text-[#00E5FF] flex-shrink-0" />
          <div className="overflow-hidden">
            <h2 className="text-sm font-bold text-white truncate">{module.title}</h2>
            <p className="text-[11px] text-slate-400 truncate">
              {module.authorOrDept} • {offlineVault.formatBytes(module.sizeBytes)} • {module.pagesCount} pages
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 flex-shrink-0">
          {/* Vault action */}
          {isCached ? (
            <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-emerald-500/15 text-emerald-400 text-xs font-semibold">
              <Check className="w-3.5 h-3.5" />
              In Vault
            </span>
          ) : isDownloading ? (
            <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-cyan-500/15 text-cyan-400 text-xs font-semibold font-mono">
              <div className="w-2.5 h-2.5 rounded-full border-2 border-cyan-400 border-t-transparent animate-spin" />
              {downloadPercent}%
            </span>
          ) : (
            <button
              type="button"
              onClick={() => onDownloadToVault(module)}
              className="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg bg-[#00E5FF] text-[#0F172A] text-xs font-bold hover:bg-[#33ebff] transition-all cursor-pointer"
            >
              <Download className="w-3.5 h-3.5" />
              Save Offline
            </button>
          )}

          {/* Bookmark */}
          <button
            type="button"
            onClick={() => setBookmarked(!bookmarked)}
            className={`p-1.5 rounded-lg border transition-colors cursor-pointer ${
              bookmarked ? 'border-amber-400/50 text-amber-400 bg-amber-400/10' : 'border-slate-700 text-slate-400 hover:text-white'
            }`}
            title="Bookmark"
          >
            <Bookmark className="w-4 h-4" />
          </button>

          {/* Font size toggle */}
          <div className="flex items-center bg-slate-900 rounded-lg p-0.5 border border-slate-700 text-xs font-medium text-slate-400">
            <button
              type="button"
              onClick={() => setFontSize('sm')}
              className={`px-1.5 py-0.5 rounded ${fontSize === 'sm' ? 'bg-[#00E5FF] text-[#0F172A] font-bold' : 'hover:text-white'}`}
            >
              A-
            </button>
            <button
              type="button"
              onClick={() => setFontSize('md')}
              className={`px-1.5 py-0.5 rounded ${fontSize === 'md' ? 'bg-[#00E5FF] text-[#0F172A] font-bold' : 'hover:text-white'}`}
            >
              A
            </button>
            <button
              type="button"
              onClick={() => setFontSize('lg')}
              className={`px-1.5 py-0.5 rounded ${fontSize === 'lg' ? 'bg-[#00E5FF] text-[#0F172A] font-bold' : 'hover:text-white'}`}
            >
              A+
            </button>
          </div>

          {/* Close button */}
          <button
            type="button"
            onClick={onClose}
            className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-700 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>
      </div>

      {/* Main Reading Canvas */}
      <div className="flex-1 overflow-y-auto px-4 sm:px-8 py-6 max-w-3xl mx-auto w-full">
        {/* Chapter pill indicator */}
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-cyan-500/10 border border-cyan-500/20 text-cyan-300 text-xs font-semibold mb-4 select-none">
          <span>Chapter {activeChapterIndex + 1} of {totalChapters}</span>
          <span className="text-cyan-500/50">•</span>
          <span>{module.category}</span>
        </div>

        <h1 className="text-xl sm:text-2xl font-bold text-white mb-6 border-b border-slate-800 pb-3">
          {currentChapter.chapter}
        </h1>

        {/* Chapter sections */}
        <div className="space-y-6">
          {currentChapter.sections.map((section, idx) => (
            <div key={idx} className="bg-slate-900/60 p-5 rounded-2xl border border-slate-800/80 shadow-inner">
              <h3 className="text-base sm:text-lg font-semibold text-[#00E5FF] mb-2.5">
                {section.heading}
              </h3>
              <p
                className={`text-slate-300 leading-relaxed ${
                  fontSize === 'sm' ? 'text-sm' : fontSize === 'lg' ? 'text-lg' : 'text-base'
                }`}
              >
                {section.body}
              </p>
            </div>
          ))}
        </div>

        {/* Additional academic study notes */}
        <div className="mt-8 p-4 rounded-xl bg-cyan-950/30 border border-cyan-500/20 text-xs text-cyan-200 leading-relaxed">
          <p className="font-semibold text-cyan-300 mb-1">Wisdom Tower Academy Academic Note:</p>
          This module is part of the standardized Ethiopian university national curriculum. Read and annotate key sections. Files cached in your Offline Vault remain available even when completely disconnected from the internet.
        </div>
      </div>

      {/* Footer Navigation */}
      <div className="flex items-center justify-between px-4 py-3 bg-[#1E293B] border-t border-slate-700 select-none">
        <button
          type="button"
          disabled={activeChapterIndex === 0}
          onClick={() => setActiveChapterIndex(p => Math.max(0, p - 1))}
          className="flex items-center gap-1 text-xs font-semibold px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 disabled:opacity-30 disabled:pointer-events-none transition-colors cursor-pointer text-slate-300"
        >
          <ChevronLeft className="w-4 h-4" />
          Previous
        </button>

        <span className="text-xs text-slate-400 font-mono">
          Page {activeChapterIndex + 1} / {totalChapters}
        </span>

        <button
          type="button"
          disabled={activeChapterIndex >= totalChapters - 1}
          onClick={() => setActiveChapterIndex(p => Math.min(totalChapters - 1, p + 1))}
          className="flex items-center gap-1 text-xs font-semibold px-3 py-1.5 rounded-lg bg-[#00E5FF] text-[#0F172A] hover:bg-[#33ebff] disabled:opacity-30 disabled:pointer-events-none transition-colors cursor-pointer font-bold"
        >
          Next Chapter
          <ChevronRight className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
};
