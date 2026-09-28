import React from 'react';
import { WifiOff, HardDrive } from 'lucide-react';

interface OfflineScreenProps {
  onRetry: () => void;
  onOpenVault: () => void;
}

export const OfflineScreen: React.FC<OfflineScreenProps> = ({ onRetry, onOpenVault }) => {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-6 bg-[#0F172A] text-center select-none">
      <div className="w-full max-w-sm rounded-3xl bg-[#1E293B] border border-cyan-500/30 p-8 shadow-2xl">
        {/* Wifi off icon */}
        <div className="w-20 h-20 mx-auto mb-6 rounded-2xl bg-[#0F172A] border border-cyan-500/30 flex items-center justify-center text-[#00E5FF]">
          <WifiOff className="w-10 h-10 stroke-[2]" />
        </div>

        <h1 className="text-xl font-extrabold text-white mb-2">Connection required</h1>
        <p className="text-sm text-slate-400 leading-relaxed mb-6">
          Please turn on your mobile data or connect to Wi-Fi to continue live updates.
        </p>

        <div className="space-y-3">
          <button
            type="button"
            onClick={onRetry}
            className="w-full py-3.5 rounded-xl bg-[#00E5FF] text-[#0F172A] font-bold text-sm hover:bg-[#33ebff] transition-all cursor-pointer shadow-md"
          >
            Try again
          </button>

          <button
            type="button"
            onClick={onOpenVault}
            className="w-full py-3 rounded-xl bg-slate-800 text-slate-200 border border-slate-700 text-xs font-semibold hover:bg-slate-700 transition-colors flex items-center justify-center gap-2 cursor-pointer"
          >
            <HardDrive className="w-4 h-4 text-cyan-400" />
            <span>Open Offline Vault</span>
          </button>
        </div>
      </div>
    </div>
  );
};
