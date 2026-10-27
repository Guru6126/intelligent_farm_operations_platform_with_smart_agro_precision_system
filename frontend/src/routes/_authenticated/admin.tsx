import { createFileRoute, redirect } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Shield } from "lucide-react";
import { toast } from "sonner";
import { PageHeader } from "@/components/AppShell";
import { api } from "@/lib/api";
import { roleLabel, type AppRole } from "@/lib/auth";

export const Route = createFileRoute("/_authenticated/admin")({
  beforeLoad: ({ context }) => {
    if (!context.session.roles.includes("admin")) throw redirect({ to: "/dashboard" });
  },
  head: () => ({ meta: [{ title: "User Management — AgroPrecision" }, { name: "description", content: "Manage user accounts and roles." }] }),
  component: AdminPage,
});

interface AdminUser { id: string; email: string; full_name: string | null; roles: AppRole[]; created_at: string | null }
const ROLES: AppRole[] = ["admin", "farmer", "field_worker"];

function AdminPage() {
  const qc = useQueryClient();
  const { data = [], isLoading } = useQuery({ queryKey: ["admin-users"], queryFn: () => api<AdminUser[]>("/api/admin/users") });
  const setRole = useMutation({
    mutationFn: (v: { id: string; role: AppRole; enabled: boolean }) =>
      api(`/api/admin/users/${v.id}/roles`, { method: "POST", body: { role: v.role, enabled: v.enabled } }),
    onSuccess: () => { toast.success("Role updated"); qc.invalidateQueries({ queryKey: ["admin-users"] }); },
    onError: (e: Error) => toast.error(e.message),
  });

  return (
    <div className="space-y-6">
      <PageHeader icon={Shield} title="User Management" subtitle="Grant or remove roles for every account." />
      <div className="overflow-x-auto rounded-2xl border bg-card shadow-soft">
        <table className="w-full text-sm">
          <thead className="bg-muted/60 text-left text-xs uppercase text-muted-foreground">
            <tr><th className="p-3">Name</th><th className="p-3">Email</th><th className="p-3">Joined</th>{ROLES.map((r) => <th key={r} className="p-3 text-center">{roleLabel[r]}</th>)}</tr>
          </thead>
          <tbody>
            {isLoading && <tr><td colSpan={6} className="p-6 text-center text-muted-foreground">Loading…</td></tr>}
            {data.map((u) => (
              <tr key={u.id} className="border-t">
                <td className="p-3 font-medium">{u.full_name ?? "—"}</td>
                <td className="p-3">{u.email}</td>
                <td className="p-3 text-muted-foreground">{u.created_at?.slice(0, 10) ?? "—"}</td>
                {ROLES.map((r) => (
                  <td key={r} className="p-3 text-center">
                    <input type="checkbox" aria-label={`${roleLabel[r]} role for ${u.email}`} className="size-4 accent-[var(--primary)]"
                      checked={u.roles.includes(r)} disabled={setRole.isPending}
                      onChange={(e) => setRole.mutate({ id: u.id, role: r, enabled: e.target.checked })} />
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
