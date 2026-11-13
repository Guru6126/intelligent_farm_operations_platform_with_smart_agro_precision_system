import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useState, useEffect } from "react";
import { z } from "zod";
import { toast } from "sonner";
import { Leaf, UserCheck } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { api } from "@/lib/api";
import { signIn, signUp } from "@/lib/auth";
import hero from "@/assets/hero-farm.jpg";

export const Route = createFileRoute("/auth")({
  validateSearch: z.object({
    redirect: z.string().optional(),
    invite: z.string().optional(),
  }),
  head: () => ({
    meta: [
      { title: "Sign in — AgroPrecision" },
      { name: "description", content: "Sign in or create your AgroPrecision farm operations account." },
      { property: "og:title", content: "Sign in — AgroPrecision" },
      { property: "og:description", content: "Access your farm operations dashboard." },
    ],
  }),
  component: AuthPage,
});

type Mode = "signin" | "signup" | "forgot";

interface InvitePreview {
  farmId: string;
  farmName: string;
  expiresAt: string;
}

function AuthPage() {
  const search = Route.useSearch();
  const inviteToken = search.invite;

  const [mode, setMode] = useState<Mode>(inviteToken ? "signup" : "signin");
  const [busy, setBusy] = useState(false);
  const [form, setForm] = useState({ email: "", password: "", fullName: "", role: "farmer" });
  const [invitePreview, setInvitePreview] = useState<InvitePreview | null>(null);
  const [inviteError, setInviteError] = useState<string | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    if (inviteToken) {
      setMode("signup");
      api<InvitePreview>(`/api/invites/${inviteToken}`)
        .then((data) => setInvitePreview(data))
        .catch((err) => setInviteError(err.message || "Invalid or expired invite link."));
    }
  }, [inviteToken]);

  import { validateValues } from "@/lib/validation";

const submit = async (e: React.FormEvent) => {
  e.preventDefault();
  // Basic validation for email and password
  const err = validateValues({ email: form.email, password: form.password });
  if (err) {
    toast.error(err);
    return;
  }
    e.preventDefault();
    setBusy(true);
    try {
      if (mode === "signin") {
        await signIn(form.email, form.password);
        const to = search.redirect && search.redirect.startsWith("/") ? search.redirect : "/dashboard";
        navigate({ to });
      } else if (mode === "signup") {
        if (form.password.length < 8) throw new Error("Password must be at least 8 characters.");
        await signUp({
          email: form.email,
          password: form.password,
          fullName: form.fullName,
          role: inviteToken ? "field_worker" : "farmer",
          inviteToken: inviteToken || undefined,
        });
        if (inviteToken) {
          toast.success("Account created and joined farm successfully!");
        }
        navigate({ to: "/dashboard" });
      } else {
        await api("/api/auth/forgot-password", { method: "POST", body: { email: form.email } });
        toast.success("If that email exists, a reset code has been issued.");
        navigate({ to: "/reset-password", search: {} });
      }
    } catch (err) {
      toast.error((err as Error).message);
    } finally {
      setBusy(false);
    }
  };

  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setForm({ ...form, [k]: e.target.value });

  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      <div className="relative hidden lg:block">
        <img src={hero} alt="" className="absolute inset-0 h-full w-full object-cover" />
        <div className="absolute inset-0 bg-hero" />
        <div className="relative flex h-full flex-col justify-end p-12 text-primary-foreground">
          <p className="font-tech text-xs uppercase tracking-[0.3em] text-lime">Farm OS</p>
          <h2 className="mt-3 text-4xl font-bold">Precision for every acre.</h2>
        </div>
      </div>
      <div className="flex items-center justify-center p-6">
        <form onSubmit={submit} className="animate-rise w-full max-w-sm space-y-5">
          <Link to="/" className="flex items-center gap-2">
            <div className="grid size-9 place-items-center rounded-xl bg-forest text-lime">
              <Leaf className="size-5" />
            </div>
            <span className="font-display text-lg font-bold">AgroPrecision</span>
          </Link>
          <div>
            <h1 className="text-3xl font-bold">
              {mode === "signin"
                ? "Welcome back"
                : mode === "signup"
                ? inviteToken
                  ? "Join as Field Worker"
                  : "Create Farmer Account"
                : "Reset password"}
            </h1>
            <p className="mt-1 text-sm text-muted-foreground">
              {mode === "forgot"
                ? "We'll issue a one-time reset code."
                : inviteToken && invitePreview
                ? `You've been invited to join ${invitePreview.farmName}.`
                : "Manage your farm operations."}
            </p>
          </div>

          {inviteToken && inviteError && (
            <div className="rounded-lg bg-destructive/10 p-3 text-sm text-destructive border border-destructive/20">
              {inviteError}
            </div>
          )}

          {inviteToken && invitePreview && (
            <div className="flex items-center gap-2.5 rounded-lg bg-primary/10 border border-primary/20 p-3 text-xs text-primary font-medium">
              <UserCheck className="size-4 shrink-0" />
              <span>Joining <strong>{invitePreview.farmName}</strong> as assigned Field Worker</span>
            </div>
          )}

          {mode === "signup" && (
            <>
              <div className="grid gap-1.5">
                <Label htmlFor="fn">Full name</Label>
                <Input id="fn" required value={form.fullName} onChange={set("fullName")} />
              </div>
              {!inviteToken && (
                <div className="rounded-lg bg-muted/40 p-2.5 text-xs text-muted-foreground border">
                  Role: <strong>Farmer</strong> (Full farm owner & management access). Field workers must be invited by a farmer.
                </div>
              )}
            </>
          )}

          <div className="grid gap-1.5">
            <Label htmlFor="em">Email</Label>
            <Input id="em" type="email" required value={form.email} onChange={set("email")} />
          </div>

          {mode !== "forgot" && (
            <div className="grid gap-1.5">
              <Label htmlFor="pw">Password</Label>
              <Input id="pw" type="password" required value={form.password} onChange={set("password")} />
            </div>
          )}

          <Button type="submit" className="w-full" size="lg" loading={busy} disabled={Boolean(inviteToken && inviteError)}>
            {mode === "signin"
              ? "Sign in"
              : mode === "signup"
              ? inviteToken
                ? "Accept Invite & Join"
                : "Create Farmer Account"
              : "Get reset code"}
          </Button>

          <div className="flex justify-between text-sm">
            {mode === "signin" ? (
              <>
                <button type="button" className="text-primary hover:underline" onClick={() => setMode("signup")}>
                  Create account
                </button>
                <button type="button" className="text-muted-foreground hover:text-primary" onClick={() => setMode("forgot")}>
                  Forgot password?
                </button>
              </>
            ) : (
              <button type="button" className="text-primary hover:underline" onClick={() => setMode("signin")}>
                Back to sign in
              </button>
            )}
          </div>
        </form>
      </div>
    </div>
  );
}
