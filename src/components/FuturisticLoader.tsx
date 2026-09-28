import React from 'react';

interface FuturisticLoaderProps {
  statusText?: string;
  subText?: string;
  progress?: number;
  isSplash?: boolean;
}

export const FuturisticLoader: React.FC<FuturisticLoaderProps> = ({
  statusText = 'Wisdom Tower Academy',
  subText,
  progress,
  isSplash = false,
}) => {
  return (
    <div
      className={`fixed inset-0 z-50 flex flex-col items-center justify-center select-none transition-opacity duration-120 ${
        isSplash ? 'bg-[#0F172A]' : 'bg-[#0F172A]'
      }`}
      style={{ animation: 'none' }}
    >
      {/* Centered Wisdom Tower Academy GIF (Pre-decoded, frame 0 instant render) */}
      <div className="relative flex flex-col items-center justify-center">
        <img
          src="/animation.gif"
          alt="Wisdom Tower Academy Loading"
          className="w-32 h-32 object-contain select-none pointer-events-none drop-shadow-[0_0_24px_rgba(0,229,255,0.35)]"
          loading="eager"
          decoding="sync"
        />

        {/* Text Info (optional) */}
        {statusText && (
          <div className="mt-4 text-center px-4 max-w-sm">
            <h3 className="text-white font-bold text-base tracking-wide">{statusText}</h3>
            {subText && <p className="text-slate-400 text-xs mt-1">{subText}</p>}
          </div>
        )}

        {progress !== undefined && progress > 0 && progress < 100 && (
          <div className="mt-4 w-44 mx-auto">
            <div className="h-1 w-full bg-slate-800 rounded-full overflow-hidden border border-cyan-500/20">
              <div
                className="h-full bg-cyan-400 transition-all duration-100 ease-out"
                style={{ width: `${progress}%` }}
              />
            </div>
            <span className="text-[11px] text-cyan-400 font-mono mt-1 block text-center">
              {progress}%
            </span>
          </div>
        )}
      </div>
    </div>
  );
};
