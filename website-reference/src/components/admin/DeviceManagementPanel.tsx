"use client";
import { useEffect, useState } from "react";
import { supabase } from "@/lib/supabase";

type Device = { user_id: string; email: string | null; full_name: string | null; device_label: string; registered_at: string; last_seen_at: string };

export default function DeviceManagementPanel() {
  const [devices, setDevices] = useState<Device[]>([]);
  const [message, setMessage] = useState("Loading devices...");
  const [busy, setBusy] = useState<string | null>(null);

  async function load() {
    const { data, error } = await supabase.rpc("admin_list_account_devices");
    if (error) { setMessage(error.message); return; }
    setDevices((data || []) as Device[]);
    setMessage(data?.length ? "" : "No registered Android devices yet.");
  }

  useEffect(() => { void load(); }, []);

  async function reset(device: Device) {
    if (!window.confirm("Reset the registered phone for " + (device.email || device.user_id) + "? This revokes the current session.")) return;
    setBusy(device.user_id);
    const { data, error } = await supabase.rpc("admin_reset_account_device", { p_user_id: device.user_id });
    setBusy(null);
    if (error) setMessage(error.message);
    else if (!data) setMessage("No active device registration was found.");
    else { setMessage("Device reset. The student can now sign in on a replacement phone."); await load(); }
  }

  return <div className="space-y-4">
    <div className="flex items-center justify-between gap-3">
      <div><h2 className="text-xl font-bold text-white">Registered student devices</h2><p className="text-sm text-white/60">One Android phone per student account.</p></div>
      <button className="rounded-xl border border-white/15 px-4 py-2 text-sm" onClick={() => void load()}>Refresh</button>
    </div>
    {message && <p className="rounded-xl border border-white/10 p-3 text-sm text-white/70">{message}</p>}
    {devices.map(device => <div key={device.user_id} className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-white/10 bg-white/[0.03] p-4">
      <div className="min-w-0"><p className="font-semibold text-white">{device.full_name || "Student"}</p><p className="text-sm text-white/60">{device.email || device.user_id}</p><p className="text-sm text-cyan-200">{device.device_label}</p><p className="text-xs text-white/40">Last sign-in: {new Date(device.last_seen_at).toLocaleString()}</p></div>
      <button disabled={busy === device.user_id} onClick={() => void reset(device)} className="rounded-xl border border-rose-400/30 bg-rose-400/10 px-4 py-2 text-sm text-rose-200 disabled:opacity-50">{busy === device.user_id ? "Resetting..." : "Reset device"}</button>
    </div>)}
  </div>;
}
