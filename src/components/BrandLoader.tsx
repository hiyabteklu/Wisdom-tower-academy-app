import React, { useState } from 'react';

interface BrandLoaderProps {
  size?: number;
  showCard?: boolean;
  className?: string;
}

export const BrandLoader: React.FC<BrandLoaderProps> = ({
  size = 120,
  showCard = true,
  className = '',
}) => {
  const [loaded, setLoaded] = useState(false);

  return (
    <div
      className={`inline-flex items-center justify-center transition-all duration-300 ${
        showCard && loaded
          ? 'bg-slate-900/80 backdrop-blur-md p-6 rounded-3xl border border-cyan-500/20 shadow-2xl'
          : ''
      } ${className}`}
    >
      <img
        src="/animation.gif"
        alt="Loading Wisdom Tower Academy"
        style={{ width: size, height: size }}
        className="object-contain"
        onLoad={() => setLoaded(true)}
      />
    </div>
  );
};
