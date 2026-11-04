"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import {
  listOrdersFromDb,
  verifyOrder,
  rejectOrder,
  statusLabel,
  type ManualOrder,
} from "@/lib/orders";
import { notifyApproval, notifyRejection, formatNotifyToast } from "@/lib/notify";
import { formatEtb } from "@/data/packages";
import { downloadCsv } from "@/lib/admin-data";
import {
  Check,
  X,
  RefreshCw,
  Phone,
  Mail,
  CreditCard,
  Download,
  Calendar,
  Eye,
} from "lucide-react";
import {
  AdminBadge,
  AdminCard,
  AdminChip,
  AdminChips,
  AdminEmpty,
  AdminPill,
  AdminSearch,
  AdminSelect,
  AdminSheet,
  AdminSkeleton,
  AdminToast,
  type AdminTone,
} from "./ui";

/**
 * Admin Payments tab.
 *
 * 2026-10-15 (Admin mobile redesign, slice A8): LAYOUT ONLY (44 px pills, one-column order cards, no blur, no
 * animation). Logic, state, database calls, approve / reject flow, notifications and CSV export unchanged.
 *
 * 2026-10-16 (Admin UI V2, slice U7): LAYOUT ONLY. Kit cards, kit stat cards, kit filter chips, kit search and
 * select, kit badges and pills (Approve is the kit primary pill, Reject the danger pill), the receipt viewer and
 * the reject confirmation are kit bottom sheets (the phone Back button closes them), a static skeleton instead of
 * the brand loader, the kit slim toast (it closes itself after 4.5 s, so the old timer effect is gone).
 * The two sheets never open from each other. verifyOrder, rejectOrder, notifyApproval, notifyRejection,
 * listOrdersFromDb, the filters, the CSV (it exports the filtered list, as before) and every text are unchanged.
 */

type FilterStatus = "pending_verification" | "all" | "verified" | "rejected";

function statusTone(status: string): AdminTone {
  if (status === "verified") return "green";
  if (status === "pending_verification") return "gold";
  return "red";
}

