import { createFileRoute } from "@tanstack/react-router";
import { Package, AlertTriangle, Bot } from "lucide-react";
import { PageHeader } from "@/components/AppShell";
import { ResourceTable, StatusPill, useRows, type ResourceConfig } from "@/components/ResourceTable";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useFarm } from "@/lib/farm-context";
import type { InventoryItem } from "@/lib/types";

export const Route = createFileRoute("/_authenticated/inventory")({
  head: () => ({ meta: [{ title: "Inventory — AgroPrecision" }, { name: "description", content: "Seeds, fertilizers, pesticides and stock movements." }] }),
  component: InventoryPage,
});

const itemCfg: ResourceConfig<InventoryItem> = {
  table: "inventory_items", title: "Stock items", singular: "Item",
  validate: (values) => {
    if (values.expiry_date) {
      const d = new Date(String(values.expiry_date));
      d.setHours(0, 0, 0, 0);
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      if (d < today) {
        return "Expiry date cannot be in the past.";
      }
    }
    return null;
  },
  fields: [
    { key: "name", label: "Item name", required: true },
    { key: "category", label: "Category", type: "select", options: ["seed", "fertilizer", "pesticide", "fuel", "tool", "other"], required: true, default: "fertilizer" },
    { key: "unit", label: "Unit", default: "kg", required: true },
    { key: "reorder_level", label: "Reorder level", type: "number", default: 0 },
    { key: "unit_cost", label: "Unit cost (₹)", type: "number" },
    { key: "supplier", label: "Supplier" },
    { key: "expiry_date", label: "Expiry date", type: "date", minDate: "today" },
  ],
  columns: [
    { key: "name", label: "Item", render: (r) => <span className="font-medium">{r.name}</span> },
    { key: "farm_id", label: "Farm", render: (r) => <FarmName id={r.farm_id} /> },
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

function FarmName({ id }: { id: string }) {
  const { farms } = useFarm();
  const farm = farms.find((f) => String(f.id) === String(id));
  return <span className="text-muted-foreground">{farm?.name ?? "—"}</span>;
}

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
  const outOfStock = low.filter((i) => Number(i.quantity) <= 0);
  const lowOnly = low.filter((i) => Number(i.quantity) > 0);
  const suggestionParts: string[] = [];
  if (outOfStock.length > 0) {
    suggestionParts.push(
      `${outOfStock.map((l) => l.name).join(", ")} ${outOfStock.length === 1 ? "is" : "are"} out of stock — please restock as soon as possible.`
    );
  }
  if (lowOnly.length > 0) {
    suggestionParts.push(
      `${lowOnly.map((l) => l.name).join(", ")} ${lowOnly.length === 1 ? "is" : "are"} running low in stock — please refill as soon as possible.`
    );
  }
  return (
    <div className="space-y-6">
      <PageHeader icon={Package} title="Inventory" subtitle="Stock changes only through movements — quantities can never go negative." />
      {low.length > 0 && (
        <div className="flex items-center gap-3 rounded-xl border border-[#b7d8bd] bg-[#f0f8f1] px-4 py-3 text-sm text-[#123b2a]">
          <div className="grid size-8 shrink-0 place-items-center rounded-lg bg-[#16834a]/15 text-[#16834a]">
            <Bot className="size-4" />
          </div>
          <span>
            {suggestionParts.join(" ")}
          </span>
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
