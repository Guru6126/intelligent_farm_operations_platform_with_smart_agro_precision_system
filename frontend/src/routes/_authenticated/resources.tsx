import { createFileRoute } from "@tanstack/react-router";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Users } from "lucide-react";
import { toast } from "sonner";
import { PageHeader } from "@/components/AppShell";
import { ResourceTable, StatusPill, useRows, type ResourceConfig } from "@/components/ResourceTable";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { updateRow } from "@/services/crud";
import { useFarm } from "@/lib/farm-context";
import type { Budget, Expense, Task } from "@/lib/types";

export const Route = createFileRoute("/_authenticated/resources")({
  head: () => ({ meta: [{ title: "Resources & Tasks — AgroPrecision" }, { name: "description", content: "Workers, tasks, budgets and expenses." }] }),
  component: ResourcesPage,
});
type R = Record<string, unknown> & { id: string };

const workerCfg: ResourceConfig<R> = {
  table: "workers", title: "Workers", singular: "Worker",
  fields: [
    { key: "name", label: "Name", required: true },
    { key: "phone", label: "Phone" },
    { key: "skill", label: "Skill" },
    { key: "daily_wage", label: "Daily wage (₹)", type: "number" },
  ],
  columns: [{ key: "name", label: "Name" }, { key: "phone", label: "Phone" }, { key: "skill", label: "Skill" }, { key: "daily_wage", label: "Wage/day", render: (r) => `₹${Number(r.daily_wage ?? 0)}` }],
};
const taskCfg: ResourceConfig<Task> = {
  table: "tasks", title: "All tasks", singular: "Task",
  fields: [
    { key: "title", label: "Title", required: true },
    { key: "description", label: "Description", type: "textarea" },
    { key: "worker_id", label: "Worker", type: "ref", ref: { table: "workers", label: "name" } },
    { key: "due_date", label: "Due date", type: "date" },
    { key: "priority", label: "Priority", type: "select", options: ["low", "medium", "high"], required: true, default: "medium" },
    { key: "status", label: "Status", type: "select", options: ["todo", "in_progress", "done"], required: true, default: "todo" },
  ],
  columns: [
    { key: "title", label: "Task" }, { key: "worker_id", label: "Worker" }, { key: "due_date", label: "Due" },
    { key: "priority", label: "Priority", render: (r) => <StatusPill value={r.priority} tone={r.priority === "high" ? "bad" : "muted"} /> },
    { key: "status", label: "Status", render: (r) => <StatusPill value={r.status} tone="good" /> },
  ],
};
const budgetCfg: ResourceConfig<Budget> = {
  table: "budgets", title: "Seasonal budgets", singular: "Budget line",
  fields: [
    { key: "season", label: "Season", required: true, default: "Rabi 2026" },
    { key: "category", label: "Category", type: "select", options: ["Seed", "Fertilizer", "Pesticide", "Labour", "Irrigation", "Equipment", "Other"], required: true },
    { key: "planned_amount", label: "Planned (₹)", type: "number", required: true },
  ],
  columns: [{ key: "season", label: "Season" }, { key: "category", label: "Category" }, { key: "planned_amount", label: "Planned", render: (r) => `₹${Number(r.planned_amount).toLocaleString()}` }],
};
const expenseCfg: ResourceConfig<Expense> = {
  table: "expenses", title: "Expenses", singular: "Expense", orderBy: "spent_on",
  fields: [
    { key: "season", label: "Season", default: "Rabi 2026" },
    { key: "category", label: "Category", type: "select", options: ["Seed", "Fertilizer", "Pesticide", "Labour", "Irrigation", "Equipment", "Other"], required: true },
    { key: "amount", label: "Amount (₹)", type: "number", required: true },
    { key: "spent_on", label: "Date", type: "date", required: true },
    { key: "description", label: "Description" },
  ],
  columns: [{ key: "spent_on", label: "Date" }, { key: "category", label: "Category" }, { key: "description", label: "Description" }, { key: "amount", label: "Amount", render: (r) => `₹${Number(r.amount).toLocaleString()}` }],
};

const COLS = [["todo", "To do"], ["in_progress", "In progress"], ["done", "Done"]] as const;

