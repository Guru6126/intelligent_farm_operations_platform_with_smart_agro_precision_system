import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Tractor, Leaf, UserPlus } from "lucide-react";
import { toast } from "sonner";
import { PageHeader } from "@/components/AppShell";
import { ResourceTable, StatusPill, type ResourceConfig } from "@/components/ResourceTable";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useFarm, type Farm } from "@/lib/farm-context";
import { canManage } from "@/lib/auth";
import { rpc } from "@/services/crud";
import type { Plot } from "@/lib/types";

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
  table: "plots", title: "Plots", singular: "Plot",
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

function FarmName({ id }: { id: string }) {
  const { farms } = useFarm();
  return <>{farms.find((f) => f.id === id)?.name ?? "—"}</>;
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
  const qc = useQueryClient();
  const m = useMutation({
    mutationFn: () => rpc("add_farm_member_by_email", { _farm: farmId, _email: email }),
    onSuccess: () => { toast.success("Field worker added to farm"); setEmail(""); qc.invalidateQueries(); },
    onError: (e: Error) => toast.error(e.message),
  });
  return (
    <form className="mt-4 flex gap-2" onSubmit={(e) => { e.preventDefault(); m.mutate(); }}>
      <Input type="email" required placeholder="worker@email.com" value={email} onChange={(e) => setEmail(e.target.value)} className="h-9" aria-label="Field worker email" />
      <Button size="sm" variant="secondary" loading={m.isPending}><UserPlus /> Add</Button>
    </form>
  );
}
