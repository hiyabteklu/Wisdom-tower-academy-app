"use client";

/*
 * Admin UI kit (slice U1 of docs/ADMIN_UI_V2_PLAN.md).
 * One shared set of building blocks so every Admin Console screen uses the look of the home grid:
 * dark cards, gold icon chips, 44 px pills, bottom sheets.
 * Rules for this file and for every panel that uses it:
 * - no header, footer or nav elements (the app-mode CSS hides them)
 * - no frosted-glass filters, no animation loops, no sideways scrolling, no fixed widths wider than the phone
 * - no raw apostrophe, quote, greater-than or closing-brace characters in JSX text (ESLint fails the build)
 * - layout only: nothing here fetches data or changes permissions
 */

import { useEffect, useRef, useState, type ReactNode, type ComponentType, type ButtonHTMLAttributes, type InputHTMLAttributes, type TextareaHTMLAttributes, type SelectHTMLAttributes } from "react";
import { createPortal } from "react-dom";
import { ChevronRight, Inbox, Search, X } from "lucide-react";

export type AdminIcon = ComponentType<{ className?: string }>;
export type AdminTone = "neutral" | "gold" | "cyan" | "green" | "red";

function cx(...parts: Array<string | false | null | undefined>): string {
  return parts.filter(Boolean).join(" ");
}

const TONE_BADGE: Record<AdminTone, string> = {
  neutral: "border-white/15 bg-white/10 text-white/90",
  gold: "border-amber-400/40 bg-amber-500/25 text-amber-200",
  cyan: "border-cyan-400/40 bg-cyan-500/25 text-cyan-200",
  green: "border-emerald-400/40 bg-emerald-500/20 text-emerald-200",
  red: "border-rose-400/40 bg-rose-500/20 text-rose-200",
};

const TONE_CHIP: Record<AdminTone, string> = {
  neutral: "border-white/15 bg-white/10 text-white/80",
  gold: "border-amber-400/20 bg-amber-500/10 text-amber-300",
  cyan: "border-cyan-400/20 bg-cyan-500/10 text-cyan-300",
  green: "border-emerald-400/20 bg-emerald-500/10 text-emerald-300",
  red: "border-rose-400/20 bg-rose-500/10 text-rose-300",
};

const FIELD_CLASS =
  "w-full min-h-[44px] rounded-xl border border-white/10 bg-wisdom-card px-3 py-2.5 text-sm text-white placeholder-wisdom-muted focus:border-amber-400 focus:outline-none disabled:opacity-50";

/* ---------- Badge and icon chip ---------- */

export function AdminBadge({ tone = "neutral", children, className }: { tone?: AdminTone; children: ReactNode; className?: string }) {
  return (
    <span
      className={cx(
        "inline-flex shrink-0 items-center rounded-full border px-1.5 py-0.5 text-[11px] font-extrabold leading-none tabular-nums",
        TONE_BADGE[tone],
        className
      )}
    >
      {children}
    </span>
  );
}

/** The gold icon chip from the home grid. */
export function AdminChip({ icon: Icon, tone = "gold", size = "md", className }: { icon: AdminIcon; tone?: AdminTone; size?: "sm" | "md"; className?: string }) {
  return (
    <span
      className={cx(
        "flex shrink-0 items-center justify-center rounded-xl border",
        size === "sm" ? "h-8 w-8" : "h-10 w-10",
        TONE_CHIP[tone],
        className
      )}
    >
      <Icon className={size === "sm" ? "h-4 w-4" : "h-5 w-5"} />
    </span>
  );
}

/** Round initials badge for people rows. */
export function AdminAvatar({ label, className }: { label: string; className?: string }) {
  const text = (label || "?").trim().slice(0, 2).toUpperCase() || "?";
  return (
    <span
      className={cx(
        "flex h-10 w-10 shrink-0 items-center justify-center rounded-full border border-amber-400/20 bg-amber-500/10 text-sm font-extrabold text-amber-300",
        className
      )}
    >
      {text}
    </span>
  );
}

/* ---------- Page header and cards ---------- */

