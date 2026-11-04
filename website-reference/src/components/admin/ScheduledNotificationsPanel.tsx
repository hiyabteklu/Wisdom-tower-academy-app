"use client";

import { useCallback, useEffect, useState } from "react";
import { Pencil, RefreshCw, Trash2 } from "lucide-react";
import { supabase } from "@/lib/supabase";
import { AdminBadge, AdminCard, AdminEmpty, AdminPill, AdminSectionTitle, AdminSheet, AdminSkeleton, AdminToast, type AdminTone } from "./ui";

/**
 * Admin list of scheduled notifications: see the schedule, edit a waiting row, cancel it.
 *
 * 2026-10-15 (notifications v2, slice R3-b4, session R3-E): the old schedule form was removed; scheduling now
 * happens in `NotificationComposerV2` (step 5 "When"). This panel lists with `admin_list_scheduled_notifications_v2`
 * and cancels with `admin_cancel_scheduled_notification` (admin's own session; the database checks admin).
 * Old rows (made before v2, no category) are listed and can be cancelled but not edited.
 * The database scheduler (pg_cron, every minute) calls /api/notifications/dispatch, which sends due rows.
 * All times are shown in Ethiopia time (EAT, UTC+3).
 *
 * 2026-10-15 (Admin mobile redesign, slice A6): LAYOUT ONLY (bordered cards, 44 px pills, wrapped text).
 *
 * 2026-10-16 (Admin UI V2, slice U4 part 2): LAYOUT ONLY. Kit cards, kit badges and pills, a bottom sheet
 * instead of the browser confirm box before Cancel, a slim toast instead of the inline message, a static
 * skeleton while loading. Exports, state and database calls are unchanged.
 */

export type ScheduledStatus = "pending" | "sending" | "sent" | "failed" | "cancelled";

export type ScheduledRowV2 = {
  id: string;
  title: string;
  body: string;
  url: string | null;
  send_at: string;
  status: ScheduledStatus;
  category: string | null;
  content: unknown;
  audiences: string[] | null;
  audience_scope: string | null;
  package_id: string | null;
  competition_id: string | null;
  created_at: string;
  sent_at: string | null;
  last_error: string | null;
  attempts: number;
};

export function formatEat(iso: string | null): string {
  if (!iso) return "";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return (
    d.toLocaleString("en-GB", {
      timeZone: "Africa/Addis_Ababa",
      day: "2-digit",
      month: "short",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
      hour12: false,
    }) + " EAT"
  );
}

export function friendlyScheduleError(message: string): string {
  if (message.includes("admin_only")) {
    return "Only an admin account can do this. Please sign in again as admin.";
  }
  if (message.includes("send_at_must_be_in_future")) return "The send time must be in the future.";
  if (message.includes("title_required")) return "Please enter a title.";
  if (message.includes("body_required")) return "Please enter a message.";
  if (message.includes("url_required_for_material")) return "Pick where this material notice should open.";
  if (message.includes("bad_content") || message.includes("content_too_large")) {
    return "The message content is not valid or is too long.";
  }
  if (message.includes("not_pending_or_not_found")) {
    return "This notification is no longer waiting to be sent.";
  }
  return message || "Something went wrong.";
}

function statusTone(status: ScheduledStatus): AdminTone {
  switch (status) {
    case "pending":
      return "cyan";
    case "sending":
      return "gold";
    case "sent":
      return "green";
    case "failed":
      return "red";
    default:
      return "neutral";
  }
}

function audienceText(r: ScheduledRowV2): string {
  if (r.audiences && r.audiences.length > 0) {
    return r.audiences.map((a) => (a.startsWith("custom:") ? "custom audience" : a)).join(", ");
  }
  return r.audience_scope ? r.audience_scope : "everyone";
}

