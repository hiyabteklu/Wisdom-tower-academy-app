"use client";

import { useCallback, useEffect, useState } from "react";
import {
  listAllCatalogForAdmin,
  upsertCatalogItem,
  deleteCatalogItem,
  seedCatalogFromStatic,
  packageToInput,
  type CatalogRow,
  type CatalogInput,
} from "@/lib/catalog";
import { academyPackages, formatEtb } from "@/data/packages";
import { Package, Plus, Pencil, Trash2, RefreshCw, Save, Database } from "lucide-react";
import {
  AdminCard,
  AdminEmpty,
  AdminField,
  AdminInput,
  AdminPageHeader,
  AdminPill,
  AdminSelect,
  AdminSheet,
  AdminSkeleton,
  AdminTextarea,
  AdminToast,
} from "./ui";

/**
 * Admin Package catalog tab.
 *
 * 2026-10-16 (Admin UI V2, slice U8 part 3): LAYOUT ONLY. Kit page header, cards, fields and pills. The editor
 * (New package / Edit package) is a kit bottom sheet with the Save pill in its footer; a save error is shown at
 * the top of the sheet (the page behind it is covered). The browser confirm before Delete is a kit bottom sheet
 * (same sentence). The success message is the kit slim toast. Data calls (listAllCatalogForAdmin,
 * upsertCatalogItem, deleteCatalogItem, seedCatalogFromStatic), the default values and every text are unchanged.
 */

const EMPTY: CatalogInput = {
  id: "",
  name: "",
  shortName: "",
  description: "",
  priceEtb: 250,
  href: "/packages",
  image: "",
  includes: [""],
  enrolledLabel: "",
  group: "custom",
  active: true,
  sortOrder: 200,
};

function rowToInput(row: CatalogRow): CatalogInput {
  return {
    id: row.id,
    name: row.name,
    shortName: row.short_name,
    description: row.description,
    priceEtb: Number(row.price_etb),
    href: row.href,
    image: row.image,
    includes: row.includes?.length ? row.includes : [""],
    enrolledLabel: row.enrolled_label,
    group: row.group_key,
    active: row.active,
    sortOrder: row.sort_order,
  };
}

