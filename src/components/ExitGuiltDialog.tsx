import React from 'react';

interface ExitGuiltDialogProps {
  isOpen: boolean;
  onStay: () => void;
  onExit: () => void;
}

export const ExitGuiltDialog: React.FC<ExitGuiltDialogProps> = ({
  isOpen,
  onStay,
  onExit,
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in">
      <div 
        className="w-full max-w-sm rounded-2xl bg-[#1E293B] border border-slate-700/60 p-6 shadow-2xl transition-all text-center"
        onClick={(e) => e.stopPropagation()}
      >
        <h3 className="text-lg font-semibold text-white">Exit app?</h3>
        <p className="mt-2 text-sm leading-relaxed text-[#94A3B8]">
          Are you sure you want to close Wisdom Tower Academy?
        </p>

        <div className="mt-6 flex items-center justify-end gap-3">
          <button
            type="button"
            onClick={onStay}
            className="px-4 py-2 text-sm font-medium text-[#94A3B8] hover:text-white rounded-lg transition-colors cursor-pointer"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={onExit}
            className="px-5 py-2 text-sm font-bold bg-[#00E5FF] text-[#0F172A] hover:bg-[#33ebff] active:scale-95 rounded-xl transition-all shadow-md cursor-pointer"
          >
            Exit
          </button>
        </div>
      </div>
    </div>
  );
};
