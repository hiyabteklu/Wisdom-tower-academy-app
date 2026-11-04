"use client";

import { useEffect, useState, useCallback } from "react";
import {
  Trash2,
  ExternalLink,
  RefreshCw,
  Send,
  CalendarClock,
  History as HistoryIcon,
  ChevronLeft,
} from "lucide-react";
import type { NotificationItem, NotificationType } from "@/lib/notifications";
import { supabase } from "@/lib/supabase";
import ScheduledNotificationsPanel, { type ScheduledRowV2 } from "./ScheduledNotificationsPanel";
import NotificationComposerV2 from "./NotificationComposerV2";
import {
  AdminBadge,
  AdminCard,
  AdminEmpty,
  AdminPill,
  AdminSectionTitle,
  AdminSheet,
  AdminSkeleton,
  AdminTile,
  AdminTileGrid,
  type AdminTone,
} from "./ui";

type PanelView = "home" | "compose" | "scheduled" | "history";

/**
 * Admin Notifications tab.
 * 2026-10-15 (notifications v2): R3-b2 replaced the four-preset form by `NotificationComposerV2`;
 * R3-b4 moved scheduling into the composer (step 5 "When") and made the scheduled list editable:
 * pressing Edit on a waiting row loads it into the composer. History is unchanged
 * apart from a badge for the `contest` type.
 *
 * 2026-10-15 (Admin mobile redesign, slice A6): LAYOUT ONLY (compact header, no animation, wrapped text).
 *
 * 2026-10-16 (Admin UI V2, slice U4 part 1): landing as three tiles (Compose, Scheduled, History) in the
 * home-grid look. LAYOUT ONLY: the composer and the scheduled list are the same components with the same props;
 * they stay mounted (only hidden) so a half-written message is not lost when the admin looks at another tile.
 * History uses kit cards and a confirm bottom sheet before a delete (the delete call itself is unchanged).
 *
 * 2026-10-16 (owner request): the "Android App (Kotlin / Capacitor) FCM Endpoint" developer note (an example
 * request for the app programmer, not an admin tool) was removed from the landing. It was static text only;
 * the endpoint `/api/notifications/register-device` itself is untouched.
 */