export function AdminPageHeader({ icon, title, subtitle, action }: { icon?: AdminIcon; title: string; subtitle?: string; action?: ReactNode }) {
  return (
    <div className="flex items-center gap-3">
      {icon ? <AdminChip icon={icon} /> : null}
      <div className="min-w-0 flex-1">
        <h2 className="truncate text-lg font-bold leading-tight text-white">{title}</h2>
        {subtitle ? <p className="mt-0.5 text-xs leading-snug text-wisdom-muted">{subtitle}</p> : null}
      </div>
      {action ? <div className="shrink-0">{action}</div> : null}
    </div>
  );
}

export function AdminCard({ children, className, tone }: { children: ReactNode; className?: string; tone?: "gold" | "red" }) {
  return (
    <div
      className={cx(
        "min-w-0 rounded-2xl border bg-wisdom-card p-3",
        tone === "gold" ? "border-amber-400/30" : tone === "red" ? "border-rose-400/30" : "border-white/10",
        className
      )}
    >
      {children}
    </div>
  );
}

/** Small uppercase group title above a list of cards or fields. */
export function AdminSectionTitle({ children, action }: { children: ReactNode; action?: ReactNode }) {
  return (
    <div className="mb-2 mt-5 flex items-center justify-between gap-2 first:mt-0">
      <h3 className="min-w-0 truncate text-xs font-extrabold uppercase tracking-wider text-wisdom-muted">{children}</h3>
      {action ? <div className="shrink-0">{action}</div> : null}
    </div>
  );
}

/* ---------- Stats ---------- */

export function AdminStat({
  label,
  value,
  tone,
  onClick,
}: {
  label: string;
  value: number | string | null | undefined;
  tone?: AdminTone;
  onClick?: () => void;
}) {
  const shown = value === null || value === undefined ? "–" : value;
  const body = (
    <>
      <div className={cx("text-xl font-extrabold tabular-nums", tone === "gold" ? "text-amber-200" : tone === "cyan" ? "text-cyan-200" : tone === "green" ? "text-emerald-200" : tone === "red" ? "text-rose-200" : "text-white")}>
        {shown}
      </div>
      <div className="text-xs leading-tight text-wisdom-muted">{label}</div>
    </>
  );
  const base = "min-h-[64px] min-w-0 rounded-2xl border border-white/10 bg-wisdom-card px-2 py-2 text-center";
  if (onClick) {
    return (
      <button type="button" onClick={onClick} className={cx(base, "active:bg-white/10")}>
        {body}
      </button>
    );
  }
  return <div className={base}>{body}</div>;
}

export function AdminStatRow({ children, columns = 3 }: { children: ReactNode; columns?: 2 | 3 }) {
  return <div className={cx("grid gap-2", columns === 2 ? "grid-cols-2" : "grid-cols-3")}>{children}</div>;
}

/* ---------- Tiles (navigation inside a section) ---------- */

export function AdminTile({ icon, label, badge, onClick, disabled }: { icon: AdminIcon; label: string; badge?: ReactNode; onClick?: () => void; disabled?: boolean }) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      className="relative flex min-h-[96px] min-w-0 flex-col items-center justify-center gap-2 rounded-2xl border border-white/10 bg-wisdom-card px-2 py-3 text-center active:bg-white/10 disabled:opacity-50"
    >
      {badge !== undefined && badge !== null ? <span className="absolute right-1.5 top-1.5">{badge}</span> : null}
      <AdminChip icon={icon} />
      <span className="line-clamp-2 text-[13px] font-semibold leading-tight text-white">{label}</span>
    </button>
  );
}

export function AdminTileGrid({ children }: { children: ReactNode }) {
  return <div className="grid grid-cols-2 gap-2 min-[340px]:grid-cols-3">{children}</div>;
}

/* ---------- Record row ---------- */

