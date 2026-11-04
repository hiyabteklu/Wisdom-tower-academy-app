"use client";

import { useCallback, useEffect, useState } from "react";
import { supabase } from "@/lib/supabase";
import {
  Database,
  CheckCircle2,
  AlertTriangle,
  ExternalLink,
  RefreshCw,
  ShieldCheck,
  Zap,
  Layers,
} from "lucide-react";
import { AdminBadge, AdminCard, AdminChip, AdminPageHeader, AdminPill, AdminSectionTitle } from "./ui";

/**
 * Admin Database hub tab.
 *
 * 2026-10-16 (Admin UI V2, slice U9 part 2): LAYOUT ONLY. Kit page header, kit cards and chips, kit badges for the
 * table status, kit pills (Open SQL Editor is a link styled like the kit primary pill). The health check
 * (one head-count query per tracked table), the table list and every text are unchanged.
 */

interface TableHealth {
  table: string;
  label: string;
  description: string;
  status: "checking" | "ok" | "missing" | "error";
  errorDetail?: string;
  rowCount?: number;
}

const TRACKED_TABLES: Omit<TableHealth, "status" | "rowCount">[] = [
  {
    table: "profiles",
    label: "Student Profiles",
    description: "Matriculation data, student IDs, streams, school names, settings",
  },
  {
    table: "orders",
    label: "Payment Orders",
    description: "Manual payment orders, transaction refs, Telebirr/CBE reconciliation",
  },
  {
    table: "enrollments",
    label: "Course Enrollments",
    description: "Active access records matching verified payment orders",
  },
  {
    table: "access_grants",
    label: "Manual Access Grants",
    description: "Direct admin-granted access, scholarship passes, beta testing overrides",
  },
  {
    table: "content_locks",
    label: "Content Access Locks",
    description: "Fine-grained chapter/hub locks, coming soon toggles, purchase gates",
  },
  {
    table: "academic_results",
    label: "Quiz & Exam Scores",
    description: "Student test submissions, scores, percentages, and progress history",
  },
  {
    table: "question_explanations",
    label: "AI Explanations Cache",
    description: "Cached Groq/OpenAI tutor explanations for zero-latency queries",
  },
  {
    table: "inquiries",
    label: "Contact & Inquiries",
    description: "Student help inquiries, contact form messages, and support records",
  },
  {
    table: "free_resource_items",
    label: "Free Resource Hub",
    description: "Scholarships, campus guides, success stories, and university articles",
  },
  {
    table: "api_rate_limits",
    label: "API Rate Limiting",
    description: "Protection against brute force on AI endpoints and checkouts",
  },
];

