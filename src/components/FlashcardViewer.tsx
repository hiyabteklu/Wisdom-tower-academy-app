import React, { useState } from 'react';
import { RotateCw, CheckCircle2, RefreshCw, ChevronLeft, ChevronRight, Zap } from 'lucide-react';
import { FLASHCARDS } from '../data/courses';

export const FlashcardViewer: React.FC = () => {
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isFlipped, setIsFlipped] = useState(false);
  const [masteredIds, setMasteredIds] = useState<Set<string>>(new Set());

  const card = FLASHCARDS[currentIndex];
  const isMastered = masteredIds.has(card.id);

  const handleNext = () => {
    setIsFlipped(false);
    setTimeout(() => {
      setCurrentIndex((prev) => (prev + 1) % FLASHCARDS.length);
    }, 150);
  };

  const handlePrev = () => {
    setIsFlipped(false);
    setTimeout(() => {
      setCurrentIndex((prev) => (prev - 1 + FLASHCARDS.length) % FLASHCARDS.length);
    }, 150);
  };

  const toggleMastered = (e: React.MouseEvent) => {
    e.stopPropagation();
    setMasteredIds((prev) => {
      const next = new Set(prev);
      if (next.has(card.id)) next.delete(card.id);
      else next.add(card.id);
      return next;
    });
  };

  return (
    <div className="w-full max-w-lg mx-auto py-2">
      {/* Header bar with streak & progress */}
      <div className="flex items-center justify-between mb-3 px-1 select-none">
        <div className="flex items-center gap-1.5 text-xs font-semibold text-cyan-400">
          <Zap className="w-4 h-4 fill-cyan-400" />
          <span>Active Flashcard Session</span>
        </div>
        <div className="text-xs text-slate-400 font-mono">
          Card <span className="text-white font-bold">{currentIndex + 1}</span> of {FLASHCARDS.length}
          <span className="ml-2 text-emerald-400 font-bold">({masteredIds.size} Mastered)</span>
        </div>
      </div>

      {/* 3D Flip Card Container */}
      <div
        className="w-full h-72 sm:h-80 perspective-1000 cursor-pointer select-none"
        onClick={() => setIsFlipped(!isFlipped)}
      >
        <div
          className={`relative w-full h-full transition-transform duration-500 transform-style-3d rounded-3xl ${
            isFlipped ? 'rotate-y-180' : ''
          }`}
        >
          {/* FRONT SIDE */}
          <div className="absolute inset-0 backface-hidden flex flex-col justify-between p-6 rounded-3xl bg-gradient-to-br from-[#1E293B] to-[#0F172A] border border-cyan-500/30 shadow-xl">
            <div className="flex items-center justify-between">
              <span className="px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider bg-cyan-500/15 text-[#00E5FF] border border-cyan-500/20">
                {card.subject}
              </span>
              <button
                type="button"
                onClick={toggleMastered}
                className={`p-1.5 rounded-full transition-colors ${
                  isMastered ? 'text-emerald-400 bg-emerald-500/20' : 'text-slate-500 hover:text-white'
                }`}
                title="Mark as Mastered"
              >
                <CheckCircle2 className="w-5 h-5" />
              </button>
            </div>

            <div className="my-auto text-center px-2">
              <p className="text-lg sm:text-xl font-semibold text-white leading-relaxed">
                {card.question}
              </p>
            </div>

            <div className="flex items-center justify-center gap-1 text-xs text-slate-400">
              <RotateCw className="w-3.5 h-3.5 text-cyan-400" />
              <span>Tap to reveal answer</span>
            </div>
          </div>

          {/* BACK SIDE */}
          <div className="absolute inset-0 backface-hidden rotate-y-180 flex flex-col justify-between p-6 rounded-3xl bg-gradient-to-br from-[#0c2340] to-[#1E293B] border border-cyan-400/50 shadow-2xl">
            <div className="flex items-center justify-between">
              <span className="px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                Answer & Key Concept
              </span>
              <span className="text-xs text-slate-400 font-medium">
                {card.subject}
              </span>
            </div>

            <div className="my-auto text-center px-2">
              <p className="text-lg sm:text-xl font-bold text-cyan-300 leading-snug">
                {card.answer}
              </p>
              <p className="mt-3 text-xs sm:text-sm text-slate-300 leading-relaxed bg-black/30 p-3 rounded-xl border border-white/5">
                {card.explanation}
              </p>
            </div>

            <div className="flex items-center justify-center gap-1 text-xs text-slate-400">
              <RotateCw className="w-3.5 h-3.5 text-cyan-400" />
              <span>Tap to flip back</span>
            </div>
          </div>
        </div>
      </div>

      {/* Navigation Controls */}
      <div className="flex items-center justify-between mt-4 px-2 select-none">
        <button
          type="button"
          onClick={handlePrev}
          className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-slate-800 text-slate-200 text-sm font-semibold hover:bg-slate-700 active:scale-95 transition-all cursor-pointer"
        >
          <ChevronLeft className="w-4 h-4" />
          Previous
        </button>

        <button
          type="button"
          onClick={() => setIsFlipped(!isFlipped)}
          className="px-4 py-2 rounded-xl bg-cyan-500/15 text-[#00E5FF] text-sm font-semibold hover:bg-cyan-500/25 active:scale-95 transition-all cursor-pointer flex items-center gap-1.5"
        >
          <RotateCw className="w-4 h-4" />
          Flip
        </button>

        <button
          type="button"
          onClick={handleNext}
          className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-[#00E5FF] text-[#0F172A] text-sm font-bold hover:bg-[#33ebff] active:scale-95 transition-all cursor-pointer"
        >
          Next
          <ChevronRight className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
};
