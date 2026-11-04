"use client";

/**
 * Notifications v2, slice R3-b3 (session R3-D). `[~]` NOT compiled, not opened in a browser.
 *
 * Audience picker for the admin composer. The chips are the registration levels (ACADEMIC_LEVEL_OPTIONS,
 * the same list as the sign-up form) plus any other level text students typed, plus the admin's own custom
 * audiences. Several chips can be selected: the result is the UNION, one notification per student.
 * Empty selection = Everyone. Counts come from the live database (functions of docs/sql/notifications_v2.sql,
 * applied 2026-10-15): admin_audience_level_counts, admin_count_notification_audience,
 * admin_list_notification_audiences, admin_add_notification_audience, admin_delete_notification_audience.
 * The value is the string[] the send route and the scheduler take: level labels and `custom:<uuid>`.
 *
 * 2026-10-15 (Admin mobile redesign, slice A6 part 2): LAYOUT ONLY (wrapping 44 px chips, 44 px targets).
 *
 * 2026-10-16 (Admin UI V2, slice U4 part 3a): LAYOUT ONLY. Kit look: gold selected chips, kit card, kit input
 * and pills for the add-audience form, and a bottom sheet instead of the browser confirm box before deleting a
 * custom audience. Logic and database calls are unchanged.
 */

import { useCallback, useEffect, useState } from "react";
import { Plus, Trash2, Users } from "lucide-react";
import { supabase } from "@/lib/supabase";
import { ACADEMIC_LEVEL_OPTIONS } from "@/lib/academic-levels";
import { AdminCard, AdminInput, AdminPill, AdminSheet } from "./ui";

type LevelCounts = {
  total: number;
  no_level: number;
  levels: { level: string; n: number }[];
};

type CustomAudience = {
  id: string;
  label: string;
  match_levels: string[];
  match_package_ids: string[];
  students: number;
};

const same = (a: string, b: string) => a.trim().toLowerCase() === b.trim().toLowerCase();