function ResourcesPage() {
  const { session } = useFarm();
  const tasks = useRows<Task>("tasks").data ?? [];
  const budgets = useRows<Budget>("budgets").data ?? [];
  const expenses = useRows<Expense>("expenses").data ?? [];
  const qc = useQueryClient();
  const move = useMutation({
    mutationFn: ({ id, status }: { id: string; status: string }) => updateRow("tasks", id, { status }),
    onSuccess: () => qc.invalidateQueries(),
    onError: (e: Error) => toast.error(e.message),
  });

  const cats = [...new Set(budgets.map((b) => b.category))];
  const recon = cats.map((c) => {
    const planned = budgets.filter((b) => b.category === c).reduce((s, b) => s + Number(b.planned_amount), 0);
    const actual = expenses.filter((e) => e.category === c).reduce((s, e) => s + Number(e.amount), 0);
    return { c, planned, actual };
  });

  return (
    <div className="space-y-6">
      <PageHeader icon={Users} title="Resource Planning" subtitle="Assign work, track budgets against real expenses." />
      <div className="grid gap-4 md:grid-cols-3">
        {COLS.map(([k, label]) => (
          <div key={k} className="rounded-2xl border bg-secondary/40 p-4">
            <h3 className="mb-3 text-sm font-semibold">{label} <span className="text-muted-foreground">({tasks.filter((t) => t.status === k).length})</span></h3>
            <div className="space-y-2">
              {tasks.filter((t) => t.status === k).map((t) => {
                const mine = t.assignee_id === session.userId;
                const can = session.roles.includes("admin") || session.roles.includes("farmer") || mine;
                return (
                  <div key={t.id} className="animate-rise rounded-xl border bg-card p-3 shadow-soft transition-all">
                    <div className="flex items-start justify-between gap-2"><span className="text-sm font-medium">{t.title}</span><StatusPill value={t.priority} tone={t.priority === "high" ? "bad" : "muted"} /></div>
                    <div className="mt-2 flex items-center justify-between text-xs text-muted-foreground">
                      <span>{t.due_date ? `Due ${t.due_date}` : "No due date"}</span>
                      {can && (
                        <select aria-label="Change status" value={t.status} onChange={(e) => move.mutate({ id: t.id, status: e.target.value })} className="rounded-md border bg-card px-1.5 py-0.5">
                          {COLS.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
                        </select>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        ))}
      </div>
      {recon.length > 0 && (
        <div className="animate-rise rounded-2xl border bg-card p-5 shadow-soft">
          <h2 className="font-semibold">Budget vs actual</h2>
          <div className="mt-4 space-y-3">
            {recon.map((r) => {
              const pct = r.planned ? Math.min(100, (r.actual / r.planned) * 100) : 0;
              const over = r.actual > r.planned;
              return (
                <div key={r.c}>
                  <div className="flex justify-between text-sm"><span>{r.c}</span><span className={over ? "font-semibold text-destructive" : "text-muted-foreground"}>₹{r.actual.toLocaleString()} / ₹{r.planned.toLocaleString()}{over && " · over budget"}</span></div>
                  <div className="mt-1 h-2 overflow-hidden rounded-full bg-muted"><div className={`h-full rounded-full transition-[width] duration-1000 ${over ? "bg-destructive" : "bg-primary"}`} style={{ width: `${pct}%` }} /></div>
                </div>
              );
            })}
          </div>
        </div>
      )}
      <Tabs defaultValue="tasks">
        <TabsList><TabsTrigger value="tasks">Tasks</TabsTrigger><TabsTrigger value="workers">Workers</TabsTrigger><TabsTrigger value="budgets">Budgets</TabsTrigger><TabsTrigger value="expenses">Expenses</TabsTrigger></TabsList>
        <TabsContent value="tasks"><ResourceTable config={taskCfg} /></TabsContent>
        <TabsContent value="workers"><ResourceTable config={workerCfg} /></TabsContent>
        <TabsContent value="budgets"><ResourceTable config={budgetCfg} /></TabsContent>
        <TabsContent value="expenses"><ResourceTable config={expenseCfg} /></TabsContent>
      </Tabs>
    </div>
  );
}
