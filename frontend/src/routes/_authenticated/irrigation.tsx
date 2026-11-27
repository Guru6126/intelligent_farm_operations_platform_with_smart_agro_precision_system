import { createFileRoute } from "@tanstack/react-router";
import { useMutation, useQueryClient, useQuery } from "@tanstack/react-query";
import { Droplets, Bot } from "lucide-react";
import { toast } from "sonner";
import { PageHeader, StatCard } from "@/components/AppShell";
import { ResourceTable, StatusPill, useRows, type ResourceConfig } from "@/components/ResourceTable";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { useFarm } from "@/lib/farm-context";
import { updateRow } from "@/services/crud";
import type { Irrigation } from "@/lib/types";

export const Route = createFileRoute("/_authenticated/irrigation")({
  head: () => ({ meta: [{ title: "Irrigation — AgroPrecision" }, { name: "description", content: "Irrigation schedules and water usage." }] }),
  component: IrrigationPage,
});

const tone = (s: string) => ((s === "active" ? "water" : s === "completed" ? "good" : s === "skipped" ? "muted" : "warn")) as "good" | "warn" | "bad" | "water" | "muted";

const cfg: ResourceConfig<Irrigation> = {
  table: "irrigation_schedules", title: "Schedules", singular: "Schedule", orderBy: "scheduled_at",
  validate: (values) => {
    if (values.scheduled_at) {
      const scheduledTime = new Date(String(values.scheduled_at)).getTime();
      // Allow 5 minutes buffer for immediate scheduling
      if (scheduledTime < Date.now() - 5 * 60 * 1000) {
        return "Irrigation schedule start time cannot be in the past.";
      }
    }
    return null;
  },
  fields: [
    { key: "plot_id", label: "Plot", type: "ref", ref: { table: "plots", label: "name" } },
    { key: "scheduled_at", label: "Start", type: "datetime", required: true, minDate: "today" },
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
    { key: "status", label: "Status", render: (r) => <IrrigationStatusSelect item={r} /> },
  ],
};

const STATUS_LABELS: Record<string, string> = {
  upcoming: "Upcoming",
  active: "Active",
  completed: "Completed",
  skipped: "Skipped",
};

function IrrigationStatusSelect({ item }: { item: Irrigation }) {
  const qc = useQueryClient();
  const mut = useMutation({
    mutationFn: (status: string) => updateRow("irrigation_schedules", item.id, { status }),
    onSuccess: () => {
      toast.success("Irrigation status updated");
      qc.invalidateQueries();
    },
    onError: (e: Error) => toast.error(e.message),
  });

  return (
    <Select value={item.status} onValueChange={(v) => mut.mutate(v)} disabled={mut.isPending}>
      <SelectTrigger className="h-8 w-[135px] text-xs">
        <SelectValue>{STATUS_LABELS[item.status] ?? item.status}</SelectValue>
      </SelectTrigger>
      <SelectContent>
        <SelectItem value="upcoming">Upcoming</SelectItem>
        <SelectItem value="active">Active</SelectItem>
        <SelectItem value="completed">Completed</SelectItem>
        <SelectItem value="skipped">Skipped</SelectItem>
      </SelectContent>
    </Select>
  );
}

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

interface ForecastData {
  daily: {
    time: string[];
    temperature_2m_max: number[];
    precipitation_sum: number[];
    wind_speed_10m_max: number[];
  };
}

function buildIrrigationWeatherMessage(alertList: { d: string; rain: number; wind: number; hi: number }[]): string {
  if (alertList.length === 0) return "";
  const parts: string[] = [];
  for (const a of alertList) {
    const dateLabel = new Date(a.d).toLocaleDateString([], { weekday: "long", month: "long", day: "numeric" });
    const reasons: string[] = [];
    if (a.hi > 40) reasons.push(`temperatures are expected to reach ${Math.round(a.hi)}°C`);
    if (a.rain > 20) reasons.push(`heavy rainfall of ${Math.round(a.rain)} mm is forecast`);
    if (a.wind > 40) reasons.push(`strong winds of up to ${Math.round(a.wind)} km/h are expected`);
    parts.push(`On ${dateLabel}, ${reasons.join(" and ")}.`);
  }
  return parts.join(" ") + " Consider rescheduling or skipping upcoming irrigation sessions on these days.";
}

function IrrigationPage() {
  const { farms } = useFarm();
  const rows = useRows<Irrigation>("irrigation_schedules", "scheduled_at").data ?? [];
  const qc = useQueryClient();

  // Use first farm that has coordinates for weather advisory
  const farm = farms.find((f) => f.latitude && f.longitude) ?? farms[0];
  const hasCoords = Boolean(farm?.latitude && farm?.longitude);

  const { data: forecast } = useQuery<ForecastData>({
    queryKey: ["forecast-irrigation", farm?.id, farm?.latitude, farm?.longitude],
    enabled: hasCoords,
    queryFn: async () => {
      const u = `https://api.open-meteo.com/v1/forecast?latitude=${farm!.latitude}&longitude=${farm!.longitude}&daily=temperature_2m_max,precipitation_sum,wind_speed_10m_max&timezone=auto`;
      const r = await fetch(u);
      if (!r.ok) throw new Error("Weather service unavailable.");
      return (await r.json()) as ForecastData;
    },
  });

  const weatherAlerts = forecast
    ? forecast.daily.time
      .map((d, i) => ({
        d,
        rain: forecast.daily.precipitation_sum[i],
        wind: forecast.daily.wind_speed_10m_max[i],
        hi: forecast.daily.temperature_2m_max[i],
      }))
      .filter((x) => x.rain > 20 || x.wind > 40 || x.hi > 40)
    : [];

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
      {weatherAlerts.length > 0 && (
        <div className="flex items-center gap-3 rounded-xl border border-[#b7d8bd] bg-[#f0f8f1] px-4 py-3 text-sm text-[#123b2a]">
          <div className="grid size-8 shrink-0 place-items-center rounded-lg bg-[#16834a]/15 text-[#16834a]">
            <Bot className="size-4" />
          </div>
          <span>
            {buildIrrigationWeatherMessage(weatherAlerts)}
          </span>
        </div>
      )}
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