export function AdminRow({
  leading,
  title,
  meta,
  badge,
  onClick,
  chevron,
  className,
}: {
  leading?: ReactNode;
  title: ReactNode;
  meta?: ReactNode;
  badge?: ReactNode;
  onClick?: () => void;
  /** Defaults to true when the row is tappable. */
  chevron?: boolean;
  className?: string;
}) {
  const showChevron = chevron ?? Boolean(onClick);
  const body = (
    <>
      {leading ? <span className="shrink-0">{leading}</span> : null}
      <span className="min-w-0 flex-1 text-left">
        <span className="block truncate text-sm font-semibold text-white">{title}</span>
        {meta ? <span className="mt-0.5 block text-xs leading-snug text-wisdom-muted">{meta}</span> : null}
      </span>
      {badge ? <span className="shrink-0">{badge}</span> : null}
      {showChevron ? <ChevronRight className="h-4 w-4 shrink-0 text-wisdom-muted" /> : null}
    </>
  );
  const base = cx("flex min-h-[56px] w-full min-w-0 items-center gap-3 rounded-2xl border border-white/10 bg-wisdom-card px-3 py-2.5", className);
  if (onClick) {
    return (
      <button type="button" onClick={onClick} className={cx(base, "active:bg-white/10")}>
        {body}
      </button>
    );
  }
  return <div className={base}>{body}</div>;
}

/* ---------- Pills and chips ---------- */

export type AdminPillTone = "ghost" | "primary" | "gold" | "danger";

const PILL_TONE: Record<AdminPillTone, string> = {
  ghost: "border-white/10 bg-wisdom-card text-wisdom-muted active:bg-white/10",
  primary: "border-transparent bg-cyan-500 font-bold text-wisdom-dark active:bg-cyan-400",
  gold: "border-amber-400/30 bg-amber-500/15 text-amber-200 active:bg-amber-500/25",
  danger: "border-rose-400/30 bg-rose-500/15 text-rose-200 active:bg-rose-500/25",
};

export function AdminPill({
  tone = "ghost",
  icon: Icon,
  fullWidth,
  className,
  children,
  type,
  ...rest
}: ButtonHTMLAttributes<HTMLButtonElement> & { tone?: AdminPillTone; icon?: AdminIcon; fullWidth?: boolean }) {
  return (
    <button
      type={type ?? "button"}
      className={cx(
        "inline-flex min-h-[44px] items-center justify-center gap-2 rounded-full border px-4 text-sm font-semibold disabled:pointer-events-none disabled:opacity-50",
        PILL_TONE[tone],
        fullWidth && "w-full",
        className
      )}
      {...rest}
    >
      {Icon ? <Icon className="h-4 w-4" /> : null}
      {children}
    </button>
  );
}

export type AdminChipOption<T extends string> = { id: T; label: string; count?: number | null };

/** Filter group that wraps onto several lines; never scrolls sideways. */
export function AdminChips<T extends string>({ options, value, onChange }: { options: AdminChipOption<T>[]; value: T; onChange: (id: T) => void }) {
  return (
    <div className="flex flex-wrap gap-2">
      {options.map((o) => {
        const active = o.id === value;
        return (
          <button
            key={o.id}
            type="button"
            aria-pressed={active}
            onClick={() => onChange(o.id)}
            className={cx(
              "inline-flex min-h-[44px] items-center gap-1.5 rounded-full border px-4 text-sm font-semibold",
              active ? "border-amber-400/40 bg-amber-500/20 text-amber-200" : "border-white/10 bg-wisdom-card text-wisdom-muted active:bg-white/10"
            )}
          >
            {o.label}
            {o.count !== undefined && o.count !== null ? <span className="text-xs tabular-nums opacity-80">{o.count}</span> : null}
          </button>
        );
      })}
    </div>
  );
}

/* ---------- Search and form fields ---------- */

export function AdminSearch({ value, onChange, placeholder }: { value: string; onChange: (value: string) => void; placeholder?: string }) {
  return (
    <div className="relative">
      <Search className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-wisdom-muted" />
      <input
        type="text"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        className="w-full min-h-[44px] rounded-full border border-white/10 bg-wisdom-card py-2.5 pl-10 pr-10 text-sm text-white placeholder-wisdom-muted focus:border-amber-400 focus:outline-none"
      />
      {value ? (
        <button
          type="button"
          aria-label="Clear search"
          onClick={() => onChange("")}
          className="absolute right-1 top-1/2 inline-flex h-9 w-9 -translate-y-1/2 items-center justify-center rounded-full text-wisdom-muted active:bg-white/10"
        >
          <X className="h-4 w-4" />
        </button>
      ) : null}
    </div>
  );
}

