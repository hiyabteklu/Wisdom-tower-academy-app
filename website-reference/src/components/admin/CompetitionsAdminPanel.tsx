"use client";

import { useCallback, useEffect, useState } from "react";
import { Ban, Eye, EyeOff, ListOrdered, Pause, Play, RefreshCw, Rocket, Trophy, CalendarClock } from "lucide-react";
import { supabase } from "@/lib/supabase";
import { EntryFlagsInfo, useEntryFlags } from "./EntryFlagsInfo";
import {
  AdminBadge,
  AdminCard,
  AdminEmpty,
  AdminField,
  AdminInput,
  AdminPill,
  AdminSectionTitle,
  AdminSelect,
  AdminSheet,
  AdminSkeleton,
  AdminTextarea,
  AdminToast,
  type AdminTone,
} from "./ui";

/**
 * Admin panel for competitions (global timed exams).
 *
 * Everything goes through the admin database functions (the database checks
 * that the caller is an admin): admin_publish_competition_from_exam,
 * admin_set_competition_published, admin_set_countdown,
 * admin_start_competition_now, admin_reschedule_competition and
 * admin_disqualify_entry. Contracts: docs/COMPETITION_API_CONTRACT.md in the
 * app repository.
 *
 * All times typed here are Ethiopia time (EAT, UTC+3), whatever the time zone
 * of the computer being used.
 *
 * Questions and answer keys are copied from an existing exam of the exam hub
 * when the contest is created. Every question must have a solution: the
 * database refuses an exam where one is missing.
 *
 * C10: the Entries list also shows the integrity flags the student's app
 * recorded during the ranked exam (EntryFlagsInfo.tsx). They are signals for
 * the admin to look at; nothing is disqualified automatically.
 *
 * Slice A5 (docs/ADMIN_MOBILE_PLAN.md): LAYOUT ONLY (2-column pill grid, 44 px targets, no animation).
 *
 * Slice U5 (docs/ADMIN_UI_V2_PLAN.md, 2026-10-16): LAYOUT ONLY. Kit cards, kit fields, kit badges and pills.
 * The browser pop-ups are replaced by bottom sheets: Start now and Cancel contest ask in a confirm sheet
 * (same sentences), Disqualify asks for the reason in a sheet with a text box (same sentence, same empty-reason
 * check), Reschedule opens as a sheet. Feedback is the kit slim toast. Data calls, checks and texts unchanged.
 */

type Competition = {
  id: string;
  title: string;
  audience: string | null;
  package_id: string | null;
  requires_package: boolean;
  published: boolean;
  countdown_state: string;
  starts_at: string;
  duration_min: number;
  ends_at: string | null;
  points: number;
  prize_title: string | null;
  prize_details: string | null;
};

type ExamResource = {
  id: string;
  title: string;
  package_id: string | null;
  durationMin: string | null;
};

type EntryRow = {
  id: string;
  user_id: string;
  status: string;
  score: number | null;
  total_count: number | null;
  time_taken_ms: number | null;
  submitted_at: string | null;
  disqualified: boolean;
  disqualified_reason: string | null;
};

type Phase = "upcoming" | "live" | "ended";

type ConfirmAction = { kind: "start" | "cancel"; c: Competition };

const EAT_OFFSET = "+03:00";
const LOCAL_DT = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/;

const CHECK_LABEL = "flex min-h-[44px] cursor-pointer items-center gap-3 text-sm text-white";