export default function PaymentsPanel({ adminEmail }: { adminEmail: string }) {
  const [orders, setOrders] = useState<ManualOrder[]>([]);
  const [loading, setLoading] = useState(false);
  const [busyId, setBusyId] = useState("");
  const [toast, setToast] = useState("");
  const [filterStatus, setFilterStatus] = useState<FilterStatus>("pending_verification");
  const [bankFilter, setBankFilter] = useState<string>("all");
  const [searchQuery, setSearchQuery] = useState("");

  // Receipt sheet state
  const [viewReceiptUrl, setViewReceiptUrl] = useState<string | null>(null);

  // Reject sheet state
  const [rejectOrderModal, setRejectOrderModal] = useState<ManualOrder | null>(null);
  const [rejectReason, setRejectReason] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    const list = await listOrdersFromDb();
    setOrders(list);
    setLoading(false);
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  // Derived filtered orders
  const filtered = useMemo(() => {
    return orders.filter((o) => {
      // Status match
      if (filterStatus !== "all" && o.status !== filterStatus) return false;

      // Bank match
      if (bankFilter !== "all") {
        const method = (o.paymentMethod || "").toLowerCase();
        if (bankFilter === "telebirr" && !method.includes("telebirr")) return false;
        if (bankFilter === "cbe" && !method.includes("cbe")) return false;
        if (bankFilter === "abyssinia" && !method.includes("abyssinia")) return false;
      }

      // Search match
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase().trim();
        const match =
          o.id.toLowerCase().includes(q) ||
          o.studentName.toLowerCase().includes(q) ||
          o.phone.toLowerCase().includes(q) ||
          (o.email || "").toLowerCase().includes(q) ||
          o.transactionRef.toLowerCase().includes(q) ||
          o.packageName.toLowerCase().includes(q);
        if (!match) return false;
      }

      return true;
    });
  }, [orders, filterStatus, bankFilter, searchQuery]);

  const pendingOrders = orders.filter((o) => o.status === "pending_verification");
  const verifiedOrders = orders.filter((o) => o.status === "verified");
  const pendingAmount = pendingOrders.reduce((sum, o) => sum + (o.amountEtb || 0), 0);
  const verifiedAmount = verifiedOrders.reduce((sum, o) => sum + (o.amountEtb || 0), 0);

  async function onVerify(id: string) {
    setBusyId(id);
    const res = await verifyOrder(id, adminEmail);
    if (!res.ok) {
      setBusyId("");
      setToast(res.error || "Verify failed");
      return;
    }
    const n = await notifyApproval(id);
    setBusyId("");
    setToast(formatNotifyToast(n, "Approved"));
    void load();
  }

  async function onConfirmReject() {
    if (!rejectOrderModal) return;
    const orderId = rejectOrderModal.id;
    setBusyId(orderId);
    const res = await rejectOrder(orderId, adminEmail);
    if (!res.ok) {
      setBusyId("");
      setToast(res.error || "Reject failed");
      return;
    }
    const n = await notifyRejection(orderId);
    setBusyId("");
    setRejectOrderModal(null);
    setRejectReason("");
    setToast(formatNotifyToast(n, "Rejected"));
    void load();
  }

  const handleExportCsv = () => {
    if (orders.length === 0) return;
    const headers = [
      "Order ID",
      "Student Name",
      "Phone",
      "Email",
      "Package Name",
      "Amount ETB",
      "Payment Method",
      "Transaction Ref",
      "Status",
      "Created At",
      "Verified At",
      "Verified By",
    ];
    const rows = filtered.map((o) => [
      o.id,
      o.studentName,
      o.phone,
      o.email || "",
      o.packageName,
      o.amountEtb,
      o.paymentMethod || "",
      o.transactionRef,
      o.status,
      o.createdAt ? new Date(o.createdAt).toLocaleString() : "",
      o.verifiedAt ? new Date(o.verifiedAt).toLocaleString() : "",
      o.verifiedBy || "",
    ]);
    downloadCsv(`wisdom_tower_orders_${new Date().toISOString().slice(0, 10)}`, headers, rows);
  };

  return (
    <div className="min-w-0 space-y-4">
      {/* Title, description and actions */}
      <div className="flex items-start gap-3">
        <AdminChip icon={CreditCard} />
        <div className="min-w-0 flex-1">
          <h3 className="break-words text-base font-bold leading-tight text-white">
            Payment Operations & Order Verification
          </h3>
          <p className="mt-0.5 text-xs leading-relaxed text-wisdom-muted">
            Manual bank slip verification, Telebirr & CBE confirmations, and automated student enrollment
          </p>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-2">
        <AdminPill icon={Download} onClick={handleExportCsv} disabled={filtered.length === 0}>
          Export CSV ({filtered.length})
        </AdminPill>
        <AdminPill tone="gold" icon={RefreshCw} onClick={() => void load()} disabled={loading}>
          {loading ? "Loading..." : "Refresh"}
        </AdminPill>
      </div>

      {/* Financial Pipeline Strip */}
      <div className="grid grid-cols-2 gap-2">
        <AdminCard tone="gold" className="min-w-0">
          <p className="text-xs font-semibold uppercase tracking-wider text-amber-300">Pending Verification</p>
          <p className="mt-1 break-words text-xl font-extrabold tabular-nums text-white">
            {formatEtb(pendingAmount)}
          </p>
          <p className="mt-1 text-xs font-medium text-wisdom-muted">
            {pendingOrders.length} order{pendingOrders.length === 1 ? "" : "s"} awaiting approval
          </p>
        </AdminCard>

        <AdminCard className="min-w-0">
          <p className="text-xs font-semibold uppercase tracking-wider text-emerald-300">Total Verified Revenue</p>
          <p className="mt-1 break-words text-xl font-extrabold tabular-nums text-white">
            {formatEtb(verifiedAmount)}
          </p>
          <p className="mt-1 text-xs font-medium text-wisdom-muted">{verifiedOrders.length} confirmed paid orders</p>
        </AdminCard>

        <AdminCard className="col-span-2">
          <p className="text-xs font-semibold uppercase tracking-wider text-wisdom-muted">All-Time Orders</p>
          <p className="mt-1 text-xl font-extrabold tabular-nums text-white">{orders.length}</p>
          <p className="mt-1 text-xs text-wisdom-muted">Across Telebirr, CBE, Abyssinia, and other rails</p>
        </AdminCard>
      </div>

      {/* Search and Filters */}
      <AdminCard className="space-y-2.5">
        <AdminSearch
          value={searchQuery}
          onChange={setSearchQuery}
          placeholder="Search order, student, phone, email, ref"
        />

        <AdminChips<FilterStatus>
          options={[
            { id: "pending_verification", label: `Pending (${pendingOrders.length})` },
            { id: "verified", label: "Verified" },
            { id: "rejected", label: "Rejected" },
            { id: "all", label: "All" },
          ]}
          value={filterStatus}
          onChange={setFilterStatus}
        />

        {/* Bank Filter */}
        <AdminSelect
          value={bankFilter}
          onChange={(e) => setBankFilter(e.target.value)}
          className="font-semibold"
        >
          <option value="all">All Payment Methods</option>
          <option value="telebirr">Telebirr</option>
          <option value="cbe">CBE</option>
          <option value="abyssinia">Bank of Abyssinia</option>
        </AdminSelect>

        <p className="text-xs tabular-nums text-wisdom-muted">
          Showing {filtered.length} of {orders.length}
        </p>
      </AdminCard>

      {/* Orders List */}
      {loading && orders.length === 0 ? (
        <AdminSkeleton rows={4} />
      ) : filtered.length === 0 ? (
        <AdminEmpty
          icon={CreditCard}
          title="No orders found"
          hint="No orders matching the selected status or search filter."
        />
      ) : (
        <div className="space-y-2">
          {filtered.map((o) => (
            <AdminCard
              key={o.id}
              tone={o.status === "pending_verification" ? "gold" : undefined}
              className={o.status === "rejected" ? "opacity-80" : undefined}
            >
              {/* Order id, status */}
              <div className="flex items-start justify-between gap-2">
                <span className="min-w-0 break-all font-mono text-sm font-bold text-white">{o.id}</span>
                <AdminBadge tone={statusTone(o.status)} className="uppercase tracking-wider">
                  {statusLabel(o.status)}
                </AdminBadge>
              </div>

              <h4 className="mt-2 break-words text-base font-bold text-white">{o.packageName}</h4>
              <div className="mt-0.5 flex items-baseline justify-between gap-2">
                <p className="text-base font-extrabold tabular-nums text-amber-300">{formatEtb(o.amountEtb)}</p>
                <span className="min-w-0 truncate text-xs text-wisdom-muted">
                  via <strong className="uppercase text-white">{o.paymentMethod || "Bank"}</strong>
                </span>
              </div>

              {/* Student details */}
              <div className="mt-2.5 space-y-1.5 border-t border-white/10 py-2.5 text-xs text-wisdom-muted">
                <p className="break-words text-sm font-semibold text-white/90">{o.studentName}</p>
                <div className="flex min-w-0 items-center gap-1.5">
                  <Phone className="h-3.5 w-3.5 shrink-0 text-amber-300/80" />
                  <span className="truncate">{o.phone}</span>
                </div>
                {o.email && (
                  <div className="flex min-w-0 items-center gap-1.5">
                    <Mail className="h-3.5 w-3.5 shrink-0 text-amber-300/80" />
                    <span className="truncate">{o.email}</span>
                  </div>
                )}
              </div>

              {/* Transaction reference and timestamp */}
              <div className="space-y-1.5 border-t border-white/10 py-2.5 text-xs text-wisdom-muted">
                <div className="break-all">
                  <span>Ref: </span>
                  <span className="select-all font-mono font-bold text-cyan-300">{o.transactionRef}</span>
                  {o.note && <span className="ml-1 italic text-wisdom-muted">({o.note})</span>}
                </div>
                <div className="flex items-center gap-1.5">
                  <Calendar className="h-3.5 w-3.5 shrink-0" />
                  <span>Placed {new Date(o.createdAt).toLocaleString()}</span>
                </div>
              </div>

              {/* Actions */}
              {(o.receiptUrl || o.status === "pending_verification") && (
                <div className="space-y-2 border-t border-white/10 pt-3">
                  {o.receiptUrl && (
                    <AdminPill fullWidth icon={Eye} onClick={() => setViewReceiptUrl(o.receiptUrl!)}>
                      View Receipt
                    </AdminPill>
                  )}

                  {o.status === "pending_verification" && (
                    <div className="grid grid-cols-2 gap-2">
                      <AdminPill
                        tone="primary"
                        icon={Check}
                        onClick={() => void onVerify(o.id)}
                        disabled={busyId === o.id}
                      >
                        {busyId === o.id ? "Verifying..." : "Approve & Unlock"}
                      </AdminPill>

                      <AdminPill
                        tone="danger"
                        icon={X}
                        onClick={() => setRejectOrderModal(o)}
                        disabled={busyId === o.id}
                      >
                        Reject
                      </AdminPill>
                    </div>
                  )}
                </div>
              )}
            </AdminCard>
          ))}
        </div>
      )}

      {/* Receipt viewer sheet */}
      <AdminSheet
        open={viewReceiptUrl !== null}
        onClose={() => setViewReceiptUrl(null)}
        title="Payment Receipt Slip"
        footer={
          <AdminPill fullWidth onClick={() => setViewReceiptUrl(null)}>
            Close Viewer
          </AdminPill>
        }
      >
        {viewReceiptUrl ? (
          <div className="flex items-center justify-center rounded-2xl bg-black/50 p-2">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src={viewReceiptUrl} alt="Payment Receipt" className="h-auto max-w-full rounded-xl object-contain" />
          </div>
        ) : null}
      </AdminSheet>

      {/* Reject order sheet */}
      <AdminSheet
        open={rejectOrderModal !== null}
        onClose={() => setRejectOrderModal(null)}
        title="Reject Payment Order"
        footer={
          <div className="flex gap-2">
            <AdminPill fullWidth onClick={() => setRejectOrderModal(null)}>
              Cancel
            </AdminPill>
            <AdminPill
              fullWidth
              tone="danger"
              onClick={() => void onConfirmReject()}
              disabled={rejectOrderModal ? busyId === rejectOrderModal.id : false}
            >
              Confirm Rejection
            </AdminPill>
          </div>
        }
      >
        {rejectOrderModal ? (
          <div className="space-y-3">
            <div className="min-w-0">
              <p className="break-all text-base font-bold text-white">Reject Order {rejectOrderModal.id}</p>
              <p className="mt-1 break-words text-xs text-wisdom-muted">
                Student: {rejectOrderModal.studentName} ({formatEtb(rejectOrderModal.amountEtb)})
              </p>
            </div>
            <p className="text-sm leading-relaxed text-wisdom-muted">
              Are you sure you want to reject this payment? The order will be flagged as rejected and the student will be notified if email or SMS is active.
            </p>
          </div>
        ) : null}
      </AdminSheet>

      <AdminToast message={toast || null} onClose={() => setToast("")} durationMs={4500} />
    </div>
  );
}
