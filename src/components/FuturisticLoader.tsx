import React from 'react';

interface FuturisticLoaderProps {
  statusText?: string;
  subText?: string;
  progress?: number;
  isSplash?: boolean;
}

export const FuturisticLoader: React.FC<FuturisticLoaderProps> = ({
  statusText = 'Wisdom Tower Academy',
  subText = 'Preparing your learning space…',
  progress,
  isSplash = false,
}) => {
  return (
    <div
      className={`fixed inset-0 z-50 flex flex-col items-center justify-center select-none ${
        isSplash ? 'bg-[#070E1B]' : 'bg-[#070E1B]/90 backdrop-blur-md'
      }`}
    >
      {/* Ambient background cyber glow */}
      <div className="absolute w-80 h-80 rounded-full bg-gradient-to-tr from-cyan-500/20 via-blue-600/10 to-transparent blur-3xl pointer-events-none animate-pulse" />

      {/* Futuristic Orbit & Dual Gear Rings */}
      <div className="relative flex flex-col items-center justify-center">
        <div className={`relative flex items-center justify-center ${isSplash ? 'w-52 h-52' : 'w-40 h-40'}`}>
          {/* Outer high-speed neon cyan spinning ring with gear ticks */}
          <svg
            className="absolute inset-0 w-full h-full animate-[spin_3s_linear_infinite]"
            viewBox="0 0 200 200"
          >
            <defs>
              <linearGradient id="cyanArc" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stopColor="#00E5FF" stopOpacity="0" />
                <stop offset="60%" stopColor="#00E5FF" stopOpacity="0.8" />
                <stop offset="100%" stopColor="#38BDF8" stopOpacity="1" />
              </linearGradient>
            </defs>
            {/* Gear circle track */}
            <circle
              cx="100"
              cy="100"
              r="88"
              fill="none"
              stroke="#00E5FF"
              strokeWidth="1.5"
              strokeOpacity="0.25"
            />
            {/* Gear ticks */}
            {Array.from({ length: 18 }).map((_, i) => {
              const angle = (i * 360) / 18;
              return (
                <line
                  key={i}
                  x1="100"
                  y1="10"
                  x2="100"
                  y2="22"
                  stroke={i % 2 === 0 ? '#00E5FF' : '#38BDF8'}
                  strokeWidth="3"
                  strokeLinecap="round"
                  transform={`rotate(${angle} 100 100)`}
                  opacity="0.85"
                />
              );
            })}
            {/* Outer neon cyan arc */}
            <circle
              cx="100"
              cy="100"
              r="94"
              fill="none"
              stroke="url(#cyanArc)"
              strokeWidth="3.5"
              strokeLinecap="round"
              strokeDasharray="420"
              strokeDashoffset="120"
            />
          </svg>

          {/* Inner counter-spinning neon violet/indigo arc */}
          <svg
            className="absolute inset-0 w-full h-full animate-[spin_2.2s_linear_infinite_reverse]"
            viewBox="0 0 200 200"
          >
            <defs>
              <linearGradient id="violetArc" x1="100%" y1="0%" x2="0%" y2="100%">
                <stop offset="0%" stopColor="#818CF8" stopOpacity="0" />
                <stop offset="50%" stopColor="#818CF8" stopOpacity="0.9" />
                <stop offset="100%" stopColor="#00E5FF" stopOpacity="1" />
              </linearGradient>
            </defs>
            <circle
              cx="100"
              cy="100"
              r="68"
              fill="none"
              stroke="url(#violetArc)"
              strokeWidth="3"
              strokeLinecap="round"
              strokeDasharray="300"
              strokeDashoffset="100"
            />
          </svg>

          {/* Center Custom Wisdom Tower Academy GIF with glowing dot */}
          <div className="relative z-10 flex items-center justify-center p-3 rounded-2xl bg-[#0F172A]/80 shadow-[0_0_30px_rgba(0,229,255,0.35)] border border-cyan-500/30">
            <img
              src="/animation.gif"
              alt="Wisdom Tower Academy Loading Animation"
              className={`object-contain pointer-events-none select-none drop-shadow-[0_0_16px_rgba(0,229,255,0.6)] ${
                isSplash ? 'w-24 h-24' : 'w-18 h-18'
              }`}
              loading="eager"
            />
          </div>
        </div>

        {/* Status text */}
        <div className="mt-6 text-center px-4 max-w-sm">
          <h2 className="text-white font-bold text-lg tracking-wide drop-shadow-sm flex items-center justify-center gap-2">
            <span>{statusText}</span>
          </h2>
          {subText && (
            <p className="text-slate-400 text-xs mt-1.5 font-medium tracking-wide">
              {subText}
            </p>
          )}
        </div>

        {/* Progress pill indicator with glowing cyan pulsing dot */}
        <div className="mt-4 flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-cyan-950/40 border border-cyan-500/30 shadow-[0_0_12px_rgba(0,229,255,0.2)]">
          <span className="w-2 h-2 rounded-full bg-cyan-400 animate-ping" />
          <span className="text-xs font-semibold text-cyan-300 font-mono tracking-wide">
            {progress !== undefined && progress > 0
              ? `Loading ${progress}%`
              : 'Syncing…'}
          </span>
        </div>

        {/* Optional Progress bar */}
        {progress !== undefined && progress > 0 && progress < 100 && (
          <div className="mt-3 w-48 mx-auto">
            <div className="h-1.5 w-full bg-slate-900 rounded-full overflow-hidden border border-cyan-500/30 p-[1px]">
              <div
                className="h-full bg-gradient-to-r from-cyan-400 to-blue-500 rounded-full transition-all duration-150 ease-out shadow-[0_0_8px_rgba(0,229,255,0.8)]"
                style={{ width: `${progress}%` }}
              />
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
