import React from 'react';
import { Menu, X, Info, Mail, HelpCircle, Shield, FileText, ChevronRight } from 'lucide-react';

interface MenuModalProps {
  isOpen: boolean;
  onClose: () => void;
  onNavigate: (route: string) => void;
}

export const MenuModal: React.FC<MenuModalProps> = ({ isOpen, onClose, onNavigate }) => {
  if (!isOpen) return null;

  const links = [
    { label: 'About', route: '/about', icon: Info },
    { label: 'Contact us', route: '/contact', icon: Mail },
    { label: 'FAQ', route: '/faq', icon: HelpCircle },
    { label: 'Privacy', route: '/privacy', icon: Shield },
    { label: 'Terms', route: '/terms', icon: FileText },
  ];

  return (
    <div
      className="fixed inset-0 z-50 flex items-start justify-start p-4 bg-black/60 backdrop-blur-xs select-none animate-fade-in"
      onClick={onClose}
    >
      <div
        className="w-full max-w-[290px] mt-12 ml-1 rounded-[22px] bg-[#080F1E]/95 border border-[#00E5FF]/25 shadow-2xl p-4 text-white overflow-hidden transition-all transform scale-100"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-[#00E5FF]/15">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-[#00E5FF]/15 flex items-center justify-center text-[#00E5FF]">
              <Menu className="w-4 h-4" />
            </div>
            <span className="font-bold text-base tracking-wide">Menu</span>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="w-8 h-8 flex items-center justify-center rounded-lg text-slate-400 hover:text-white hover:bg-white/5 transition-colors cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* 5 clean links */}
        <div className="py-2 space-y-1">
          {links.map((link) => {
            const Icon = link.icon;
            return (
              <button
                key={link.label}
                type="button"
                onClick={() => {
                  onClose();
                  onNavigate(link.route);
                }}
                className="w-full flex items-center justify-between p-2.5 rounded-xl hover:bg-[#00E5FF]/15 active:bg-[#00E5FF]/25 transition-all group text-left cursor-pointer"
              >
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-lg bg-[#00E5FF]/10 text-[#00E5FF] flex items-center justify-center group-hover:scale-105 transition-transform">
                    <Icon className="w-4 h-4" />
                  </div>
                  <span className="text-sm font-medium text-slate-200 group-hover:text-white">
                    {link.label}
                  </span>
                </div>
                <ChevronRight className="w-3.5 h-3.5 text-slate-500 group-hover:text-cyan-400 transition-colors" />
              </button>
            );
          })}
        </div>

        {/* Footer */}
        <div className="pt-3 border-t border-white/10 text-center">
          <span className="text-[11px] text-slate-400/70 font-normal">
            Wisdom Tower Academy
          </span>
        </div>
      </div>
    </div>
  );
};
