"use client";
import { useCallback, useEffect, useState } from "react";
import { supabase } from "@/lib/supabase";

type Device = { user_id: string; email: string | null; full_name: string | null; device_label: string; registered_at: string; last_seen_at: string };
type DeviceEvent = { event_id: number; user_id: string; email: string | null; full_name: string | null; device_label: string; event_type: string; created_at: string };

export default function DeviceManagementPanel() {
  const [devices, setDevices] = useState<Device[]>([]);
  const [events, setEvents] = useState<DeviceEvent[]>([]);
  const [message, setMessage] = useState("Loading devices...");
  const [busy, setBusy] = useState<string | null>(null);
  const blockedAttempts = events.filter(event => event.event_type === "blocked_new_device").length;

  const load = useCallback(async () => {
    const [deviceResult, eventResult] = await Promise.all([
      supabase.rpc("admin_list_account_devices"),
      supabase.rpc("admin_list_account_device_events")
    ]);
    if (deviceResult.error) { setMessage(deviceResult.error.message); return; }
    setDevices((deviceResult.data || []) as Device[]);
    setEvents((eventResult.data || []) as DeviceEvent[]);
    setMessage(deviceResult.data?.length ? "" : "No registered Android devices yet.");
    if (eventResult.error) setMessage("Device list loaded; audit history could not be loaded: " + eventResult.error.message);
  }, []);

  useEffect(() => { void load(); }, [load]);

  async function reset(device: Device) {
    if (!window.confirm("Reset the registered phone for " + (device.email || device.user_id) + "? This revokes the current session.")) return;
    setBusy(device.user_id);
    const { data, error } = await supabase.rpc("admin_reset_account_device", { p_user_id: device.user_id });
    setBusy(null);
    if (error) setMessage(error.message);
    else if (!data) setMessage("No active device registration was found.");
    else { setMessage("Device reset. The student can now sign in on a replacement phone."); await load(); }
  }

  return <div className="space-y-6">
    <div className="flex items-center justify-between gap-3">
      <div><h2 className="text-xl font-bold text-white">Registered student devices</h2><p className="text-sm text-white/60">One Android phone per student account. A second phone is logged as a blocked attempt.</p></div>
      <button className="min-h-11 rounded-xl border border-white/15 px-4 py-2 text-sm" onClick={() => void load()}>Refresh</button>
    </div>
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
      <div className="rounded-xl border border-white/10 bg-white/[0.03] p-4"><p className="text-xs text-white/50">Registered phones</p><p className="mt-1 text-2xl font-bold text-white tabular-nums">{devices.length}</p></div>
      <div className="rounded-xl border border-rose-400/25 bg-rose-400/[0.06] p-4"><p className="text-xs text-rose-100/60">Blocked new-device attempts</p><p className="mt-1 text-2xl font-bold text-rose-200 tabular-nums">{blockedAttempts}</p></div>
      <div className="col-span-2 rounded-xl border border-white/10 bg-white/[0.03] p-4 sm:col-span-1"><p className="text-xs text-white/50">Recent audit events</p><p className="mt-1 text-2xl font-bold text-white tabular-nums">{events.length}</p></div>
    </div>
    {message && <p className="rounded-xl border border-white/10 p-3 text-sm text-white/70">{message}</p>}
    {devices.map(device => <div key={device.user_id} className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-white/10 bg-white/[0.03] p-4">
      <div className="min-w-0"><p className="font-semibold text-white">{device.full_name || "Student"}</p><p className="text-sm text-white/60">{device.email || device.user_id}</p><p className="text-sm text-cyan-200">{device.device_label}</p><p className="text-xs text-white/40">Registered: {new Date(device.registered_at).toLocaleString()} · Last sign-in: {new Date(device.last_seen_at).toLocaleString()}</p></div>
      <button disabled={busy === device.user_id} onClick={() => void reset(device)} className="min-h-11 rounded-xl border border-rose-400/30 bg-rose-400/10 px-4 py-2 text-sm text-rose-200 disabled:opacity-50">{busy === device.user_id ? "Resetting..." : "Reset device"}</button>
    </div>)}
    <section className="space-y-3 border-t border-white/10 pt-5">
      <div><h3 className="font-bold text-white">Security activity & blocked sign-ins</h3><p className="text-xs text-white/50">Latest 100 events. Repeated blocked attempts should be checked against the student and device history.</p></div>
      {events.length === 0 ? <p className="rounded-xl border border-dashed border-white/15 p-5 text-sm text-white/50">No device activity recorded yet.</p> : events.map(event => <div key={event.event_id} className="flex flex-wrap items-center justify-between gap-2 rounded-xl border border-white/8 bg-white/[0.02] p-3">
        <div className="min-w-0"><p className="truncate text-sm font-semibold text-white">{event.full_name || event.email || event.user_id}</p><p className="text-xs text-white/55">{event.email || ""} · {event.device_label}</p></div>
        <div className="text-right"><p className={"text-xs font-bold " + (event.event_type === "blocked_new_device" ? "text-rose-300" : event.event_type === "admin_reset" ? "text-amber-300" : "text-emerald-300")}>{event.event_type.replace(/_/g, " ")}</p><p className="text-[11px] text-white/40">{new Date(event.created_at).toLocaleString()}</p></div>
      </div>)}
    </section>
  </div>;
}
