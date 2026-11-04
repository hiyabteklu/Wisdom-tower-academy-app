"use client";

// Admin UI V2, slice U8 part 2 (2026-10-16): LAYOUT ONLY, kit look. Logic, lock keys, data calls and texts are unchanged.
// Earlier layout slice A12 (2026-10-15) is kept in spirit: no horizontal scrolling, 44 px touch targets, no animate-* / blur / shadow.
// Changes now: kit page header, cards, badges, pills and tiles; the breadcrumb is a plain div (the kit forbids nav elements);
// folders are kit tiles; the long "All active overrides" list no longer has its own inner scroll box (it scrolls with the page,
// so a finger cannot get stuck in a nested scroller); the message is the kit slim toast (closes itself after 3.5 s).

import { useCallback, useEffect, useMemo, useState } from "react";
import {
  ADMIN_CONTENT_TREE,
  type AdminNavNode,
} from "@/data/admin-nav";
import {
  listAllLocks,
  setLock,
  clearLock,
  packageLockKey,
  scopeLockKey,
  hubLockKey,
  type LockMode,
  type ContentLockRow,
} from "@/lib/content-locks";
import { Lock, Unlock, ChevronRight, FolderOpen, RefreshCw, Shield } from "lucide-react";
import {
  AdminBadge,
  AdminCard,
  AdminPageHeader,
  AdminPill,
  AdminSectionTitle,
  AdminTile,
  AdminTileGrid,
  AdminToast,
  type AdminTone,
} from "./ui";

const MODES: { id: LockMode; label: string; hint: string }[] = [
  {
    id: "open",
    label: "Open",
    hint: "Anyone can access (no purchase gate)",
  },
  {
    id: "require_purchase",
    label: "Require sign-in / purchase",
    hint: "Locked until user owns the package (or free-for-registered)",
  },
  {
    id: "coming_soon",
    label: "Coming soon",
    hint: "Shown as uploading / not ready",
  },
  {
    id: "locked",
    label: "Hard locked",
    hint: "Fully closed (same UI as coming soon)",
  },
];

const PACKAGE_SHORTCUTS: { id: string; label: string }[] = [
  { id: "freshman", label: "Freshman package" },
  { id: "ece-y3-sem-1", label: "ECE Year 3 · Semester 1" },
  { id: "ece-y3-sem-2", label: "ECE Year 3 · Semester 2" },
];

type Crumb = { id: string; label: string; node: AdminNavNode };

const HUB_IDS = [
  "books",
  "short-notes",
  "videos",
  "flashcards",
  "question-banks",
  "exams",
  "life-savers",
];

const MODE_TONE: Record<LockMode, AdminTone> = {
  open: "green",
  require_purchase: "gold",
  coming_soon: "cyan",
  locked: "red",
};

function modeBadge(mode: LockMode | undefined) {
  if (!mode) return <AdminBadge tone="neutral">default</AdminBadge>;
  return <AdminBadge tone={MODE_TONE[mode]}>{mode.replace("_", " ")}</AdminBadge>;
}

/** The four lock modes as a 2-column grid of pills, plus a reset pill under them. */
function ModeButtons({
  current,
  busy,
  hasLock,
  withIcons,
  resetLabel,
  onPick,
  onReset,
}: {
  current: LockMode | undefined;
  busy: boolean;
  hasLock: boolean;
  withIcons?: boolean;
  resetLabel: string;
  onPick: (mode: LockMode) => void;
  onReset: () => void;
}) {
  return (
    <div className="grid grid-cols-2 gap-2">
      {MODES.map((m) => (
        <AdminPill
          key={m.id}
          tone={current === m.id ? "gold" : "ghost"}
          icon={withIcons ? (m.id === "open" ? Unlock : Lock) : undefined}
          disabled={busy}
          onClick={() => onPick(m.id)}
          className="px-2 py-2 text-center leading-tight"
        >
          {m.label}
        </AdminPill>
      ))}
      <AdminPill fullWidth disabled={busy || !hasLock} onClick={onReset} className="col-span-2">
        {resetLabel}
      </AdminPill>
    </div>
  );
}

