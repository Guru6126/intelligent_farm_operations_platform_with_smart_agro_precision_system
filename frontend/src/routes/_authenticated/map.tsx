import { createFileRoute, ClientOnly } from "@tanstack/react-router";
import { lazy, Suspense } from "react";
import { Map as MapIcon } from "lucide-react";
import { PageHeader } from "@/components/AppShell";

const FarmMap = lazy(() => import("@/components/FarmMap-new"));

export const Route = createFileRoute("/_authenticated/map")({
  head: () => ({ meta: [{ title: "GIS Map — AgroPrecision" }, { name: "description", content: "See farms on a map and draw plot boundaries." }] }),
  component: MapPage,
});

function MapPage() {
  return (
    <div className="space-y-6">
      <PageHeader icon={MapIcon} title="GIS Map" subtitle="Farms by location. Pick a plot and click on the map to draw its boundary." />
      <ClientOnly fallback={<div className="h-[560px] animate-pulse rounded-2xl border bg-muted" />}>
        <Suspense fallback={<div className="h-[560px] animate-pulse rounded-2xl border bg-muted" />}>
          <FarmMap />
        </Suspense>
      </ClientOnly>
    </div>
  );
}
