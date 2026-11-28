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

interface AdminUser {
  id: string;
  user_id?: string;
  email: string;
  full_name: string | null;
  first_name?: string | null;
  last_name?: string | null;
  phone_number?: string | null;
  gender?: string | null;
  date_of_birth?: string | null;
  city?: string | null;
  state?: string | null;
  pincode?: string | null;
  roles: AppRole[];
  created_at: string | null;
}
const ROLES: AppRole[] = ["admin", "farmer", "field_worker"];

function AdminPage() {
  const qc = useQueryClient();
  const { data = [], isLoading } = useQuery({ queryKey: ["admin-users"], queryFn: () => api<AdminUser[]>("/api/admin/users") });
  const setRole = useMutation({
    mutationFn: async (v: { user: AdminUser; role: AppRole; enabled: boolean }) => {
      if (v.enabled) {
        const others = v.user.roles.filter((r) => r !== v.role);
        for (const other of others) {
          await api(`/api/admin/users/${v.user.id}/roles`, { method: "POST", body: { role: other, enabled: false } });
        }
      }
      return api(`/api/admin/users/${v.user.id}/roles`, { method: "POST", body: { role: v.role, enabled: v.enabled } });
    },
    onSuccess: () => { toast.success("Role updated"); qc.invalidateQueries({ queryKey: ["admin-users"] }); },
    onError: (e: Error) => toast.error(e.message),
  });

  return (
    <div className="space-y-6">
      <PageHeader icon={Shield} title="User Management" subtitle="Grant or remove roles for every account." />
      <div className="overflow-x-auto rounded-2xl border bg-card shadow-soft">
        <table className="w-full text-sm">
          <thead className="bg-muted/60 text-left text-xs uppercase text-muted-foreground">
            <tr>
              <th className="p-3">Name</th>
              <th className="p-3">Email</th>
              <th className="p-3">Phone</th>
              <th className="p-3">City / State</th>
              <th className="p-3">Joined</th>
              {ROLES.map((r) => <th key={r} className="p-3 text-center">{roleLabel[r]}</th>)}
            </tr>
          </thead>
          <tbody>
            {isLoading && <tr><td colSpan={8} className="p-6 text-center text-muted-foreground">Loading…</td></tr>}
            {data.map((u) => (
              <tr key={u.id} className="border-t">
                <td className="p-3 font-medium">
                  {u.first_name || u.last_name ? `${u.first_name ?? ""} ${u.last_name ?? ""}`.trim() : (u.full_name ?? "—")}
                </td>
                <td className="p-3">{u.email}</td>
                <td className="p-3 text-muted-foreground">{u.phone_number ?? "—"}</td>
                <td className="p-3 text-muted-foreground">
                  {u.city || u.state ? `${u.city ?? ""}${u.city && u.state ? ", " : ""}${u.state ?? ""}` : "—"}
                </td>
                <td className="p-3 text-muted-foreground">{u.created_at?.slice(0, 10) ?? "—"}</td>
                {ROLES.map((r) => {
                  const isProtected = u.email.toLowerCase() === "learningaws2005@gmail.com";
                  return (
                    <td key={r} className="p-3 text-center">
                      <input type="checkbox" aria-label={`${roleLabel[r]} role for ${u.email}`} className="size-4 accent-[var(--primary)]"
                        checked={u.roles.includes(r)} disabled={setRole.isPending || isProtected}
                        title={isProtected ? "Primary admin account cannot be modified" : undefined}
                        onChange={(e) => !isProtected && setRole.mutate({ user: u, role: r, enabled: e.target.checked })} />
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
