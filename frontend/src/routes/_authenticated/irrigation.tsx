import { createFileRoute } from "@tanstack/react-router";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Droplets } from "lucide-react";
import { toast } from "sonner";
import { PageHeader, StatCard } from "@/components/AppShell";
import { ResourceTable, StatusPill, useRows, type ResourceConfig } from "@/components/ResourceTable";
import { Button } from "@/components/ui/button";
import { updateRow } from "@/services/crud";
import type { Irrigation } from "@/lib/types";

export const Route = createFileRoute("/_authenticated/irrigation")({
  head: () => ({ meta: [{ title: "Irrigation — AgroPrecision" }, { name: "description", content: "Irrigation schedules and water usage." }] }),
  component: IrrigationPage,
});

const tone = (s: string) => ((s === "active" ? "water" : s === "completed" ? "good" : s === "skipped" ? "muted" : "warn")) as "good" | "warn" | "bad" | "water" | "muted";

const cfg: ResourceConfig<Irrigation> = {
  table: "irrigation_schedules", title: "Schedules", singular: "Schedule", orderBy: "scheduled_at",
  fields: [
    { key: "plot_id", label: "Plot", type: "ref", ref: { table: "plots", label: "name" } },
    { key: "scheduled_at", label: "Start", type: "datetime", required: true },
    { key: "duration_min", label: "Duration (min)", type: "number", required: true, default: 60 },
    { key: "water_litres", label: "Planned water (L)", type: "number" },
    { key: "method", label: "Method", type: "select", options: ["drip", "sprinkler", "flood", "furrow"], default: "drip" },
    { key: "water_source", label: "Water source" },
    { key: "status", label: "Status", type: "select", options: ["upcoming", "active", "completed", "skipped"], required: true, default: "upcoming" },
  ],
  columns: [
    { key: "scheduled_at", label: "Start" },
    { key: "plot_id", label: "Plot" },
    { key: "method", label: "Method" },
    { key: "water_litres", label: "Water (L)", render: (r) => Number(r.water_litres).toLocaleString() },
    { key: "status", label: "Status", render: (r) => <StatusPill value={r.status} tone={tone(r.status)} /> },
  ],
};

function Drops() {
  return (
    <>
      {[12, 28, 46, 64, 82].map((l, i) => (
        <span key={l} className="absolute top-3" style={{ left: `${l}%` }}>
          <span className="block size-2 rounded-full rounded-tl-none rotate-45 bg-water/70 animate-drop" style={{ animationDelay: `${i * 0.35}s` }} />
          <span className="absolute left-1/2 top-9 block size-4 -translate-x-1/2 rounded-full border border-water/60 animate-ripple" style={{ animationDelay: `${i * 0.35 + 1.2}s` }} />
        </span>
      ))}
    </>
  );
}

function IrrigationPage() {
  const rows = useRows<Irrigation>("irrigation_schedules", "scheduled_at").data ?? [];
  const qc = useQueryClient();
  const active = rows.filter((r) => r.status === "active");
  const used = rows.filter((r) => r.status === "completed").reduce((s, r) => s + Number(r.water_litres), 0);
  const planned = rows.filter((r) => r.status === "upcoming").reduce((s, r) => s + Number(r.water_litres), 0);
  const complete = useMutation({
    mutationFn: (id: string) => updateRow("irrigation_schedules", id, { status: "completed" }),
    onSuccess: () => { toast.success("Marked completed"); qc.invalidateQueries(); },
    onError: (e: Error) => toast.error(e.message),
  });
  return (
    <div className="space-y-6">
      <PageHeader icon={Droplets} title="Irrigation" subtitle="Water indicators are visual only — no equipment is controlled from here." decoration={<Drops />} />
      <div className="grid gap-4 sm:grid-cols-3">
        <StatCard icon={Droplets} label="Active now" value={active.length} delay={0} />
        <StatCard icon={Droplets} label="Water used (L)" value={used.toLocaleString()} hint="Completed schedules" delay={60} />
        <StatCard icon={Droplets} label="Planned (L)" value={planned.toLocaleString()} hint="Upcoming schedules" delay={120} />
      </div>
      {active.map((a) => (
        <div key={a.id} className="animate-rise rounded-2xl border border-water/40 bg-card p-5 shadow-soft">
          <div className="flex flex-wrap items-center gap-3">
            <StatusPill value="active" tone="water" />
            <span className="text-sm">{a.method} · {a.duration_min} min · started {new Date(a.scheduled_at).toLocaleTimeString([], { timeStyle: "short" })}</span>
            <Button size="sm" variant="secondary" className="ml-auto" onClick={() => complete.mutate(a.id)} loading={complete.isPending}>Mark completed</Button>
          </div>
          <div className="water-flow mt-4 h-1 rounded-full opacity-70" aria-hidden />
        </div>
      ))}
      <ResourceTable config={cfg} />
    </div>
  );
}
