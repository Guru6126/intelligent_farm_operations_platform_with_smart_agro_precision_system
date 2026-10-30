import { useMemo } from "react";
import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Area, AreaChart, Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { LayoutDashboard, Tractor, Sprout, Package, Droplets, ListChecks, Wallet, Database } from "lucide-react";
import { toast } from "sonner";
import { PageHeader, StatCard } from "@/components/AppShell";
import { useRows, StatusPill } from "@/components/ResourceTable";
import { Button } from "@/components/ui/button";
import { useFarm } from "@/lib/farm-context";
import { canManage } from "@/lib/auth";
import { rpc } from "@/services/crud";
import { useCountUp } from "@/hooks/use-count-up";
import type { Crop, Expense, InventoryItem, Irrigation, Task, Weather, Plot } from "@/lib/types";

export const Route = createFileRoute("/_authenticated/dashboard")({
  head: () => ({ meta: [{ title: "Dashboard — AgroPrecision" }, { name: "description", content: "Farm operations overview." }] }),
  component: Dashboard,
});

function Num({ v, prefix = "" }: { v: number; prefix?: string }) {
  const n = useCountUp(v);
  return <>{prefix}{Math.round(n).toLocaleString()}</>;
}

function Dashboard() {
  const { farms, session, currentFarm } = useFarm();
  const qc = useQueryClient();
  const plots = useRows<Plot>("plots").data ?? [];
  const crops = useRows<Crop>("crops").data ?? [];
  const inv = useRows<InventoryItem>("inventory_items").data ?? [];
  const irr = useRows<Irrigation>("irrigation_schedules", "scheduled_at").data ?? [];
  const tasks = useRows<Task>("tasks").data ?? [];
  const exp = useRows<Expense>("expenses", "spent_on").data ?? [];
  const weather = useRows<Weather>("weather_records", "recorded_on").data ?? [];

  const seed = useMutation({
    mutationFn: () => rpc("seed_demo_data"),
    onSuccess: () => { toast.success("Demo farm loaded"); qc.invalidateQueries(); },
    onError: (e: Error) => toast.error(e.message),
  });

  const lowStock = inv.filter((i) => Number(i.quantity) <= Number(i.reorder_level));
  const activeCrops = crops.filter((c) => c.stage !== "harvested");
  const openTasks = tasks.filter((t) => t.status !== "done");
  const upcoming = irr.filter((i) => i.status === "upcoming" || i.status === "active").sort((a, b) => a.scheduled_at.localeCompare(b.scheduled_at));
  const totalSpend = exp.reduce((s, e) => s + Number(e.amount), 0);
  const uniquePlots = useMemo(() => {
    const map = new Map<string, Plot>();
    for (const p of plots) {
      const key = `${p.farm_id}:${String(p.name ?? "").trim().toLowerCase()}`;
      const existing = map.get(key);
      if (!existing || (p.boundary_geojson && !existing.boundary_geojson)) {
        map.set(key, p);
      }
    }
    return Array.from(map.values());
  }, [plots]);
  const acres = uniquePlots.reduce((s: number, p: Plot) => s + Number(p.area_acres ?? 0), 0);

  const byCat = Object.entries(exp.reduce<Record<string, number>>((m, e) => ((m[e.category] = (m[e.category] ?? 0) + Number(e.amount)), m), {}))
    .map(([category, amount]) => ({ category, amount }));
  const wx = [...weather].sort((a, b) => a.recorded_on.localeCompare(b.recorded_on)).slice(-14)
    .map((w) => ({ day: w.recorded_on.slice(5), temp: Number(w.temperature_c), rain: Number(w.rainfall_mm) }));

  return (
    <div className="space-y-6">
      <PageHeader icon={LayoutDashboard} title={`Hello, ${session.fullName?.split(" ")[0] ?? "there"}`}
        subtitle={currentFarm ? `Overview for ${currentFarm.name}` : `Overview across ${farms.length} farm${farms.length === 1 ? "" : "s"}`} />

      {farms.length === 0 && (
        <div className="animate-rise flex flex-wrap items-center gap-4 rounded-2xl border border-dashed border-primary/40 bg-secondary/60 p-6">
          <Database className="size-8 text-primary" />
          <div className="flex-1">
            <h2 className="font-semibold">No farms yet</h2>
            <p className="text-sm text-muted-foreground">
              {canManage(session.roles) ? "Create your first farm, or load a clearly labelled demo farm with sample records." : "Ask a farmer to add you to their farm using your email."}
            </p>
          </div>
          {canManage(session.roles) && (
            <div className="flex gap-2">
              <Button asChild variant="outline"><Link to="/farms">Create farm</Link></Button>
              <Button onClick={() => seed.mutate()} loading={seed.isPending}>Load demo data</Button>
            </div>
          )}
        </div>
      )}

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard icon={Tractor} label="Plots" value={<Num v={uniquePlots.length} />} hint={`${acres.toFixed(1)} acres mapped`} delay={0} />
        <StatCard icon={Sprout} label="Active crops" value={<Num v={activeCrops.length} />} hint={`${crops.length} total plans`} delay={60} />
        <StatCard icon={Package} label="Low stock" value={<Num v={lowStock.length} />} hint={lowStock.map((l) => l.name).join(", ") || "All items healthy"} delay={120} tone={lowStock.length ? "warn" : undefined} />
        <StatCard icon={Wallet} label="Total spend" value={<Num v={totalSpend} prefix="₹" />} hint={`${exp.length} expense records`} delay={180} />
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="animate-rise rounded-2xl border bg-card p-5 shadow-soft lg:col-span-2" style={{ animationDelay: "200ms" }}>
          <h2 className="font-semibold">Weather trend (recorded)</h2>
          <div className="mt-4 h-64">
            {wx.length ? (
              <ResponsiveContainer>
                <AreaChart data={wx}>
                  <defs><linearGradient id="t" x1="0" x2="0" y1="0" y2="1"><stop offset="0%" stopColor="var(--chart-1)" stopOpacity={0.4} /><stop offset="100%" stopColor="var(--chart-1)" stopOpacity={0} /></linearGradient></defs>
                  <CartesianGrid stroke="var(--border)" vertical={false} />
                  <XAxis dataKey="day" fontSize={11} stroke="var(--muted-foreground)" />
                  <YAxis fontSize={11} stroke="var(--muted-foreground)" />
                  <Tooltip />
                  <Area type="monotone" dataKey="temp" name="Temp °C" stroke="var(--chart-1)" fill="url(#t)" strokeWidth={2} />
                  <Area type="monotone" dataKey="rain" name="Rain mm" stroke="var(--chart-4)" fill="transparent" strokeWidth={2} />
                </AreaChart>
              </ResponsiveContainer>
            ) : <Empty text="No weather records yet." />}
          </div>
        </div>
        <div className="animate-rise rounded-2xl border bg-card p-5 shadow-soft" style={{ animationDelay: "260ms" }}>
          <h2 className="flex items-center gap-2 font-semibold"><Droplets className="size-4 text-water" /> Next irrigation</h2>
          <ul className="mt-4 space-y-3">
            {upcoming.slice(0, 5).map((i) => (
              <li key={i.id} className="flex items-center justify-between gap-2 text-sm">
                <span>{new Date(i.scheduled_at).toLocaleString([], { dateStyle: "medium", timeStyle: "short" })}</span>
                <StatusPill value={i.status} tone={i.status === "active" ? "water" : "muted"} />
              </li>
            ))}
            {!upcoming.length && <Empty text="Nothing scheduled." />}
          </ul>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <div className="animate-rise rounded-2xl border bg-card p-5 shadow-soft" style={{ animationDelay: "300ms" }}>
          <h2 className="font-semibold">Spend by category</h2>
          <div className="mt-4 h-60">
            {byCat.length ? (
              <ResponsiveContainer>
                <BarChart data={byCat}>
                  <CartesianGrid stroke="var(--border)" vertical={false} />
                  <XAxis dataKey="category" fontSize={11} stroke="var(--muted-foreground)" />
                  <YAxis fontSize={11} stroke="var(--muted-foreground)" />
                  <Tooltip />
                  <Bar dataKey="amount" fill="var(--chart-1)" radius={[6, 6, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            ) : <Empty text="No expenses yet." />}
          </div>
        </div>
        <div className="animate-rise rounded-2xl border bg-card p-5 shadow-soft" style={{ animationDelay: "340ms" }}>
          <h2 className="flex items-center gap-2 font-semibold"><ListChecks className="size-4 text-primary" /> Open tasks</h2>
          <ul className="mt-4 divide-y">
            {openTasks.slice(0, 6).map((t) => (
              <li key={t.id} className="flex items-center justify-between py-2.5 text-sm">
                <span>{t.title}</span>
                <span className="flex gap-2"><StatusPill value={t.priority} tone={t.priority === "high" ? "bad" : "muted"} /><StatusPill value={t.status} tone="good" /></span>
              </li>
            ))}
            {!openTasks.length && <Empty text="No open tasks." />}
          </ul>
        </div>
      </div>
    </div>
  );
}

function Empty({ text }: { text: string }) {
  return <div className="grid h-full place-items-center py-6 text-sm text-muted-foreground">{text}</div>;
}
