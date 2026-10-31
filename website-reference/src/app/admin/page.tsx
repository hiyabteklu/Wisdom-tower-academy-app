"use client";

import { Suspense, useEffect, useState, useCallback, useMemo } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { supabase } from "@/lib/supabase";
import { isAdminEmail } from "@/lib/admin";
import { ensureProfile } from "@/lib/profile";
import type { User } from "@supabase/supabase-js";
import PaymentsPanel from "@/components/admin/PaymentsPanel";
import AnalyticsPanel from "@/components/admin/AnalyticsPanel";
import UsersPanel from "@/components/admin/UsersPanel";
import InquiriesPanel from "@/components/admin/InquiriesPanel";
import CatalogPanel from "@/components/admin/CatalogPanel";
import ContentPanel from "@/components/admin/ContentPanel";
import FreeResourcesPanel from "@/components/admin/FreeResourcesPanel";
import LocksPanel from "@/components/admin/LocksPanel";
import AccessGrantsPanel from "@/components/admin/AccessGrantsPanel";
import DatabaseHubPanel from "@/components/admin/DatabaseHubPanel";
import NotificationsPanel from "@/components/admin/NotificationsPanel";
import CompetitionsAdminPanel from "@/components/admin/CompetitionsAdminPanel";
import DeviceManagementPanel from "@/components/admin/DeviceManagementPanel";
import ExamIntegrityPanel from "@/components/admin/ExamIntegrityPanel";
import BrandLoader from "@/components/BrandLoader";
import {
  LogOut,
  CreditCard,
  Inbox,
  Users,
  LayoutDashboard,
  ExternalLink,
  GraduationCap,
  Package,
  BookOpen,
  Shield,
  KeyRound,
  ArrowLeft,
  Library,
  Database,
  Search,
  Command,
  Eye,
  Sliders,
  CheckCircle2,
  Bell,
  Trophy,
  Smartphone,
} from "lucide-react";

type AcademyTab =
  | "overview"
  | "notifications"
  | "competitions"
  | "users"
  | "payments"
  | "grants"
  | "content"
  | "locks"
  | "catalog"
  | "free-resources"
  | "inquiries"
  | "database"
  | "devices"
  | "exam-integrity";

const VALID_TABS: AcademyTab[] = [
  "overview",
  "notifications",
  "competitions",
  "users",
  "payments",
  "grants",
  "content",
  "locks",
  "catalog",
  "free-resources",
  "inquiries",
  "database",
  "devices",
  "exam-integrity",
];

const ADMIN_TAB_STORAGE_KEY = "wt-admin-active-tab";

const TAB_ALIASES: Record<string, AcademyTab> = {
  overview: "overview",
  notifications: "notifications",
  notification: "notifications",
  competitions: "competitions",
  competition: "competitions",
  contests: "competitions",
  contest: "competitions",
  users: "users",
  user: "users",
  payments: "payments",
  payment: "payments",
  grants: "grants",
  grant: "grants",
  content: "content",
  contents: "content",
  locks: "locks",
  lock: "locks",
  catalog: "catalog",
  pricing: "catalog",
  "free-resources": "free-resources",
  "free-resource": "free-resources",
  free: "free-resources",
  inquiries: "inquiries",
  inquiry: "inquiries",
  database: "database",
  db: "database",
  devices: "devices",
  device: "devices",
  "exam-integrity": "exam-integrity",
  integrity: "exam-integrity",
  exam: "exam-integrity",
};

function parseTab(raw: string | null | undefined): AcademyTab | null {
  if (!raw) return null;
  const normalized = raw.trim().toLowerCase();
  if (normalized in TAB_ALIASES) return TAB_ALIASES[normalized];
  if ((VALID_TABS as string[]).includes(normalized)) return normalized as AcademyTab;
  return null;
}

function resolveInitialTab(urlParam: string | null): AcademyTab {
  const parsedParam = parseTab(urlParam);
  if (parsedParam) return parsedParam;
  if (typeof window !== "undefined") {
    try {
      const windowTab = new URLSearchParams(window.location.search).get("tab");
      const parsedWindow = parseTab(windowTab);
      if (parsedWindow) return parsedWindow;

      const stored = window.localStorage.getItem(ADMIN_TAB_STORAGE_KEY);
      const parsedStored = parseTab(stored);
      if (parsedStored) return parsedStored;
    } catch {}
  }
  return "overview";
}

