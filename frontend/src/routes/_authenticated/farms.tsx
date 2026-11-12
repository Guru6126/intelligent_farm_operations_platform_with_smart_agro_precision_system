<<<<<<< Updated upstream
import { createFileRoute, Link } from "@tanstack/react-router";
import { useState, useMemo } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  Tractor,
  Leaf,
  UserPlus,
  Link2,
  Copy,
  Check,
  Sprout,
  Layers,
  Calendar,
  ArrowUpRight,
  MapPin,
  CheckCircle2,
  Clock,
  Compass,
  AlertCircle,
} from "lucide-react";
=======
import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Tractor, Leaf, UserPlus } from "lucide-react";
>>>>>>> Stashed changes
import { toast } from "sonner";
import { PageHeader } from "@/components/AppShell";
import { ResourceTable, StatusPill, type ResourceConfig } from "@/components/ResourceTable";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useFarm, type Farm } from "@/lib/farm-context";
import { canManage } from "@/lib/auth";
import { rpc } from "@/services/crud";
<<<<<<< Updated upstream
import { api } from "@/lib/api";
import type { Plot, Crop } from "@/lib/types";
import { cropCfg, STAGES } from "./crops";
=======
import type { Plot } from "@/lib/types";
>>>>>>> Stashed changes

export const Route = createFileRoute("/_authenticated/farms")({
  head: () => ({ meta: [{ title: "Farms & Plots — AgroPrecision" }, { name: "description", content: "Manage farms, plots and members." }] }),
  component: FarmsPage,
});

const farmCfg: ResourceConfig<Farm & Record<string, unknown>> = {
  table: "farms", title: "Farms", singular: "Farm", farmScoped: false, orderBy: "created_at",
  fields: [
    { key: "name", label: "Farm name", required: true },
    { key: "location", label: "Location / village" },
    { key: "latitude", label: "Latitude", type: "number" },
    { key: "longitude", label: "Longitude", type: "number" },
    { key: "total_area_acres", label: "Total area (acres)", type: "number" },
    { key: "soil_type", label: "Soil type" },
  ],
  columns: [
    { key: "name", label: "Name", render: (r) => <span className="font-medium">{r.name} {r.is_demo && <StatusPill value="demo" tone="warn" />}</span> },
    { key: "location", label: "Location" },
    { key: "total_area_acres", label: "Acres" },
    { key: "soil_type", label: "Soil" },
  ],
};

const plotCfg: ResourceConfig<Plot> = {
<<<<<<< Updated upstream
  table: "plots",
  title: "Plots & Assigned Crops",
  singular: "Plot",
  validate: (values, existingPlots) => {
    const name = String(values.name ?? "").trim();
    if (!name) return "Plot name is required.";
    const farmId = String(values.farm_id ?? "");
    if (!farmId) return "Please choose a farm.";
    const isDuplicate = existingPlots.some(
      (p) =>
        String(p.farm_id) === farmId &&
        p.name.trim().toLowerCase() === name.toLowerCase() &&
        p.id !== values.id,
    );
    if (isDuplicate) {
      return `A plot named "${name}" already exists on this farm. Duplication of plots is not allowed.`;
    }
    return null;
  },
=======
  table: "plots", title: "Plots", singular: "Plot",
>>>>>>> Stashed changes
  fields: [
    { key: "name", label: "Plot name", required: true },
    { key: "area_acres", label: "Area (acres)", type: "number" },
    { key: "soil_ph", label: "Soil pH", type: "number" },
  ],
  columns: [
    { key: "name", label: "Plot" },
    { key: "farm_id", label: "Farm", render: (r) => <FarmName id={r.farm_id} /> },
    { key: "area_acres", label: "Acres" },
    { key: "soil_ph", label: "Soil pH" },
    { key: "boundary_geojson", label: "Boundary", render: (r) => r.boundary_geojson ? <StatusPill value="mapped" tone="good" /> : <StatusPill value="not mapped" /> },
  ],
};

