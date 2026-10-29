import { createFileRoute, Link } from "@tanstack/react-router";
import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  Tractor,
  Leaf,
  UserPlus,
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
import { toast } from "sonner";
import { PageHeader, StatCard } from "@/components/AppShell";
import { ResourceTable, StatusPill, useRows, type ResourceConfig } from "@/components/ResourceTable";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useFarm, type Farm } from "@/lib/farm-context";
import { canManage } from "@/lib/auth";
import { rpc } from "@/services/crud";
import type { Plot, Crop } from "@/lib/types";
import { cropCfg, STAGES } from "./crops";

export const Route = createFileRoute("/_authenticated/farms")({
  head: () => ({
    meta: [
      { title: "Farms & Plots — AgroPrecision" },
      { name: "description", content: "Manage farms, plots, and cultivate crops." },
    ],
  }),
  component: FarmsPage,
});

const farmCfg: ResourceConfig<Farm & Record<string, unknown>> = {
  table: "farms",
  title: "Farms Registry",
  singular: "Farm",
  farmScoped: false,
  orderBy: "created_at",
  fields: [
    { key: "name", label: "Farm name", required: true },
    { key: "location", label: "Location / village" },
    { key: "latitude", label: "Latitude", type: "number" },
    { key: "longitude", label: "Longitude", type: "number" },
    { key: "total_area_acres", label: "Total area (acres)", type: "number" },
    { key: "soil_type", label: "Soil type" },
  ],
  columns: [
    {
      key: "name",
      label: "Name",
      render: (r) => (
        <span className="font-medium">
          {r.name} {r.is_demo && <StatusPill value="demo" tone="warn" />}
        </span>
      ),
    },
    { key: "location", label: "Location" },
    { key: "total_area_acres", label: "Acres" },
    { key: "soil_type", label: "Soil" },
  ],
};

function FarmName({ id }: { id: string }) {
  const { farms } = useFarm();
  return <>{farms.find((f) => f.id === id)?.name ?? "—"}</>;
}

function PlotCropBadge({ plotId }: { plotId: string }) {
  const { data: crops = [] } = useRows<Crop>("crops");
  const plotCrops = crops.filter((c) => c.plot_id === plotId);

  if (plotCrops.length === 0) {
    return <span className="text-xs text-muted-foreground italic">No crop (fallow)</span>;
  }

  return (
    <div className="flex flex-col gap-1.5 py-0.5">
      {plotCrops.map((c) => (
        <div key={c.id} className="flex flex-wrap items-center gap-1.5">
          <span className="inline-flex items-center gap-1 font-medium text-foreground text-xs sm:text-sm">
            <Sprout className="size-3.5 text-primary" />
            {c.crop_name}
          </span>
          {c.variety && <span className="text-xs text-muted-foreground">({c.variety})</span>}
          <StatusPill
            value={c.stage}
            tone={
              c.stage === "harvested"
                ? "muted"
                : c.stage === "flowering" || c.stage === "harvest_ready"
                  ? "warn"
                  : "good"
            }
          />
        </div>
      ))}
    </div>
  );
}

function PlotHarvestDue({ plotId }: { plotId: string }) {
  const { data: crops = [] } = useRows<Crop>("crops");
  const plotCrops = crops.filter((c) => c.plot_id === plotId);

  if (plotCrops.length === 0) {
    return <span className="text-xs text-muted-foreground">—</span>;
  }

  return (
    <div className="flex flex-col gap-1 text-xs">
      {plotCrops.map((c) => (
        <div key={c.id} className="flex items-center gap-1 text-muted-foreground">
          <Clock className="size-3 text-muted-foreground/80" />
          <span>{c.expected_harvest ? `Due: ${c.expected_harvest}` : "Harvest date TBD"}</span>
          {c.season && <span className="text-[11px] opacity-75">· {c.season}</span>}
        </div>
      ))}
    </div>
  );
}

const plotCfg: ResourceConfig<Plot> = {
  table: "plots",
  title: "Plots & Assigned Crops",
  singular: "Plot",
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
    {
      key: "crop_details",
      label: "Current Crop",
      render: (r) => <PlotCropBadge plotId={r.id} />,
    },
    {
      key: "harvest_window",
      label: "Harvest Due",
      render: (r) => <PlotHarvestDue plotId={r.id} />,
    },
    {
      key: "boundary_geojson",
      label: "Boundary",
      render: (r) =>
        r.boundary_geojson ? (
          <Link
            to="/map"
            className="inline-flex items-center gap-1 text-xs font-medium text-emerald-600 dark:text-emerald-400 hover:underline"
            title="View plot boundary on GIS Map"
          >
            <StatusPill value="mapped" tone="good" />
            <ArrowUpRight className="size-3" />
          </Link>
        ) : (
          <Link
            to="/map"
            className="inline-flex items-center gap-1.5 group"
            title="Click to plot and mark boundary directly on map"
          >
            <StatusPill value="not mapped" tone="warn" />
            <span className="inline-flex items-center gap-0.5 text-xs font-medium text-primary group-hover:underline">
              <Compass className="size-3" /> Mark on Map <ArrowUpRight className="size-3" />
            </span>
          </Link>
        ),
    },
  ],
};

