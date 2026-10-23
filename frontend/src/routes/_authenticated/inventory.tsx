import { createFileRoute } from "@tanstack/react-router";
import { Package, AlertTriangle } from "lucide-react";
import { PageHeader } from "@/components/AppShell";
import { ResourceTable, StatusPill, useRows, type ResourceConfig } from "@/components/ResourceTable";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import type { InventoryItem } from "@/lib/types";

export const Route = createFileRoute("/_authenticated/inventory")({
  head: () => ({ meta: [{ title: "Inventory — AgroPrecision" }, { name: "description", content: "Seeds, fertilizers, pesticides and stock movements." }] }),
  component: InventoryPage,
});

const itemCfg: ResourceConfig<InventoryItem> = {
  table: "inventory_items", title: "Stock items", singular: "Item",
  fields: [
    { key: "name", label: "Item name", required: true },
    { key: "category", label: "Category", type: "select", options: ["seed", "fertilizer", "pesticide", "fuel", "tool", "other"], required: true, default: "fertilizer" },
    { key: "unit", label: "Unit", default: "kg", required: true },
    { key: "reorder_level", label: "Reorder level", type: "number", default: 0 },
    { key: "unit_cost", label: "Unit cost (₹)", type: "number" },
    { key: "supplier", label: "Supplier" },
    { key: "expiry_date", label: "Expiry date", type: "date" },
  ],
  columns: [
    { key: "name", label: "Item", render: (r) => <span className="font-medium">{r.name}</span> },
    { key: "category", label: "Category", render: (r) => <StatusPill value={r.category} /> },
    { key: "quantity", label: "In stock", render: (r) => <StockBar item={r} /> },
    { key: "supplier", label: "Supplier" },
    { key: "expiry_date", label: "Expiry", render: (r) => r.expiry_date ? <ExpiryTag d={r.expiry_date} /> : "—" },
  ],
};

const txnCfg: ResourceConfig<Record<string, unknown> & { id: string }> = {
  table: "stock_transactions", title: "Stock movements", singular: "Movement", writeAccess: "all", allowEdit: false, allowDelete: false,
  fields: [
    { key: "item_id", label: "Item", type: "ref", ref: { table: "inventory_items", label: "name" }, required: true },
    { key: "txn_type", label: "Type", type: "select", options: ["in", "out"], required: true, default: "out" },
    { key: "quantity", label: "Quantity", type: "number", required: true },
    { key: "note", label: "Note (e.g. used on plot)" },
  ],
  columns: [
    { key: "created_at", label: "When" },
    { key: "item_id", label: "Item" },
    { key: "txn_type", label: "Type", render: (r) => <StatusPill value={r.txn_type === "in" ? "stock in" : "consumed"} tone={r.txn_type === "in" ? "good" : "water"} /> },
    { key: "quantity", label: "Qty" },
    { key: "note", label: "Note" },
  ],
};

function StockBar({ item }: { item: InventoryItem }) {
  const q = Number(item.quantity), r = Number(item.reorder_level);
  const max = Math.max(q, r * 3, 1);
  const low = q <= r;
  return (
    <div className="w-40">
      <div className="flex justify-between text-xs"><span className={low ? "font-semibold text-destructive" : ""}>{q.toLocaleString()} {item.unit}</span>{low && <span className="flex items-center gap-1 text-destructive"><AlertTriangle className="size-3" />Low</span>}</div>
      <div className="mt-1 h-1.5 overflow-hidden rounded-full bg-muted">
        <div className={`h-full rounded-full transition-[width] duration-1000 ${low ? "bg-destructive" : "bg-primary"}`} style={{ width: `${(q / max) * 100}%` }} />
      </div>
    </div>
  );
}

function ExpiryTag({ d }: { d: string }) {
  const days = Math.round((new Date(d).getTime() - Date.now()) / 86400000);
  return <StatusPill value={days < 0 ? "expired" : `${days}d left`} tone={days < 0 ? "bad" : days < 30 ? "warn" : "muted"} />;
}

function InventoryPage() {
  const items = useRows<InventoryItem>("inventory_items").data ?? [];
  const low = items.filter((i) => Number(i.quantity) <= Number(i.reorder_level));
  return (
    <div className="space-y-6">
      <PageHeader icon={Package} title="Inventory" subtitle="Stock changes only through movements — quantities can never go negative." />
      {low.length > 0 && (
        <div role="alert" className="animate-pulse-once flex items-center gap-3 rounded-xl border border-destructive/30 bg-destructive-soft px-4 py-3 text-sm">
          <AlertTriangle className="size-4 text-destructive" />
          <span><strong>Low stock:</strong> {low.map((l) => l.name).join(", ")}</span>
        </div>
      )}
      <Tabs defaultValue="items">
        <TabsList><TabsTrigger value="items">Items</TabsTrigger><TabsTrigger value="txns">Stock in / out</TabsTrigger></TabsList>
        <TabsContent value="items"><ResourceTable config={itemCfg} /></TabsContent>
        <TabsContent value="txns"><ResourceTable config={txnCfg} /></TabsContent>
      </Tabs>
    </div>
  );
}