<<<<<<< Updated upstream
function FarmsPage() {
  const { farms, session } = useFarm();
  const { data: plots = [] } = useRows<Plot>("plots");
  const { data: crops = [] } = useRows<Crop>("crops");

  // Deduplicate plots defensively by farm_id and name (preferring mapped boundaries / assigned crops)
  const uniquePlots = useMemo(() => {
    const map = new Map<string, Plot>();
    for (const p of plots) {
      const key = `${p.farm_id}:${String(p.name ?? "").trim().toLowerCase()}`;
      const existing = map.get(key);
      if (!existing) {
        map.set(key, p);
      } else {
        const pScore = (p.boundary_geojson ? 2 : 0) + (crops.some((c) => c.plot_id === p.id) ? 1 : 0);
        const exScore = (existing.boundary_geojson ? 2 : 0) + (crops.some((c) => c.plot_id === existing.id) ? 1 : 0);
        if (pScore > exScore) {
          map.set(key, p);
        }
      }
    }
    return Array.from(map.values());
  }, [plots, crops]);

  // Summary computations
  const totalPlotAcres = uniquePlots.reduce((acc, p) => acc + Number(p.area_acres ?? 0), 0);
  const plotsWithCrops = uniquePlots.filter((p) => crops.some((c) => c.plot_id === p.id && c.stage !== "harvested"));
  const activeCrops = crops.filter((c) => c.stage !== "harvested");
  const uniqueCropNames = Array.from(new Set(activeCrops.map((c) => c.crop_name)));

  return (
    <div className="space-y-6">
      <PageHeader
        icon={Tractor}
        title="Farms & Plots"
        subtitle="Manage farm boundaries, plot acreage, and track crop cultivation across all fields."
        decoration={
          <Leaf className="absolute -right-6 -top-6 size-40 rotate-12 text-lime/10 animate-cloud" />
        }
      />

      {/* Overview Stat Cards */}
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          icon={Tractor}
          label="Total Farms"
          value={farms.length}
          hint={`${farms.reduce((s, f) => s + Number(f.total_area_acres ?? 0), 0).toFixed(1)} total acres`}
          delay={0}
        />
        <StatCard
          icon={Layers}
          label="Total Plots"
          value={uniquePlots.length}
          hint={`${totalPlotAcres.toFixed(1)} acres divided`}
          delay={60}
        />
        <StatCard
          icon={Sprout}
          label="Cultivated Plots"
          value={plotsWithCrops.length}
          hint={
            uniquePlots.length > 0
              ? `${Math.round((plotsWithCrops.length / uniquePlots.length) * 100)}% plots cultivated`
              : "No plots registered"
          }
          delay={120}
        />
        <StatCard
          icon={Calendar}
          label="Active Crops"
          value={activeCrops.length}
          hint={uniqueCropNames.length > 0 ? uniqueCropNames.join(", ") : "No active crops"}
          delay={180}
        />
      </div>

      {/* Farm Overview Cards with Cultivated Crop Highlights */}
      <div>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-base font-semibold text-foreground flex items-center gap-2">
            <MapPin className="size-4 text-primary" /> Registered Farm Locations ({farms.length})
          </h2>
        </div>
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {farms.map((f, i) => {
            const farmPlots = uniquePlots.filter((p) => p.farm_id === f.id);
            const farmCrops = crops.filter((c) => c.farm_id === f.id && c.stage !== "harvested");
            return (
              <div
                key={f.id}
                className="lift animate-grow rounded-2xl border bg-card p-5 shadow-soft flex flex-col justify-between"
                style={{ animationDelay: `${i * 70}ms` }}
              >
                <div>
                  <div className="flex items-start justify-between">
                    <h3 className="text-lg font-semibold">{f.name}</h3>
                    {f.is_demo && <StatusPill value="demo data" tone="warn" />}
                  </div>
                  <p className="mt-1 text-sm text-muted-foreground">
                    {f.location ?? "No location"} · {f.total_area_acres ?? "?"} acres
                  </p>
                  <p className="mt-0.5 font-tech text-xs text-muted-foreground">
                    {f.latitude && f.longitude ? `${f.latitude}, ${f.longitude}` : "No coordinates"}
                  </p>

                  {/* Cultivated Crops on this Farm */}
                  <div className="mt-3 pt-3 border-t">
                    <div className="flex items-center justify-between text-xs text-muted-foreground mb-1.5">
                      <span className="font-medium text-foreground">Cultivated Crops:</span>
                      <span>
                        {farmPlots.length} plot{farmPlots.length === 1 ? "" : "s"}
                      </span>
                    </div>
                    {farmCrops.length > 0 ? (
                      <div className="flex flex-wrap gap-1.5">
                        {farmCrops.map((c) => (
                          <Badge
                            key={c.id}
                            variant="secondary"
                            className="gap-1 text-xs font-normal bg-secondary/80 hover:bg-secondary"
                          >
                            <Sprout className="size-3 text-primary" />
                            <span className="font-medium">{c.crop_name}</span>
                            {c.variety && <span className="opacity-75">({c.variety})</span>}
                            <span className="capitalize text-[10px] text-primary/80 font-mono">
                              · {c.stage}
                            </span>
                          </Badge>
                        ))}
                      </div>
                    ) : (
                      <p className="text-xs text-muted-foreground italic">No active crops assigned</p>
                    )}
                  </div>
                </div>

                {canManage(session.roles) && f.owner_id === session.userId && (
                  <AddMember farmId={f.id} />
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* Main Content Tabs: Plots & Cultivation, Crop Plans, Farms Registry */}
      <Tabs defaultValue="plots" className="space-y-5">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b pb-3">
          <TabsList className="bg-card border shadow-xs">
            <TabsTrigger value="plots" className="gap-2">
              <Tractor className="size-4" />
              <span>Plots & Cultivation</span>
              <span className="rounded-full bg-primary/10 text-primary px-1.5 py-0.2 text-[11px] font-mono">
                {uniquePlots.length}
              </span>
            </TabsTrigger>
            <TabsTrigger value="crops" className="gap-2">
              <Sprout className="size-4" />
              <span>Crop Plans & Details</span>
              <span className="rounded-full bg-primary/10 text-primary px-1.5 py-0.2 text-[11px] font-mono">
                {crops.length}
              </span>
            </TabsTrigger>
            <TabsTrigger value="farms" className="gap-2">
              <Leaf className="size-4" />
              <span>Farms</span>
              <span className="rounded-full bg-primary/10 text-primary px-1.5 py-0.2 text-[11px] font-mono">
                {farms.length}
              </span>
            </TabsTrigger>
          </TabsList>

          <div className="flex items-center gap-2">
            <Button asChild variant="outline" size="sm" className="gap-1.5">
              <Link to="/crops">
                <span>Crop Lifecycle</span>
                <ArrowUpRight className="size-3.5" />
              </Link>
            </Button>
            <Button asChild variant="outline" size="sm" className="gap-1.5">
              <Link to="/map">
                <Compass className="size-3.5" />
                <span>GIS Plot Map</span>
              </Link>
            </Button>
          </div>
        </div>

        {/* Tab 1: Plots & Cultivated Crops Visual Cards + Table */}
        <TabsContent value="plots" className="space-y-6">
          <PlotCropGrid plots={uniquePlots} crops={crops} farms={farms} />
          <ResourceTable config={plotCfg} />
        </TabsContent>

        {/* Tab 2: Crop Plans Table */}
        <TabsContent value="crops" className="space-y-4">
          <div className="rounded-2xl border bg-card/70 p-4 text-sm text-muted-foreground flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              <Sprout className="size-5 text-primary" />
              <span>
                Plan, assign, and update crops across all plots. Changes made here immediately reflect in your plot schedules and GIS map.
              </span>
            </div>
            <Button asChild variant="link" size="sm" className="gap-1 text-primary">
              <Link to="/crops">
                Manage Activities & Harvests <ArrowUpRight className="size-3.5" />
              </Link>
            </Button>
          </div>
          <ResourceTable config={cropCfg} />
        </TabsContent>

        {/* Tab 3: Farms Registry */}
        <TabsContent value="farms" className="space-y-4">
          <ResourceTable config={farmCfg} />
        </TabsContent>
      </Tabs>
    </div>
  );
=======
function FarmName({ id }: { id: string }) {
  const { farms } = useFarm();
  return <>{farms.find((f) => f.id === id)?.name ?? "—"}</>;
>>>>>>> Stashed changes
}

function FarmsPage() {
  const { farms, session } = useFarm();
  return (
    <div className="space-y-6">
      <PageHeader icon={Tractor} title="Farms & Plots" subtitle="Register farms, divide them into plots, add field workers."
        decoration={<Leaf className="absolute -right-6 -top-6 size-40 rotate-12 text-lime/10 animate-cloud" />} />
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        {farms.map((f, i) => (
          <div key={f.id} className="lift animate-grow rounded-2xl border bg-card p-5 shadow-soft" style={{ animationDelay: `${i * 70}ms` }}>
            <div className="flex items-start justify-between">
              <h3 className="text-lg font-semibold">{f.name}</h3>
              {f.is_demo && <StatusPill value="demo data" tone="warn" />}
            </div>
            <p className="mt-1 text-sm text-muted-foreground">{f.location ?? "No location"} · {f.total_area_acres ?? "?"} acres</p>
            <p className="mt-1 font-tech text-xs text-muted-foreground">{f.latitude && f.longitude ? `${f.latitude}, ${f.longitude}` : "No coordinates"}</p>
            {canManage(session.roles) && f.owner_id === session.userId && <AddMember farmId={f.id} />}
          </div>
        ))}
      </div>
      <ResourceTable config={farmCfg} />
      <ResourceTable config={plotCfg} />
    </div>
  );
}

function AddMember({ farmId }: { farmId: string }) {
  const [email, setEmail] = useState("");
  const [copied, setCopied] = useState(false);
  const [inviteUrl, setInviteUrl] = useState<string | null>(null);
  const qc = useQueryClient();

  const addExisting = useMutation({
    mutationFn: () => rpc("add_farm_member_by_email", { _farm: farmId, _email: email }),
    onSuccess: () => { toast.success("Field worker added to farm"); setEmail(""); qc.invalidateQueries(); },
    onError: (e: Error) => toast.error(e.message),
  });
<<<<<<< Updated upstream

  const genInvite = useMutation({
    mutationFn: () => api<{ token: string; inviteUrl: string }>("/api/invites", { method: "POST", body: { farmId } }),
    onSuccess: (res) => {
      // Use current window origin (e.g. http://localhost:8080) so invite link always matches current port
      const url = typeof window !== "undefined"
        ? `${window.location.origin}/auth?invite=${res.token}`
        : res.inviteUrl;
      setInviteUrl(url);
      navigator.clipboard.writeText(url);
      setCopied(true);
      toast.success("Invite link generated & copied to clipboard!");
      setTimeout(() => setCopied(false), 3000);
    },
    onError: (e: Error) => toast.error(e.message),
  });

  return (
    <div className="mt-4 pt-3.5 border-t border-border/60 space-y-3">
      <div className="flex items-center justify-between">
        <span className="text-xs font-semibold text-foreground flex items-center gap-1.5">
          <UserPlus className="size-3.5 text-primary" /> Assign Field Worker
        </span>
      </div>

      {/* Option 1: Add already registered worker */}
      <form
        className="flex flex-col sm:flex-row gap-2"
        onSubmit={(e) => {
          e.preventDefault();
          addExisting.mutate();
        }}
      >
        <Input
          type="email"
          required
          placeholder="Existing user's email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className="h-8 text-xs flex-1 bg-background"
          aria-label="Field worker email"
        />
        <Button
          type="submit"
          size="sm"
          variant="secondary"
          className="h-8 px-3 text-xs font-semibold text-foreground shrink-0 border border-border"
          loading={addExisting.isPending}
        >
          <UserPlus className="size-3.5 mr-1" /> Add Existing
        </Button>
      </form>

      {/* Option 2: Generate link for new worker */}
      <div className="rounded-lg bg-muted/40 p-2.5 border border-border/50 space-y-2">
        <div className="flex items-center justify-between gap-2">
          <span className="text-[11px] text-muted-foreground">Or invite a new worker via link:</span>
          <Button
            type="button"
            size="sm"
            variant="default"
            className="h-7 px-2.5 text-xs font-medium gap-1 text-primary-foreground bg-primary hover:bg-primary/90 shrink-0"
            loading={genInvite.isPending}
            onClick={() => genInvite.mutate()}
          >
            {copied ? <Check className="size-3.5 text-lime" /> : <Link2 className="size-3.5" />}
            {copied ? "Copied!" : "Generate Invite Link"}
          </Button>
        </div>

        {inviteUrl && (
          <div className="flex items-center gap-2 pt-1 border-t border-border/40">
            <input
              type="text"
              readOnly
              value={inviteUrl}
              className="text-[11px] text-foreground font-mono bg-background border px-2 py-1 rounded w-full select-all"
            />
            <Button
              type="button"
              size="sm"
              variant="outline"
              className="h-7 px-2 text-xs shrink-0"
              onClick={() => {
                navigator.clipboard.writeText(inviteUrl);
                setCopied(true);
                toast.success("Copied to clipboard!");
                setTimeout(() => setCopied(false), 2000);
              }}
            >
              <Copy className="size-3" />
            </Button>
          </div>
        )}
      </div>
    </div>
=======
  return (
    <form className="mt-4 flex gap-2" onSubmit={(e) => { e.preventDefault(); m.mutate(); }}>
      <Input type="email" required placeholder="worker@email.com" value={email} onChange={(e) => setEmail(e.target.value)} className="h-9" aria-label="Field worker email" />
      <Button size="sm" variant="secondary" loading={m.isPending}><UserPlus /> Add</Button>
    </form>
>>>>>>> Stashed changes
  );
}
