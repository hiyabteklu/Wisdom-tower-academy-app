"use client";

/**
 * Notification drafts inbox (slice DRAFTS-1). Claude saves finished drafts (title, category, structured blocks,
 * optional path) into public.notification_drafts. This list shows the waiting ones; "Load" hands one to the
 * composer, which fills its boxes and preview. Nothing is sent from here. Table is admin-only (RLS).
 */

import { useCallback, useEffect, useState } from "react";
import { Inbox, RefreshCw } from "lucide-react";
import { supabase } from "@/lib/supabase";
import { AdminPill } from "./ui";

export type NotificationDraft = {
  id: string;
  title: string;
  category: string;
  content: unknown;
  url: string | null;
  note: string | null;
  created_at: string;
};

export default function NotificationDraftsInbox({
  onLoad,
  refreshKey,
}: {
  onLoad: (draft: NotificationDraft) => void;
  refreshKey: number;
}) {
  const [drafts, setDrafts] = useState<NotificationDraft[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);

  const load = useCallback(async () => {
    setError(null);
    const { data, error: err } = await supabase
      .from("notification_drafts")
      .select("id,title,category,content,url,note,created_at")
      .eq("status", "draft")
      .order("created_at", { ascending: false })
      .limit(20);
    if (err) {
      setError("Could not load drafts.");
      setDrafts([]);
      return;
    }
    setDrafts((data ?? []) as NotificationDraft[]);
  }, []);

  useEffect(() => {
    void load();
  }, [load, refreshKey]);

  async function discard(id: string) {
    setBusyId(id);
    await supabase.from("notification_drafts").delete().eq("id", id);
    setBusyId(null);
    void load();
  }

  return (
    <div className="space-y-2 rounded-2xl border border-white/10 bg-wisdom-card p-3">
      <div className="flex items-center justify-between gap-2">
        <p className="flex items-center gap-2 text-sm font-bold text-white">
          <Inbox className="h-4 w-4 text-amber-300" />
          Drafts{drafts ? ` (${drafts.length})` : ""}
        </p>
        <AdminPill onClick={() => void load()} icon={RefreshCw}>
          Refresh
        </AdminPill>
      </div>
      {error && <p className="text-xs text-rose-300">{error}</p>}
      {drafts && drafts.length === 0 && !error && (
        <p className="text-xs text-wisdom-muted">
          No drafts waiting. Ask Claude to write one and it appears here.
        </p>
      )}
      {drafts?.map((d) => (
        <div key={d.id} className="space-y-2 rounded-xl border border-white/10 p-3">
          <p className="break-words text-sm font-semibold text-white">{d.title}</p>
          <p className="text-xs text-wisdom-muted">
            {d.category}
            {d.note ? ` / ${d.note}` : ""}
          </p>
          <div className="flex gap-2">
            <AdminPill tone="primary" fullWidth onClick={() => onLoad(d)}>
              Load into composer
            </AdminPill>
            <AdminPill fullWidth disabled={busyId === d.id} onClick={() => void discard(d.id)}>
              Discard
            </AdminPill>
          </div>
        </div>
      ))}
    </div>
  );
}