export default function DatabaseHubPanel() {
  const [healthList, setHealthList] = useState<TableHealth[]>(
    TRACKED_TABLES.map((t) => ({ ...t, status: "checking" }))
  );
  const [checking, setChecking] = useState(false);

  const runHealthCheck = useCallback(async () => {
    setChecking(true);
    const updated: TableHealth[] = await Promise.all(
      TRACKED_TABLES.map(async (item) => {
        try {
          const { count, error } = await supabase
            .from(item.table)
            .select("*", { count: "exact", head: true });

          if (error) {
            const msg = error.message.toLowerCase();
            if (msg.includes("does not exist") || msg.includes("relation") || msg.includes("404")) {
              return {
                ...item,
                status: "missing",
                errorDetail: "Table not created yet",
              };
            }
            return {
              ...item,
              status: "error",
              errorDetail: error.message,
            };
          }

          return {
            ...item,
            status: "ok",
            rowCount: count ?? 0,
          };
        } catch (e) {
          return {
            ...item,
            status: "error",
            errorDetail: e instanceof Error ? e.message : "Network error",
          };
        }
      })
    );
    setHealthList(updated);
    setChecking(false);
  }, []);

  useEffect(() => {
    void runHealthCheck();
  }, [runHealthCheck]);

  const healthyCount = healthList.filter((h) => h.status === "ok").length;
  const missingCount = healthList.filter((h) => h.status === "missing").length;

  return (
    <div className="min-w-0 space-y-4">
      <AdminPageHeader
        icon={Database}
        title="Supabase & System Architecture Hub"
        subtitle="Real-time diagnostic health check and table status"
      />

      <div className="grid grid-cols-2 gap-2">
        <AdminPill
          icon={RefreshCw}
          onClick={() => void runHealthCheck()}
          disabled={checking}
          className="px-3 text-center leading-tight"
        >
          {checking ? "Checking…" : "Recheck Tables"}
        </AdminPill>
        <a
          href="https://supabase.com/dashboard/project/_/sql/new"
          target="_blank"
          rel="noreferrer"
          className="inline-flex min-h-[44px] items-center justify-center gap-2 rounded-full border border-transparent bg-cyan-500 px-3 text-center text-sm font-bold leading-tight text-wisdom-dark active:bg-cyan-400"
        >
          Open SQL Editor
          <ExternalLink className="h-3.5 w-3.5 shrink-0" />
        </a>
      </div>

      {/* Health overview: first card full width, the other two side by side */}
      <div className="grid grid-cols-2 gap-2">
        <AdminCard className="col-span-2">
          <p className="text-xs font-semibold uppercase tracking-wider text-wisdom-muted">Database Health</p>
          <div className="mt-1 flex flex-wrap items-baseline gap-2">
            <p className="text-2xl font-extrabold tabular-nums text-white">
              {healthyCount} / {healthList.length}
            </p>
            <span className="text-xs font-semibold text-emerald-300">Active Tables</span>
          </div>
          <p className="mt-1 break-words text-sm text-wisdom-muted">
            {missingCount === 0
              ? "All core tables and schemas are online"
              : `${missingCount} table(s) need initialization`}
          </p>
        </AdminCard>

        <AdminCard className="min-w-0 space-y-1">
          <p className="text-xs font-semibold uppercase tracking-wider text-wisdom-muted">Security & RLS</p>
          <div className="pt-1">
            <AdminChip icon={ShieldCheck} tone="green" size="sm" />
          </div>
          <p className="break-words text-sm font-bold text-white">Row Level Security</p>
          <p className="break-words text-xs text-wisdom-muted">
            Strict isolation: students access own data; admins manage all
          </p>
        </AdminCard>

        <AdminCard className="min-w-0 space-y-1">
          <p className="text-xs font-semibold uppercase tracking-wider text-wisdom-muted">Auto-ID Sequencing</p>
          <div className="pt-1">
            <AdminChip icon={Zap} tone="gold" size="sm" />
          </div>
          <p className="break-words text-sm font-bold text-white">WTA-XXXXX Digital IDs</p>
          <p className="break-words text-xs text-wisdom-muted">
            Auto-assigned to students upon registration with 1-year expiry
          </p>
        </AdminCard>
      </div>

      {/* Table diagnostic list: stacked rows, no table, no sideways scroll */}
      <div>
        <AdminSectionTitle>Live Database Diagnostic Matrix</AdminSectionTitle>
        <p className="mb-2 flex items-start gap-1.5 text-xs text-wisdom-muted">
          <Layers className="mt-0.5 h-3.5 w-3.5 shrink-0 text-amber-300" />
          <span className="min-w-0 break-words">
            Current accessibility status of all application tables via authenticated client
          </span>
        </p>

        <div className="space-y-2">
          {healthList.map((item) => (
            <AdminCard key={item.table}>
              <p className="break-all font-mono text-xs font-bold text-cyan-300">public.{item.table}</p>
              <p className="mt-0.5 break-words text-sm font-medium text-white/90">{item.label}</p>
              <p className="mt-0.5 break-words text-xs text-wisdom-muted">{item.description}</p>

              <div className="mt-2 flex flex-wrap items-center gap-2">
                {item.status === "ok" && (
                  <>
                    <AdminBadge tone="green" className="gap-1">
                      <CheckCircle2 className="h-3.5 w-3.5" />
                      Active
                    </AdminBadge>
                    <span className="text-xs tabular-nums text-wisdom-muted">
                      {item.rowCount} row{item.rowCount === 1 ? "" : "s"}
                    </span>
                  </>
                )}
                {item.status === "missing" && (
                  <AdminBadge tone="gold" className="gap-1">
                    <AlertTriangle className="h-3.5 w-3.5" />
                    Needs Setup
                  </AdminBadge>
                )}
                {item.status === "error" && (
                  <AdminBadge tone="red" className="gap-1">
                    <AlertTriangle className="h-3.5 w-3.5" />
                    Permission / Error
                  </AdminBadge>
                )}
                {item.status === "checking" && <span className="text-xs text-wisdom-muted">Checking…</span>}
              </div>
              {(item.status === "missing" || item.status === "error") && item.errorDetail && (
                <p className="mt-1.5 break-words text-xs text-wisdom-muted">{item.errorDetail}</p>
              )}
            </AdminCard>
          ))}
        </div>
      </div>

      {/* The old "master SQL script" was retired (2026-10-15): it was a stale copy that would re-create
          policies the live project no longer has (open read/update for every signed-in user). */}
      <AdminCard tone="gold">
        <div className="flex items-start gap-3">
          <AdminChip icon={ShieldCheck} />
          <div className="min-w-0">
            <h3 className="break-words text-sm font-semibold text-white">Setup script retired</h3>
            <p className="mt-1 break-words text-sm leading-relaxed text-wisdom-muted">
              The old copy-and-paste master script was removed on purpose. It no longer matches the live database
              and would bring back open permissions. Database changes are now made as tested migrations by the
              developer; do not paste old SQL into the SQL Editor.
            </p>
            <p className="mt-2 break-words text-sm leading-relaxed text-wisdom-muted">
              A table marked Needs Setup above means it is missing in the live project: tell the developer which
              one instead of creating it by hand.
            </p>
          </div>
        </div>
      </AdminCard>
    </div>
  );
}