export default function ScheduledNotificationsPanel({
  refreshKey = 0,
  onEdit,
}: {
  refreshKey?: number;
  onEdit?: (row: ScheduledRowV2) => void;
}) {
  const [rows, setRows] = useState<ScheduledRowV2[]>([]);
  const [loading, setLoading] = useState(true);
  const [feedback, setFeedback] = useState<{ type: "success" | "error"; message: string } | null>(null);
  const [pendingCancel, setPendingCancel] = useState<ScheduledRowV2 | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    const { data, error } = await supabase.rpc("admin_list_scheduled_notifications_v2", {
      p_limit: 50,
    });
    if (error) {
      setFeedback({ type: "error", message: friendlyScheduleError(error.message) });
      setRows([]);
    } else {
      setRows(Array.isArray(data) ? (data as ScheduledRowV2[]) : []);
    }
    setLoading(false);
  }, []);

  useEffect(() => {
    void load();
  }, [load, refreshKey]);

  async function handleCancel(id: string) {
    setFeedback(null);
    const { error } = await supabase.rpc("admin_cancel_scheduled_notification", { p_id: id });
    if (error) {
      setFeedback({ type: "error", message: friendlyScheduleError(error.message) });
    } else {
      setFeedback({ type: "success", message: "Cancelled." });
    }
    void load();
  }

  async function confirmCancel() {
    const target = pendingCancel;
    setPendingCancel(null);
    if (target) await handleCancel(target.id);
  }

  return (
    <div>
      <AdminSectionTitle
        action={
          <button
            type="button"
            onClick={() => void load()}
            disabled={loading}
            aria-label="Refresh"
            className="grid h-11 w-11 cursor-pointer place-items-center rounded-full border border-white/10 bg-wisdom-card text-white active:bg-white/10 disabled:opacity-50"
          >
            <RefreshCw className="h-4 w-4" />
          </button>
        }
      >
        Scheduled Notifications ({rows.length})
      </AdminSectionTitle>

      {loading ? (
        <AdminSkeleton rows={3} />
      ) : rows.length === 0 ? (
        <AdminEmpty title="Nothing scheduled yet." />
      ) : (
        <div className="space-y-2">
          {rows.map((r) => {
            const canEdit = Boolean(onEdit && r.category);
            return (
              <AdminCard key={r.id}>
                <div className="mb-1 flex flex-wrap items-center gap-x-2 gap-y-1">
                  <AdminBadge tone={statusTone(r.status)} className="uppercase tracking-wider">
                    {r.status}
                  </AdminBadge>
                  {r.category && (
                    <AdminBadge tone="gold" className="uppercase tracking-wider">
                      {r.category}
                    </AdminBadge>
                  )}
                </div>
                <p className="text-xs text-slate-400">{formatEat(r.send_at)}</p>
                <p className="break-all font-mono text-xs text-slate-500">
                  {audienceText(r)}
                  {r.package_id ? ` / package ${r.package_id}` : ""}
                </p>
                <h4 className="mt-1.5 break-words text-sm font-bold text-white">{r.title}</h4>
                <p className="mt-0.5 line-clamp-2 break-words text-sm text-wisdom-muted">{r.body}</p>
                {r.url && <p className="mt-1 break-all text-xs text-cyan-300">Link: {r.url}</p>}
                {r.last_error && <p className="mt-1 break-words text-xs text-rose-300">{r.last_error}</p>}

                {r.status === "pending" && (
                  <div className={`mt-3 grid gap-2 ${canEdit ? "grid-cols-2" : "grid-cols-1"}`}>
                    {canEdit && onEdit && (
                      <AdminPill tone="gold" icon={Pencil} onClick={() => onEdit(r)}>
                        Edit
                      </AdminPill>
                    )}
                    <AdminPill tone="danger" icon={Trash2} onClick={() => setPendingCancel(r)}>
                      Cancel
                    </AdminPill>
                  </div>
                )}
              </AdminCard>
            );
          })}
        </div>
      )}

      {/* Confirm before cancelling a waiting notification */}
      <AdminSheet
        open={pendingCancel !== null}
        onClose={() => setPendingCancel(null)}
        title="Cancel scheduled notification"
        footer={
          <div className="flex gap-2">
            <AdminPill fullWidth onClick={() => setPendingCancel(null)}>
              Keep it
            </AdminPill>
            <AdminPill fullWidth tone="danger" icon={Trash2} onClick={() => void confirmCancel()}>
              Cancel it
            </AdminPill>
          </div>
        }
      >
        <p className="text-sm text-wisdom-muted">Cancel this scheduled notification? It will not be sent.</p>
        <p className="mt-2 break-words text-sm font-semibold text-white">{pendingCancel?.title}</p>
        {pendingCancel ? <p className="mt-0.5 text-xs text-slate-400">{formatEat(pendingCancel.send_at)}</p> : null}
      </AdminSheet>

      <AdminToast
        message={feedback ? feedback.message : null}
        tone={feedback ? (feedback.type === "success" ? "green" : "red") : "neutral"}
        onClose={() => setFeedback(null)}
      />
    </div>
  );
}