function AdminDashboardInner() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [academyTab, setAcademyTab] = useState<AcademyTab>(() =>
    resolveInitialTab(searchParams.get("tab"))
  );
  const [tabSearch, setTabSearch] = useState("");

  // Live badges for pending items
  const [pendingPaymentsCount, setPendingPaymentsCount] = useState<number>(0);
  const [newInquiriesCount, setNewInquiriesCount] = useState<number>(0);
  const [usersCount, setUsersCount] = useState<number>(0);
  const [examFlagsCount, setExamFlagsCount] = useState<number>(0);

  // Sync tab from URL on load / browser back-forward / refresh
  useEffect(() => {
    const fromUrl = parseTab(searchParams.get("tab"));
    if (fromUrl) {
      setAcademyTab(fromUrl);
      try {
        window.localStorage.setItem(ADMIN_TAB_STORAGE_KEY, fromUrl);
      } catch {}
    } else {
      // If refreshed on /admin without ?tab=, keep active section from storage
      try {
        const stored = window.localStorage.getItem(ADMIN_TAB_STORAGE_KEY);
        const parsedStored = parseTab(stored);
        if (parsedStored && parsedStored !== "overview") {
          setAcademyTab(parsedStored);
          const params = new URLSearchParams(searchParams.toString());
          params.set("tab", parsedStored);
          const newUrl = `/admin?${params.toString()}`;
          if (typeof window !== "undefined") {
            window.history.replaceState(null, "", newUrl);
          }
          router.replace(newUrl, { scroll: false });
        }
      } catch {}
    }
  }, [searchParams, router]);

  const goTab = useCallback(
    (id: AcademyTab) => {
      setAcademyTab(id);
      try {
        window.localStorage.setItem(ADMIN_TAB_STORAGE_KEY, id);
      } catch {}
      const params = new URLSearchParams(searchParams.toString());
      params.set("tab", id);
      const newUrl = `/admin?${params.toString()}`;
      if (typeof window !== "undefined") {
        window.history.replaceState(null, "", newUrl);
      }
      router.replace(newUrl, { scroll: false });
    },
    [router, searchParams]
  );

  // Load user session & quick badges
  useEffect(() => {
    supabase.auth.getSession().then(async ({ data: { session } }) => {
      const u = session?.user ?? null;
      if (!u || !isAdminEmail(u.email)) {
        router.replace(u ? "/account" : "/login");
        return;
      }
      await ensureProfile(u);
      setUser(u);
      setLoading(false);

      // Fetch counts for badges
      try {
        const [ordersRes, inqRes, usersRes] = await Promise.all([
          supabase.from("orders").select("id", { count: "exact", head: true }).eq("status", "pending_verification"),
          supabase.from("inquiries").select("id", { count: "exact", head: true }).eq("status", "new"),
          supabase.from("profiles").select("id", { count: "exact", head: true }),
          supabase.from("competition_entries").select("id", { count: "exact", head: true }).eq("mode", "ranked").not("id", "is", null),
        ]);
        if (ordersRes.count != null) setPendingPaymentsCount(ordersRes.count);
        if (inqRes.count != null) setNewInquiriesCount(inqRes.count);
        if (usersRes.count != null) setUsersCount(usersRes.count);
        const { count: flagCount } = await supabase.from("competition_flags").select("id", { count: "exact", head: true });
        if (flagCount != null) setExamFlagsCount(flagCount);
      } catch (e) {
        console.warn("[admin badge counts]", e);
      }
    });
  }, [router]);

  const handleLogout = async () => {
    await supabase.auth.signOut();
    router.push("/login");
  };

  const academyTabs: {
    id: AcademyTab;
    label: string;
    icon: React.ComponentType<{ className?: string }>;
    badge?: number | string;
    badgeColor?: string;
  }[] = useMemo(
    () => [
      { id: "overview", label: "Overview", icon: LayoutDashboard },
      {
        id: "notifications",
        label: "Push Broadcasts",
        icon: Bell,
        badgeColor: "bg-sky-500/20 text-sky-300 border-sky-400/40",
      },
      { id: "competitions", label: "Competitions", icon: Trophy },
      {
        id: "users",
        label: "Scholars & Users",
        icon: Users,
        badge: usersCount > 0 ? usersCount : undefined,
      },
      {
        id: "payments",
        label: "Payments",
        icon: CreditCard,
        badge: pendingPaymentsCount > 0 ? pendingPaymentsCount : undefined,
        badgeColor: "bg-amber-500/20 text-amber-300 border-amber-400/40",
      },
      { id: "grants", label: "Access Grants", icon: KeyRound },
      { id: "content", label: "Course Content", icon: BookOpen },
      { id: "locks", label: "Content Locks", icon: Shield },
      { id: "catalog", label: "Pricing & Catalog", icon: Package },
      { id: "free-resources", label: "Free Resources", icon: Library },
      {
        id: "inquiries",
        label: "Inquiries",
        icon: Inbox,
        badge: newInquiriesCount > 0 ? newInquiriesCount : undefined,
        badgeColor: "bg-cyan-500/20 text-cyan-300 border-cyan-400/40",
      },
      { id: "database", label: "Database & SQL Hub", icon: Database },
      { id: "devices", label: "Registered Devices", icon: Smartphone },
      { id: "exam-integrity", label: "Exam Integrity", icon: Shield, badge: examFlagsCount > 0 ? examFlagsCount : undefined, badgeColor: "bg-rose-500/20 text-rose-200 border-rose-400/40" },
    ],
    [pendingPaymentsCount, newInquiriesCount, usersCount, examFlagsCount]
  );

  const filteredTabs = useMemo(() => {
    if (!tabSearch.trim()) return academyTabs;
    const q = tabSearch.toLowerCase();
    return academyTabs.filter((t) => t.label.toLowerCase().includes(q) || t.id.includes(q));
  }, [academyTabs, tabSearch]);

  if (loading || !user) {
    return (
      <div className="min-h-[70vh] flex flex-col items-center justify-center gap-3" data-wta-spinner="true">
        <BrandLoader size="md" label="Authenticating executive privileges..." />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-wisdom-dark text-white pb-20">
      {/* Top Navigation & Status Bar */}
      <header className="border-b border-white/8 bg-wisdom-card/60 backdrop-blur-md sticky top-0 z-40">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-3.5 flex flex-wrap items-center justify-between gap-3">
          {/* Brand & Identity */}
          <div className="flex items-center gap-3 min-w-0">
            <Link
              href="/account"
              className="inline-flex items-center justify-center w-9 h-9 rounded-xl border border-white/12 bg-white/5 text-wisdom-muted hover:text-white hover:bg-white/10 shrink-0 transition-colors"
              title="Return to Student Account"
            >
              <ArrowLeft className="w-4 h-4" />
            </Link>

            <div className="p-2 rounded-xl bg-gradient-to-br from-amber-500/20 to-orange-600/10 text-amber-300 border border-amber-400/20 shrink-0">
              <GraduationCap className="w-5 h-5" />
            </div>

            <div className="min-w-0">
              <div className="flex items-center gap-2">
                <span className="font-bold text-sm sm:text-base text-white truncate">
                  Wisdom Tower Academy
                </span>
                <span className="hidden sm:inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider bg-emerald-500/15 text-emerald-300 border border-emerald-400/30">
                  <span className="h-1.5 w-1.5 rounded-full bg-emerald-400 animate-pulse" />
                  Admin Console
                </span>
              </div>
              <p className="text-xs text-wisdom-muted truncate hidden sm:block">
                Logged in as <span className="text-amber-200/90 font-medium">{user.email}</span>
              </p>
            </div>
          </div>

          {/* Quick Links & Actions */}
          <div className="flex items-center gap-2">
            <Link
              href="/academy"
              target="_blank"
              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-white/10 bg-white/5 text-xs font-semibold text-wisdom-muted hover:text-white hover:bg-white/10 transition-colors"
              title="View Public Academy"
            >
              <Eye className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">Live Academy</span>
            </Link>

            <a
              href="https://supabase.com/dashboard"
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-white/10 bg-white/5 text-xs font-semibold text-wisdom-muted hover:text-white hover:bg-white/10 transition-colors"
            >
              <Database className="w-3.5 h-3.5 text-cyan-400" />
              <span className="hidden sm:inline">Supabase</span>
              <ExternalLink className="w-3 h-3 opacity-60" />
            </a>

            <button
              type="button"
              onClick={handleLogout}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-rose-500/20 bg-rose-500/10 text-xs font-semibold text-rose-300 hover:bg-rose-500/20 transition-colors"
            >
              <LogOut className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">Logout</span>
            </button>
          </div>
        </div>
      </header>

      {/* Main Workspace */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-6">
        {/* Horizontal Navigation Command Strip */}
        <div className="mb-6 space-y-3">
          <div className="flex items-center justify-between gap-3 flex-wrap">
            {/* Quick tab filter / search */}
            <div className="relative w-full sm:w-64">
              <Search className="w-3.5 h-3.5 text-wisdom-muted absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                value={tabSearch}
                onChange={(e) => setTabSearch(e.target.value)}
                placeholder="Filter admin sections..."
                className="w-full pl-9 pr-3 py-1.5 rounded-xl bg-wisdom-card border border-white/10 text-xs text-white placeholder-wisdom-muted focus:outline-none focus:border-amber-400 transition-colors"
              />
            </div>

            <div className="text-xs text-wisdom-muted flex items-center gap-2">
              <span className="h-2 w-2 rounded-full bg-emerald-400" />
              <span>All 10 modules operational</span>
            </div>
          </div>

          {/* Tab Ribbon (horizontal scrollable on mobile) */}
          <div className="overflow-x-auto pb-1.5 -mx-4 px-4 sm:mx-0 sm:px-0">
            <div className="flex items-center gap-2 min-w-max">
              {filteredTabs.map(({ id, label, icon: Icon, badge, badgeColor }) => {
                const active = academyTab === id;
                return (
                  <button
                    key={id}
                    type="button"
                    onClick={() => goTab(id)}
                    className={`inline-flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs sm:text-sm font-semibold border transition-all ${
                      active
                        ? "border-amber-400/50 bg-amber-500/15 text-amber-200 shadow-sm shadow-amber-500/10 font-bold"
                        : "border-white/10 bg-wisdom-card/60 text-wisdom-muted hover:text-white hover:border-white/20"
                    }`}
                  >
                    <Icon className={`w-4 h-4 shrink-0 ${active ? "text-amber-300" : ""}`} />
                    <span>{label}</span>
                    {badge !== undefined && (
                      <span
                        className={`text-[10px] font-extrabold px-1.5 py-0.5 rounded-md border tabular-nums ${
                          badgeColor || "bg-white/10 text-white/90 border-white/15"
                        }`}
                      >
                        {badge}
                      </span>
                    )}
                  </button>
                );
              })}
            </div>
          </div>
        </div>

        {/* Dynamic Admin Panel Container */}
        <section className="rounded-3xl border border-white/10 bg-wisdom-card/40 p-4 sm:p-6 lg:p-7 shadow-2xl min-h-[60vh] backdrop-blur-sm">
          {academyTab === "overview" && <AnalyticsPanel />}
          {academyTab === "notifications" && <NotificationsPanel />}
          {academyTab === "competitions" && <CompetitionsAdminPanel />}
          {academyTab === "users" && <UsersPanel adminEmail={user.email || undefined} />}
          {academyTab === "payments" && user.email && <PaymentsPanel adminEmail={user.email} />}
          {academyTab === "grants" && user.email && <AccessGrantsPanel adminEmail={user.email} />}
          {academyTab === "content" && <ContentPanel />}
          {academyTab === "locks" && <LocksPanel />}
          {academyTab === "catalog" && <CatalogPanel />}
          {academyTab === "free-resources" && <FreeResourcesPanel />}
          {academyTab === "inquiries" && <InquiriesPanel />}
          {academyTab === "database" && <DatabaseHubPanel />}
          {academyTab === "devices" && <DeviceManagementPanel />}
          {academyTab === "exam-integrity" && <ExamIntegrityPanel />}
        </section>
      </main>
    </div>
  );
}

function AdminFallback() {
  return (
    <div className="min-h-[60vh] flex flex-col items-center justify-center gap-3" data-wta-spinner="true">
      <BrandLoader size="md" label="Loading admin environment..." />
    </div>
  );
}

export default function AdminPage() {
  return (
    <Suspense fallback={<AdminFallback />}>
      <AdminDashboardInner />
    </Suspense>
  );
}
