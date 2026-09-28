import React, { useState } from 'react';
import { ArrowLeft, Bell, CheckCheck, BookOpen, ShoppingBag, Calendar, Info } from 'lucide-react';
import { INITIAL_NOTIFICATIONS } from '../data/courses';
import { NotificationItem } from '../types';

interface NotificationsScreenProps {
  onBack: () => void;
  onNavigateToTab: (tab: 'learning' | 'packages' | 'account') => void;
}

export const NotificationsScreen: React.FC<NotificationsScreenProps> = ({
  onBack,
  onNavigateToTab,
}) => {
  const [notifications, setNotifications] = useState<NotificationItem[]>(INITIAL_NOTIFICATIONS);
  const [filter, setFilter] = useState<'all' | 'unread'>('all');

  const markAllRead = () => {
    setNotifications(prev => prev.map(n => ({ ...n, read: true })));
  };

  const markSingleRead = (id: string) => {
    setNotifications(prev => prev.map(n => n.id === id ? ({ ...n, read: true }) : n));
  };

  const filtered = notifications.filter(n => filter === 'all' || !n.read);

  const getIcon = (type: NotificationItem['type']) => {
    switch (type) {
      case 'material':
        return <BookOpen className="w-4 h-4 text-cyan-400" />;
      case 'order':
        return <ShoppingBag className="w-4 h-4 text-emerald-400" />;
      case 'exam':
        return <Calendar className="w-4 h-4 text-amber-400" />;
      default:
        return <Info className="w-4 h-4 text-indigo-400" />;
    }
  };

  return (
    <div className="pb-24 pt-2 max-w-3xl mx-auto px-4 space-y-4 select-none animate-fade-in">
      {/* Header bar */}
      <div className="flex items-center justify-between pb-2 border-b border-slate-800">
        <div className="flex items-center gap-2.5">
          <button
            type="button"
            onClick={onBack}
            className="p-2 rounded-xl text-slate-400 hover:text-white hover:bg-slate-800 transition-colors cursor-pointer"
            aria-label="Back"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <h2 className="text-lg font-bold text-white flex items-center gap-2">
              Notifications
              <span className="text-xs px-2 py-0.5 rounded-full bg-cyan-500/20 text-[#00E5FF] font-semibold">
                /notifications
              </span>
            </h2>
            <p className="text-xs text-slate-400">Course announcements & order status</p>
          </div>
        </div>

        <button
          type="button"
          onClick={markAllRead}
          className="text-xs font-semibold text-[#00E5FF] hover:underline flex items-center gap-1 cursor-pointer"
        >
          <CheckCheck className="w-3.5 h-3.5" />
          Mark all read
        </button>
      </div>

      {/* Filter tabs */}
      <div className="flex gap-2">
        <button
          type="button"
          onClick={() => setFilter('all')}
          className={`px-3 py-1 rounded-xl text-xs font-semibold transition-colors cursor-pointer ${
            filter === 'all'
              ? 'bg-cyan-500/20 text-[#00E5FF] border border-cyan-500/30'
              : 'bg-slate-900 text-slate-400 border border-slate-800'
          }`}
        >
          All ({notifications.length})
        </button>
        <button
          type="button"
          onClick={() => setFilter('unread')}
          className={`px-3 py-1 rounded-xl text-xs font-semibold transition-colors cursor-pointer ${
            filter === 'unread'
              ? 'bg-cyan-500/20 text-[#00E5FF] border border-cyan-500/30'
              : 'bg-slate-900 text-slate-400 border border-slate-800'
          }`}
        >
          Unread ({notifications.filter(n => !n.read).length})
        </button>
      </div>

      {/* Notifications list */}
      <div className="space-y-2.5">
        {filtered.length > 0 ? (
          filtered.map((item) => (
            <div
              key={item.id}
              onClick={() => {
                markSingleRead(item.id);
                if (item.link === '/learning') onNavigateToTab('learning');
                if (item.link === '/account') onNavigateToTab('account');
              }}
              className={`p-4 rounded-2xl border transition-all cursor-pointer ${
                !item.read
                  ? 'bg-[#1E293B] border-cyan-500/40 shadow-sm'
                  : 'bg-[#1E293B]/60 border-slate-800/80 text-slate-300'
              }`}
            >
              <div className="flex items-start gap-3">
                <div className="w-8 h-8 rounded-lg bg-slate-900 flex items-center justify-center flex-shrink-0 mt-0.5">
                  {getIcon(item.type)}
                </div>
                <div className="flex-1 overflow-hidden">
                  <div className="flex items-center justify-between gap-2">
                    <h4 className={`text-sm font-bold truncate ${!item.read ? 'text-white' : 'text-slate-200'}`}>
                      {item.title}
                    </h4>
                    <span className="text-[10px] text-slate-400 font-mono flex-shrink-0">
                      {item.timestamp}
                    </span>
                  </div>
                  <p className="text-xs text-slate-400 mt-1 leading-relaxed">
                    {item.message}
                  </p>
                </div>
              </div>
            </div>
          ))
        ) : (
          <div className="p-8 text-center text-slate-400 text-sm">
            No notifications in this category.
          </div>
        )}
      </div>
    </div>
  );
};
