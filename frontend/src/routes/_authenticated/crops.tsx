import { createFileRoute } from "@tanstack/react-router";
import { Sprout } from "lucide-react";
import { PageHeader } from "@/components/AppShell";
import { ResourceTable, StatusPill, useRows, type ResourceConfig } from "@/components/ResourceTable";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import type { Crop, Harvest } from "@/lib/types";

export const Route = createFileRoute("/_authenticated/crops")({
  head: () => ({ meta: [{ title: "Crop Lifecycle — AgroPrecision" }, { name: "description", content: "Plan crops, track stages, activities and harvests." }] }),
  component: CropsPage,
});

export const STAGES = ["planned", "sown", "vegetative", "flowering", "harvest_ready", "harvested"];

const cropCfg: ResourceConfig<Crop> = {
  table: "crops", title: "Crop plans", singular: "Crop",
  fields: [
    { key: "crop_name", label: "Crop", required: true },
    { key: "variety", label: "Variety" },
    { key: "plot_id", label: "Plot", type: "ref", ref: { table: "plots", label: "name" } },
    { key: "season", label: "Season", default: "Rabi 2026" },
    { key: "stage", label: "Stage", type: "select", options: STAGES, required: true, default: "planned" },
    { key: "sowing_date", label: "Sowing date", type: "date" },
    { key: "expected_harvest", label: "Expected harvest", type: "date" },
  ],
  columns: [
    { key: "crop_name", label: "Crop", render: (r) => <span className="font-medium">{r.crop_name} <span className="text-muted-foreground">{r.variety}</span></span> },
    { key: "plot_id", label: "Plot" },
    { key: "season", label: "Season" },
    { key: "stage", label: "Stage", render: (r) => <StatusPill value={r.stage} tone={r.stage === "harvested" ? "muted" : "good"} /> },
    { key: "expected_harvest", label: "Harvest due" },
  ],
};

const activityCfg: ResourceConfig<Record<string, unknown> & { id: string }> = {
  table: "crop_activities", title: "Activity log", singular: "Activity", writeAccess: "all", orderBy: "activity_date",
  fields: [
    { key: "crop_id", label: "Crop", type: "ref", ref: { table: "crops", label: "crop_name" } },
    { key: "activity", label: "Activity", required: true, type: "select", options: ["Sowing", "Fertilizer application", "Pesticide spray", "Weeding", "Irrigation", "Pest / disease observed", "Other"] },
    { key: "activity_date", label: "Date (blank = today)", type: "date" },
    { key: "notes", label: "Notes", type: "textarea" },
  ],
  columns: [
    { key: "activity_date", label: "Date" },
    { key: "crop_id", label: "Crop" },
    { key: "activity", label: "Activity" },
    { key: "notes", label: "Notes" },
  ],
};

const harvestCfg: ResourceConfig<Harvest> = {
  table: "harvests", title: "Harvest records", singular: "Harvest", orderBy: "harvest_date",
  fields: [
    { key: "crop_id", label: "Crop", type: "ref", ref: { table: "crops", label: "crop_name" } },
    { key: "harvest_date", label: "Date", type: "date", required: true },
    { key: "quantity_kg", label: "Quantity (kg)", type: "number", required: true },
    { key: "revenue", label: "Revenue (₹)", type: "number" },
  ],
  columns: [
    { key: "harvest_date", label: "Date" },
    { key: "crop_id", label: "Crop" },
    { key: "quantity_kg", label: "Qty (kg)", render: (r) => Number(r.quantity_kg).toLocaleString() },
    { key: "revenue", label: "Revenue", render: (r) => `₹${Number(r.revenue).toLocaleString()}` },
  ],
};

function CropsPage() {
  const crops = useRows<Crop>("crops").data ?? [];
  return (
    <div className="space-y-6">
      <PageHeader icon={Sprout} title="Crop Lifecycle" subtitle="From planning to harvest, stage by stage."
        decoration={<Sprout className="absolute bottom-0 right-8 size-28 text-lime/15 animate-grow" />} />
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {crops.filter((c) => c.stage !== "harvested").map((c, i) => {
          const idx = STAGES.indexOf(c.stage);
          return (
            <div key={c.id} className="lift animate-grow rounded-2xl border bg-card p-5 shadow-soft" style={{ animationDelay: `${i * 80}ms` }}>
              <div className="flex items-center justify-between">
                <h3 className="font-semibold">{c.crop_name}</h3>
                <span className="font-tech text-xs text-muted-foreground">{c.season}</span>
              </div>
              <ol className="mt-4 flex items-center gap-1" aria-label={`Stage: ${c.stage}`}>
                {STAGES.map((s, si) => (
                  <li key={s} className="flex-1">
                    <div className={`h-2 rounded-full transition-all duration-700 ${si <= idx ? "bg-primary" : "bg-muted"}`} style={{ transitionDelay: `${si * 90}ms` }} title={s} />
                  </li>
                ))}
              </ol>
              <p className="mt-2 text-sm capitalize text-muted-foreground">{c.stage.replace("_", " ")} · harvest {c.expected_harvest ?? "TBD"}</p>
            </div>
          );
        })}
      </div>
      <Tabs defaultValue="plans">
        <TabsList><TabsTrigger value="plans">Crop plans</TabsTrigger><TabsTrigger value="activities">Activities</TabsTrigger><TabsTrigger value="harvests">Harvests</TabsTrigger></TabsList>
        <TabsContent value="plans"><ResourceTable config={cropCfg} /></TabsContent>
        <TabsContent value="activities"><ResourceTable config={activityCfg} /></TabsContent>
        <TabsContent value="harvests"><ResourceTable config={harvestCfg} /></TabsContent>
      </Tabs>
    </div>
  );
}
