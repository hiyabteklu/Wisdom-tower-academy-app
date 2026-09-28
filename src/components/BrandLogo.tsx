import React from 'react';

interface BrandLogoProps {
  size?: number;
  className?: string;
}

export const BrandLogo: React.FC<BrandLogoProps> = ({ size = 34, className = '' }) => {
  return (
    <div 
      className={`relative inline-flex items-center justify-center rounded-lg overflow-hidden border border-[#00E5FF]/20 bg-[#1E293B] shadow-sm flex-shrink-0 ${className}`}
      style={{ width: size, height: size }}
    >
      <img
        src="/logo.png"
        alt="Wisdom Tower Academy"
        className="w-full h-full object-cover"
        onError={(e) => {
          // Fallback if image fails to load
          const target = e.currentTarget;
          target.style.display = 'none';
        }}
      />
    </div>
  );
};