export function AdminField({ label, hint, children }: { label: string; hint?: string; children: ReactNode }) {
  return (
    <label className="block min-w-0">
      <span className="mb-1 block text-xs font-semibold text-wisdom-muted">{label}</span>
      {children}
      {hint ? <span className="mt-1 block text-xs leading-snug text-wisdom-muted">{hint}</span> : null}
    </label>
  );
}

export function AdminInput({ className, ...rest }: InputHTMLAttributes<HTMLInputElement>) {
  return <input className={cx(FIELD_CLASS, className)} {...rest} />;
}

export function AdminTextarea({ className, rows = 4, ...rest }: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea rows={rows} className={cx(FIELD_CLASS, "resize-none", className)} {...rest} />;
}

export function AdminSelect({ className, children, ...rest }: SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <select className={cx(FIELD_CLASS, className)} {...rest}>
      {children}
    </select>
  );
}

/* ---------- Bottom sheet ---------- */

type SheetRescue = { left: number; top: number; width: number; height: number };

/**
 * Editors and confirmations open as a bottom sheet: rounded top corners, plain dim backdrop,
 * its own vertical scroll that does not chain into the page. The phone Back button closes it
 * (one history entry is pushed while it is open).
 *
 * U11 (2026-10-16). In the Android app WebView the panel used to collapse to its 1 px border (measured
 * `top 636 bottom 637 h 1`, window 360x637) so Add, Edit and Delete only dimmed the screen. Probable cause (not
 * proven): the class max-h-[88vh] resolved to about zero there. Confirmed on the owner's phone 2026-10-16 that
 * the sheet now opens. Measures kept in this component:
 * 1. Portal directly under document.body.
 * 2. Panel max-height set INLINE in pixels from window.innerHeight (88 percent), not with the vh unit.
 * 3. About 120 ms after opening the panel is measured: under 40 px tall forces a pixel height; outside the
 *    window switches the wrapper to absolute positioning at the current scroll position.
 * The TEMP-DEBUG measure line was removed after the owner confirmed.
 */
export function AdminSheet({ open, onClose, title, children, footer }: { open: boolean; onClose: () => void; title: string; children: ReactNode; footer?: ReactNode }) {
  const closeRef = useRef(onClose);
  closeRef.current = onClose;
  const panelRef = useRef<HTMLDivElement | null>(null);
  const [rescue, setRescue] = useState<SheetRescue | null>(null);
  const [forceH, setForceH] = useState<number | null>(null);

  useEffect(() => {
    if (!open) return;
    let pushed = false;
    try {
      window.history.pushState(null, "", window.location.href);
      pushed = true;
    } catch {}
    const onPop = () => {
      pushed = false;
      closeRef.current();
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") closeRef.current();
    };
    window.addEventListener("popstate", onPop);
    window.addEventListener("keydown", onKey);
    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      window.removeEventListener("popstate", onPop);
      window.removeEventListener("keydown", onKey);
      document.body.style.overflow = prevOverflow;
      if (pushed) {
        try {
          window.history.back();
        } catch {}
      }
    };
  }, [open]);

  // Measure the panel after layout; fix a collapsed height and a position outside the window.
  useEffect(() => {
    if (!open) {
      setRescue(null);
      setForceH(null);
      return;
    }
    const id = window.setTimeout(() => {
      const el = panelRef.current;
      if (!el) return;
      const r = el.getBoundingClientRect();
      const vw = window.innerWidth;
      const vh = window.innerHeight;
      const tooSmall = r.height < 40;
      const misplaced = r.left < -1 || r.right > vw + 1 || r.top < 0 || (!tooSmall && Math.abs(r.bottom - vh) > 2);
      if (tooSmall) setForceH(Math.round(vh * 0.88));
      if (misplaced) setRescue({ left: window.scrollX, top: window.scrollY, width: vw, height: vh });
    }, 120);
    return () => window.clearTimeout(id);
  }, [open]);

  if (!open || typeof document === "undefined") return null;

  const wrapperStyle = rescue
    ? { position: "absolute" as const, left: rescue.left, top: rescue.top, width: rescue.width, height: rescue.height }
    : undefined;
  const panelStyle = {
    maxHeight: Math.round(window.innerHeight * 0.88),
    ...(forceH !== null ? { height: forceH } : {}),
  };

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-end justify-center" style={wrapperStyle} role="dialog" aria-modal="true" aria-label={title}>
      <div className="absolute inset-0 touch-none bg-black/60" onClick={onClose} aria-hidden="true" />
      <div
        ref={panelRef}
        style={panelStyle}
        className="relative flex w-full max-w-xl min-w-0 flex-col break-words rounded-t-3xl border border-b-0 border-white/10 bg-wisdom-dark text-white"
      >
        <div className="flex shrink-0 items-center gap-2 border-b border-white/10 py-2 pl-4 pr-2">
          <h2 className="min-w-0 flex-1 truncate text-base font-bold text-white">{title}</h2>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close"
            className="inline-flex h-11 w-11 shrink-0 items-center justify-center rounded-full border border-white/10 bg-wisdom-card text-white active:bg-white/10"
          >
            <X className="h-5 w-5" />
          </button>
        </div>
        <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain px-4 py-4">{children}</div>
        {footer ? <div className="shrink-0 border-t border-white/10 px-4 pb-[max(1rem,env(safe-area-inset-bottom))] pt-3">{footer}</div> : null}
      </div>
    </div>,
    document.body
  );
}

