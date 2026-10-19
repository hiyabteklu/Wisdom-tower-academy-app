"use client";

import { useCallback, useEffect, useState } from "react";
import { supabase } from "@/lib/supabase";
import { Database, CheckCircle2, AlertTriangle, RefreshCw, Layers } from "lucide-react";

// N10b (2026-10-19, owner order): this panel is READ-ONLY status. It used to carry the master SQL script with a
// Copy button, an Open SQL Editor link and setup steps. Those are removed on purpose: only the owner works on the
// database, other admins only see its health. It still only counts rows (HEAD request), it writes nothing.

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
              return { ...item, status: "missing", errorDetail: "Table not created yet" };
            }
            return { ...item, status: "error", errorDetail: error.message };
          }

          return { ...item, status: "ok", rowCount: count ?? 0 };
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
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-white/8 pb-4">
        <div>
          <h2 className="text-xl font-bold text-white flex items-center gap-2">
            <Database className="w-5 h-5 text-cyan-400" />
            Database status
          </h2>
          <p className="text-sm text-wisdom-muted mt-0.5">
            Read only. This page only counts the rows of each table. It cannot run SQL or change any data.
          </p>
        </div>
        <button
          type="button"
          onClick={runHealthCheck}
          disabled={checking}
          className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl border border-white/12 text-sm font-semibold hover:bg-white/5 disabled:opacity-50 transition-colors"
        >
          <RefreshCw className={`w-4 h-4 ${checking ? "animate-spin text-cyan-400" : ""}`} />
          Recheck Tables
        </button>
      </div>

      <div className="rounded-2xl border border-white/10 bg-wisdom-card p-4 sm:p-5">
        <p className="text-xs font-semibold uppercase tracking-wider text-wisdom-muted">Database Health</p>
        <div className="flex items-baseline gap-2 mt-1">
          <p className="text-2xl sm:text-3xl font-bold text-white tabular-nums">
            {healthyCount} / {healthList.length}
          </p>
          <span className="text-xs text-emerald-400 font-semibold">Active Tables</span>
        </div>
        <p className="text-xs text-wisdom-muted mt-1">
          {missingCount === 0
            ? "No table reported as missing"
            : `${missingCount} table(s) reported as missing`}
        </p>
      </div>

      <div className="rounded-2xl border border-white/10 bg-wisdom-card overflow-hidden">
        <div className="p-4 sm:p-5 border-b border-white/8">
          <h3 className="font-semibold text-white flex items-center gap-2">
            <Layers className="w-4 h-4 text-cyan-400" />
            Table status
          </h3>
          <p className="text-xs text-wisdom-muted mt-0.5">
            Whether each application table answers for the signed-in account, and its row count
          </p>
        </div>

        <div className="divide-y divide-white/6">
          {healthList.map((item) => (
            <div
              key={item.table}
              className="p-3.5 sm:px-5 sm:py-3 flex items-center justify-between gap-3 text-sm"
            >
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2">
                  <span className="font-mono text-xs font-bold text-cyan-300">public.{item.table}</span>
                  <span className="text-xs text-white/90 font-medium">· {item.label}</span>
                </div>
                <p className="text-xs text-wisdom-muted mt-0.5 truncate">{item.description}</p>
              </div>

              <div className="flex items-center gap-3 shrink-0">
                {item.status === "ok" && (
                  <div className="flex items-center gap-2">
                    <span className="text-xs text-wisdom-muted tabular-nums">
                      {item.rowCount} row{item.rowCount === 1 ? "" : "s"}
                    </span>
                    <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-500/15 text-emerald-300 border border-emerald-400/30">
                      <CheckCircle2 className="w-3.5 h-3.5" />
                      Active
                    </span>
                  </div>
                )}
                {item.status === "missing" && (
                  <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-amber-500/15 text-amber-300 border border-amber-400/30">
                    <AlertTriangle className="w-3.5 h-3.5" />
                    Table missing
                  </span>
                )}
                {item.status === "error" && (
                  <span
                    className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-rose-500/15 text-rose-300 border border-rose-400/30"
                    title={item.errorDetail}
                  >
                    <AlertTriangle className="w-3.5 h-3.5" />
                    Permission / Error
                  </span>
                )}
                {item.status === "checking" && (
                  <span className="text-xs text-wisdom-muted animate-pulse">Checking…</span>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
