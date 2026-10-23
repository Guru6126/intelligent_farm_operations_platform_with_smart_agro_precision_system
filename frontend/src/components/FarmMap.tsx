import { useMemo, useState } from "react";
import { MapContainer, TileLayer, Marker, Popup, Polygon, Polyline, useMapEvents, CircleMarker } from "react-leaflet";
import L from "leaflet";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { useFarm } from "@/lib/farm-context";
import { useRows } from "@/components/ResourceTable";
import { updateRow } from "@/services/crud";
import { canManage } from "@/lib/auth";
import type { Plot } from "@/lib/types";

const pin = L.divIcon({
  className: "",
  html: '<div style="width:18px;height:18px;border-radius:50%;background:var(--primary);border:3px solid var(--lime);box-shadow:0 0 12px var(--lime)"></div>',
  iconSize: [18, 18],
  iconAnchor: [9, 9],
});

type LatLng = [number, number];

function ringOf(g: unknown): LatLng[] | null {
  const coords = (g as { coordinates?: number[][][] } | null)?.coordinates?.[0];
  return Array.isArray(coords) ? coords.map(([lng, lat]) => [lat, lng] as LatLng) : null;
}

function ClickCapture({ onClick }: { onClick: (p: LatLng) => void }) {
  useMapEvents({ click: (e) => onClick([e.latlng.lat, e.latlng.lng]) });
  return null;
}

export default function FarmMap() {
  const { farms, session, currentFarm } = useFarm();
  const plots = useRows<Plot>("plots").data ?? [];
  const qc = useQueryClient();
  const [plotId, setPlotId] = useState("");
  const [points, setPoints] = useState<LatLng[]>([]);
  const located = farms.filter((f) => f.latitude != null && f.longitude != null);
  const center: LatLng = useMemo(() => {
    const f = currentFarm?.latitude != null ? currentFarm : located[0];
    return f ? [Number(f.latitude), Number(f.longitude)] : [20.59, 78.96];
  }, [currentFarm, located]);

  const save = useMutation({
    mutationFn: () => {
      const ring = [...points, points[0]].map(([lat, lng]) => [lng, lat]);
      return updateRow("plots", plotId, { boundary_geojson: { type: "Polygon", coordinates: [ring] } });
    },
    onSuccess: () => { toast.success("Plot boundary saved"); setPoints([]); qc.invalidateQueries(); },
    onError: (e: Error) => toast.error(e.message),
  });

  const manage = canManage(session.roles);

  return (
    <div className="space-y-4">
      {manage && (
        <div className="flex flex-wrap items-center gap-3 rounded-2xl border bg-card p-4 shadow-soft">
          <select aria-label="Plot to draw" value={plotId} onChange={(e) => { setPlotId(e.target.value); setPoints([]); }} className="h-9 rounded-lg border bg-card px-3 text-sm">
            <option value="">Choose a plot to draw…</option>
            {plots.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
          </select>
          <span className="text-sm text-muted-foreground">{plotId ? `${points.length} point${points.length === 1 ? "" : "s"} — click the map to add corners` : "Select a plot first"}</span>
          <div className="ml-auto flex gap-2">
            <Button size="sm" variant="outline" disabled={!points.length} onClick={() => setPoints(points.slice(0, -1))}>Undo</Button>
            <Button size="sm" variant="outline" disabled={!points.length} onClick={() => setPoints([])}>Clear</Button>
            <Button size="sm" disabled={!plotId || points.length < 3} loading={save.isPending} onClick={() => save.mutate()}>Save boundary</Button>
          </div>
        </div>
      )}
      <div className="h-[560px] overflow-hidden rounded-2xl border shadow-soft">
        <MapContainer key={center.join(",")} center={center} zoom={located.length ? 14 : 5} className="h-full w-full">
          <TileLayer attribution='&copy; OpenStreetMap contributors' url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
          {located.map((f) => (
            <Marker key={f.id} position={[Number(f.latitude), Number(f.longitude)]} icon={pin}>
              <Popup><strong>{f.name}</strong><br />{f.location ?? ""}<br />{f.total_area_acres ?? "?"} acres</Popup>
            </Marker>
          ))}
          {plots.map((p) => {
            const ring = ringOf(p.boundary_geojson);
            return ring ? (
              <Polygon key={p.id} positions={ring} pathOptions={{ color: "var(--primary)", fillColor: "var(--lime)", fillOpacity: 0.25 }}>
                <Popup>{p.name} · {p.area_acres ?? "?"} acres</Popup>
              </Polygon>
            ) : null;
          })}
          {plotId && <ClickCapture onClick={(pt) => setPoints((s) => [...s, pt])} />}
          {points.length > 1 && <Polyline positions={[...points, ...(points.length > 2 ? [points[0]] : [])]} pathOptions={{ color: "var(--lime)", dashArray: "6 6" }} />}
          {points.map((pt, i) => <CircleMarker key={i} center={pt} radius={5} pathOptions={{ color: "var(--lime)" }} />)}
        </MapContainer>
      </div>
      {!located.length && <p className="text-sm text-muted-foreground">Add latitude and longitude to a farm to see it on the map.</p>}
    </div>
  );
}
