import React from 'react';
import { Home, BookOpen, Package, User, Settings } from 'lucide-react';
import { TabType } from '../types';

interface BottomNavProps {
  currentTab: TabType;
  onSelectTab: (tab: TabType) => void;
}

export const BottomNav: React.FC<BottomNavProps> = ({ currentTab, onSelectTab }) => {
  const tabs = [
    { key: 'home' as TabType, label: 'Home', icon: Home },
    { key: 'learning' as TabType, label: 'Learning', icon: BookOpen },
    { key: 'packages' as TabType, label: 'Packages', icon: Package },
    { key: 'account' as TabType, label: 'Account', icon: User },
    { key: 'settings' as TabType, label: 'Settings', icon: Settings },
  ];

  return (
    <nav className="fixed bottom-0 left-0 right-0 z-40 bg-[#0F172A]/95 backdrop-blur-md border-t border-white/10 select-none pb-[env(safe-area-inset-bottom)]">
      <div className="max-w-md mx-auto flex items-center justify-around h-16 px-1">
        {tabs.map((tab) => {
          const Icon = tab.icon;
          const isActive = currentTab === tab.key;

          return (
            <button
              key={tab.key}
              type="button"
              onClick={() => onSelectTab(tab.key)}
              className={`flex-1 flex flex-col items-center justify-center py-1 transition-all relative group cursor-pointer ${
                isActive ? 'text-[#00E5FF]' : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              {/* Active ambient indicator glow */}
              {isActive && (
                <div className="absolute top-0 w-8 h-1 bg-[#00E5FF] rounded-full shadow-[0_0_8px_#00E5FF]" />
              )}

              <div className={`p-1 rounded-xl transition-transform ${isActive ? 'scale-110' : 'group-hover:scale-105'}`}>
                <Icon className={`w-5 h-5 ${isActive ? 'stroke-[2.4]' : 'stroke-[1.8]'}`} />
              </div>

              <span className={`text-[11px] font-medium tracking-tight mt-0.5 ${isActive ? 'font-bold text-[#00E5FF]' : ''}`}>
                {tab.label}
              </span>
            </button>
          );
        })}
      </div>
    </nav>
  );
};
