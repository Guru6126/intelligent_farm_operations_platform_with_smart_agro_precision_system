import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { UserCog } from "lucide-react";
import { toast } from "sonner";
import { PageHeader } from "@/components/AppShell";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { api } from "@/lib/api";
import { roleLabel } from "@/lib/auth";
import { useFarm } from "@/lib/farm-context";

export const Route = createFileRoute("/_authenticated/profile")({
  head: () => ({ meta: [{ title: "Profile — AgroPrecision" }, { name: "description", content: "Your account details and password." }] }),
  component: ProfilePage,
});

function ProfilePage() {
  const { session } = useFarm();
  const qc = useQueryClient();
  const { data } = useQuery({ queryKey: ["account"], queryFn: () => api<{ email: string; fullName: string | null; phone: string | null }>("/api/account") });
  const [form, setForm] = useState({ fullName: "", phone: "" });
  const [pw, setPw] = useState({ currentPassword: "", newPassword: "" });
  useEffect(() => { if (data) setForm({ fullName: data.fullName ?? "", phone: data.phone ?? "" }); }, [data]);

  const save = useMutation({
    mutationFn: () => api("/api/account", { method: "PUT", body: form }),
    onSuccess: () => { toast.success("Profile saved"); qc.invalidateQueries(); },
    onError: (e: Error) => toast.error(e.message),
  });
  const change = useMutation({
    mutationFn: () => api("/api/account/password", { method: "POST", body: pw }),
    onSuccess: () => { toast.success("Password changed"); setPw({ currentPassword: "", newPassword: "" }); },
    onError: (e: Error) => toast.error(e.message),
  });

  return (
    <div className="space-y-6">
      <PageHeader icon={UserCog} title="Profile" subtitle={`${session.email} · ${session.roles.map((r) => roleLabel[r]).join(", ")}`} />
      <div className="grid gap-6 lg:grid-cols-2">
        <form className="animate-rise space-y-4 rounded-2xl border bg-card p-6 shadow-soft" onSubmit={(e) => { e.preventDefault(); save.mutate(); }}>
          <h2 className="font-semibold">Your details</h2>
          <div className="grid gap-1.5"><Label htmlFor="fn">Full name</Label><Input id="fn" required value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} /></div>
          <div className="grid gap-1.5"><Label htmlFor="ph">Phone</Label><Input id="ph" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} /></div>
          <Button type="submit" loading={save.isPending}>Save</Button>
        </form>
        <form className="animate-rise space-y-4 rounded-2xl border bg-card p-6 shadow-soft" onSubmit={(e) => {
          e.preventDefault();
          if (pw.newPassword.length < 8) return toast.error("New password must be at least 8 characters.");
          change.mutate();
        }}>
          <h2 className="font-semibold">Change password</h2>
          <div className="grid gap-1.5"><Label htmlFor="cp">Current password</Label><Input id="cp" type="password" required value={pw.currentPassword} onChange={(e) => setPw({ ...pw, currentPassword: e.target.value })} /></div>
          <div className="grid gap-1.5"><Label htmlFor="np">New password</Label><Input id="np" type="password" required value={pw.newPassword} onChange={(e) => setPw({ ...pw, newPassword: e.target.value })} /></div>
          <Button type="submit" variant="secondary" loading={change.isPending}>Update password</Button>
        </form>
      </div>
    </div>
  );
}
