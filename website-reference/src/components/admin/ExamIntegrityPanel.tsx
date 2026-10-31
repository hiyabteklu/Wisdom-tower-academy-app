"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { supabase } from "@/lib/supabase";
import { AlertTriangle, CheckCircle2, Clock3, Eye, RefreshCw, ShieldCheck } from "lucide-react";

type EventRow = {
  id: string; kind: string; detail: Record<string, unknown> | null;
  created_at: string; away_ms: number | null;
};
type ReportRow = {
  entry_id: string; user_id: string; student_name: string; email: string | null;
  student_id: string | null; competition_title: string; mode: string; attempt_status: string;
  started_at: string; submitted_at: string | null; score: number | null; total_count: number | null;
  flag_count: number; leave_count: number; total_away_ms: number; longest_away_ms: number;
  last_flag_at: string; reviewed_at: string | null; review_note: string | null; events: EventRow[];
};

function duration(ms: number | null | undefined) {
  const seconds = Math.max(0, Math.floor((ms || 0) / 1000));
  const h = Math.floor(seconds / 3600), m = Math.floor((seconds % 3600) / 60), s = seconds % 60;
  return h ? `${h}h ${m}m ${s}s` : m ? `${m}m ${s}s` : `${s}s`;
}
function eventLabel(kind: string) {
  return ({ app_background: "Left the exam", app_foreground: "Returned to the exam",
    overlay_open: "Overlay opened", copy_attempt: "Copy attempt", other: "Other activity" } as Record<string,string>)[kind] || kind.replace(/_/g, " ");
}

