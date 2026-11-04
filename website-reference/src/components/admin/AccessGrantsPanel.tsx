"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import {
  listAccessGrants,
  grantAccess,
  grantAllPackages,
  revokeAccess,
  revokeAllForEmail,
  ALL_PACKAGES_ID,
  type AccessGrant,
} from "@/lib/access-grants";
import { academyPackages } from "@/data/packages";
import { KeyRound, Plus, RefreshCw, Trash2, ShieldCheck } from "lucide-react";
import {
  AdminCard,
  AdminEmpty,
  AdminField,
  AdminInput,
  AdminPageHeader,
  AdminPill,
  AdminSearch,
  AdminSelect,
  AdminSheet,
  AdminSkeleton,
  AdminToast,
} from "./ui";

/**
 * Admin Access Grants tab.
 *
 * 2026-10-16 (Admin UI V2, slice U8 part 1): LAYOUT ONLY. Kit page header, kit cards, kit fields and pills,
 * the search is the kit search, grants are kit cards, the browser confirm boxes before Revoke and Revoke all are
 * a kit bottom sheet (same two sentences), the message is the kit slim toast, a static skeleton while loading.
 * listAccessGrants, grantAccess, grantAllPackages, revokeAccess, revokeAllForEmail and every text are unchanged.
 */

type Props = { adminEmail: string };

type PendingRevoke = { kind: "one"; id: string; label: string } | { kind: "all"; email: string };

