import { createFileRoute } from "@tanstack/react-router";
import { Wrench } from "lucide-react";
import { PageHeader } from "@/components/AppShell";
import { ResourceTable, StatusPill, useRows, type ResourceConfig } from "@/components/ResourceTable";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import type { Equipment } from "@/lib/types";

export const Route = createFileRoute("/_authenticated/equipment")({
  head: () => ({ meta: [{ title: "Equipment — AgroPrecision" }, { name: "description", content: "Machinery, bookings and maintenance." }] }),
  component: EquipmentPage,
});

const st = (s: string) => ((s === "available" ? "good" : s === "in_use" ? "water" : s === "maintenance" ? "warn" : "bad")) as "good" | "warn" | "bad" | "water" | "muted";
type R = Record<string, unknown> & { id: string };

const eqCfg: ResourceConfig<Equipment> = {
  table: "equipment", title: "Machinery", singular: "Equipment",
  fields: [
    { key: "name", label: "Name", required: true },
    { key: "type", label: "Type", type: "select", options: ["tractor", "sprayer", "harvester", "pump", "implement", "other"] },
    { key: "status", label: "Status", type: "select", options: ["available", "in_use", "maintenance", "broken"], required: true, default: "available" },
    { key: "next_service", label: "Next service", type: "date" },
  ],
  columns: [
    { key: "name", label: "Name", render: (r) => <span className="font-medium">{r.name}</span> },
    { key: "type", label: "Type" },
    { key: "status", label: "Status", render: (r) => <StatusPill value={r.status} tone={st(r.status)} /> },
    { key: "next_service", label: "Next service" },
  ],
};
const bookCfg: ResourceConfig<R> = {
  table: "equipment_bookings", title: "Bookings", singular: "Booking", writeAccess: "all", orderBy: "starts_at",
  fields: [
    { key: "equipment_id", label: "Equipment", type: "ref", ref: { table: "equipment", label: "name" }, required: true },
    { key: "starts_at", label: "From", type: "datetime", required: true },
    { key: "ends_at", label: "To", type: "datetime", required: true },
    { key: "purpose", label: "Purpose" },
    { key: "status", label: "Status", type: "select", options: ["pending", "approved", "cancelled"], default: "pending", required: true },
  ],
  columns: [
    { key: "equipment_id", label: "Equipment" },
    { key: "starts_at", label: "From" },
    { key: "ends_at", label: "To" },
    { key: "purpose", label: "Purpose" },
    { key: "status", label: "Status", render: (r) => <StatusPill value={String(r.status)} tone={r.status === "approved" ? "good" : r.status === "pending" ? "warn" : "muted"} /> },
  ],
};
const mCfg: ResourceConfig<R> = {
  table: "maintenance_records", title: "Maintenance & breakdowns", singular: "Record", orderBy: "service_date",
  fields: [
    { key: "equipment_id", label: "Equipment", type: "ref", ref: { table: "equipment", label: "name" }, required: true },
    { key: "service_date", label: "Date", type: "date", required: true },
    { key: "description", label: "Work done", type: "textarea" },
    { key: "cost", label: "Cost (₹)", type: "number" },
  ],
  columns: [
    { key: "service_date", label: "Date" },
    { key: "equipment_id", label: "Equipment" },
    { key: "description", label: "Work done" },
    { key: "cost", label: "Cost", render: (r) => `₹${Number(r.cost ?? 0).toLocaleString()}` },
  ],
};

function EquipmentPage() {
  const eq = useRows<Equipment>("equipment").data ?? [];
  return (
    <div className="space-y-6">
      <PageHeader icon={Wrench} title="Equipment & Machinery" subtitle="Overlapping bookings are blocked automatically." />
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {eq.map((e, i) => {
          const due = e.next_service ? Math.round((new Date(e.next_service).getTime() - Date.now()) / 86400000) : null;
          return (
            <div key={e.id} className="lift animate-slide-in rounded-2xl border bg-card p-5 shadow-soft" style={{ animationDelay: `${i * 60}ms` }}>
              <div className="flex items-center justify-between"><h3 className="font-semibold">{e.name}</h3><StatusPill value={e.status} tone={st(e.status)} /></div>
              <p className={`mt-2 text-sm ${due !== null && due <= 7 ? "font-medium text-foreground" : "text-muted-foreground"}`}>
                {due === null ? "No service date" : due < 0 ? `Service overdue by ${-due}d` : `Service in ${due}d`}
              </p>
            </div>
          );
        })}
      </div>
      <Tabs defaultValue="eq">
        <TabsList><TabsTrigger value="eq">Machinery</TabsTrigger><TabsTrigger value="book">Bookings</TabsTrigger><TabsTrigger value="m">Maintenance</TabsTrigger></TabsList>
        <TabsContent value="eq"><ResourceTable config={eqCfg} /></TabsContent>
        <TabsContent value="book"><ResourceTable config={bookCfg} /></TabsContent>
        <TabsContent value="m"><ResourceTable config={mCfg} /></TabsContent>
      </Tabs>
    </div>
  );
}