export default function ExamIntegrityPanel() {
  const [rows, setRows] = useState<ReportRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [filter, setFilter] = useState<"all" | "pending" | "reviewed">("pending");
  const [expanded, setExpanded] = useState<string | null>(null);
  const [busy, setBusy] = useState<string | null>(null);
  const [notes, setNotes] = useState<Record<string,string>>({});

  const load = useCallback(async () => {
    setLoading(true); setError("");
    const { data, error: rpcError } = await supabase.rpc("admin_exam_integrity_report");
    if (rpcError) setError(rpcError.message);
    else setRows((data || []) as ReportRow[]);
    setLoading(false);
  }, []);
  useEffect(() => { void load(); }, [load]);

  const visible = useMemo(() => rows.filter(r =>
    filter === "all" || (filter === "pending" ? !r.reviewed_at : !!r.reviewed_at)
  ), [rows, filter]);
  const pendingCount = rows.filter(r => !r.reviewed_at).length;
  const totalLeaves = rows.reduce((n,r) => n + (r.leave_count || 0), 0);
  const totalFlags = rows.reduce((n,r) => n + (r.flag_count || 0), 0);

  async function markReviewed(row: ReportRow) {
    setBusy(row.entry_id);
    const { error: rpcError } = await supabase.rpc("admin_review_exam_integrity", {
      p_entry_id: row.entry_id, p_review_note: notes[row.entry_id] || null
    });
    setBusy(null);
    if (rpcError) setError(rpcError.message); else await load();
  }

  return <div className="space-y-6">
    <div className="flex flex-wrap items-start justify-between gap-3">
      <div><div className="flex items-center gap-2"><ShieldCheck className="h-5 w-5 text-amber-300"/><h2 className="text-xl font-bold text-white">Exam integrity review</h2></div>
        <p className="mt-1 text-sm text-white/55">App exits and other recorded activity during ranked exams. Flags are review signals, not automatic proof of cheating.</p></div>
      <button onClick={() => void load()} disabled={loading} className="inline-flex min-h-10 items-center gap-2 rounded-xl border border-white/15 px-3 text-sm text-white/80"><RefreshCw className={`h-4 w-4 ${loading ? "animate-spin" : ""}`}/>Refresh</button>
    </div>

    <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
      <div className="rounded-2xl border border-amber-400/25 bg-amber-400/[0.07] p-4"><p className="text-xs text-amber-100/65">Awaiting review</p><p className="mt-1 text-3xl font-bold text-amber-200 tabular-nums">{pendingCount}</p></div>
      <div className="rounded-2xl border border-white/10 bg-white/[0.03] p-4"><p className="text-xs text-white/55">Flagged attempts</p><p className="mt-1 text-3xl font-bold text-white tabular-nums">{rows.length}</p></div>
      <div className="rounded-2xl border border-white/10 bg-white/[0.03] p-4"><p className="text-xs text-white/55">Recorded leave events</p><p className="mt-1 text-3xl font-bold text-white tabular-nums">{totalLeaves}</p></div>
      <div className="rounded-2xl border border-white/10 bg-white/[0.03] p-4"><p className="text-xs text-white/55">Total activity flags</p><p className="mt-1 text-3xl font-bold text-white tabular-nums">{totalFlags}</p></div>
    </div>

    {error && <p className="rounded-xl border border-rose-400/30 bg-rose-400/10 p-3 text-sm text-rose-200">{error}</p>}
    <div className="flex flex-wrap items-center justify-between gap-3 border-b border-white/10 pb-3">
      <div className="flex gap-2">
        {(["pending","all","reviewed"] as const).map(v => <button key={v} onClick={() => setFilter(v)} className={`rounded-lg px-3 py-2 text-xs font-semibold capitalize ${filter === v ? "bg-amber-400/15 text-amber-200 border border-amber-300/30" : "border border-white/10 text-white/55"}`}>{v === "pending" ? `Needs review (${pendingCount})` : v === "all" ? `All attempts (${rows.length})` : "Reviewed"}</button>)}
      </div>
      <span className="text-xs text-white/45">{visible.length} records</span>
    </div>

    {loading && rows.length === 0 ? <p className="py-12 text-center text-sm text-white/50">Loading exam activity…</p> :
      visible.length === 0 ? <div className="rounded-2xl border border-dashed border-white/15 p-10 text-center"><CheckCircle2 className="mx-auto h-8 w-8 text-emerald-300/70"/><p className="mt-2 font-semibold text-white">No records in this view</p><p className="mt-1 text-xs text-white/45">When a ranked exam records an app exit or other flag, it will appear here.</p></div> :
      <div className="space-y-3">{visible.map(row => <article key={row.entry_id} className="overflow-hidden rounded-2xl border border-white/10 bg-white/[0.025]">
        <div className="flex flex-wrap items-start justify-between gap-3 p-4">
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-2"><h3 className="font-bold text-white">{row.student_name || "Student"}</h3>
              {row.reviewed_at ? <span className="rounded-md border border-emerald-400/25 bg-emerald-400/10 px-2 py-0.5 text-[10px] font-bold text-emerald-200">Reviewed</span> : <span className="rounded-md border border-amber-400/25 bg-amber-400/10 px-2 py-0.5 text-[10px] font-bold text-amber-200">Needs review</span>}
            </div>
            <p className="mt-0.5 truncate text-xs text-white/50">{row.email || row.student_id || row.user_id}</p>
            <p className="mt-2 text-sm text-white/75">{row.competition_title}</p>
            <p className="mt-1 text-[11px] text-white/40">Started {new Date(row.started_at).toLocaleString()} · {row.attempt_status}{row.score != null ? ` · Score ${row.score}/${row.total_count ?? "?"}` : ""}</p>
          </div>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 min-w-[250px]">
            <div className="rounded-xl bg-white/[0.04] p-2"><p className="text-[10px] text-white/40">Leaves</p><p className="font-bold text-white">{row.leave_count}</p></div>
            <div className="rounded-xl bg-white/[0.04] p-2"><p className="text-[10px] text-white/40">Away total</p><p className="font-bold text-white">{duration(row.total_away_ms)}</p></div>
            <div className="rounded-xl bg-white/[0.04] p-2"><p className="text-[10px] text-white/40">Longest</p><p className="font-bold text-white">{duration(row.longest_away_ms)}</p></div>
            <div className="rounded-xl bg-white/[0.04] p-2"><p className="text-[10px] text-white/40">Flags</p><p className="font-bold text-amber-200">{row.flag_count}</p></div>
          </div>
        </div>
        <div className="flex flex-wrap items-center justify-between gap-2 border-t border-white/8 px-4 py-3">
          <button onClick={() => setExpanded(expanded === row.entry_id ? null : row.entry_id)} className="inline-flex items-center gap-2 text-xs font-semibold text-cyan-200"><Eye className="h-4 w-4"/>{expanded === row.entry_id ? "Hide event timeline" : "View event timeline"}</button>
          {!row.reviewed_at && <button disabled={busy === row.entry_id} onClick={() => void markReviewed(row)} className="rounded-lg border border-emerald-400/25 bg-emerald-400/10 px-3 py-2 text-xs font-bold text-emerald-200 disabled:opacity-50">{busy === row.entry_id ? "Saving…" : "Mark reviewed"}</button>}
        </div>
        {expanded === row.entry_id && <div className="space-y-3 border-t border-white/8 bg-black/10 p-4">
          <div className="flex items-center gap-2 text-xs font-bold text-white/75"><Clock3 className="h-4 w-4"/>Full activity timeline</div>
          {(row.events || []).map(ev => <div key={ev.id} className="flex flex-wrap items-start justify-between gap-2 border-l-2 border-white/15 pl-3 py-1">
            <div><p className="text-sm text-white">{eventLabel(ev.kind)}</p><p className="text-[11px] text-white/40">{new Date(ev.created_at).toLocaleString()}</p></div>
            {ev.kind === "app_background" && <span className="text-xs font-semibold text-amber-200">{ev.away_ms == null ? "No return event recorded" : `Away ${duration(ev.away_ms)}`}</span>}
          </div>)}
          {!row.reviewed_at && <div className="pt-2"><label className="mb-1 block text-xs text-white/55">Review note (optional)</label><textarea value={notes[row.entry_id] || ""} onChange={e => setNotes(s => ({...s,[row.entry_id]:e.target.value}))} rows={2} placeholder="Record what you checked or why this was cleared…" className="w-full rounded-xl border border-white/10 bg-white/[0.04] p-3 text-sm text-white outline-none focus:border-amber-300/40"/><button disabled={busy === row.entry_id} onClick={() => void markReviewed(row)} className="mt-2 rounded-lg bg-emerald-500/15 px-3 py-2 text-xs font-bold text-emerald-200 border border-emerald-400/25">Save review</button></div>}
          {row.reviewed_at && <p className="text-xs text-emerald-200/75">Reviewed {new Date(row.reviewed_at).toLocaleString()}{row.review_note ? ` · ${row.review_note}` : ""}</p>}
        </div>}
      </article>)}</div>}
    <p className="text-[11px] leading-relaxed text-white/35"><AlertTriangle className="mr-1 inline h-3 w-3"/>A background event can also happen because of an incoming call, system dialog, accidental gesture, or legitimate interruption. Review context before taking action. Away duration is calculated from the recorded background and next foreground event; an unmatched exit has no completed duration.</p>
  </div>;
}
