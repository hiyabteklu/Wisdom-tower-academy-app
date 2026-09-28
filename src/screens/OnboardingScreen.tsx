import React, { useState } from 'react';
import { School, CheckCircle2, Brain, Bell } from 'lucide-react';
import { BrandLoader } from '../components/BrandLoader';

interface OnboardingScreenProps {
  onFinished: () => void;
}

export const OnboardingScreen: React.FC<OnboardingScreenProps> = ({ onFinished }) => {
  const [currentPage, setCurrentPage] = useState(0);

  const pages = [
    {
      title: 'Welcome to\nWisdom Tower Academy',
      body: 'Your complete academic companion for Ethiopia.',
      showGif: true,
    },
    {
      title: 'Everything for your journey',
      body: 'The most complete online learning platform in Ethiopia. Pathways, packages, and materials for your entire academic path.',
      icon: School,
    },
    {
      title: 'Zero margin for error',
      body: 'Accuracy-first learning materials built for real exams and real results.',
      icon: CheckCircle2,
    },
    {
      title: 'Powerful AI, free with your course',
      body: 'Built-in AI support for every purchased course. Study smarter, not harder.',
      icon: Brain,
    },
    {
      title: 'Stay on track',
      body: 'Turn on notifications for learning updates, exams, opportunities, and tips that help you improve.',
      icon: Bell,
    },
  ];

  const page = pages[currentPage];
  const isLast = currentPage === pages.length - 1;

  const handleNext = () => {
    if (currentPage < pages.length - 1) {
      setCurrentPage(p => p + 1);
    } else {
      onFinished();
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex flex-col justify-between bg-gradient-to-b from-[#0B1220] via-[#0F172A] to-[#0B1628] text-white p-6 sm:p-8 select-none overflow-hidden">
      {/* Top row: Skip */}
      <div className="flex justify-end h-8">
        {!isLast && (
          <button
            type="button"
            onClick={onFinished}
            className="text-sm font-medium text-[#94A3B8] hover:text-white transition-colors cursor-pointer"
          >
            Skip
          </button>
        )}
      </div>

      {/* Center content */}
      <div className="flex-1 flex flex-col items-center justify-center max-w-md mx-auto text-center px-4 my-auto">
        {page.showGif ? (
          <div className="mb-8">
            <BrandLoader size={160} showCard={false} />
          </div>
        ) : (
          <div className="mb-8">
            <div className="w-28 h-28 rounded-3xl bg-gradient-to-br from-[#1E293B] to-[#0F172A] p-0.5 shadow-2xl border border-cyan-500/20 flex items-center justify-center">
              <div className="w-full h-full rounded-[22px] bg-[#1E293B] flex items-center justify-center text-[#00E5FF]">
                {page.icon && React.createElement(page.icon, { className: 'w-12 h-12 stroke-[1.8]' })}
              </div>
            </div>
          </div>
        )}

        <h1 className="text-2xl sm:text-3xl font-extrabold text-white leading-tight whitespace-pre-line tracking-tight mb-4">
          {page.title}
        </h1>

        <p className="text-base text-[#94A3B8] leading-relaxed max-w-sm">
          {page.body}
        </p>
      </div>

      {/* Bottom controls */}
      <div className="w-full max-w-md mx-auto flex flex-col items-center">
        {/* Progress dots */}
        <div className="flex items-center gap-2 mb-6">
          {pages.map((_, idx) => (
            <div
              key={idx}
              className={`h-2 rounded-full transition-all duration-300 ${
                idx === currentPage ? 'w-6 bg-[#00E5FF]' : 'w-2 bg-slate-700'
              }`}
            />
          ))}
        </div>

        {/* Buttons */}
        {!isLast ? (
          <button
            type="button"
            onClick={handleNext}
            className="w-full py-4 rounded-2xl bg-[#00E5FF] text-[#0F172A] font-bold text-base hover:bg-[#33ebff] active:scale-98 transition-all shadow-lg cursor-pointer"
          >
            Next
          </button>
        ) : (
          <div className="w-full space-y-2.5">
            <button
              type="button"
              onClick={onFinished}
              className="w-full py-4 rounded-2xl bg-[#00E5FF] text-[#0F172A] font-bold text-base hover:bg-[#33ebff] active:scale-98 transition-all shadow-lg cursor-pointer"
            >
              Start learning
            </button>
            <button
              type="button"
              onClick={onFinished}
              className="w-full py-3 rounded-2xl border border-slate-700/80 text-[#94A3B8] text-sm hover:text-white transition-colors cursor-pointer"
            >
              Not now
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
