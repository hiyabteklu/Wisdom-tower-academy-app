import React, { useState } from 'react';
import { HardDrive, Trash2, Wifi, Bell, Shield, Info, RefreshCw, Check } from 'lucide-react';
import { offlineVault } from '../services/offlineVault';

interface SettingsScreenProps {
  onResetOnboarding: () => void;
  onClearVault: () => void;
}

export const SettingsScreen: React.FC<SettingsScreenProps> = ({
  onResetOnboarding,
  onClearVault,
}) => {
  const [wifiOnly, setWifiOnly] = useState(true);
  const [examAlerts, setExamAlerts] = useState(true);
  const [autoCache, setAutoCache] = useState(true);
  const [vaultItems, setVaultItems] = useState(offlineVault.getStoredItems());
  const [showClearConfirm, setShowClearConfirm] = useState(false);

  const totalVaultBytes = offlineVault.getTotalVaultSize();

  const handleDeleteItem = (fileId: string) => {
    offlineVault.removeStoredItem(fileId);
    setVaultItems(offlineVault.getStoredItems());
  };

  const handleClearAll = () => {
    offlineVault.clearAllVault();
    setVaultItems([]);
    setShowClearConfirm(false);
    onClearVault();
  };

  return (
    <div className="pb-24 pt-3 max-w-4xl mx-auto px-4 space-y-6 select-none">
      {/* Title */}
      <div>
        <h2 className="text-xl sm:text-2xl font-bold text-white">App Settings</h2>
        <p className="text-xs text-slate-400 mt-1">
          Manage your Offline Vault storage, download behavior, and notifications.
        </p>
      </div>

      {/* Offline Vault Storage Card */}
      <div className="rounded-3xl bg-[#1E293B] border border-cyan-500/20 p-5 shadow-xl space-y-4">
        <div className="flex items-center justify-between pb-3 border-b border-slate-800">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-cyan-500/15 text-[#00E5FF] flex items-center justify-center">
              <HardDrive className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-sm font-bold text-white">Offline Vault Storage</h3>
              <p className="text-xs text-slate-400 font-mono">
                {offlineVault.formatBytes(totalVaultBytes)} used • {vaultItems.length} modules stored
              </p>
            </div>
          </div>

          {vaultItems.length > 0 && (
            <button
              type="button"
              onClick={() => setShowClearConfirm(true)}
              className="text-xs font-semibold text-rose-400 hover:text-rose-300 transition-colors cursor-pointer"
            >
              Clear All
            </button>
          )}
        </div>

        {/* List of stored items */}
        {vaultItems.length > 0 ? (
          <div className="space-y-2 max-h-56 overflow-y-auto pr-1">
            {vaultItems.map((item) => (
              <div
                key={item.fileId}
                className="flex items-center justify-between p-2.5 rounded-xl bg-slate-900/60 border border-slate-800 text-xs"
              >
                <div className="overflow-hidden pr-2">
                  <p className="font-semibold text-white truncate">{item.title}</p>
                  <p className="text-slate-400 font-mono text-[10px]">
                    {offlineVault.formatBytes(item.sizeBytes)} • Saved
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => handleDeleteItem(item.fileId)}
                  className="p-1 text-slate-500 hover:text-rose-400 transition-colors cursor-pointer flex-shrink-0"
                  title="Remove from vault"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            ))}
          </div>
        ) : (
          <p className="text-xs text-slate-400 text-center py-2">
            No downloaded modules in the Offline Vault yet. Browse Learning to save modules for offline reading.
          </p>
        )}

        {/* Clear confirm prompt */}
        {showClearConfirm && (
          <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/30 flex items-center justify-between text-xs">
            <span className="text-rose-300 font-medium">Remove all {vaultItems.length} offline files?</span>
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => setShowClearConfirm(false)}
                className="px-2.5 py-1 rounded bg-slate-800 text-slate-300 hover:text-white"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleClearAll}
                className="px-2.5 py-1 rounded bg-rose-500 text-white font-bold"
              >
                Confirm Delete
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Network & Downloads Preferences */}
      <div className="rounded-3xl bg-[#1E293B] border border-slate-800 p-5 shadow-lg space-y-4">
        <h3 className="text-sm font-bold text-white uppercase tracking-wider text-cyan-400">
          Downloads & Data
        </h3>

        <div className="space-y-3.5 divide-y divide-slate-800">
          <div className="flex items-center justify-between pt-1">
            <div className="flex items-center gap-3">
              <Wifi className="w-4 h-4 text-cyan-400" />
              <div>
                <div className="text-sm font-semibold text-white">Download on Wi-Fi Only</div>
                <div className="text-xs text-slate-400">Prevent large module downloads over mobile data</div>
              </div>
            </div>
            <input
              type="checkbox"
              checked={wifiOnly}
              onChange={(e) => setWifiOnly(e.target.checked)}
              className="w-4 h-4 accent-[#00E5FF] cursor-pointer"
            />
          </div>

          <div className="flex items-center justify-between pt-3">
            <div className="flex items-center gap-3">
              <HardDrive className="w-4 h-4 text-cyan-400" />
              <div>
                <div className="text-sm font-semibold text-white">Auto-Cache Opened Books</div>
                <div className="text-xs text-slate-400">Automatically save viewed chapters into vault</div>
              </div>
            </div>
            <input
              type="checkbox"
              checked={autoCache}
              onChange={(e) => setAutoCache(e.target.checked)}
              className="w-4 h-4 accent-[#00E5FF] cursor-pointer"
            />
          </div>

          <div className="flex items-center justify-between pt-3">
            <div className="flex items-center gap-3">
              <Bell className="w-4 h-4 text-cyan-400" />
              <div>
                <div className="text-sm font-semibold text-white">Exam Milestone Alerts</div>
                <div className="text-xs text-slate-400">MoE schedule announcements & national exam updates</div>
              </div>
            </div>
            <input
              type="checkbox"
              checked={examAlerts}
              onChange={(e) => setExamAlerts(e.target.checked)}
              className="w-4 h-4 accent-[#00E5FF] cursor-pointer"
            />
          </div>
        </div>
      </div>

      {/* App Information & Development Tools */}
      <div className="rounded-3xl bg-[#1E293B] border border-slate-800 p-5 shadow-lg space-y-3">
        <h3 className="text-sm font-bold text-white uppercase tracking-wider text-cyan-400">
          About Applet
        </h3>

        <div className="space-y-2 text-xs text-slate-300">
          <div className="flex justify-between py-1 border-b border-slate-800">
            <span className="text-slate-400">Application</span>
            <span className="font-semibold text-white">Wisdom Tower Academy</span>
          </div>
          <div className="flex justify-between py-1 border-b border-slate-800">
            <span className="text-slate-400">Version</span>
            <span className="font-mono text-cyan-300">1.0.0 (React Web)</span>
          </div>
          <div className="flex justify-between py-1 border-b border-slate-800">
            <span className="text-slate-400">Architecture</span>
            <span className="text-slate-300">Offline Vault + 5-Tab Native Chrome</span>
          </div>
          <div className="flex justify-between py-1">
            <span className="text-slate-400">Source Platform</span>
            <span className="text-slate-300">wisdom-tower-academy.live</span>
          </div>
        </div>

        <div className="pt-3 border-t border-slate-800">
          <button
            type="button"
            onClick={onResetOnboarding}
            className="w-full py-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 border border-slate-700 text-xs font-semibold text-slate-300 hover:text-white transition-colors flex items-center justify-center gap-1.5 cursor-pointer"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            Replay Introduction / Onboarding
          </button>
        </div>
      </div>
    </div>
  );
};
