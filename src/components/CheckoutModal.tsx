import React, { useState } from 'react';
import { X, Check, ShieldCheck, CreditCard, Sparkles } from 'lucide-react';
import { CoursePackage } from '../types';

interface CheckoutModalProps {
  pkg: CoursePackage | null;
  onClose: () => void;
  onSuccess: (pkg: CoursePackage) => void;
}

export const CheckoutModal: React.FC<CheckoutModalProps> = ({ pkg, onClose, onSuccess }) => {
  const [selectedMethod, setSelectedMethod] = useState<'telebirr' | 'cbe' | 'chapa'>('telebirr');
  const [isProcessing, setIsProcessing] = useState(false);
  const [isDone, setIsDone] = useState(false);

  if (!pkg) return null;

  const handlePay = () => {
    setIsProcessing(true);
    setTimeout(() => {
      setIsProcessing(false);
      setIsDone(true);
      setTimeout(() => {
        onSuccess(pkg);
      }, 1200);
    }, 1500);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm select-none animate-fade-in">
      <div
        className="w-full max-w-md rounded-3xl bg-[#1E293B] border border-cyan-500/30 p-6 shadow-2xl relative overflow-hidden"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Background glow */}
        <div className="absolute top-0 right-0 w-36 h-36 bg-cyan-500/10 rounded-full blur-2xl pointer-events-none" />

        {isDone ? (
          <div className="py-8 text-center flex flex-col items-center justify-center">
            <div className="w-16 h-16 rounded-full bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 flex items-center justify-center mb-4 animate-bounce">
              <Check className="w-8 h-8 stroke-[3]" />
            </div>
            <h3 className="text-xl font-bold text-white">Enrollment Confirmed!</h3>
            <p className="text-slate-300 text-sm mt-2 max-w-xs">
              You are now enrolled in <span className="text-cyan-400 font-semibold">{pkg.title}</span>. All course modules are unlocked in your Vault.
            </p>
          </div>
        ) : (
          <>
            {/* Header */}
            <div className="flex items-center justify-between pb-3 border-b border-slate-700">
              <div>
                <h3 className="text-lg font-bold text-white">Complete Enrollment</h3>
                <p className="text-xs text-slate-400">Official Wisdom Tower Academy Payment</p>
              </div>
              <button
                type="button"
                onClick={onClose}
                className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Package details */}
            <div className="my-4 p-4 rounded-2xl bg-slate-900/80 border border-slate-800">
              <div className="flex items-start justify-between">
                <div>
                  <h4 className="font-bold text-white text-base">{pkg.title}</h4>
                  <p className="text-xs text-slate-400 mt-0.5">{pkg.category} Track • Lifetime Access</p>
                </div>
                <div className="text-right">
                  <div className="text-lg font-extrabold text-[#00E5FF]">
                    {pkg.priceETB} ETB
                  </div>
                  {pkg.originalPriceETB && (
                    <div className="text-xs text-slate-500 line-through">
                      {pkg.originalPriceETB} ETB
                    </div>
                  )}
                </div>
              </div>
            </div>

            {/* Payment Method Selector */}
            <div className="mb-5">
              <label className="block text-xs font-semibold uppercase text-slate-400 mb-2 tracking-wider">
                Select Ethiopian Payment Method
              </label>
              <div className="grid grid-cols-3 gap-2">
                {[
                  { id: 'telebirr', name: 'telebirr', subtitle: 'Mobile Money', color: 'bg-emerald-500/10 border-emerald-500/30' },
                  { id: 'cbe', name: 'CBE Birr', subtitle: 'Commercial Bank', color: 'bg-purple-500/10 border-purple-500/30' },
                  { id: 'chapa', name: 'Chapa', subtitle: 'Card / All Banks', color: 'bg-cyan-500/10 border-cyan-500/30' },
                ].map((item) => (
                  <button
                    key={item.id}
                    type="button"
                    onClick={() => setSelectedMethod(item.id as any)}
                    className={`p-3 rounded-xl border text-left transition-all cursor-pointer ${
                      selectedMethod === item.id
                        ? 'border-cyan-400 bg-cyan-500/20 shadow-sm'
                        : 'border-slate-700 bg-slate-900 hover:bg-slate-800'
                    }`}
                  >
                    <div className="text-sm font-bold text-white">{item.name}</div>
                    <div className="text-[10px] text-slate-400 truncate">{item.subtitle}</div>
                  </button>
                ))}
              </div>
            </div>

            {/* Security note */}
            <div className="flex items-center gap-2 text-xs text-slate-400 mb-5">
              <ShieldCheck className="w-4 h-4 text-emerald-400 flex-shrink-0" />
              <span>Direct encrypted checkout. Immediate access granted.</span>
            </div>

            {/* Action button */}
            <button
              type="button"
              disabled={isProcessing}
              onClick={handlePay}
              className="w-full py-3.5 rounded-xl bg-[#00E5FF] text-[#0F172A] font-bold text-base hover:bg-[#33ebff] active:scale-98 transition-all flex items-center justify-center gap-2 cursor-pointer shadow-lg disabled:opacity-50"
            >
              {isProcessing ? (
                <>
                  <div className="w-5 h-5 rounded-full border-2 border-slate-900 border-t-transparent animate-spin" />
                  Verifying transaction…
                </>
              ) : (
                <>
                  <CreditCard className="w-5 h-5" />
                  Pay {pkg.priceETB} ETB with {selectedMethod === 'telebirr' ? 'telebirr' : selectedMethod === 'cbe' ? 'CBE Birr' : 'Chapa'}
                </>
              )}
            </button>
          </>
        )}
      </div>
    </div>
  );
};