export default function NotificationsPanel() {
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [loaded, setLoaded] = useState(false);
  const [editRow, setEditRow] = useState<ScheduledRowV2 | null>(null);
  const [scheduleRefresh, setScheduleRefresh] = useState(0);
  const [view, setView] = useState<PanelView>("home");
  const [pendingDelete, setPendingDelete] = useState<NotificationItem | null>(null);

  const loadNotifications = useCallback(async () => {
    setLoading(true);
    try {
      // Listing every notification is admin-only: send the signed-in admin's access token.
      const { data: sessionData } = await supabase.auth.getSession();
      const accessToken = sessionData.session?.access_token;
      const res = await fetch("/api/notifications?admin=true", {
        headers: accessToken ? { Authorization: `Bearer ${accessToken}` } : {},
      });
      const data = await res.json();
      if (data.ok && Array.isArray(data.notifications)) {
        setNotifications(data.notifications);
        setLoaded(true);
      }
    } catch {
      /* ignore */
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadNotifications();
  }, [loadNotifications]);

  async function handleDelete(id: string) {
    try {
      // Deleting is admin-only: send the signed-in admin's access token.
      const { data: sessionData } = await supabase.auth.getSession();
      const accessToken = sessionData.session?.access_token;
      const res = await fetch(`/api/notifications?id=${encodeURIComponent(id)}`, {
        method: "DELETE",
        headers: accessToken ? { Authorization: `Bearer ${accessToken}` } : {},
      });
      if (res.ok) {
        setNotifications((prev) => prev.filter((n) => n.id !== id));
      }
    } catch {
      /* ignore */
    }
  }

  async function confirmDelete() {
    const target = pendingDelete;
    setPendingDelete(null);
    if (target) await handleDelete(target.id);
  }

  // Pressing Edit on a waiting scheduled row loads it into the composer, so show the composer.
  function startEdit(row: ScheduledRowV2 | null) {
    setEditRow(row);
    if (row) setView("compose");
  }

  const getTypeTone = (t: NotificationType): AdminTone => {
    switch (t) {
      case "material":
        return "cyan";
      case "admin":
        return "gold";
      case "contest":
        return "gold";
      case "payment":
        return "green";
      default:
        return "neutral";
    }
  };

  const titleOfView: Record<Exclude<PanelView, "home">, string> = {
    compose: "Compose",
    scheduled: "Scheduled",
    history: "History",
  };

  return (
    <div className="space-y-4">
      {view === "home" ? (
        <>
          <p className="text-xs leading-relaxed text-wisdom-muted">
            Publish real-time announcements, content releases, and contest alerts directly to the Android app and website.
          </p>
          <AdminTileGrid>
            <AdminTile icon={Send} label="Compose" onClick={() => setView("compose")} />
            <AdminTile icon={CalendarClock} label="Scheduled" onClick={() => setView("scheduled")} />
            <AdminTile
              icon={HistoryIcon}
              label="History"
              badge={loaded ? <AdminBadge tone="gold">{notifications.length}</AdminBadge> : undefined}
              onClick={() => setView("history")}
            />
          </AdminTileGrid>
        </>
      ) : (
        <div className="flex items-center gap-2">
          <AdminPill icon={ChevronLeft} onClick={() => setView("home")}>
            Back
          </AdminPill>
          <h3 className="min-w-0 flex-1 truncate text-base font-bold text-white">{titleOfView[view]}</h3>
          {view === "history" ? (
            <button
              type="button"
              onClick={() => loadNotifications()}
              disabled={loading}
              aria-label="Refresh"
              className="grid h-11 w-11 shrink-0 cursor-pointer place-items-center rounded-full border border-white/10 bg-wisdom-card text-white active:bg-white/10 disabled:opacity-50"
            >
              <RefreshCw className="h-4 w-4" />
            </button>
          ) : null}
        </div>
      )}

      {/* Composer v2 (send now, schedule, edit a waiting row). Kept mounted so a draft survives. */}
      <div className={view === "compose" ? "" : "hidden"}>
        <NotificationComposerV2
          editRow={editRow}
          onEditDone={() => setEditRow(null)}
          onSent={() => void loadNotifications()}
          onScheduled={() => setScheduleRefresh((n) => n + 1)}
        />
      </div>

      {/* Scheduled notifications: list, edit, cancel */}
      <div className={view === "scheduled" ? "" : "hidden"}>
        <ScheduledNotificationsPanel refreshKey={scheduleRefresh} onEdit={startEdit} />
      </div>

      {/* History */}
      {view === "history" ? (
        <div>
          <AdminSectionTitle>Notification Broadcast History ({notifications.length})</AdminSectionTitle>
          {loading && notifications.length === 0 ? (
            <AdminSkeleton rows={3} />
          ) : notifications.length === 0 ? (
            <AdminEmpty title="No notifications broadcasted yet." />
          ) : (
            <div className="space-y-2">
              {notifications.map((n) => (
                <AdminCard key={n.id} className="flex items-start gap-2">
                  <div className="min-w-0 flex-1">
                    <div className="mb-1 flex flex-wrap items-center gap-x-2 gap-y-1">
                      <AdminBadge tone={getTypeTone(n.type)} className="uppercase tracking-wider">
                        {n.type}
                      </AdminBadge>
                      <span className="break-all font-mono text-xs text-slate-400">
                        Target: {n.target === "all" ? "All Users" : n.target}
                      </span>
                    </div>
                    <p className="mb-1 text-xs text-slate-500">{new Date(n.createdAt).toLocaleString()}</p>
                    <h4 className="break-words text-sm font-bold text-white">{n.title}</h4>
                    <p className="mt-0.5 line-clamp-2 break-words text-sm text-wisdom-muted">{n.body}</p>
                    {n.url && (
                      <a href={n.url} className="mt-1.5 inline-flex items-start gap-1 break-all text-xs text-cyan-300">
                        <span>Link: {n.url}</span>
                        <ExternalLink className="mt-0.5 h-3 w-3 shrink-0" />
                      </a>
                    )}
                  </div>

                  <button
                    type="button"
                    onClick={() => setPendingDelete(n)}
                    title="Delete notification"
                    aria-label="Delete notification"
                    className="grid h-11 w-11 shrink-0 cursor-pointer place-items-center rounded-full text-slate-400 active:bg-rose-500/10 active:text-rose-400"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </AdminCard>
              ))}
            </div>
          )}
        </div>
      ) : null}

      {/* Confirm before deleting a broadcast */}
      <AdminSheet
        open={pendingDelete !== null}
        onClose={() => setPendingDelete(null)}
        title="Delete notification"
        footer={
          <div className="flex gap-2">
            <AdminPill fullWidth onClick={() => setPendingDelete(null)}>
              Cancel
            </AdminPill>
            <AdminPill fullWidth tone="danger" icon={Trash2} onClick={() => void confirmDelete()}>
              Delete
            </AdminPill>
          </div>
        }
      >
        <p className="break-words text-sm font-semibold text-white">{pendingDelete?.title}</p>
        <p className="mt-1 line-clamp-3 break-words text-sm text-wisdom-muted">{pendingDelete?.body}</p>
      </AdminSheet>
    </div>
  );
}
