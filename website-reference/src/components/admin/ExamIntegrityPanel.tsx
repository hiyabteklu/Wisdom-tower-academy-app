"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { supabase } from "@/lib/supabase";
import { AlertTriangle, CheckCircle2, Clock3, RefreshCw, ShieldCheck } from "lucide-react";

type EventRow = {
  id: string;
  kind: string;
  detail: Record<string, unknown> | null;
  created_at: string;
  away_ms: number | null;
};
type ReportRow = {
  entry_id: string;
  user_id: string;
  student_name: string;
  email: string | null;
  student_id: string | null;
  competition_title: string;
  mode: string;
  attempt_status: string;
  started_at: string;
  submitted_at: string | null;
  score: number | null;
  total_count: number | null;
  flag_count: number;
  leave_count: number;
  total_away_ms: number;
  longest_away_ms: number;
  last_flag_at: string;
  reviewed_at: string | null;
  review_note: string | null;
  events: EventRow[];
};

function duration(ms: number | null | undefined) {
  const seconds = Math.max(0, Math.floor((ms || 0) / 1000));
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = seconds % 60;
  return h ? h + "h " + m + "m " + s + "s" : m ? m + "m " + s + "s" : s + "s";
}
function formatEAT(iso: string) {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return iso;
  return date.toLocaleString("en-GB", {
    timeZone: "Africa/Addis_Ababa",
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  }) + " EAT";
}
function eventLabel(kind: string) {
  return ({
    app_background: "Left the app",
    app_foreground: "Returned to the exam",
    overlay_open: "Overlay opened",
    copy_attempt: "Copy attempt",
    other: "Other activity",
  } as Record<string, string>)[kind] || kind.replace(/_/g, " ");
}

