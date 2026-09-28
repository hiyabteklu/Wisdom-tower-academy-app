import React from 'react';
import { Check, Star, Users, Zap, Shield, Sparkles } from 'lucide-react';
import { COURSE_PACKAGES } from '../data/courses';
import { CoursePackage } from '../types';

interface PackagesScreenProps {
  onSelectPackage: (pkg: CoursePackage) => void;
  enrolledPackageIds: Set<string>;
}

export const PackagesScreen: React.FC<PackagesScreenProps> = ({
  onSelectPackage,
  enrolledPackageIds,
}) => {
  return (
    <div className="pb-24 pt-3 max-w-4xl mx-auto px-4 space-y-6">
      {/* Header */}
      <div className="text-center max-w-lg mx-auto">
        <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-cyan-500/15 border border-cyan-500/30 text-[#00E5FF] text-xs font-bold uppercase tracking-wider mb-2">
          <Sparkles className="w-3.5 h-3.5" />
          <span>Curriculum Packages</span>
        </div>
        <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
          Unlock Complete Academic Packages
        </h1>
        <p className="text-xs sm:text-sm text-slate-400 mt-1.5">
          Full university pathways, past exam banks, 3D flashcards, and verified teacher notes.
        </p>
      </div>

      {/* Package cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
        {COURSE_PACKAGES.map((pkg) => {
          const isEnrolled = enrolledPackageIds.has(pkg.id);

          return (
            <div
              key={pkg.id}
              className={`rounded-3xl p-6 sm:p-7 flex flex-col justify-between transition-all relative overflow-hidden ${
                pkg.badge
                  ? 'bg-gradient-to-b from-[#1E293B] to-[#0F172A] border-2 border-cyan-400/50 shadow-2xl'
                  : 'bg-[#1E293B] border border-slate-800 hover:border-slate-700 shadow-lg'
              }`}
            >
              {/* Badge */}
              {pkg.badge && (
                <div className="absolute top-4 right-4">
                  <span className="px-3 py-1 rounded-full text-[10px] font-extrabold uppercase tracking-wider bg-[#00E5FF] text-[#0F172A] shadow-md">
                    {pkg.badge}
                  </span>
                </div>
              )}

              <div>
                <span className="text-xs font-bold text-cyan-400 uppercase tracking-wider">
                  {pkg.category} Track
                </span>
                <h3 className="text-xl font-bold text-white mt-1 pr-16">{pkg.title}</h3>
                <p className="text-xs sm:text-sm text-slate-300 mt-2 leading-relaxed">
                  {pkg.tagline}
                </p>

                {/* Price and meta */}
                <div className="mt-5 pb-5 border-b border-slate-800 flex items-baseline justify-between">
                  <div className="flex items-baseline gap-2">
                    <span className="text-3xl font-black text-[#00E5FF] tracking-tight">
                      {pkg.priceETB} ETB
                    </span>
                    {pkg.originalPriceETB && (
                      <span className="text-sm text-slate-500 line-through">
                        {pkg.originalPriceETB} ETB
                      </span>
                    )}
                  </div>

                  <div className="flex items-center gap-1 text-xs text-amber-400 font-semibold">
                    <Star className="w-3.5 h-3.5 fill-amber-400" />
                    <span>{pkg.rating}</span>
                    <span className="text-slate-500">({(pkg.enrolledStudents / 1000).toFixed(1)}k)</span>
                  </div>
                </div>

                {/* Feature Checklist */}
                <div className="mt-5 space-y-2.5">
                  <p className="text-xs font-semibold uppercase text-slate-400 tracking-wider">
                    Included in this package:
                  </p>
                  {pkg.features.map((feat, idx) => (
                    <div key={idx} className="flex items-start gap-2.5 text-xs sm:text-sm text-slate-300">
                      <div className="w-4 h-4 rounded-full bg-cyan-500/20 text-[#00E5FF] flex items-center justify-center flex-shrink-0 mt-0.5">
                        <Check className="w-3 h-3 stroke-[3]" />
                      </div>
                      <span>{feat}</span>
                    </div>
                  ))}
                </div>
              </div>

              {/* Bottom Enrollment Action */}
              <div className="mt-7">
                {isEnrolled ? (
                  <div className="w-full py-3.5 rounded-2xl bg-emerald-500/15 border border-emerald-500/30 text-emerald-400 font-bold text-sm text-center flex items-center justify-center gap-2">
                    <Check className="w-4 h-4 stroke-[3]" />
                    Already Enrolled & Active
                  </div>
                ) : (
                  <button
                    type="button"
                    onClick={() => onSelectPackage(pkg)}
                    className="w-full py-3.5 rounded-2xl bg-[#00E5FF] text-[#0F172A] font-extrabold text-sm sm:text-base hover:bg-[#33ebff] active:scale-98 transition-all shadow-lg flex items-center justify-center gap-2 cursor-pointer"
                  >
                    <Zap className="w-4 h-4 fill-slate-900" />
                    Enroll for {pkg.priceETB} ETB
                  </button>
                )}
              </div>
            </div>
          );
        })}
      </div>

      {/* Guarantee Banner */}
      <div className="p-4 rounded-2xl bg-slate-900/80 border border-slate-800 text-center text-xs text-slate-400 flex items-center justify-center gap-2">
        <Shield className="w-4 h-4 text-emerald-400 flex-shrink-0" />
        <span>Supported with telebirr, CBE Birr & Chapa with instant receipt and guarantee.</span>
      </div>
    </div>
  );
};