export default function CatalogPanel() {
  const [rows, setRows] = useState<CatalogRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState("");
  const [error, setError] = useState("");
  const [editing, setEditing] = useState<CatalogInput | null>(null);
  const [isNew, setIsNew] = useState(false);
  const [saving, setSaving] = useState(false);
  const [busyId, setBusyId] = useState("");
  const [pendingDelete, setPendingDelete] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    const res = await listAllCatalogForAdmin();
    if (res.error) {
      setError(
        res.error.includes("relation") || res.error.includes("does not exist")
          ? "Table missing: run docs/catalog-setup.sql in Supabase SQL Editor."
          : res.error
      );
      setRows([]);
    } else {
      setRows(res.rows);
    }
    setLoading(false);
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  function openNew() {
    setError("");
    setIsNew(true);
    setEditing({ ...EMPTY, id: `custom-${Date.now().toString(36)}` });
  }

  function openEdit(row: CatalogRow) {
    setError("");
    setIsNew(false);
    setEditing(rowToInput(row));
  }

  function setField<K extends keyof CatalogInput>(key: K, value: CatalogInput[K]) {
    setEditing((prev) => (prev ? { ...prev, [key]: value } : prev));
  }

  async function save() {
    if (!editing) return;
    setSaving(true);
    const res = await upsertCatalogItem({
      ...editing,
      includes: editing.includes.map((s) => s.trim()).filter(Boolean),
    });
    setSaving(false);
    if (!res.ok) {
      setError(res.error || "Save failed");
      return;
    }
    setToast(isNew ? "Package created" : "Package updated");
    setEditing(null);
    await load();
  }

  async function remove(id: string) {
    setBusyId(id);
    const res = await deleteCatalogItem(id);
    setBusyId("");
    if (!res.ok) {
      setError(res.error || "Delete failed");
      return;
    }
    setToast("Deleted");
    await load();
  }

  async function confirmDelete() {
    const id = pendingDelete;
    setPendingDelete(null);
    if (id) await remove(id);
  }

  async function seed() {
    setBusyId("seed");
    const res = await seedCatalogFromStatic();
    setBusyId("");
    if (!res.ok) {
      setError(res.error || "Seed failed");
      return;
    }
    setToast(res.error || `Seeded ${res.count} packages from static list`);
    await load();
  }

  return (
    <div className="min-w-0 space-y-4">
      <AdminPageHeader
        icon={Package}
        title="Package catalog"
        subtitle="Prices and About text shown on /packages and offer banners. Default new price: 250 ETB."
      />

      <div className="grid grid-cols-2 gap-2">
        <AdminPill icon={RefreshCw} onClick={() => void load()}>
          {loading ? "Refreshing…" : "Refresh"}
        </AdminPill>
        <AdminPill icon={Database} onClick={() => void seed()} disabled={busyId === "seed"}>
          {busyId === "seed" ? "Seeding…" : "Seed from static"}
        </AdminPill>
        <AdminPill tone="primary" icon={Plus} onClick={openNew} className="col-span-2">
          Add package
        </AdminPill>
      </div>

      {error && !editing && (
        <AdminCard tone="red">
          <p className="break-words text-sm text-rose-200">{error}</p>
        </AdminCard>
      )}

      {loading ? (
        <AdminSkeleton rows={3} />
      ) : rows.length === 0 ? (
        <AdminEmpty icon={Package} title="No catalog rows yet. Seed from static packages or add one." />
      ) : (
        <div className="space-y-2">
          {rows.map((row) => (
            <AdminCard key={row.id} className="space-y-3">
              <div className="min-w-0">
                <p className="break-words text-sm font-semibold text-white">{row.name}</p>
                <p className="break-all text-xs text-wisdom-muted">{row.id}</p>
                <p className="mt-1 text-sm font-bold text-amber-300">{formatEtb(Number(row.price_etb))}</p>
                {row.description && (
                  <p className="mt-1 line-clamp-3 break-words text-xs text-wisdom-muted">{row.description}</p>
                )}
              </div>
              <div className="grid grid-cols-2 gap-2">
                <AdminPill tone="gold" icon={Pencil} onClick={() => openEdit(row)}>
                  Edit
                </AdminPill>
                <AdminPill
                  tone="danger"
                  icon={Trash2}
                  onClick={() => setPendingDelete(row.id)}
                  disabled={busyId === row.id}
                >
                  Delete
                </AdminPill>
              </div>
            </AdminCard>
          ))}
        </div>
      )}

      <p className="text-xs text-wisdom-muted">
        Static fallback list has {academyPackages.length} packages. DB rows override prices when
        present.
      </p>

      {/* Editor sheet */}
      <AdminSheet
        open={editing !== null}
        onClose={() => setEditing(null)}
        title={isNew ? "New package" : "Edit package"}
        footer={
          <AdminPill fullWidth tone="primary" icon={Save} onClick={() => void save()} disabled={saving}>
            {saving ? "Saving…" : "Save package"}
          </AdminPill>
        }
      >
        {editing ? (
          <div className="space-y-3">
            {error && (
              <AdminCard tone="red">
                <p className="break-words text-sm text-rose-200">{error}</p>
              </AdminCard>
            )}

            <div className="grid grid-cols-2 gap-3">
              <AdminField label="ID">
                <AdminInput
                  value={editing.id}
                  onChange={(e) => setField("id", e.target.value)}
                  disabled={!isNew}
                />
              </AdminField>
              <AdminField label="Price (ETB)">
                <AdminInput
                  type="number"
                  value={editing.priceEtb}
                  onChange={(e) => setField("priceEtb", Number(e.target.value) || 0)}
                />
              </AdminField>
            </div>

            <AdminField label="Name">
              <AdminInput value={editing.name} onChange={(e) => setField("name", e.target.value)} />
            </AdminField>

            <div className="grid grid-cols-2 gap-3">
              <AdminField label="Short name">
                <AdminInput
                  value={editing.shortName}
                  onChange={(e) => setField("shortName", e.target.value)}
                />
              </AdminField>
              <AdminField label="Group">
                <AdminSelect
                  value={editing.group}
                  onChange={(e) => setField("group", e.target.value as CatalogInput["group"])}
                >
                  <option value="grades">grades</option>
                  <option value="branch">branch</option>
                  <option value="special">special</option>
                  <option value="custom">custom</option>
                </AdminSelect>
              </AdminField>
            </div>

            <AdminField label="About this package (shown to buyers)">
              <AdminTextarea
                value={editing.description}
                onChange={(e) => setField("description", e.target.value)}
                rows={6}
                placeholder="What students get, who it is for, and what is included..."
              />
            </AdminField>

            <AdminField label="Preview href">
              <AdminInput
                value={editing.href}
                onChange={(e) => setField("href", e.target.value)}
                placeholder="/academy/freshman"
              />
            </AdminField>

            <AdminField label="Image URL">
              <AdminInput value={editing.image} onChange={(e) => setField("image", e.target.value)} />
            </AdminField>

            <AdminField label="Includes (one per line)">
              <AdminTextarea
                value={editing.includes.join("\n")}
                onChange={(e) => setField("includes", e.target.value.split("\n"))}
                rows={4}
              />
            </AdminField>

            <div className="grid grid-cols-2 gap-3">
              <AdminField label="Enrolled label">
                <AdminInput
                  value={editing.enrolledLabel}
                  onChange={(e) => setField("enrolledLabel", e.target.value)}
                />
              </AdminField>
              <AdminField label="Sort order">
                <AdminInput
                  type="number"
                  value={editing.sortOrder}
                  onChange={(e) => setField("sortOrder", Number(e.target.value) || 0)}
                />
              </AdminField>
            </div>

            <label className="flex min-h-[44px] cursor-pointer items-center gap-3 text-sm text-white">
              <input
                type="checkbox"
                className="h-5 w-5 shrink-0 accent-amber-400"
                checked={editing.active}
                onChange={(e) => setField("active", e.target.checked)}
              />
              Active (sellable)
            </label>
          </div>
        ) : null}
      </AdminSheet>

      {/* Confirm before deleting */}
      <AdminSheet
        open={pendingDelete !== null}
        onClose={() => setPendingDelete(null)}
        title="Delete package"
        footer={
          <div className="flex gap-2">
            <AdminPill fullWidth onClick={() => setPendingDelete(null)}>
              Back
            </AdminPill>
            <AdminPill fullWidth tone="danger" icon={Trash2} onClick={() => void confirmDelete()}>
              Delete
            </AdminPill>
          </div>
        }
      >
        <p className="break-words text-sm text-wisdom-muted">Delete package {pendingDelete}?</p>
      </AdminSheet>

      <AdminToast message={toast || null} tone="green" onClose={() => setToast("")} durationMs={3000} />
    </div>
  );
}