export default function NotificationAudiencePicker({
  value,
  onChange,
  onCountChange,
}: {
  value: string[];
  onChange: (next: string[]) => void;
  onCountChange?: (count: number | null) => void;
}) {
  const [counts, setCounts] = useState<LevelCounts | null>(null);
  const [customs, setCustoms] = useState<CustomAudience[]>([]);
  const [count, setCount] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  const [adding, setAdding] = useState(false);
  const [newLabel, setNewLabel] = useState("");
  const [newLevels, setNewLevels] = useState<string[]>([]);
  const [newPackages, setNewPackages] = useState("");
  const [busy, setBusy] = useState(false);
  const [pendingRemove, setPendingRemove] = useState<CustomAudience | null>(null);

  const load = useCallback(async () => {
    setError(null);
    const [c, a] = await Promise.all([
      supabase.rpc("admin_audience_level_counts"),
      supabase.rpc("admin_list_notification_audiences"),
    ]);
    if (c.error) setError(c.error.message);
    else setCounts(c.data as LevelCounts);
    if (a.error) setError(a.error.message);
    else setCustoms(Array.isArray(a.data) ? (a.data as CustomAudience[]) : []);
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  // Live count of the current selection.
  useEffect(() => {
    let cancelled = false;
    async function run() {
      if (value.length === 0) {
        const total = counts ? counts.total : null;
        setCount(total);
        onCountChange?.(total);
        return;
      }
      const { data, error: err } = await supabase.rpc("admin_count_notification_audience", {
        p_audiences: value,
      });
      if (cancelled) return;
      const n = err || typeof data !== "number" ? null : data;
      setCount(n);
      onCountChange?.(n);
    }
    void run();
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [value, counts]);

  const countFor = (label: string): number | null => {
    const row = counts?.levels.find((l) => same(l.level, label));
    return counts ? (row ? row.n : 0) : null;
  };

  const has = (entry: string) => value.some((v) => same(v, entry));
  const toggle = (entry: string) =>
    onChange(has(entry) ? value.filter((v) => !same(v, entry)) : [...value, entry]);

  const extraLevels = (counts?.levels ?? []).filter(
    (l) => !ACADEMIC_LEVEL_OPTIONS.some((o) => same(o, l.level))
  );

  async function addCustom() {
    setBusy(true);
    setError(null);
    const packages = newPackages
      .split(",")
      .map((p) => p.trim())
      .filter(Boolean);
    const { error: err } = await supabase.rpc("admin_add_notification_audience", {
      p_label: newLabel,
      p_match_levels: newLevels,
      p_match_package_ids: packages,
    });
    setBusy(false);
    if (err) {
      setError(err.message);
      return;
    }
    setAdding(false);
    setNewLabel("");
    setNewLevels([]);
    setNewPackages("");
    await load();
  }

  async function removeCustom(c: CustomAudience) {
    setError(null);
    const { error: err } = await supabase.rpc("admin_delete_notification_audience", { p_id: c.id });
    if (err) {
      setError(err.message);
      return;
    }
    onChange(value.filter((v) => v !== `custom:${c.id}`));
    await load();
  }

  async function confirmRemove() {
    const target = pendingRemove;
    setPendingRemove(null);
    if (target) await removeCustom(target);
  }

  const chip = (selected: boolean) =>
    `inline-flex items-center justify-center gap-1.5 min-h-[44px] px-4 rounded-full border text-sm font-semibold cursor-pointer ${
      selected
        ? "border-amber-400/40 bg-amber-500/20 text-amber-200"
        : "border-white/10 bg-wisdom-card text-wisdom-muted active:bg-white/10"
    }`;

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap gap-2">
        <button type="button" onClick={() => onChange([])} className={chip(value.length === 0)}>
          Everyone{counts ? ` (${counts.total})` : ""}
        </button>

        {ACADEMIC_LEVEL_OPTIONS.map((level) => {
          const n = countFor(level);
          return (
            <button key={level} type="button" onClick={() => toggle(level)} className={chip(has(level))}>
              {level}
              {n !== null ? ` (${n})` : ""}
            </button>
          );
        })}

        {extraLevels.map((l) => (
          <button key={l.level} type="button" onClick={() => toggle(l.level)} className={chip(has(l.level))}>
            {l.level} ({l.n})
          </button>
        ))}

        {customs.map((c) => (
          <span key={c.id} className="inline-flex items-center">
            <button type="button" onClick={() => toggle(`custom:${c.id}`)} className={chip(has(`custom:${c.id}`))}>
              {c.label} ({c.students})
            </button>
            <button
              type="button"
              onClick={() => setPendingRemove(c)}
              aria-label={`Delete audience ${c.label}`}
              className="ml-1 grid h-11 w-11 cursor-pointer place-items-center rounded-full text-slate-500 active:bg-rose-500/10 active:text-rose-400"
            >
              <Trash2 className="h-4 w-4" />
            </button>
          </span>
        ))}

        <button
          type="button"
          onClick={() => setAdding((v) => !v)}
          className="inline-flex min-h-[44px] cursor-pointer items-center justify-center gap-1 rounded-full border border-dashed border-white/20 px-4 text-sm text-slate-300 active:bg-white/10"
        >
          <Plus className="h-3.5 w-3.5" />
          Custom audience
        </button>
      </div>

      <p className="flex flex-wrap items-center gap-x-1.5 gap-y-0.5 text-xs text-slate-400">
        <Users className="h-3.5 w-3.5 shrink-0 text-amber-300" />
        <span>{value.length === 0 ? "Reaches everyone:" : "Reaches:"}</span>
        <span className="font-semibold text-white">{count === null ? "…" : `${count} student${count === 1 ? "" : "s"}`}</span>
        {counts && counts.no_level > 0 && (
          <span className="text-slate-500">
            ({counts.no_level} have no level saved: only Everyone reaches them)
          </span>
        )}
      </p>

      {adding && (
        <AdminCard className="space-y-3">
          <AdminInput
            type="text"
            value={newLabel}
            maxLength={60}
            onChange={(e) => setNewLabel(e.target.value)}
            placeholder="Audience name, e.g. Exit exam group"
          />
          <div className="flex flex-wrap gap-2">
            <span className="w-full text-xs text-wisdom-muted">Matches these levels:</span>
            {[...ACADEMIC_LEVEL_OPTIONS.map(String), ...extraLevels.map((l) => l.level)].map((level) => {
              const on = newLevels.some((l) => same(l, level));
              return (
                <button
                  key={level}
                  type="button"
                  onClick={() =>
                    setNewLevels(on ? newLevels.filter((l) => !same(l, level)) : [...newLevels, level])
                  }
                  className={chip(on)}
                >
                  {level}
                </button>
              );
            })}
          </div>
          <AdminInput
            type="text"
            value={newPackages}
            onChange={(e) => setNewPackages(e.target.value)}
            placeholder="Optional: package ids separated by commas, e.g. ece-y3-sem-1"
          />
          <div className="grid grid-cols-2 gap-2">
            <AdminPill onClick={() => setAdding(false)}>Cancel</AdminPill>
            <AdminPill
              tone="primary"
              disabled={busy || !newLabel.trim() || (newLevels.length === 0 && !newPackages.trim())}
              onClick={() => void addCustom()}
            >
              {busy ? "Saving..." : "Save audience"}
            </AdminPill>
          </div>
        </AdminCard>
      )}

      {error && <p className="break-words text-xs text-rose-300">{error}</p>}

      {/* Confirm before deleting a custom audience */}
      <AdminSheet
        open={pendingRemove !== null}
        onClose={() => setPendingRemove(null)}
        title="Delete audience"
        footer={
          <div className="flex gap-2">
            <AdminPill fullWidth onClick={() => setPendingRemove(null)}>
              Cancel
            </AdminPill>
            <AdminPill fullWidth tone="danger" icon={Trash2} onClick={() => void confirmRemove()}>
              Delete
            </AdminPill>
          </div>
        }
      >
        <p className="break-words text-sm text-wisdom-muted">
          {pendingRemove ? `Delete the audience "${pendingRemove.label}"?` : ""}
        </p>
      </AdminSheet>
    </div>
  );
}