/* ---------- Empty state, toast, key/value, skeleton ---------- */

export function AdminEmpty({ icon: Icon = Inbox, title, hint, action }: { icon?: AdminIcon; title: string; hint?: string; action?: ReactNode }) {
  return (
    <div className="flex flex-col items-center gap-2 rounded-2xl border border-white/10 bg-wisdom-card px-4 py-8 text-center">
      <AdminChip icon={Icon} />
      <p className="text-sm font-semibold text-white">{title}</p>
      {hint ? <p className="text-xs leading-snug text-wisdom-muted">{hint}</p> : null}
      {action ? <div className="mt-1">{action}</div> : null}
    </div>
  );
}

/** One slim bar at the bottom. Pass message null to hide it. Closes itself after durationMs. */
export function AdminToast({ message, tone = "neutral", onClose, durationMs = 4000 }: { message: string | null | undefined; tone?: "neutral" | "green" | "red"; onClose: () => void; durationMs?: number }) {
  const closeRef = useRef(onClose);
  closeRef.current = onClose;

  useEffect(() => {
    if (!message) return;
    const id = window.setTimeout(() => closeRef.current(), durationMs);
    return () => window.clearTimeout(id);
  }, [message, durationMs]);

  if (!message) return null;
  const border = tone === "green" ? "border-emerald-400/40" : tone === "red" ? "border-rose-400/40" : "border-white/15";
  return (
    <div className="pointer-events-none fixed inset-x-3 bottom-[max(1rem,env(safe-area-inset-bottom))] z-[60] flex justify-center">
      <div className={cx("pointer-events-auto flex max-w-md min-w-0 items-center gap-2 rounded-full border bg-wisdom-card py-1.5 pl-4 pr-1.5 text-sm text-white", border)} role="status">
        <span className="min-w-0 flex-1">{message}</span>
        <button
          type="button"
          onClick={onClose}
          aria-label="Dismiss"
          className="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-wisdom-muted active:bg-white/10"
        >
          <X className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
}

/** Label and value pairs in two columns (one column when columns is 1). */
export function AdminKeyValue({ items, columns = 2 }: { items: Array<{ label: string; value: ReactNode }>; columns?: 1 | 2 }) {
  return (
    <dl className={cx("grid gap-x-3 gap-y-2", columns === 2 ? "grid-cols-2" : "grid-cols-1")}>
      {items.map((it, i) => (
        <div key={i} className="min-w-0">
          <dt className="text-xs text-wisdom-muted">{it.label}</dt>
          <dd className="break-words text-sm font-medium text-white">{it.value}</dd>
        </div>
      ))}
    </dl>
  );
}

/** Static loading bars (no pulse animation). */
export function AdminSkeleton({ rows = 3 }: { rows?: number }) {
  return (
    <div className="space-y-2" aria-hidden="true">
      {Array.from({ length: rows }).map((_, i) => (
        <div key={i} className="h-14 rounded-2xl border border-white/5 bg-white/5" />
      ))}
    </div>
  );
}
