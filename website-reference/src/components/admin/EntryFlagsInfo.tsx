"use client";

import { useEffect, useMemo, useState } from "react";
import { supabase } from "@/lib/supabase";

/**
 * C10: integrity flags of competition entries, for the admin.
 *
 * The student's app records a flag (table competition_flags, written by the
 * database function report_competition_flag) when it leaves the app during a
 * RANKED exam: kind "app_background" when it goes away and "app_foreground"
 * (with detail.away_ms, milliseconds away) when it comes back. Only admins can
 * read the table (row security policy compf_admin_all).
 *
 * A flag is a signal to look at, not proof: a phone call or a system dialog
 * also takes the app to the background. Nothing here disqualifies anybody;
 * the admin decides with the existing Disqualify button.
 *
 * Slice U5 (2026-10-16): LAYOUT ONLY. The summary link is a 44 px tap target and the small texts are at
 * least 11 px, in the kit colours. Hook and logic unchanged.
 */

export type FlagRow = {
  entry_id: string;
  kind: string;
  detail: Record<string, unknown> | null;
  created_at: string;
};

export function useEntryFlags(entryIds: string[]) {
  const [flags, setFlags] = useState<Record<string, FlagRow[]>>({});
  const [error, setError] = useState<string | null>(null);
  const key = useMemo(() => entryIds.slice().sort().join(","), [entryIds]);

  useEffect(() => {
    let cancelled = false;
    async function run() {
      setError(null);
      if (!key) {
        setFlags({});
        return;
      }
      const ids = key.split(",");
      const { data, error: err } = await supabase
        .from("competition_flags")
        .select("entry_id,kind,detail,created_at")
        .in("entry_id", ids)
        .order("created_at", { ascending: true })
        .limit(5000);
      if (cancelled) return;
      if (err) {
        setError(err.message || "Could not load flags.");
        setFlags({});
        return;
      }
      const map: Record<string, FlagRow[]> = {};
      for (const row of (data ?? []) as unknown as FlagRow[]) {
        (map[row.entry_id] ||= []).push(row);
      }
      setFlags(map);
    }
    void run();
    return () => {
      cancelled = true;
    };
  }, [key]);

  return { flags, error };
}

function formatAway(ms: number): string {
  const s = Math.max(0, Math.round(ms / 1000));
  return `${Math.floor(s / 60)}m ${String(s % 60).padStart(2, "0")}s`;
}

function formatEat(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return (
    d.toLocaleString("en-GB", {
      timeZone: "Africa/Addis_Ababa",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
      hour12: false,
    }) + " EAT"
  );
}

function label(kind: string): string {
  switch (kind) {
    case "app_background":
      return "Left the app";
    case "app_foreground":
      return "Came back";
    case "overlay_open":
      return "Opened an overlay";
    case "copy_attempt":
      return "Tried to copy";
    default:
      return "Other";
  }
}

export function EntryFlagsInfo({ flags }: { flags: FlagRow[] | undefined }) {
  const [open, setOpen] = useState(false);
  if (!flags || flags.length === 0) return null;

  const left = flags.filter((f) => f.kind === "app_background").length;
  const back = flags.filter((f) => f.kind === "app_foreground").length;
  // a return without a recorded departure still counts as one time away
  const leaves = Math.max(left, back);
  let awayMs = 0;
  for (const f of flags) {
    if (f.kind === "app_foreground") {
      const v = Number(f.detail?.away_ms);
      if (Number.isFinite(v) && v > 0) awayMs += v;
    }
  }
  const others = flags.filter(
    (f) => f.kind !== "app_background" && f.kind !== "app_foreground"
  ).length;

  const parts: string[] = [];
  if (leaves > 0) {
    parts.push(
      `Left the app ${leaves} time${leaves === 1 ? "" : "s"}` +
        (awayMs > 0 ? ` (${formatAway(awayMs)} away in total)` : "")
    );
  }
  if (others > 0) parts.push(`${others} other flag${others === 1 ? "" : "s"}`);

  return (
    <span className="mt-1 block">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className="flex min-h-[44px] w-full cursor-pointer items-center text-left text-xs text-amber-300 underline underline-offset-2 active:text-amber-200"
      >
        {parts.join(", ")} {open ? "(hide)" : "(details)"}
      </button>
      {open && (
        <span className="mt-1 block space-y-0.5 rounded-2xl border border-white/10 bg-wisdom-card px-3 py-2">
          {flags.map((f, i) => {
            const away = Number(f.detail?.away_ms);
            return (
              <span key={i} className="block text-[11px] text-slate-300">
                {formatEat(f.created_at)} - {label(f.kind)}
                {f.kind === "app_foreground" && Number.isFinite(away) && away > 0
                  ? ` after ${formatAway(away)}`
                  : ""}
              </span>
            );
          })}
          <span className="block pt-1 text-[11px] text-slate-500">
            A flag is a signal to look at, not proof (a phone call can also send the app to the
            background). Nothing is disqualified automatically.
          </span>
        </span>
      )}
    </span>
  );
}
