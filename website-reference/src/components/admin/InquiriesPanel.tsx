"use client";

import { useCallback, useEffect, useState } from "react";
import {
  listInquiries,
  updateInquiryStatus,
  type InquiryRow,
} from "@/lib/admin-data";
import { Inbox, RefreshCw, Mail, Check } from "lucide-react";
import {
  AdminBadge,
  AdminCard,
  AdminEmpty,
  AdminPageHeader,
  AdminPill,
  AdminSkeleton,
  AdminToast,
  type AdminTone,
} from "./ui";

/**
 * Admin Contact inquiries tab.
 *
 * 2026-10-16 (Admin UI V2, slice U9 part 1): LAYOUT ONLY. Kit page header, cards, badges and pills; the current
 * status pill is gold and shows a check mark; slim toast (closes itself after 2.5 s); static skeleton.
 * listInquiries, updateInquiryStatus and every text are unchanged.
 */

const STATUSES = ["new", "read", "replied", "archived"] as const;

function statusTone(status: string): AdminTone {
  if (status === "new") return "cyan";
  if (status === "replied") return "green";
  return "neutral";
}

export default function InquiriesPanel() {
  const [items, setItems] = useState<InquiryRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState("");
  const [toast, setToast] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setItems(await listInquiries());
    setLoading(false);
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  async function setStatus(id: string, status: string) {
    setBusyId(id);
    const res = await updateInquiryStatus(id, status);
    setBusyId("");
    if (res.ok) {
      setToast(`Marked ${status}`);
      void load();
    } else {
      setToast(res.error || "Update failed");
    }
  }

  const newCount = items.filter((i) => i.status === "new").length;

  return (
    <div className="min-w-0 space-y-4">
      <AdminPageHeader icon={Inbox} title="Contact inquiries" subtitle={`${newCount} new · ${items.length} total`} />

      <AdminPill fullWidth icon={RefreshCw} onClick={() => void load()} disabled={loading}>
        {loading ? "Refreshing…" : "Refresh"}
      </AdminPill>

      {loading && items.length === 0 ? (
        <AdminSkeleton rows={3} />
      ) : items.length === 0 ? (
        <AdminEmpty
          icon={Inbox}
          title="Inbox empty"
          hint="Messages from the Contact page land in the inquiries table."
        />
      ) : (
        <div className="space-y-2">
          {items.map((q) => (
            <AdminCard key={q.id} className="space-y-2">
              <div className="flex items-start justify-between gap-2">
                <div className="min-w-0">
                  <p className="break-words text-sm font-semibold text-white">{q.name}</p>
                  <p className="flex items-start gap-1 break-all text-xs text-wisdom-muted">
                    <Mail className="mt-0.5 h-3.5 w-3.5 shrink-0" />
                    {q.email}
                  </p>
                </div>
                <AdminBadge tone={statusTone(q.status)} className="uppercase tracking-wider">
                  {q.status}
                </AdminBadge>
              </div>
              {q.service && <p className="break-words text-xs text-cyan-300/90">Interest: {q.service}</p>}
              <p className="whitespace-pre-wrap break-words text-sm leading-relaxed text-white/85">{q.message}</p>
              <p className="text-xs text-wisdom-muted">{new Date(q.created_at).toLocaleString()}</p>
              <div className="grid grid-cols-2 gap-2 pt-1">
                {STATUSES.map((st) => (
                  <AdminPill
                    key={st}
                    tone={q.status === st ? "gold" : "ghost"}
                    icon={q.status === st ? Check : undefined}
                    disabled={busyId === q.id || q.status === st}
                    onClick={() => void setStatus(q.id, st)}
                  >
                    {st}
                  </AdminPill>
                ))}
              </div>
            </AdminCard>
          ))}
        </div>
      )}

      <AdminToast message={toast || null} onClose={() => setToast("")} durationMs={2500} />
    </div>
  );
}
