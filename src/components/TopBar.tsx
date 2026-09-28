import React, { useState } from 'react';
import { Menu, RefreshCw, Bell } from 'lucide-react';
import { BrandLogo } from './BrandLogo';

interface TopBarProps {
  onOpenMenu: () => void;
  onOpenNotifications: () => void;
  onRefresh: () => void;
  unreadCount?: number;
  isRefreshing?: boolean;
}

export const TopBar: React.FC<TopBarProps> = ({
  onOpenMenu,
  onOpenNotifications,
  onRefresh,
  unreadCount = 2,
  isRefreshing = false,
}) => {
  const [spin, setSpin] = useState(false);

  const handleRefreshClick = () => {
    setSpin(true);
    onRefresh();
    setTimeout(() => setSpin(false), 900);
  };

  return (
    <header className="sticky top-0 z-40 w-full bg-[#0F172A] border-b border-white/10 select-none">
      <div className="max-w-7xl mx-auto px-2 sm:px-4 h-[54px] flex items-center justify-between">
        {/* Left: Menu button */}
        <button
          type="button"
          onClick={onOpenMenu}
          className="w-11 h-11 flex items-center justify-center rounded-xl text-[#00E5FF] hover:bg-cyan-500/10 active:scale-90 transition-all cursor-pointer"
          aria-label="Open navigation menu"
        >
          <Menu className="w-6 h-6" />
        </button>

        {/* Center: Brand Logo + Academy Name */}
        <div className="flex items-center gap-2.5 overflow-hidden px-2">
          <BrandLogo size={34} />
          <span className="text-white font-bold text-[15px] sm:text-base tracking-wide truncate">
            Wisdom Tower Academy
          </span>
        </div>

        {/* Right: Refresh & Notifications */}
        <div className="flex items-center gap-1">
          <button
            type="button"
            onClick={handleRefreshClick}
            className="w-11 h-11 flex items-center justify-center rounded-xl text-[#00E5FF] hover:bg-cyan-500/10 active:scale-90 transition-all cursor-pointer"
            aria-label="Refresh content"
          >
            <RefreshCw className={`w-5 h-5 ${spin || isRefreshing ? 'animate-spin' : ''}`} />
          </button>

          <button
            type="button"
            onClick={onOpenNotifications}
            className="relative w-11 h-11 flex items-center justify-center rounded-xl text-[#00E5FF] hover:bg-cyan-500/10 active:scale-90 transition-all cursor-pointer"
            aria-label="View notifications"
          >
            <Bell className="w-5 h-5" />
            {unreadCount > 0 && (
              <span className="absolute top-2 right-2 flex h-2.5 w-2.5">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-cyan-400 opacity-75" />
                <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-[#00E5FF]" />
              </span>
            )}
          </button>
        </div>
      </div>
    </header>
  );
};
