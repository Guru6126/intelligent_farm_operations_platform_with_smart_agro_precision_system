import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { z } from "zod";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { api } from "@/lib/api";

export const Route = createFileRoute("/reset-password")({
  validateSearch: z.object({ token: z.string().optional() }),
  head: () => ({
    meta: [
      { title: "Set a new password — AgroPrecision" },
      { name: "description", content: "Choose a new password for your AgroPrecision account." },
      { property: "og:title", content: "Set a new password — AgroPrecision" },
      { property: "og:description", content: "Reset your AgroPrecision password." },
    ],
  }),
  component: Reset,
});

function Reset() {
  const { token = "" } = Route.useSearch();
  const [code, setCode] = useState(token);
  const [pw, setPw] = useState("");
  const [busy, setBusy] = useState(false);
  const navigate = useNavigate();
  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (pw.length < 8) return toast.error("Password must be at least 8 characters.");
    setBusy(true);
    try {
      await api("/api/auth/reset-password", { method: "POST", body: { token: code, password: pw } });
      toast.success("Password updated — please sign in");
      navigate({ to: "/auth" });
    } catch (err) {
      toast.error((err as Error).message);
    } finally {
      setBusy(false);
    }
  };
  return (
    <div className="flex min-h-screen items-center justify-center p-6">
      <form onSubmit={submit} className="animate-rise w-full max-w-sm space-y-4 rounded-2xl border bg-card p-8 shadow-soft">
        <h1 className="text-2xl font-bold">Set a new password</h1>
        <div className="grid gap-1.5"><Label htmlFor="code">Reset code</Label><Input id="code" value={code} onChange={(e) => setCode(e.target.value)} required /></div>
        <div className="grid gap-1.5"><Label htmlFor="pw">New password</Label><Input id="pw" type="password" value={pw} onChange={(e) => setPw(e.target.value)} required /></div>
        <Button type="submit" className="w-full" loading={busy}>Update password</Button>
      </form>
    </div>
  );
}