export default function ExamIntegrityPanel() {
  const [rows, setRows] = useState<ReportRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [filter, setFilter] = useState<"all" | "pending" | "reviewed">("pending");
  const [expanded, setExpanded] = useState<string | null>(null);
  const [busy, setBusy] = useState<string | null>(null);
  const [notes, setNotes] = useState<Record<string, string>>({});

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    const { data, error: rpcError } = await supabase.rpc("admin_exam_integrity_report");
    if (rpcError) setError(rpcError.message);
    else setRows((data || []) as ReportRow[]);
    setLoading(false);
  }, []);

  useEffect(() => { void load(); }, [load]);

  const visible = useMemo(() => rows.filter((row) =>
    filter === "all" || (filter === "pending" ? !row.reviewed_at : !!row.reviewed_at)
  ), [rows, filter]);

  const pendingRows = rows.filter((row) => !row.reviewed_at);
  const pendingCount = pendingRows.length;
  const pendingStudents = new Set(pendingRows.map((row) => row.user_id)).size;
  const flaggedStudents = new Set(rows.map((row) => row.user_id)).size;
  const totalLeaves = rows.reduce((sum, row) => sum + (row.leave_count || 0), 0);
  const totalFlags = rows.reduce((sum, row) => sum + (row.flag_count || 0), 0);

  async function markReviewed(row: ReportRow) {
    setBusy(row.entry_id);
    const { error: rpcError } = await supabase.rpc("admin_review_exam_integrity", {
      p_entry_id: row.entry_id,
      p_review_note: notes[row.entry_id] || null,
    });
    setBusy(null);
    if (rpcError) setError(rpcError.message);
    else await load();
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <div className="flex items-center gap-2">
            <ShieldCheck className="h-5 w-5 text-amber-300" />
            <h2 className="text-xl font-bold text-white">Exam integrity review</h2>
            {pendingStudents > 0 && (
              <span className="rounded-md border border-rose-400/30 bg-rose-400/10 px-2 py-0.5 text-xs font-extrabold text-rose-200">
                {pendingStudents} students to review
              </span>
            )}
          </div>
          <p className="mt-1 max-w-3xl text-sm text-white/55">
            Ranked-exam app exits, returns and other recorded activity. Each student's leave count,
            total time away, longest absence and recent event timeline are visible without opening a separate details screen.
          </p>
        </div>
        <button onClick={() => void load()} disabled={loading}
          className="inline-flex min-h-10 items-center gap-2 rounded-xl border border-white/15 px-3 text-sm text-white/80 disabled:opacity-50">
          <RefreshCw className={"h-4 w-4 " + (loading ? "animate-spin" : "")} /> Refresh
        </button>
      </div>

      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <div className="rounded-2xl border border-amber-400/25 bg-amber-400/[0.07] p-4">
          <p className="text-xs text-amber-100/65">Students awaiting review</p>
          <p className="mt-1 text-3xl font-bold tabular-nums text-amber-200">{pendingStudents}</p>
        </div>
        <div className="rounded-2xl border border-white/10 bg-white/[0.03] p-4">
          <p className="text-xs text-white/55">Flagged students</p>
          <p className="mt-1 text-3xl font-bold tabular-nums text-white">{flaggedStudents}</p>
        </div>
        <div className="rounded-2xl border border-white/10 bg-white/[0.03] p-4">
          <p className="text-xs text-white/55">Recorded app exits</p>
          <p className="mt-1 text-3xl font-bold tabular-nums text-white">{totalLeaves}</p>
        </div>
        <div className="rounded-2xl border border-white/10 bg-white/[0.03] p-4">
          <p className="text-xs text-white/55">Total activity events</p>
          <p className="mt-1 text-3xl font-bold tabular-nums text-white">{totalFlags}</p>
        </div>
      </div>

      {error && <p className="rounded-xl border border-rose-400/30 bg-rose-400/10 p-3 text-sm text-rose-200">{error}</p>}

      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-white/10 pb-3">
        <div className="flex flex-wrap gap-2">
          {(["pending", "all", "reviewed"] as const).map((value) => (
            <button key={value} onClick={() => setFilter(value)}
              className={"rounded-lg border px-3 py-2 text-xs font-semibold capitalize " +
                (filter === value ? "border-amber-300/30 bg-amber-400/15 text-amber-100" : "border-white/10 text-white/55")}>
              {value === "pending" ? "Needs review (" + pendingCount + ")" : value === "all" ? "All attempts (" + rows.length + ")" : "Reviewed"}
            </button>
          ))}
        </div>
        <span className="text-xs text-white/45">
          {visible.length} attempts · {new Set(visible.map((row) => row.user_id)).size} students
        </span>
      </div>

      {loading && rows.length === 0 ? (
        <p className="py-12 text-center text-sm text-white/50">Loading exam activity…</p>
      ) : visible.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-white/15 p-10 text-center">
          <CheckCircle2 className="mx-auto h-8 w-8 text-emerald-300/70" />
          <p className="mt-2 font-semibold text-white">No records in this view</p>
          <p className="mt-1 text-xs text-white/45">Flagged ranked-exam activity will appear here automatically.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {visible.map((row) => {
            const events = row.events || [];
            const showAll = expanded === row.entry_id;
            const visibleEvents = showAll ? events : events.slice(-4);
            return (
              <article key={row.entry_id} className="overflow-hidden rounded-2xl border border-white/10 bg-white/[0.025]">
                <div className="flex flex-wrap items-start justify-between gap-3 p-4">
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <h3 className="font-bold text-white">{row.student_name || "Student"}</h3>
                      {row.reviewed_at
                        ? <span className="rounded-md border border-emerald-400/25 bg-emerald-400/10 px-2 py-0.5 text-[10px] font-bold text-emerald-200">Reviewed</span>
                        : <span className="rounded-md border border-amber-400/25 bg-amber-400/10 px-2 py-0.5 text-[10px] font-bold text-amber-200">Needs review</span>}
                    </div>
                    <p className="mt-0.5 truncate text-xs text-white/50">{row.email || row.student_id || row.user_id}</p>
                    <p className="mt-2 text-sm text-white/75">{row.competition_title}</p>
                    <p className="mt-1 text-[11px] text-white/40">
                      Started {formatEAT(row.started_at)} · {row.attempt_status}
                      {row.score != null ? " · Score " + row.score + "/" + (row.total_count ?? "?") : ""}
                    </p>
                  </div>

                  <div className="grid min-w-[250px] grid-cols-2 gap-2 sm:grid-cols-4">
                    <div className="rounded-xl bg-white/[0.04] p-2">
                      <p className="text-[10px] text-white/40">Leave count</p>
                      <p className="font-bold text-white">{row.leave_count}</p>
                    </div>
                    <div className="rounded-xl bg-white/[0.04] p-2">
                      <p className="text-[10px] text-white/40">Total away</p>
                      <p className="font-bold text-white">{duration(row.total_away_ms)}</p>
                    </div>
                    <div className="rounded-xl bg-white/[0.04] p-2">
                      <p className="text-[10px] text-white/40">Longest away</p>
                      <p className="font-bold text-white">{duration(row.longest_away_ms)}</p>
                    </div>
                    <div className="rounded-xl bg-white/[0.04] p-2">
                      <p className="text-[10px] text-white/40">Activity flags</p>
                      <p className="font-bold text-amber-200">{row.flag_count}</p>
                    </div>
                  </div>
                </div>

                <div className="space-y-3 border-t border-white/8 bg-black/10 px-4 py-3">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <div className="flex items-center gap-2 text-xs font-bold text-white/75">
                      <Clock3 className="h-4 w-4" /> Leave / return timeline
                      <span className="font-normal text-white/40">({events.length} events · Ethiopia time)</span>
                    </div>
                    <div className="flex items-center gap-3">
                      {events.length > 4 && (
                        <button onClick={() => setExpanded(showAll ? null : row.entry_id)}
                          className="text-xs font-semibold text-cyan-200">
                          {showAll ? "Show recent only" : "Show all " + events.length}
                        </button>
                      )}
                      {!row.reviewed_at && (
                        <button onClick={() => setExpanded(showAll ? null : row.entry_id)}
                          className="text-xs font-semibold text-white/55">
                          {showAll ? "Hide note" : "Add review note"}
                        </button>
                      )}
                      {!row.reviewed_at && (
                        <button disabled={busy === row.entry_id} onClick={() => void markReviewed(row)}
                          className="rounded-lg border border-emerald-400/25 bg-emerald-400/10 px-3 py-2 text-xs font-bold text-emerald-200 disabled:opacity-50">
                          {busy === row.entry_id ? "Saving…" : "Mark reviewed"}
                        </button>
                      )}
                    </div>
                  </div>

                  {visibleEvents.length === 0 ? (
                    <p className="text-xs text-white/45">No timeline events.</p>
                  ) : (
                    <div className="grid gap-2 sm:grid-cols-2">
                      {visibleEvents.map((event) => (
                        <div key={event.id} className="flex items-center justify-between gap-3 rounded-xl border border-white/[0.07] bg-white/[0.025] px-3 py-2">
                          <div className="min-w-0">
                            <p className={"text-xs font-semibold " +
                              (event.kind === "app_background" ? "text-amber-200" :
                               event.kind === "app_foreground" ? "text-emerald-200" : "text-white/80")}>
                              {eventLabel(event.kind)}
                            </p>
                            <p className="text-[11px] text-white/40">{formatEAT(event.created_at)}</p>
                          </div>
                          {event.kind === "app_background" && (
                            <span className="shrink-0 text-xs font-semibold text-amber-100/80">
                              {event.away_ms == null ? "No return recorded" : "Away " + duration(event.away_ms)}
                            </span>
                          )}
                          {event.kind === "app_foreground" && typeof event.detail?.away_ms === "number" && (
                            <span className="shrink-0 text-xs text-emerald-100/70">
                              Back after {duration(event.detail.away_ms)}
                            </span>
                          )}
                        </div>
                      ))}
                    </div>
                  )}

                  {showAll && !row.reviewed_at && (
                    <div className="border-t border-white/8 pt-3">
                      <label className="mb-1 block text-xs text-white/55">Review note (optional)</label>
                      <textarea value={notes[row.entry_id] || ""}
                        onChange={(event) => setNotes((current) => ({ ...current, [row.entry_id]: event.target.value }))}
                        rows={2} placeholder="Record what you checked or why this attempt was cleared…"
                        className="w-full rounded-xl border border-white/10 bg-white/[0.04] p-3 text-sm text-white outline-none focus:border-amber-300/40" />
                      <button disabled={busy === row.entry_id} onClick={() => void markReviewed(row)}
                        className="mt-2 rounded-lg border border-emerald-400/25 bg-emerald-500/15 px-3 py-2 text-xs font-bold text-emerald-200 disabled:opacity-50">
                        Save review note
                      </button>
                    </div>
                  )}
                  {row.reviewed_at && (
                    <p className="text-xs text-emerald-200/70">
                      Reviewed {formatEAT(row.reviewed_at)}{row.review_note ? " · " + row.review_note : ""}
                    </p>
                  )}
                </div>
              </article>
            );
          })}
        </div>
      )}

      <p className="text-[11px] leading-relaxed text-white/35">
        <AlertTriangle className="mr-1 inline h-3 w-3" />
        These are flagged students, not automatically confirmed cheaters. Incoming calls, system dialogs,
        accidental gestures and legitimate interruptions can also move the app to the background.
        Review the timeline and context before disqualifying anyone.
      </p>
    </div>
  );
}