function FarmsPage() {
  const { farms, session } = useFarm();
  const { data: plots = [] } = useRows<Plot>("plots");
  const { data: crops = [] } = useRows<Crop>("crops");

  // Summary computations
  const totalPlotAcres = plots.reduce((acc, p) => acc + Number(p.area_acres ?? 0), 0);
  const plotsWithCrops = plots.filter((p) => crops.some((c) => c.plot_id === p.id && c.stage !== "harvested"));
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
          value={plots.length}
          hint={`${totalPlotAcres.toFixed(1)} acres divided`}
          delay={60}
        />
        <StatCard
          icon={Sprout}
          label="Cultivated Plots"
          value={plotsWithCrops.length}
          hint={
            plots.length > 0
              ? `${Math.round((plotsWithCrops.length / plots.length) * 100)}% plots cultivated`
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
            const farmPlots = plots.filter((p) => p.farm_id === f.id);
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
                {plots.length}
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
          <PlotCropGrid plots={plots} crops={crops} farms={farms} />
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
}

/** Visual grid displaying each plot along with its assigned crop lifecycle details */
function PlotCropGrid({
  plots,
  crops,
  farms,
}: {
  plots: Plot[];
  crops: Crop[];
  farms: Farm[];
}) {
  if (plots.length === 0) {
    return null;
  }

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <h3 className="text-base font-semibold text-foreground flex items-center gap-2">
          <Sprout className="size-4 text-primary" /> Plot Cultivation & Crop Progress
        </h3>
        <span className="text-xs text-muted-foreground font-mono">
          {plots.length} plot{plots.length === 1 ? "" : "s"} monitored
        </span>
      </div>

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {plots.map((plot, i) => {
          const farm = farms.find((f) => f.id === plot.farm_id);
          const plotCrops = crops.filter((c) => c.plot_id === plot.id);
          const activeCrop = plotCrops.find((c) => c.stage !== "harvested") || plotCrops[0];

          return (
            <div
              key={plot.id}
              className="lift animate-grow rounded-2xl border bg-card p-5 shadow-soft flex flex-col justify-between"
              style={{ animationDelay: `${i * 60}ms` }}
            >
              <div>
                {/* Plot Title & Farm Info */}
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <h4 className="font-semibold text-base text-foreground flex items-center gap-1.5">
                      {plot.name}
                    </h4>
                    <p className="text-xs text-muted-foreground flex items-center gap-1 mt-0.5">
                      <MapPin className="size-3 text-muted-foreground" />
                      {farm?.name ?? "Unassigned Farm"}
                    </p>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <Badge variant="outline" className="text-xs font-mono font-normal">
                      {plot.area_acres ?? "—"} acres
                    </Badge>
                    {plot.soil_ph && (
                      <Badge variant="secondary" className="text-xs font-mono font-normal">
                        pH {plot.soil_ph}
                      </Badge>
                    )}
                  </div>
                </div>

                {/* Crop Cultivation Details */}
                <div className="mt-4 rounded-xl border bg-secondary/30 p-3.5">
                  {activeCrop ? (
                    <div className="space-y-3">
                      <div className="flex items-center justify-between gap-2">
                        <div className="flex items-center gap-2">
                          <div className="grid size-8 place-items-center rounded-lg bg-primary/10 text-primary">
                            <Sprout className="size-4" />
                          </div>
                          <div>
                            <span className="font-semibold text-sm text-foreground">
                              {activeCrop.crop_name}
                            </span>
                            {activeCrop.variety && (
                              <span className="ml-1 text-xs text-muted-foreground">
                                ({activeCrop.variety})
                              </span>
                            )}
                            {activeCrop.season && (
                              <p className="text-[11px] text-muted-foreground">{activeCrop.season}</p>
                            )}
                          </div>
                        </div>
                        <StatusPill
                          value={activeCrop.stage}
                          tone={
                            activeCrop.stage === "harvested"
                              ? "muted"
                              : activeCrop.stage === "flowering" || activeCrop.stage === "harvest_ready"
                                ? "warn"
                                : "good"
                          }
                        />
                      </div>

                      {/* 6-Stage Progress Stepper */}
                      <div>
                        <div className="flex items-center justify-between text-[11px] text-muted-foreground mb-1">
                          <span className="font-medium capitalize text-foreground">
                            Stage: {activeCrop.stage.replace(/_/g, " ")}
                          </span>
                          <span className="font-mono text-[10px]">
                            {STAGES.indexOf(activeCrop.stage) + 1}/{STAGES.length}
                          </span>
                        </div>
                        <ol className="flex items-center gap-1" aria-label={`Stage: ${activeCrop.stage}`}>
                          {STAGES.map((s, si) => {
                            const activeIdx = STAGES.indexOf(activeCrop.stage);
                            const isCompleted = si <= activeIdx;
                            const isCurrent = si === activeIdx;
                            return (
                              <li key={s} className="flex-1">
                                <div
                                  className={`h-1.5 rounded-full transition-all duration-500 ${isCompleted
                                      ? isCurrent
                                      : "bg-muted"
                                    } ${isCompleted ? "bg-primary" : ""}`}
                                  title={s.replace(/_/g, " ")}
                                />
                              </li>
                            );
                          })}
                        </ol>
                      </div>

                      {/* Planting & Harvest Dates */}
                      <div className="grid grid-cols-2 gap-2 text-xs pt-1 border-t border-border/60">
                        <div>
                          <span className="text-[10px] uppercase tracking-wider text-muted-foreground block">
                            Sowing Date
                          </span>
                          <span className="font-mono text-foreground">
                            {activeCrop.sowing_date ?? "Not recorded"}
                          </span>
                        </div>
                        <div>
                          <span className="text-[10px] uppercase tracking-wider text-muted-foreground block">
                            Expected Harvest
                          </span>
                          <span className="font-mono font-medium text-foreground">
                            {activeCrop.expected_harvest ?? "TBD"}
                          </span>
                        </div>
                      </div>
                    </div>
                  ) : (
                    <div className="text-center py-3">
                      <p className="text-xs text-muted-foreground italic">
                        No crop currently planted in this plot (Fallow land).
                      </p>
                      <p className="text-[11px] text-muted-foreground/80 mt-1">
                        Use the Crop Plans tab to assign a crop to this plot.
                      </p>
                    </div>
                  )}
                </div>
              </div>

              {/* Bottom Details: GIS Boundary & Link */}
              <div className="mt-3 flex items-center justify-between text-xs pt-2.5 border-t border-border/50">
                <span className="flex items-center gap-1.5">
                  {plot.boundary_geojson ? (
                    <span className="inline-flex items-center gap-1 font-medium text-emerald-600 dark:text-emerald-400">
                      <CheckCircle2 className="size-3.5" />
                      GIS Mapped
                    </span>
                  ) : (
                    <span className="inline-flex items-center gap-1 font-medium text-amber-600 dark:text-amber-500">
                      <AlertCircle className="size-3.5" />
                      Boundary pending
                    </span>
                  )}
                </span>
                {plot.boundary_geojson ? (
                  <Button asChild size="sm" variant="ghost" className="h-7 px-2 text-xs text-primary hover:text-primary gap-1 font-medium">
                    <Link to="/map">
                      View on Map <ArrowUpRight className="size-3" />
                    </Link>
                  </Button>
                ) : (
                  <Button
                    asChild
                    size="sm"
                    className="h-7 px-2.5 text-xs gap-1.5 font-medium bg-primary text-primary-foreground hover:bg-primary/90 shadow-xs"
                  >
                    <Link
                      to="/map"
                      title={`Mark boundary on map for ${plot.name} (${farm?.name ?? "Farm"})`}
                    >
                      <Compass className="size-3.5" />
                      Mark Boundary <ArrowUpRight className="size-3" />
                    </Link>
                  </Button>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

function AddMember({ farmId }: { farmId: string }) {
  const [email, setEmail] = useState("");
  const qc = useQueryClient();
  const m = useMutation({
    mutationFn: () => rpc("add_farm_member_by_email", { _farm: farmId, _email: email }),
    onSuccess: () => {
      toast.success("Field worker added to farm");
      setEmail("");
      qc.invalidateQueries();
    },
    onError: (e: Error) => toast.error(e.message),
  });

  return (
    <form
      className="mt-4 flex gap-2"
      onSubmit={(e) => {
        e.preventDefault();
        m.mutate();
      }}
    >
      <Input
        type="email"
        required
        placeholder="worker@email.com"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        className="h-9"
        aria-label="Field worker email"
      />
      <Button size="sm" variant="secondary" loading={m.isPending}>
        <UserPlus /> Add
      </Button>
    </form>
  );
}
