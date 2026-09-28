import React from 'react';
import { ArrowLeft, Mail, Shield, FileText, HelpCircle, Info, ExternalLink } from 'lucide-react';

interface InfoPageProps {
  route: string;
  onBack: () => void;
}

export const InfoPages: React.FC<InfoPageProps> = ({ route, onBack }) => {
  const renderContent = () => {
    switch (route) {
      case '/about':
        return (
          <div className="space-y-4">
            <h2 className="text-xl font-bold text-white flex items-center gap-2">
              <Info className="w-5 h-5 text-[#00E5FF]" />
              About Wisdom Tower Academy
            </h2>
            <p className="text-sm text-slate-300 leading-relaxed">
              Wisdom Tower Academy is Ethiopia's premier digital learning companion, dedicated to providing rigorous, accuracy-first higher education materials, freshman university preparation, and national remedial curriculum tools.
            </p>
            <p className="text-sm text-slate-300 leading-relaxed">
              Built in partnership with top Ethiopian university lecturers from Addis Ababa University, ASTU, and Bahir Dar University, our mission is to eliminate educational inequality through zero-data offline storage and accessible academic excellence.
            </p>
            <div className="p-4 rounded-2xl bg-slate-900 border border-slate-800 text-xs text-slate-400">
              <p className="font-semibold text-white mb-1">Key Milestones:</p>
              <ul className="list-disc list-inside space-y-1">
                <li>27 Full University Curriculum Modules</li>
                <li>Over 14,000 enrolled Ethiopian students</li>
                <li>Zero-data Offline Vault system</li>
              </ul>
            </div>
          </div>
        );

      case '/contact':
        return (
          <div className="space-y-4">
            <h2 className="text-xl font-bold text-white flex items-center gap-2">
              <Mail className="w-5 h-5 text-[#00E5FF]" />
              Contact & Academic Support
            </h2>
            <p className="text-sm text-slate-300 leading-relaxed">
              Have questions regarding module access, Telebirr/CBE payment verification, or university pathways? Our Ethiopian support team is ready to help.
            </p>
            <div className="space-y-3 mt-4">
              <div className="p-4 rounded-2xl bg-slate-900 border border-slate-800">
                <h4 className="text-xs font-bold uppercase text-cyan-400">Telegram Channel & Chat</h4>
                <p className="text-sm text-white mt-1">@wisdomtower</p>
                <a
                  href="https://t.me/wisdomtower"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="mt-2 text-xs text-cyan-400 flex items-center gap-1 hover:underline"
                >
                  Open Telegram Discussion <ExternalLink className="w-3 h-3" />
                </a>
              </div>
              <div className="p-4 rounded-2xl bg-slate-900 border border-slate-800">
                <h4 className="text-xs font-bold uppercase text-cyan-400">Official Email</h4>
                <p className="text-sm text-white mt-1">support@wisdom-tower-academy.live</p>
              </div>
            </div>
          </div>
        );

      case '/faq':
        return (
          <div className="space-y-4">
            <h2 className="text-xl font-bold text-white flex items-center gap-2">
              <HelpCircle className="w-5 h-5 text-[#00E5FF]" />
              Frequently Asked Questions (FAQ)
            </h2>
            <div className="space-y-3 text-sm">
              <div className="p-4 rounded-2xl bg-slate-900 border border-slate-800">
                <h4 className="font-bold text-white">How does the Offline Vault work?</h4>
                <p className="text-xs text-slate-300 mt-1 leading-relaxed">
                  When you tap "Download" or "Read" while online, the app stores the complete course module in your device's private storage. You can reopen the app anywhere without internet or mobile data.
                </p>
              </div>
              <div className="p-4 rounded-2xl bg-slate-900 border border-slate-800">
                <h4 className="font-bold text-white">Are these official Ethiopian university modules?</h4>
                <p className="text-xs text-slate-300 mt-1 leading-relaxed">
                  Yes. All 27 modules adhere strictly to the Ministry of Education (MoE) standardized curriculum for freshman university and remedial tracks.
                </p>
              </div>
              <div className="p-4 rounded-2xl bg-slate-900 border border-slate-800">
                <h4 className="font-bold text-white">How can I pay for course packages?</h4>
                <p className="text-xs text-slate-300 mt-1 leading-relaxed">
                  We support Telebirr, Commercial Bank of Ethiopia (CBE Birr), and Chapa payment gateways with instant automated activation.
                </p>
              </div>
            </div>
          </div>
        );

      case '/privacy':
        return (
          <div className="space-y-4">
            <h2 className="text-xl font-bold text-white flex items-center gap-2">
              <Shield className="w-5 h-5 text-[#00E5FF]" />
              Privacy Policy
            </h2>
            <p className="text-sm text-slate-300 leading-relaxed">
              At Wisdom Tower Academy, we protect your personal academic privacy. We do not sell your personal data or share your learning records with external third parties.
            </p>
            <p className="text-xs text-slate-400 leading-relaxed">
              Downloaded books and modules are stored locally in your app's isolated private vault. Diagnostic test scores and progress data are used solely to improve your learning experience.
            </p>
          </div>
        );

      case '/terms':
        return (
          <div className="space-y-4">
            <h2 className="text-xl font-bold text-white flex items-center gap-2">
              <FileText className="w-5 h-5 text-[#00E5FF]" />
              Terms of Service
            </h2>
            <p className="text-sm text-slate-300 leading-relaxed">
              Course modules, flashcards, and exam preparation material are provided for personal educational use by registered students of Wisdom Tower Academy.
            </p>
            <p className="text-xs text-slate-400 leading-relaxed">
              Redistribution, unauthorized commercial duplication, or public rehosting of proprietary study content is strictly prohibited under Ethiopian intellectual property laws.
            </p>
          </div>
        );

      default:
        return null;
    }
  };

  return (
    <div className="pb-24 pt-2 max-w-3xl mx-auto px-4 space-y-4 select-none animate-fade-in">
      <div className="flex items-center gap-2.5 pb-2 border-b border-slate-800">
        <button
          type="button"
          onClick={onBack}
          className="p-2 rounded-xl text-slate-400 hover:text-white hover:bg-slate-800 transition-colors cursor-pointer"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>
        <span className="text-xs font-mono text-cyan-400">{route}</span>
      </div>

      <div className="p-4 sm:p-6 rounded-3xl bg-[#1E293B] border border-slate-800 shadow-xl">
        {renderContent()}
      </div>
    </div>
  );
};
