import { createFileRoute } from "@tanstack/react-router";
import { Bar, BarChart, CartesianGrid, Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { BarChart3, Download } from "lucide-react";
import { PageHeader, StatCard } from "@/components/AppShell";
import { useRows } from "@/components/ResourceTable";
import { Button } from "@/components/ui/button";
import { downloadCsv } from "@/services/crud";
import type { Crop, Expense, Harvest, Irrigation, Budget } from "@/lib/types";

export const Route = createFileRoute("/_authenticated/analytics")({
  head: () => ({ meta: [{ title: "Analytics & Reports — AgroPrecision" }, { name: "description", content: "Yield, cost, water and budget reports." }] }),
  component: Analytics,
});

const COLORS = ["var(--chart-1)", "var(--chart-2)", "var(--chart-3)", "var(--chart-4)", "var(--chart-5)"];

function Analytics() {
  const harvests = useRows<Harvest>("harvests", "harvest_date").data ?? [];
  const crops = useRows<Crop>("crops").data ?? [];
  const exp = useRows<Expense>("expenses", "spent_on").data ?? [];
  const irr = useRows<Irrigation>("irrigation_schedules", "scheduled_at").data ?? [];
  const budgets = useRows<Budget>("budgets").data ?? [];

  const cropName = (id: string | null) => crops.find((c) => c.id === id)?.crop_name ?? "Unassigned";
  const yieldBy = Object.values(harvests.reduce<Record<string, { crop: string; kg: number; revenue: number }>>((m, h) => {
    const k = cropName(h.crop_id); m[k] ??= { crop: k, kg: 0, revenue: 0 }; m[k].kg += Number(h.quantity_kg); m[k].revenue += Number(h.revenue); return m;
  }, {}));
  const costBy = Object.entries(exp.reduce<Record<string, number>>((m, e) => ((m[e.category] = (m[e.category] ?? 0) + Number(e.amount)), m), {})).map(([name, value]) => ({ name, value }));
  const revenue = harvests.reduce((s, h) => s + Number(h.revenue), 0);
  const cost = exp.reduce((s, e) => s + Number(e.amount), 0); // each expense counted once
  const planned = budgets.reduce((s, b) => s + Number(b.planned_amount), 0);
  const water = irr.filter((i) => i.status === "completed").reduce((s, i) => s + Number(i.water_litres), 0);

  return (
    <div className="space-y-6">
      <PageHeader icon={BarChart3} title="Analytics & Reports" subtitle="All figures are calculated from your recorded data.">
        <div className="flex flex-wrap gap-2">
          <Button variant="lime" size="sm" onClick={() => downloadCsv("yield-report.csv", yieldBy)} disabled={!yieldBy.length}><Download /> Yield</Button>
          <Button variant="outline" size="sm" onClick={() => downloadCsv("expense-report.csv", exp)} disabled={!exp.length}><Download /> Expenses</Button>
        </div>
      </PageHeader>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard icon={BarChart3} label="Revenue" value={`₹${revenue.toLocaleString()}`} delay={0} />
        <StatCard icon={BarChart3} label="Total cost" value={`₹${cost.toLocaleString()}`} hint={planned ? `${Math.round((cost / planned) * 100)}% of ₹${planned.toLocaleString()} budget` : undefined} delay={60} />
        <StatCard icon={BarChart3} label="Net" value={`₹${(revenue - cost).toLocaleString()}`} delay={120} />
        <StatCard icon={BarChart3} label="Water used" value={`${(water / 1000).toFixed(1)} kL`} delay={180} />
      </div>
      <div className="grid gap-6 lg:grid-cols-2">
        <div className="animate-rise rounded-2xl border bg-card p-5 shadow-soft">
          <h2 className="font-semibold">Yield by crop (kg)</h2>
          <div className="mt-4 h-72">
            {yieldBy.length ? (
              <ResponsiveContainer><BarChart data={yieldBy}><CartesianGrid stroke="var(--border)" vertical={false} /><XAxis dataKey="crop" fontSize={11} /><YAxis fontSize={11} /><Tooltip /><Bar dataKey="kg" fill="var(--chart-1)" radius={[6, 6, 0, 0]} /></BarChart></ResponsiveContainer>
            ) : <p className="grid h-full place-items-center text-sm text-muted-foreground">No harvests recorded.</p>}
          </div>
        </div>
        <div className="animate-rise rounded-2xl border bg-card p-5 shadow-soft" style={{ animationDelay: "80ms" }}>
          <h2 className="font-semibold">Cost breakdown</h2>
          <div className="mt-4 h-72">
            {costBy.length ? (
              <ResponsiveContainer><PieChart><Pie data={costBy} dataKey="value" nameKey="name" innerRadius={55} outerRadius={95} paddingAngle={2}>{costBy.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}</Pie><Tooltip /><Legend /></PieChart></ResponsiveContainer>
            ) : <p className="grid h-full place-items-center text-sm text-muted-foreground">No expenses recorded.</p>}
          </div>
        </div>
      </div>
    </div>
  );
}
