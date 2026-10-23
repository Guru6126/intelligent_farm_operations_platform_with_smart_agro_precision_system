import { useState, type ReactNode } from "react";
import { Link, useNavigate } from "@tanstack/react-router";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import {
  LayoutDashboard, Tractor, Map, Sprout, Package, CloudSun, Droplets, Wrench, Users, Bell, BarChart3,
  UserCog, LogOut, Menu, X, Leaf, Shield,
} from "lucide-react";
import { signOut as clearSession } from "@/lib/auth";
import { useFarm } from "@/lib/farm-context";
import { roleLabel } from "@/lib/auth";
import { listRows } from "@/services/crud";
import { cn } from "@/lib/utils";

const nav = [
  { to: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { to: "/farms", label: "Farms & Plots", icon: Tractor },
  { to: "/map", label: "GIS Map", icon: Map },
  { to: "/crops", label: "Crop Lifecycle", icon: Sprout },
  { to: "/inventory", label: "Inventory", icon: Package },
  { to: "/weather", label: "Weather", icon: CloudSun },
  { to: "/irrigation", label: "Irrigation", icon: Droplets },
  { to: "/equipment", label: "Equipment", icon: Wrench },
  { to: "/resources", label: "Resources & Tasks", icon: Users },
  { to: "/notifications", label: "Notifications", icon: Bell },
  { to: "/analytics", label: "Analytics & Reports", icon: BarChart3 },
  { to: "/profile", label: "Profile", icon: UserCog },
] as const;

export function AppShell({ children }: { children: ReactNode }) {
  const { session, farms, farmId, setFarmId } = useFarm();
  const [open, setOpen] = useState(false);
  const qc = useQueryClient();
  const navigate = useNavigate();
  const { data: unread = [] } = useQuery({
    queryKey: ["notifications-unread"],
    queryFn: () => listRows<{ id: string; is_read: boolean }>("notifications", { orderBy: "created_at" }),
    select: (rows) => rows.filter((r) => !r.is_read),
  });

  const signOut = async () => {
    await qc.cancelQueries();
    qc.clear();
    clearSession();
    navigate({ to: "/auth", replace: true });
  };

  const isAdmin = session.roles.includes("admin");

  return (
    <div className="flex min-h-screen bg-background">
      <aside className={cn(
        "fixed inset-y-0 left-0 z-40 flex w-64 flex-col bg-sidebar text-sidebar-foreground transition-transform duration-300 lg:sticky lg:top-0 lg:h-screen lg:translate-x-0",
        open ? "translate-x-0" : "-translate-x-full",
      )}>
        <div className="flex items-center gap-2 px-5 py-5">
          <div className="grid size-9 place-items-center rounded-xl bg-sidebar-primary text-sidebar-primary-foreground"><Leaf className="size-5" /></div>
          <div className="leading-tight">
            <div className="font-display text-base font-bold">AgroPrecision</div>
            <div className="font-tech text-[10px] uppercase tracking-widest text-sidebar-foreground/60">Farm OS</div>
          </div>
          <button className="ml-auto lg:hidden" aria-label="Close menu" onClick={() => setOpen(false)}><X className="size-5" /></button>
        </div>
        <nav className="flex-1 space-y-0.5 overflow-y-auto px-3 pb-4">
          {nav.map((n) => (
            <Link key={n.to} to={n.to} onClick={() => setOpen(false)}
              className="group flex items-center gap-3 rounded-lg px-3 py-2 text-sm text-sidebar-foreground/80 transition hover:bg-sidebar-accent hover:text-sidebar-accent-foreground"
              activeProps={{ className: "bg-sidebar-accent text-sidebar-accent-foreground shadow-[inset_3px_0_0_var(--lime)]" }}>
              <n.icon className="size-4 transition group-hover:text-sidebar-primary" />
              <span className="flex-1">{n.label}</span>
              {n.to === "/notifications" && unread.length > 0 && (
                <span className="rounded-full bg-sidebar-primary px-1.5 text-[10px] font-bold text-sidebar-primary-foreground">{unread.length}</span>
              )}
            </Link>
          ))}
          {isAdmin && (
            <Link to="/admin" onClick={() => setOpen(false)}
              className="flex items-center gap-3 rounded-lg px-3 py-2 text-sm text-sidebar-foreground/80 transition hover:bg-sidebar-accent"
              activeProps={{ className: "bg-sidebar-accent text-sidebar-accent-foreground" }}>
              <Shield className="size-4" /> User Management
            </Link>
          )}
        </nav>
        <div className="border-t border-sidebar-border p-4">
          <div className="truncate text-sm font-medium">{session.fullName || session.email}</div>
          <div className="font-tech text-[11px] text-sidebar-primary">{session.roles.map((r) => roleLabel[r]).join(" · ") || "No role"}</div>
          <button onClick={signOut} className="mt-3 flex w-full items-center gap-2 rounded-lg px-2 py-1.5 text-sm text-sidebar-foreground/70 transition hover:bg-sidebar-accent hover:text-sidebar-accent-foreground">
            <LogOut className="size-4" /> Sign out
          </button>
        </div>
      </aside>
      {open && <div className="fixed inset-0 z-30 bg-evergreen/50 lg:hidden" onClick={() => setOpen(false)} />}

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-20 flex items-center gap-3 border-b bg-background/85 px-4 py-3 backdrop-blur lg:px-8">
          <button className="rounded-lg p-2 hover:bg-secondary lg:hidden" aria-label="Open menu" onClick={() => setOpen(true)}><Menu className="size-5" /></button>
          <label className="text-xs font-medium uppercase tracking-wide text-muted-foreground" htmlFor="farm-switch">Farm</label>
          <select id="farm-switch" value={farmId ?? "all"} onChange={(e) => setFarmId(e.target.value === "all" ? null : e.target.value)}
            className="h-9 max-w-[16rem] rounded-lg border bg-card px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
            <option value="all">All farms ({farms.length})</option>
            {farms.map((f) => <option key={f.id} value={f.id}>{f.name}</option>)}
          </select>
          <Link to="/notifications" className="relative ml-auto rounded-lg p-2 transition hover:bg-secondary" aria-label="Notifications">
            <Bell className="size-5" />
            {unread.length > 0 && <span className="absolute right-1.5 top-1.5 size-2 rounded-full bg-primary ring-2 ring-background" />}
          </Link>
        </header>
        <main className="flex-1 px-4 py-6 lg:px-8 lg:py-8">{children}</main>
      </div>
    </div>
  );
}

export function PageHeader({ title, subtitle, icon: Icon, children, decoration }: {
  title: string; subtitle?: string; icon: React.ComponentType<{ className?: string }>; children?: ReactNode; decoration?: ReactNode;
}) {
  return (
    <div className="relative mb-6 overflow-hidden rounded-2xl bg-forest px-6 py-6 text-primary-foreground shadow-soft animate-rise">
      <div aria-hidden className="pointer-events-none absolute inset-0">{decoration}</div>
      <div className="relative flex flex-wrap items-center gap-4">
        <div className="grid size-12 place-items-center rounded-xl bg-accent text-accent-foreground"><Icon className="size-6" /></div>
        <div className="flex-1">
          <h1 className="text-2xl font-bold md:text-3xl">{title}</h1>
          {subtitle && <p className="mt-0.5 text-sm text-primary-foreground/75">{subtitle}</p>}
        </div>
        {children}
      </div>
    </div>
  );
}

export function StatCard({ label, value, hint, icon: Icon, delay = 0, tone }: {
  label: string; value: ReactNode; hint?: ReactNode; icon: React.ComponentType<{ className?: string }>; delay?: number; tone?: "warn";
}) {
  return (
    <div className={cn("lift animate-rise rounded-2xl border bg-card p-5 shadow-soft", tone === "warn" && "animate-pulse-once border-destructive/30")} style={{ animationDelay: `${delay}ms` }}>
      <div className="flex items-center justify-between">
        <span className="text-xs font-medium uppercase tracking-wide text-muted-foreground">{label}</span>
        <Icon className={cn("size-4 text-primary", tone === "warn" && "text-destructive")} />
      </div>
      <div className="mt-2 font-display text-3xl font-bold">{value}</div>
      {hint && <div className="mt-1 text-xs text-muted-foreground">{hint}</div>}
    </div>
  );
}
