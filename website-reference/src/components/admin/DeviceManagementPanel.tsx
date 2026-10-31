"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { supabase } from "@/lib/supabase";
import { AlertTriangle, CheckCircle2, RefreshCw, ShieldAlert, Smartphone } from "lucide-react";

type Device = {
  user_id: string;
  email: string | null;
  full_name: string | null;
  device_label: string;
  registered_at: string;
  last_seen_at: string;
};
type DeviceEvent = {
  event_id: number;
  user_id: string;
  email: string | null;
  full_name: string | null;
  device_label: string;
  event_type: string;
  created_at: string;
  reviewed_at: string | null;
  review_note: string | null;
};
type EventFilter = "pending" | "all" | "reviewed";

function formatDate(value: string) {
  return new Date(value).toLocaleString("en-GB", {
    timeZone: "Africa/Addis_Ababa",
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }) + " EAT";
}

export default function DeviceManagementPanel() {
  const [devices, setDevices] = useState<Device[]>([]);
  const [events, setEvents] = useState<DeviceEvent[]>([]);
  const [pendingCount, setPendingCount] = useState(0);
  const [filter, setFilter] = useState<EventFilter>("pending");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    const [deviceResult, eventResult, countResult] = await Promise.all([
      supabase.rpc("admin_list_account_devices"),
      supabase.rpc("admin_list_account_device_events"),
      supabase.rpc("admin_count_pending_device_alerts"),
    ]);

    if (deviceResult.error) setError(deviceResult.error.message);
    else setDevices((deviceResult.data || []) as Device[]);

    if (eventResult.error) setError((current) => current || eventResult.error.message);
    else setEvents((eventResult.data || []) as DeviceEvent[]);

    if (countResult.error) setError((current) => current || countResult.error.message);
    else setPendingCount(Number(countResult.data || 0));

    setLoading(false);
  }, []);

  useEffect(() => { void load(); }, [load]);

  const blockedEvents = useMemo(
    () => events.filter((event) => event.event_type === "blocked_new_device"),
    [events]
  );
  const visibleEvents = useMemo(() => {
    if (filter === "pending") return blockedEvents.filter((event) => !event.reviewed_at);
    if (filter === "reviewed") return blockedEvents.filter((event) => !!event.reviewed_at);
    return events;
  }, [events, blockedEvents, filter]);

  async function reset(device: Device) {
    if (!window.confirm(
      "Reset the registered phone for " + (device.email || device.user_id) +
      "? This revokes the current session and lets the student register a replacement phone."
    )) return;

    setBusy("reset:" + device.user_id);
    const { data, error: rpcError } = await supabase.rpc("admin_reset_account_device", {
      p_user_id: device.user_id,
    });
    setBusy(null);
    if (rpcError) setError(rpcError.message);
    else if (!data) setMessage("No active device registration was found.");
    else {
      setMessage("Device reset. The student can now sign in on a replacement phone.");
      await load();
    }
  }

  async function markReviewed(event: DeviceEvent) {
    const note = window.prompt(
      "Optional review note for this blocked sign-in attempt. Leave blank if no note is needed:",
      ""
    );
    if (note === null) return;

    setBusy("event:" + event.event_id);
    const { error: rpcError } = await supabase.rpc("admin_review_account_device_event", {
      p_event_id: event.event_id,
      p_review_note: note.trim() || null,
    });
    setBusy(null);
    if (rpcError) setError(rpcError.message);
    else {
      setMessage("Security attempt marked as reviewed.");
      await load();
    }
  }

  return (
    <div className="space-y-7">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <div className="flex items-center gap-2">
            <ShieldAlert className="h-5 w-5 text-amber-300" />
            <h2 className="text-xl font-bold text-white">Device security & account abuse review</h2>
          </div>
          <p className="mt-1 max-w-3xl text-sm text-white/55">
            Review blocked attempts to sign in from a second phone, inspect the device model,
            and manage the one-phone registration for each student account.
          </p>
        </div>
        <button onClick={() => void load()} disabled={loading}
          className="inline-flex min-h-10 items-center gap-2 rounded-xl border border-white/15 px-3 text-sm text-white/80 disabled:opacity-50">
          <RefreshCw className={"h-4 w-4 " + (loading ? "animate-spin" : "")} /> Refresh
        </button>
      </div>

      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <div className="rounded-2xl border border-white/10 bg-white/[0.03] p-4">
          <p className="text-xs text-white/50">Registered phones</p>
          <p className="mt-1 text-3xl font-bold tabular-nums text-white">{devices.length}</p>
        </div>
        <div className="rounded-2xl border border-rose-400/25 bg-rose-400/[0.06] p-4">
          <p className="text-xs text-rose-100/65">Needs review</p>
          <p className="mt-1 text-3xl font-bold tabular-nums text-rose-200">{pendingCount}</p>
        </div>
        <div className="rounded-2xl border border-white/10 bg-white/[0.03] p-4">
          <p className="text-xs text-white/50">Blocked attempts in recent log</p>
          <p className="mt-1 text-3xl font-bold tabular-nums text-white">{blockedEvents.length}</p>
        </div>
        <div className="rounded-2xl border border-white/10 bg-white/[0.03] p-4">
          <p className="text-xs text-white/50">Reviewed attempts in recent log</p>
          <p className="mt-1 text-3xl font-bold tabular-nums text-white">
            {blockedEvents.filter((event) => !!event.reviewed_at).length}
          </p>
        </div>
      </div>

      {error && <p className="rounded-xl border border-rose-400/30 bg-rose-400/10 p-3 text-sm text-rose-200">{error}</p>}
      {message && <p className="rounded-xl border border-emerald-400/20 bg-emerald-400/[0.06] p-3 text-sm text-emerald-100">{message}</p>}

      <section className="space-y-3">
        <div className="flex items-center gap-2 border-b border-white/10 pb-3">
          <Smartphone className="h-4 w-4 text-cyan-200" />
          <div>
            <h3 className="font-bold text-white">Registered student devices</h3>
            <p className="text-xs text-white/45">One Android phone per account. Device labels are model names, not hardware serial numbers.</p>
          </div>
        </div>
        {devices.length === 0 && !loading ? (
          <div className="rounded-xl border border-dashed border-white/15 p-6 text-center text-sm text-white/50">
            No registered Android devices yet. A device appears after a student signs in with the updated app.
          </div>
        ) : devices.map((device) => (
          <article key={device.user_id} className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-white/10 bg-white/[0.025] p-4">
            <div className="min-w-0">
              <p className="font-semibold text-white">{device.full_name || "Student"}</p>
              <p className="text-xs text-white/50">{device.email || device.user_id}</p>
              <p className="mt-2 text-sm font-semibold text-cyan-200">{device.device_label}</p>
              <p className="mt-1 text-[11px] text-white/40">
                Registered {formatDate(device.registered_at)} · Last sign-in {formatDate(device.last_seen_at)}
              </p>
            </div>
            <button disabled={busy === "reset:" + device.user_id}
              onClick={() => void reset(device)}
              className="min-h-10 rounded-xl border border-rose-400/30 bg-rose-400/10 px-4 py-2 text-sm font-semibold text-rose-200 disabled:opacity-50">
              {busy === "reset:" + device.user_id ? "Resetting…" : "Reset registered phone"}
            </button>
          </article>
        ))}
      </section>

      <section className="space-y-4 border-t border-white/10 pt-6">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <div className="flex items-center gap-2">
              <AlertTriangle className="h-4 w-4 text-rose-300" />
              <h3 className="font-bold text-white">Blocked second-device sign-ins</h3>
              {pendingCount > 0 && <span className="rounded-md border border-rose-400/30 bg-rose-400/10 px-2 py-0.5 text-xs font-extrabold text-rose-200">{pendingCount} pending</span>}
            </div>
            <p className="mt-1 text-xs text-white/45">Each blocked attempt is added to the review queue. Reviewing it does not automatically unlock the account.</p>
          </div>
          <div className="flex flex-wrap gap-2">
            {(["pending", "all", "reviewed"] as const).map((value) => (
              <button key={value} onClick={() => setFilter(value)}
                className={"rounded-lg border px-3 py-2 text-xs font-semibold capitalize " +
                  (filter === value ? "border-amber-300/30 bg-amber-400/15 text-amber-100" : "border-white/10 text-white/55")}>
                {value === "pending" ? "Needs review" : value}
              </button>
            ))}
          </div>
        </div>

        {loading && events.length === 0 ? (
          <p className="py-8 text-center text-sm text-white/45">Loading security activity…</p>
        ) : visibleEvents.length === 0 ? (
          <div className="rounded-xl border border-dashed border-white/15 p-7 text-center">
            <CheckCircle2 className="mx-auto h-7 w-7 text-emerald-300/70" />
            <p className="mt-2 text-sm font-semibold text-white">No attempts in this view</p>
            <p className="mt-1 text-xs text-white/45">New blocked sign-ins will appear here automatically.</p>
          </div>
        ) : visibleEvents.map((event) => (
          <article key={event.event_id} className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-rose-400/15 bg-rose-400/[0.025] p-4">
            <div className="min-w-0 flex-1">
              <div className="flex flex-wrap items-center gap-2">
                <p className="font-semibold text-white">{event.full_name || "Student"}</p>
                {event.event_type === "blocked_new_device"
                  ? event.reviewed_at
                    ? <span className="rounded-md border border-emerald-400/25 bg-emerald-400/10 px-2 py-0.5 text-[10px] font-bold text-emerald-200">Reviewed</span>
                    : <span className="rounded-md border border-rose-400/25 bg-rose-400/10 px-2 py-0.5 text-[10px] font-bold text-rose-200">Needs review</span>
                  : <span className="rounded-md border border-white/10 bg-white/[0.04] px-2 py-0.5 text-[10px] font-bold text-white/55">Logged</span>}
              </div>
              <p className="text-xs text-white/50">{event.email || event.user_id}</p>
              <p className="mt-1 text-xs font-semibold text-rose-200">{event.event_type === "blocked_new_device" ? "Blocked second-device sign-in" : event.event_type.replace(/_/g, " ")}</p>
              <p className="mt-1 text-sm text-cyan-200">{event.device_label}</p>
              <p className="mt-1 text-[11px] text-white/40">{formatDate(event.created_at)}</p>
              {event.review_note && <p className="mt-2 text-xs text-emerald-100/70">Review note: {event.review_note}</p>}
            </div>
            {event.event_type === "blocked_new_device" && !event.reviewed_at && (
              <button disabled={busy === "event:" + event.event_id}
                onClick={() => void markReviewed(event)}
                className="min-h-10 rounded-xl border border-emerald-400/25 bg-emerald-400/10 px-3 py-2 text-xs font-bold text-emerald-200 disabled:opacity-50">
                {busy === "event:" + event.event_id ? "Saving…" : "Mark reviewed"}
              </button>
            )}
          </article>
        ))}
        <p className="text-[11px] leading-relaxed text-white/35">
          A blocked attempt is a security signal, not proof of malicious intent. A student may have changed phones legitimately; verify identity before resetting their registered device.
        </p>
      </section>
    </div>
  );
}