export default function AccessGrantsPanel({ adminEmail }: Props) {
  const [grants, setGrants] = useState<AccessGrant[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [toast, setToast] = useState("");
  const [busy, setBusy] = useState(false);

  const [email, setEmail] = useState("");
  const [packageId, setPackageId] = useState<string>(ALL_PACKAGES_ID);
  const [note, setNote] = useState("Beta access");
  const [filter, setFilter] = useState("");
  const [pendingRevoke, setPendingRevoke] = useState<PendingRevoke | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    const res = await listAccessGrants();
    if (res.error) {
      setError(
        res.error.includes("relation") || res.error.includes("does not exist")
          ? "Table missing: run docs/access-grants-setup.sql in Supabase SQL Editor."
          : res.error
      );
      setGrants([]);
    } else {
      setGrants(res.grants);
    }
    setLoading(false);
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const filtered = useMemo(() => {
    const q = filter.trim().toLowerCase();
    if (!q) return grants;
    return grants.filter(
      (g) =>
        g.email.includes(q) ||
        g.packageId.toLowerCase().includes(q) ||
        (g.packageName || "").toLowerCase().includes(q) ||
        (g.note || "").toLowerCase().includes(q)
    );
  }, [grants, filter]);

  async function onGrant(e: React.FormEvent) {
    e.preventDefault();
    if (!email.trim()) {
      setToast("Email is required");
      return;
    }
    setBusy(true);

    if (packageId === ALL_PACKAGES_ID) {
      const res = await grantAllPackages({
        email,
        grantedBy: adminEmail,
        note: note || "Beta / full access",
      });
      setBusy(false);
      if (!res.ok) {
        setToast(res.error || "Grant failed");
        return;
      }
      setToast(`Granted full access to ${email.trim().toLowerCase()} (${res.count} rows)`);
    } else {
      const res = await grantAccess({
        email,
        packageId,
        grantedBy: adminEmail,
        note: note || null,
        source: "beta",
      });
      setBusy(false);
      if (!res.ok) {
        setToast(res.error || "Grant failed");
        return;
      }
      setToast(`Granted ${packageId} → ${email.trim().toLowerCase()}`);
    }

    setEmail("");
    void load();
  }

  async function onRevoke(id: string) {
    setBusy(true);
    const res = await revokeAccess(id);
    setBusy(false);
    if (!res.ok) {
      setToast(res.error || "Revoke failed");
      return;
    }
    setToast("Revoked");
    void load();
  }

  async function onRevokeEmail(em: string) {
    setBusy(true);
    const res = await revokeAllForEmail(em);
    setBusy(false);
    if (!res.ok) {
      setToast(res.error || "Revoke failed");
      return;
    }
    setToast(`Removed ${res.removed ?? 0} grant(s) for ${em}`);
    void load();
  }

  async function confirmRevoke() {
    const p = pendingRevoke;
    setPendingRevoke(null);
    if (!p) return;
    if (p.kind === "one") await onRevoke(p.id);
    else await onRevokeEmail(p.email);
  }

  return (
    <div className="min-w-0 space-y-4">
      {error && (
        <AdminCard tone="red">
          <p className="break-words text-sm text-rose-200">{error}</p>
        </AdminCard>
      )}

      <AdminPageHeader
        icon={KeyRound}
        title="Access grants"
        subtitle="Whitelist emails for beta users: unlock specific packages or the whole catalog without payment. Works even before they sign up (matched by email)."
      />

      <AdminPill fullWidth icon={RefreshCw} onClick={() => void load()} disabled={loading}>
        {loading ? "Refreshing…" : "Refresh"}
      </AdminPill>

      <form onSubmit={onGrant} className="min-w-0">
        <AdminCard tone="gold" className="space-y-3">
          <p className="flex items-center gap-2 text-xs font-extrabold uppercase tracking-wider text-amber-200">
            <ShieldCheck className="h-4 w-4 shrink-0" />
            Grant access
          </p>
          <AdminField label="Email">
            <AdminInput
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="beta.user@example.com"
              autoComplete="off"
            />
          </AdminField>
          <AdminField label="Package">
            <AdminSelect value={packageId} onChange={(e) => setPackageId(e.target.value)}>
              <option value={ALL_PACKAGES_ID}>★ All packages (full access)</option>
              {academyPackages.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name}
                </option>
              ))}
            </AdminSelect>
          </AdminField>
          <AdminField label="Note (optional)">
            <AdminInput
              value={note}
              onChange={(e) => setNote(e.target.value)}
              placeholder="Beta tester · March cohort"
            />
          </AdminField>
          <AdminPill type="submit" tone="primary" fullWidth icon={Plus} disabled={busy}>
            {busy ? "Saving…" : "Grant access"}
          </AdminPill>
          <p className="break-words text-xs leading-snug text-wisdom-muted">
            User does not need an account yet. When they sign in with this email,
            packages unlock automatically: no cart or payment.
          </p>
        </AdminCard>
      </form>

      <div className="space-y-2">
        <AdminSearch value={filter} onChange={setFilter} placeholder="Filter by email or package…" />
        <p className="text-xs text-wisdom-muted">{filtered.length} grant(s)</p>
      </div>

      {loading && grants.length === 0 ? (
        <AdminSkeleton rows={3} />
      ) : filtered.length === 0 ? (
        <AdminEmpty icon={KeyRound} title="No access grants yet. Add a beta email above." />
      ) : (
        <div className="space-y-2">
          {filtered.map((g) => (
            <AdminCard key={g.id} className="space-y-3">
              <div className="min-w-0">
                <p className="break-all text-sm font-semibold text-white">{g.email}</p>
                <p className="mt-0.5 break-words text-xs text-wisdom-muted">
                  <span
                    className={
                      g.packageId === ALL_PACKAGES_ID
                        ? "font-semibold text-amber-300"
                        : "text-cyan-300"
                    }
                  >
                    {g.packageName || g.packageId}
                  </span>
                  {g.note ? ` · ${g.note}` : ""}
                  {g.grantedBy ? ` · by ${g.grantedBy}` : ""}
                  {g.userId ? " · linked account" : " · email only (pre-signup)"}
                </p>
              </div>
              <div className="grid grid-cols-2 gap-2">
                <AdminPill
                  tone="danger"
                  icon={Trash2}
                  disabled={busy}
                  onClick={() => setPendingRevoke({ kind: "one", id: g.id, label: `${g.email} / ${g.packageId}` })}
                >
                  Revoke
                </AdminPill>
                <AdminPill disabled={busy} onClick={() => setPendingRevoke({ kind: "all", email: g.email })}>
                  Revoke all
                </AdminPill>
              </div>
            </AdminCard>
          ))}
        </div>
      )}

      {/* Confirm before revoking */}
      <AdminSheet
        open={pendingRevoke !== null}
        onClose={() => setPendingRevoke(null)}
        title={pendingRevoke?.kind === "all" ? "Revoke all grants" : "Revoke access"}
        footer={
          <div className="flex gap-2">
            <AdminPill fullWidth onClick={() => setPendingRevoke(null)}>
              Back
            </AdminPill>
            <AdminPill fullWidth tone="danger" icon={Trash2} onClick={() => void confirmRevoke()}>
              {pendingRevoke?.kind === "all" ? "Revoke all" : "Revoke"}
            </AdminPill>
          </div>
        }
      >
        <p className="break-words text-sm text-wisdom-muted">
          {pendingRevoke?.kind === "all"
            ? `Revoke ALL grants for ${pendingRevoke.email}?`
            : pendingRevoke
              ? `Revoke access for ${pendingRevoke.label}?`
              : ""}
        </p>
      </AdminSheet>

      <AdminToast message={toast || null} onClose={() => setToast("")} />
    </div>
  );
}