export default function LocksPanel() {
  const [crumbs, setCrumbs] = useState<Crumb[]>([]);
  const [locks, setLocks] = useState<ContentLockRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [busyKey, setBusyKey] = useState("");
  const [toast, setToast] = useState("");
  const [error, setError] = useState("");

  const lockMap = useMemo(() => {
    const m = new Map<string, ContentLockRow>();
    for (const l of locks) m.set(l.lockKey, l);
    return m;
  }, [locks]);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    const res = await listAllLocks();
    if (res.error) {
      setError(
        res.error.includes("relation") || res.error.includes("does not exist")
          ? "Table missing: run docs/content-locks-setup.sql in Supabase SQL Editor."
          : res.error
      );
      setLocks([]);
    } else {
      setLocks(res.locks);
    }
    setLoading(false);
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  async function applyMode(lockKey: string, mode: LockMode, label?: string) {
    setBusyKey(lockKey);
    const res = await setLock({ lockKey, mode, label: label || null });
    setBusyKey("");
    if (!res.ok) {
      setToast(res.error || "Save failed");
      return;
    }
    setToast(`Saved · ${lockKey} → ${mode}`);
    void load();
  }

  async function resetToDefault(lockKey: string) {
    setBusyKey(lockKey);
    const res = await clearLock(lockKey);
    setBusyKey("");
    if (!res.ok) {
      setToast(res.error || "Clear failed");
      return;
    }
    setToast(`Cleared · ${lockKey} (uses code default)`);
    void load();
  }

  function enter(node: AdminNavNode) {
    setCrumbs((c) => [...c, { id: node.id, label: node.label, node }]);
  }

  function goTo(index: number) {
    setCrumbs((c) => c.slice(0, index + 1));
  }

  function resetRoot() {
    setCrumbs([]);
  }

  const current = crumbs[crumbs.length - 1]?.node;
  const listNodes =
    crumbs.length === 0 ? ADMIN_CONTENT_TREE : current?.children || [];
  const scopePath = current?.scopePath;
  const packageId = current?.packageId;
  const isHub = Boolean(scopePath) && HUB_IDS.includes(current?.id || "");

  const activeKey = isHub
    ? hubLockKey(scopePath!, current!.id)
    : scopePath
      ? scopeLockKey(scopePath)
      : packageId
        ? packageLockKey(packageId)
        : null;

  const activeLock = activeKey ? lockMap.get(activeKey) : undefined;

  return (
    <div className="min-w-0 space-y-4 break-words">
      {error && (
        <AdminCard tone="red">
          <p className="text-sm text-rose-200">{error}</p>
        </AdminCard>
      )}

      <AdminPageHeader
        icon={Shield}
        title="Content locks"
        subtitle="Lock or unlock packages, subjects, and hubs (books, exams, etc.). Specific exams stay controlled by the Published checkbox in Content."
      />

      <AdminPill fullWidth icon={RefreshCw} onClick={() => void load()} disabled={loading}>
        {loading ? "Refreshing…" : "Refresh"}
      </AdminPill>

      {/* Package-level shortcuts */}
      <div>
        <AdminSectionTitle>Package shortcuts</AdminSectionTitle>
        <div className="space-y-2">
          {PACKAGE_SHORTCUTS.map((p) => {
            const key = packageLockKey(p.id);
            const lock = lockMap.get(key);
            return (
              <AdminCard key={p.id} className="space-y-2">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="min-w-0 text-sm font-semibold text-white">{p.label}</span>
                  {modeBadge(lock?.mode)}
                </div>
                <ModeButtons
                  current={lock?.mode}
                  busy={busyKey === key}
                  hasLock={Boolean(lock)}
                  resetLabel="Default"
                  onPick={(mode) => void applyMode(key, mode, p.label)}
                  onReset={() => void resetToDefault(key)}
                />
              </AdminCard>
            );
          })}
        </div>
      </div>

      {/* Tree navigation (breadcrumb) */}
      <div className="flex flex-wrap items-center gap-x-1 text-sm">
        <button
          type="button"
          onClick={resetRoot}
          className="min-h-[44px] px-1 font-medium text-amber-300"
        >
          Academy
        </button>
        {crumbs.map((c, i) => (
          <span key={c.id + i} className="flex items-center gap-1">
            <ChevronRight className="h-3.5 w-3.5 shrink-0 text-wisdom-muted" />
            <button
              type="button"
              onClick={() => goTo(i)}
              className={`min-h-[44px] px-1 text-left ${
                i === crumbs.length - 1
                  ? "font-semibold text-white"
                  : "text-cyan-300"
              }`}
            >
              {c.label}
            </button>
          </span>
        ))}
      </div>

      {/* Controls for current node */}
      {activeKey && (
        <AdminCard tone="gold" className="space-y-3">
          <div className="flex flex-wrap items-center gap-2">
            {activeLock?.mode === "open" || !activeLock ? (
              <Unlock className="h-4 w-4 shrink-0 text-emerald-300" />
            ) : (
              <Lock className="h-4 w-4 shrink-0 text-rose-300" />
            )}
            <p className="min-w-0 text-sm font-semibold text-white">{current?.label || "Selection"}</p>
            {modeBadge(activeLock?.mode)}
          </div>
          <p className="break-all font-mono text-xs text-wisdom-muted">{activeKey}</p>
          <ModeButtons
            current={activeLock?.mode}
            busy={busyKey === activeKey}
            hasLock={Boolean(activeLock)}
            withIcons
            resetLabel="Reset to code default"
            onPick={(mode) => void applyMode(activeKey, mode, current?.label || activeKey)}
            onReset={() => void resetToDefault(activeKey)}
          />
          <p className="text-xs leading-snug text-wisdom-muted">
            Hub-level locks override subject locks; subject locks override package
            locks. Individual exams: use <strong className="text-white">Published</strong>{" "}
            in the Content tab.
          </p>
        </AdminCard>
      )}

      {/* Children list */}
      {!isHub && listNodes.length > 0 && (
        <AdminTileGrid>
          {listNodes.map((node) => {
            const childKey =
              node.scopePath && HUB_IDS.includes(node.id)
                ? hubLockKey(node.scopePath, node.id)
                : node.scopePath
                  ? scopeLockKey(node.scopePath)
                  : node.packageId
                    ? packageLockKey(node.packageId)
                    : null;
            const childLock = childKey ? lockMap.get(childKey) : undefined;
            return (
              <AdminTile
                key={node.id}
                icon={FolderOpen}
                label={node.label}
                badge={childLock ? modeBadge(childLock.mode) : undefined}
                onClick={() => enter(node)}
              />
            );
          })}
        </AdminTileGrid>
      )}

      {isHub && (
        <AdminCard>
          <p className="py-3 text-center text-sm text-wisdom-muted">
            This is a hub. Use the buttons above to lock/unlock the whole hub.
            Open the <strong className="text-white">Content</strong> tab to publish
            or unpublish individual exams and items.
          </p>
        </AdminCard>
      )}

      {locks.length > 0 && crumbs.length === 0 && (
        <div>
          <AdminSectionTitle>All active overrides ({locks.length})</AdminSectionTitle>
          <AdminCard className="space-y-2">
            {locks.map((l) => (
              <div key={l.lockKey} className="flex flex-wrap items-center gap-2 text-xs text-wisdom-muted">
                <span className="min-w-0 break-all font-mono text-cyan-300/90">{l.lockKey}</span>
                {modeBadge(l.mode)}
              </div>
            ))}
          </AdminCard>
        </div>
      )}

      <AdminToast message={toast || null} onClose={() => setToast("")} durationMs={3500} />
    </div>
  );
}
