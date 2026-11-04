import { createFileRoute } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Bell, CheckCheck, Trash2, AlertTriangle, Info, OctagonAlert } from "lucide-react";
import { toast } from "sonner";
import { PageHeader } from "@/components/AppShell";
import { Button } from "@/components/ui/button";
import { deleteRow, listRows, updateRow } from "@/services/crud";
import { api } from "@/lib/api";
import type { Notification } from "@/lib/types";

export const Route = createFileRoute("/_authenticated/notifications")({
  head: () => ({ meta: [{ title: "Notifications — AgroPrecision" }, { name: "description", content: "Alerts for stock, tasks and farm events." }] }),
  component: NotificationsPage,
});

function NotificationsPage() {
  const qc = useQueryClient();
  const { data = [], isLoading } = useQuery({ queryKey: ["notifications"], queryFn: () => listRows<Notification>("notifications", { orderBy: "created_at" }) });
  const markAll = useMutation({
    mutationFn: () => api("/api/notifications/read-all", { method: "POST" }),
    onSuccess: () => qc.invalidateQueries(), onError: (e: Error) => toast.error(e.message),
  });
  const markOne = useMutation({
    mutationFn: (id: string) => updateRow("notifications", id, { is_read: true }),
    onSuccess: () => qc.invalidateQueries(),
  });
  const del = useMutation({ mutationFn: (id: string) => deleteRow("notifications", id), onSuccess: () => qc.invalidateQueries() });

  return (
    <div className="space-y-6">
      <PageHeader icon={Bell} title="Notifications" subtitle="Generated automatically from stock levels, task assignments and more.">
        <Button variant="lime" onClick={() => markAll.mutate()} loading={markAll.isPending} disabled={!data.some((n) => !n.is_read)}><CheckCheck /> Mark all read</Button>
      </PageHeader>
      {isLoading && <p className="text-sm text-muted-foreground">Loading…</p>}
      {!isLoading && !data.length && <p className="rounded-2xl border bg-card p-10 text-center text-muted-foreground">You're all caught up.</p>}
      <ul className="space-y-3">
        {data.map((n, i) => {
          const Icon = n.severity === "critical" ? OctagonAlert : n.severity === "warning" ? AlertTriangle : Info;
          return (
            <li key={n.id} onClick={() => !n.is_read && markOne.mutate(n.id)}
              className={`animate-slide-in flex cursor-pointer items-start gap-4 rounded-2xl border bg-card p-4 shadow-soft transition hover:border-primary/30 ${n.is_read ? "opacity-70" : "border-l-4 border-l-primary"}`}
              style={{ animationDelay: `${Math.min(i, 10) * 40}ms` }}>
              <Icon className={`mt-0.5 size-5 shrink-0 ${n.severity === "critical" ? "text-destructive" : n.severity === "warning" ? "text-sun" : "text-primary"}`} />
              <div className="flex-1">
                <div className="flex items-center gap-2">
                  <span className="font-medium">{n.title}</span>
                  <span className="rounded-full bg-muted px-2 text-[10px] uppercase text-muted-foreground">{n.severity}</span>
                  {!n.is_read && <span className="text-xs font-semibold text-primary">Unread</span>}
                </div>
                {n.body && <p className="mt-0.5 text-sm text-muted-foreground">{n.body}</p>}
                <p className="mt-1 font-tech text-[11px] text-muted-foreground">{new Date(n.created_at).toLocaleString()}</p>
              </div>
              <button aria-label="Delete notification" className="rounded-md p-1.5 text-muted-foreground hover:bg-destructive-soft hover:text-destructive" onClick={(e) => { e.stopPropagation(); del.mutate(n.id); }}><Trash2 className="size-4" /></button>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