function formatEat(iso: string | null): string {
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

function toEatIso(local: string): string | null {
  if (!LOCAL_DT.test(local)) return null;
  const d = new Date(`${local}:00${EAT_OFFSET}`);
  return Number.isNaN(d.getTime()) ? null : d.toISOString();
}

function friendlyError(message: string): string {
  const m = message || "";
  if (m.includes("admin_only")) return "Only an admin account can do this. Please sign in again as admin.";
  if (m.includes("exam_not_found")) return "That exam was not found in the exam hub.";
  if (m.includes("exam_has_no_questions")) return "That exam has no questions.";
  if (m.includes("start_must_be_in_the_future")) return "The start time must be in the future.";
  if (m.includes("duration_required")) return "This exam has no duration. Please type the duration in minutes.";
  const bad = m.match(/bad_question_at_position_(\d+)/);
  if (bad) {
    return `Question ${bad[1]} of the exam is incomplete (it needs a prompt, 2 to 8 choices, one correct choice and a solution). Fix it in the exam hub and try again.`;
  }
  if (m.includes("competition_not_found")) return "That competition no longer exists.";
  if (m.includes("already_started")) return "The contest already started, so it cannot be paused.";
  if (m.includes("reason_required")) return "Please type the reason.";
  if (m.includes("entry_not_found")) return "That entry was not found.";
  if (m.toLowerCase().includes("lock") || m.toLowerCase().includes("started")) {
    return `The database refused this change (a contest that already started is locked): ${m}`;
  }
  return m || "Something went wrong.";
}

function phaseOf(c: Competition, nowMs: number): Phase {
  const start = new Date(c.starts_at).getTime();
  const end = c.ends_at
    ? new Date(c.ends_at).getTime()
    : start + (c.duration_min || 0) * 60_000;
  if (nowMs < start) return "upcoming";
  if (nowMs < end) return "live";
  return "ended";
}

function toneOf(kind: string): AdminTone {
  switch (kind) {
    case "live":
      return "green";
    case "upcoming":
      return "cyan";
    case "paused":
      return "gold";
    case "cancelled":
      return "red";
    default:
      return "neutral";
  }
}

function formatTime(ms: number | null): string {
  if (ms == null) return "";
  const s = Math.round(ms / 1000);
  return `${Math.floor(s / 60)}m ${String(s % 60).padStart(2, "0")}s`;
}

export default function CompetitionsAdminPanel() {
  const [rows, setRows] = useState<Competition[]>([]);
  const [exams, setExams] = useState<ExamResource[]>([]);
  const [nowMs, setNowMs] = useState(0);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);

  // Create form
  const [resourceId, setResourceId] = useState("");
  const [title, setTitle] = useState("");
  const [startsLocal, setStartsLocal] = useState("");
  const [durationMin, setDurationMin] = useState("");
  const [audience, setAudience] = useState("");
  const [points, setPoints] = useState("0");
  const [prizeTitle, setPrizeTitle] = useState("");
  const [prizeDetails, setPrizeDetails] = useState("");
  const [requiresPackage, setRequiresPackage] = useState(false);
  const [publishNow, setPublishNow] = useState(false);

  // Reschedule (a sheet, one row at a time)
  const [editId, setEditId] = useState<string | null>(null);
  const [editStart, setEditStart] = useState("");
  const [editDuration, setEditDuration] = useState("");

  // Confirm sheet (Start now, Cancel contest) and the disqualify sheet
  const [confirmAct, setConfirmAct] = useState<ConfirmAction | null>(null);
  const [dqEntry, setDqEntry] = useState<EntryRow | null>(null);
  const [dqReason, setDqReason] = useState("");

  // Entries (one competition at a time)
  const [entriesFor, setEntriesFor] = useState<string | null>(null);
  const [entries, setEntries] = useState<EntryRow[]>([]);
  const [names, setNames] = useState<Record<string, string>>({});
  const [entriesLoading, setEntriesLoading] = useState(false);
  const { flags: entryFlags, error: flagsError } = useEntryFlags(entries.map((r) => r.id));

  const load = useCallback(async () => {
    setLoading(true);
    const [compRes, examRes] = await Promise.all([
      supabase
        .from("competitions")
        .select(
          "id,title,audience,package_id,requires_package,published,countdown_state,starts_at,duration_min,ends_at,points,prize_title,prize_details"
        )
        .order("starts_at", { ascending: false })
        .limit(100),
      supabase
        .from("learning_resources")
        .select("id,title,package_id,durationMin:meta->>durationMin")
        .eq("hub", "exams")
        .eq("content_type", "exam")
        .order("title", { ascending: true }),
    ]);
    if (compRes.error) {
      setFeedback({ type: "error", message: friendlyError(compRes.error.message) });
      setRows([]);
    } else {
      setRows((compRes.data ?? []) as unknown as Competition[]);
    }
    if (!examRes.error) {
      setExams((examRes.data ?? []) as unknown as ExamResource[]);
    }
    setNowMs(Date.now());
    setLoading(false);
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  async function run(
    key: string,
    call: () => PromiseLike<{ error: { message: string } | null }>,
    okMessage: string
  ) {
    setBusy(key);
    setFeedback(null);
    const { error } = await call();
    setBusy(null);
    if (error) {
      setFeedback({ type: "error", message: friendlyError(error.message) });
    } else {
      setFeedback({ type: "success", message: okMessage });
    }
    await load();
  }

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    setFeedback(null);

    if (!resourceId) {
      setFeedback({ type: "error", message: "Please choose an exam from the exam hub." });
      return;
    }
    const startsIso = toEatIso(startsLocal);
    if (!startsIso || new Date(startsIso).getTime() <= Date.now() + 60_000) {
      setFeedback({
        type: "error",
        message: "Pick a start time at least one minute from now (Ethiopia time).",
      });
      return;
    }
    let duration: number | null = null;
    if (durationMin.trim()) {
      if (!/^\d{1,3}$/.test(durationMin.trim()) || Number(durationMin) < 1) {
        setFeedback({ type: "error", message: "Duration must be a whole number of minutes." });
        return;
      }
      duration = Number(durationMin.trim());
    }
    if (!/^\d{1,6}$/.test(points.trim() || "0")) {
      setFeedback({ type: "error", message: "Points must be a whole number (0 or more)." });
      return;
    }

    setBusy("create");
    const { error } = await supabase.rpc("admin_publish_competition_from_exam", {
      p_resource_id: resourceId,
      p_starts_at: startsIso,
      p_title: title.trim() || null,
      p_audience: audience.trim() || null,
      p_duration_min: duration,
      p_points: Number(points.trim() || "0"),
      p_prize_title: prizeTitle.trim() || null,
      p_prize_details: prizeDetails.trim() || null,
      p_requires_package: requiresPackage,
      p_publish: publishNow,
    });
    setBusy(null);
    if (error) {
      setFeedback({ type: "error", message: friendlyError(error.message) });
      return;
    }
    setFeedback({
      type: "success",
      message: publishNow
        ? "Competition created and visible to students."
        : "Competition created as hidden. Use Show to publish it when ready.",
    });
    setResourceId("");
    setTitle("");
    setStartsLocal("");
    setDurationMin("");
    setAudience("");
    setPoints("0");
    setPrizeTitle("");
    setPrizeDetails("");
    setRequiresPackage(false);
    setPublishNow(false);
    await load();
  }

  async function handleReschedule(c: Competition) {
    const startsIso = toEatIso(editStart);
    if (!startsIso || new Date(startsIso).getTime() <= Date.now() + 60_000) {
      setFeedback({
        type: "error",
        message: "Pick a new start at least one minute from now (Ethiopia time).",
      });
      return;
    }
    let duration: number | null = null;
    if (editDuration.trim()) {
      if (!/^\d{1,3}$/.test(editDuration.trim()) || Number(editDuration) < 1) {
        setFeedback({ type: "error", message: "Duration must be a whole number of minutes." });
        return;
      }
      duration = Number(editDuration.trim());
    }
    await run(
      `resched-${c.id}`,
      () =>
        supabase.rpc("admin_reschedule_competition", {
          p_id: c.id,
          p_starts_at: startsIso,
          p_duration_min: duration,
        }),
      "Rescheduled."
    );
    setEditId(null);
  }

  async function openEntries(id: string) {
    if (entriesFor === id) {
      setEntriesFor(null);
      return;
    }
    setEntriesFor(id);
    setEntriesLoading(true);
    setEntries([]);
    const { data, error } = await supabase
      .from("competition_entries")
      .select(
        "id,user_id,status,score,total_count,time_taken_ms,submitted_at,disqualified,disqualified_reason"
      )
      .eq("competition_id", id)
      .eq("mode", "ranked")
      .order("score", { ascending: false, nullsFirst: false })
      .limit(200);
    if (error) {
      setFeedback({ type: "error", message: friendlyError(error.message) });
      setEntriesLoading(false);
      return;
    }
    const list = (data ?? []) as unknown as EntryRow[];
    setEntries(list);
    const ids = Array.from(new Set(list.map((r) => r.user_id)));
    if (ids.length > 0) {
      const { data: profs } = await supabase
        .from("profiles")
        .select("id,full_name")
        .in("id", ids);
      const map: Record<string, string> = {};
      for (const p of (profs ?? []) as unknown as { id: string; full_name: string | null }[]) {
        map[p.id] = p.full_name?.trim() || "(no name)";
      }
      setNames(map);
    }
    setEntriesLoading(false);
  }

  async function handleDisqualify(entry: EntryRow, reason: string) {
    if (!reason.trim()) {
      setFeedback({ type: "error", message: "Please type the reason." });
      return;
    }
    setDqEntry(null);
    setDqReason("");
    setBusy(`dq-${entry.id}`);
    setFeedback(null);
    const { error } = await supabase.rpc("admin_disqualify_entry", {
      p_entry_id: entry.id,
      p_reason: reason.trim(),
    });
    setBusy(null);
    if (error) {
      setFeedback({ type: "error", message: friendlyError(error.message) });
      return;
    }
    setFeedback({ type: "success", message: "Entry disqualified." });
    if (entriesFor) {
      const id = entriesFor;
      setEntriesFor(null);
      await openEntries(id);
    }
  }

  function runConfirmed() {
    const act = confirmAct;
    setConfirmAct(null);
    if (!act) return;
    const c = act.c;
    if (act.kind === "start") {
      void run(
        `start-${c.id}`,
        () => supabase.rpc("admin_start_competition_now", { p_id: c.id }),
        "Started now."
      );
    } else {
      void run(
        `cancel-${c.id}`,
        () =>
          supabase.rpc("admin_set_countdown", {
            p_id: c.id,
            p_state: "cancelled",
          }),
        "Cancelled."
      );
    }
  }

  const editingComp = editId ? rows.find((r) => r.id === editId) ?? null : null;

  return (
    <div className="min-w-0 space-y-3">
      <div className="flex items-start justify-between gap-3">
        <p className="min-w-0 text-xs leading-relaxed text-wisdom-muted">
          Create a contest from an exam of the exam hub, then show, pause, start, reschedule or
          cancel it. All times are Ethiopia time.
        </p>
        <button
          type="button"
          onClick={() => void load()}
          disabled={loading}
          aria-label="Refresh"
          className="inline-flex h-11 w-11 shrink-0 cursor-pointer items-center justify-center rounded-full border border-white/10 bg-wisdom-card text-white active:bg-white/10 disabled:opacity-50"
        >
          <RefreshCw className="h-4 w-4" />
        </button>
      </div>

      {/* Create form */}
      <form onSubmit={handleCreate} className="min-w-0">
        <AdminCard className="space-y-3">
          <div>
            <h3 className="text-base font-bold text-white">Create a Competition</h3>
            <p className="mt-0.5 text-xs leading-relaxed text-wisdom-muted">
              Questions, correct answers and solutions are copied from the chosen exam. Every
              question must have a solution. The contest is created hidden unless you tick
              &quot;Show to students now&quot;.
            </p>
          </div>

          <AdminField label="Exam from the exam hub *">
            <AdminSelect value={resourceId} onChange={(e) => setResourceId(e.target.value)}>
              <option value="">Choose an exam...</option>
              {exams.map((x) => (
                <option key={x.id} value={x.id}>
                  {x.title}
                  {x.package_id ? ` (${x.package_id})` : ""}
                  {x.durationMin ? ` - ${x.durationMin} min` : ""}
                </option>
              ))}
            </AdminSelect>
          </AdminField>

          <AdminField label="Title (optional, default: the exam title)">
            <AdminInput type="text" value={title} onChange={(e) => setTitle(e.target.value)} />
          </AdminField>

          <AdminField label="Starts at (Ethiopia time) *">
            <AdminInput
              type="datetime-local"
              value={startsLocal}
              onChange={(e) => setStartsLocal(e.target.value)}
            />
          </AdminField>

          <div className="grid grid-cols-2 gap-3">
            <AdminField label="Minutes (default: the exam's own)">
              <AdminInput
                type="text"
                inputMode="numeric"
                value={durationMin}
                onChange={(e) => setDurationMin(e.target.value)}
              />
            </AdminField>
            <AdminField label="Points for the account total">
              <AdminInput
                type="text"
                inputMode="numeric"
                value={points}
                onChange={(e) => setPoints(e.target.value)}
              />
            </AdminField>
          </div>

          <AdminField label="Audience (optional, default: the exam's package)">
            <AdminInput type="text" value={audience} onChange={(e) => setAudience(e.target.value)} />
          </AdminField>

          <AdminField label="Prize title (shown only after the contest ends)">
            <AdminInput type="text" value={prizeTitle} onChange={(e) => setPrizeTitle(e.target.value)} />
          </AdminField>

          <AdminField label="Prize details (shown only after the contest ends)">
            <AdminTextarea rows={2} value={prizeDetails} onChange={(e) => setPrizeDetails(e.target.value)} />
          </AdminField>

          <label className={CHECK_LABEL}>
            <input
              type="checkbox"
              checked={requiresPackage}
              onChange={(e) => setRequiresPackage(e.target.checked)}
              className="h-5 w-5 shrink-0 accent-amber-400"
            />
            <span className="min-w-0">Only students who own the exam&apos;s package may enter</span>
          </label>
          <label className={CHECK_LABEL}>
            <input
              type="checkbox"
              checked={publishNow}
              onChange={(e) => setPublishNow(e.target.checked)}
              className="h-5 w-5 shrink-0 accent-amber-400"
            />
            <span className="min-w-0">Show to students now</span>
          </label>

          <AdminPill type="submit" tone="primary" fullWidth icon={Trophy} disabled={busy === "create"}>
            {busy === "create" ? "Creating..." : "Create competition"}
          </AdminPill>
        </AdminCard>
      </form>

      {/* List */}
      <div className="min-w-0">
        <AdminSectionTitle>All Competitions ({rows.length})</AdminSectionTitle>

        {loading ? (
          <AdminSkeleton rows={3} />
        ) : rows.length === 0 ? (
          <AdminEmpty icon={Trophy} title="No competitions yet." />
        ) : (
          <div className="space-y-3">
            {rows.map((c) => {
              const phase = phaseOf(c, nowMs);
              const cancelled = c.countdown_state === "cancelled";
              const paused = c.countdown_state === "paused";
              const upcoming = phase === "upcoming" && !cancelled;
              return (
                <AdminCard key={c.id} className="space-y-2">
                  <div className="min-w-0">
                    <div className="mb-1 flex flex-wrap items-center gap-1.5">
                      <AdminBadge
                        tone={toneOf(cancelled ? "cancelled" : paused ? "paused" : phase)}
                        className="uppercase tracking-wider"
                      >
                        {cancelled ? "cancelled" : paused ? "paused" : phase}
                      </AdminBadge>
                      <AdminBadge tone={c.published ? "green" : "neutral"} className="uppercase tracking-wider">
                        {c.published ? "visible" : "hidden"}
                      </AdminBadge>
                      <span className="min-w-0 font-mono text-xs text-slate-400">
                        {c.audience ? c.audience : "no audience set"}
                        {c.requires_package ? " / package required" : ""}
                      </span>
                    </div>
                    <h4 className="break-words text-sm font-bold text-white">{c.title}</h4>
                    <p className="mt-0.5 text-xs text-wisdom-muted">
                      {formatEat(c.starts_at)} - {c.duration_min} min
                      {c.ends_at ? ` - ends ${formatEat(c.ends_at)}` : ""} - {c.points} point
                      {c.points === 1 ? "" : "s"}
                    </p>
                    {c.prize_title && (
                      <p className="mt-0.5 break-words text-xs text-slate-400">Prize: {c.prize_title}</p>
                    )}
                  </div>

                  <div className="grid grid-cols-2 gap-2">
                    <AdminPill
                      icon={c.published ? EyeOff : Eye}
                      disabled={busy !== null}
                      onClick={() =>
                        void run(
                          `pub-${c.id}`,
                          () =>
                            supabase.rpc("admin_set_competition_published", {
                              p_id: c.id,
                              p_published: !c.published,
                            }),
                          c.published ? "Hidden from students." : "Now visible to students."
                        )
                      }
                    >
                      {c.published ? "Hide" : "Show"}
                    </AdminPill>

                    {upcoming && !paused && (
                      <AdminPill
                        icon={Pause}
                        disabled={busy !== null}
                        onClick={() =>
                          void run(
                            `pause-${c.id}`,
                            () =>
                              supabase.rpc("admin_set_countdown", { p_id: c.id, p_state: "paused" }),
                            "Countdown paused (students see it as postponed)."
                          )
                        }
                      >
                        Pause countdown
                      </AdminPill>
                    )}

                    {paused && !cancelled && (
                      <AdminPill
                        icon={Play}
                        disabled={busy !== null}
                        onClick={() =>
                          void run(
                            `resume-${c.id}`,
                            () =>
                              supabase.rpc("admin_set_countdown", { p_id: c.id, p_state: "running" }),
                            "Countdown running again."
                          )
                        }
                      >
                        Resume countdown
                      </AdminPill>
                    )}

                    {upcoming && (
                      <AdminPill
                        tone="gold"
                        icon={Rocket}
                        disabled={busy !== null}
                        onClick={() => setConfirmAct({ kind: "start", c })}
                      >
                        Start now
                      </AdminPill>
                    )}

                    {upcoming && (
                      <AdminPill
                        icon={CalendarClock}
                        disabled={busy !== null}
                        onClick={() => {
                          setEditId(c.id);
                          setEditStart("");
                          setEditDuration(String(c.duration_min));
                        }}
                      >
                        Reschedule
                      </AdminPill>
                    )}

                    {!cancelled && phase !== "ended" && (
                      <AdminPill
                        tone="danger"
                        icon={Ban}
                        disabled={busy !== null}
                        onClick={() => setConfirmAct({ kind: "cancel", c })}
                      >
                        Cancel contest
                      </AdminPill>
                    )}

                    <AdminPill icon={ListOrdered} disabled={busy !== null} onClick={() => void openEntries(c.id)}>
                      {entriesFor === c.id ? "Hide entries" : "Entries"}
                    </AdminPill>
                  </div>

                  {entriesFor === c.id && (
                    <div className="min-w-0 rounded-2xl border border-white/10 bg-white/[0.03] p-3">
                      {entriesLoading ? (
                        <p className="text-xs text-wisdom-muted">Loading entries...</p>
                      ) : entries.length === 0 ? (
                        <p className="text-xs text-wisdom-muted">No ranked entries yet.</p>
                      ) : (
                        <div className="divide-y divide-white/10">
                          {flagsError && (
                            <p className="pb-2 text-xs text-rose-300">
                              Flags could not be loaded: {flagsError}
                            </p>
                          )}
                          {entries.map((en) => (
                            <div key={en.id} className="flex flex-col gap-2 py-2.5">
                              <div className="min-w-0 text-xs">
                                <span className="text-sm font-bold text-white">
                                  {names[en.user_id] ?? "(unknown)"}
                                </span>
                                <span className="text-slate-400">
                                  {" "}
                                  - {en.status}
                                  {en.score != null && en.total_count != null
                                    ? ` - ${en.score} / ${en.total_count}`
                                    : ""}
                                  {en.time_taken_ms != null ? ` - ${formatTime(en.time_taken_ms)}` : ""}
                                </span>
                                {en.disqualified && (
                                  <span className="block text-rose-300">
                                    Disqualified: {en.disqualified_reason}
                                  </span>
                                )}
                                <EntryFlagsInfo flags={entryFlags[en.id]} />
                              </div>
                              {!en.disqualified && (
                                <AdminPill
                                  tone="danger"
                                  fullWidth
                                  disabled={busy !== null}
                                  onClick={() => {
                                    setDqReason("");
                                    setDqEntry(en);
                                  }}
                                >
                                  Disqualify
                                </AdminPill>
                              )}
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  )}
                </AdminCard>
              );
            })}
          </div>
        )}
      </div>

      {/* Reschedule sheet */}
      <AdminSheet
        open={editingComp !== null}
        onClose={() => setEditId(null)}
        title="Reschedule"
        footer={
          <div className="flex gap-2">
            <AdminPill fullWidth onClick={() => setEditId(null)}>
              Close
            </AdminPill>
            <AdminPill
              fullWidth
              tone="primary"
              disabled={busy !== null}
              onClick={() => {
                if (editingComp) void handleReschedule(editingComp);
              }}
            >
              Save new time
            </AdminPill>
          </div>
        }
      >
        <div className="space-y-3">
          <p className="break-words text-sm font-semibold text-white">{editingComp?.title}</p>
          <AdminField label="New start (Ethiopia time)">
            <AdminInput type="datetime-local" value={editStart} onChange={(e) => setEditStart(e.target.value)} />
          </AdminField>
          <AdminField label="Minutes">
            <AdminInput
              type="text"
              inputMode="numeric"
              value={editDuration}
              onChange={(e) => setEditDuration(e.target.value)}
            />
          </AdminField>
        </div>
      </AdminSheet>

      {/* Confirm sheet: Start now and Cancel contest */}
      <AdminSheet
        open={confirmAct !== null}
        onClose={() => setConfirmAct(null)}
        title={confirmAct?.kind === "start" ? "Start now" : "Cancel contest"}
        footer={
          <div className="flex gap-2">
            <AdminPill fullWidth onClick={() => setConfirmAct(null)}>
              Back
            </AdminPill>
            <AdminPill
              fullWidth
              tone={confirmAct?.kind === "start" ? "gold" : "danger"}
              icon={confirmAct?.kind === "start" ? Rocket : Ban}
              onClick={runConfirmed}
            >
              {confirmAct?.kind === "start" ? "Start now" : "Cancel contest"}
            </AdminPill>
          </div>
        }
      >
        <p className="break-words text-sm text-wisdom-muted">
          {confirmAct?.kind === "start"
            ? "Start this contest right now? Students can enter immediately."
            : "Cancel this contest? Students will see it as cancelled."}
        </p>
        <p className="mt-2 break-words text-sm font-semibold text-white">{confirmAct?.c.title}</p>
      </AdminSheet>

      {/* Disqualify sheet */}
      <AdminSheet
        open={dqEntry !== null}
        onClose={() => setDqEntry(null)}
        title="Disqualify entry"
        footer={
          <div className="flex gap-2">
            <AdminPill fullWidth onClick={() => setDqEntry(null)}>
              Back
            </AdminPill>
            <AdminPill
              fullWidth
              tone="danger"
              onClick={() => {
                if (dqEntry) void handleDisqualify(dqEntry, dqReason);
              }}
            >
              Disqualify
            </AdminPill>
          </div>
        }
      >
        <div className="space-y-3">
          <p className="break-words text-sm text-wisdom-muted">
            Reason for disqualifying this entry (the student sees it, and the entry leaves the leaderboard):
          </p>
          <AdminTextarea rows={3} value={dqReason} onChange={(e) => setDqReason(e.target.value)} />
        </div>
      </AdminSheet>

      <AdminToast
        message={feedback ? feedback.message : null}
        tone={feedback ? (feedback.type === "success" ? "green" : "red") : "neutral"}
        onClose={() => setFeedback(null)}
        durationMs={7000}
      />
    </div>
  );
}
