
import { useMemo, useState } from "react";
import {
  MapContainer,
  TileLayer,
  Marker,
  Popup,
  Polygon,
  Polyline,
  useMapEvents,
  CircleMarker,
} from "react-leaflet";
import L from "leaflet";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { useFarm } from "@/lib/farm-context";
import { useRows } from "@/components/ResourceTable";
import { updateRow } from "@/services/crud";
import { canManage } from "@/lib/auth";
import type { Plot } from "@/lib/types";
import {
  calculateAreaAcres,
  calculateAreaHectares,
} from "@/lib/areaCalculator";

const pin = L.divIcon({
  className: "",
  html: '<div style="width:18px;height:18px;border-radius:50%;background:var(--primary);border:3px solid var(--lime);box-shadow:0 0 12px var(--lime)"></div>',
  iconSize: [18, 18],
  iconAnchor: [9, 9],
});

type LatLng = [number, number];

function ringOf(g: unknown): LatLng[] | null {
  const coords = (
    g as { coordinates?: number[][][] } | null
  )?.coordinates?.[0];

  return Array.isArray(coords)
    ? coords.map(([lng, lat]) => [lat, lng] as LatLng)
    : null;
}

function ClickCapture({
  onClick,
}: {
  onClick: (point: LatLng) => void;
}) {
  useMapEvents({
    click: (event) => {
      onClick([event.latlng.lat, event.latlng.lng]);
    },
  });

  return null;
}

export default function FarmMap() {
  const { farms, session, currentFarm } = useFarm();
  const plots = useRows<Plot>("plots").data ?? [];
  const queryClient = useQueryClient();

  const [plotId, setPlotId] = useState("");
  const [points, setPoints] = useState<LatLng[]>([]);

  const located = farms.filter(
    (farm) => farm.latitude != null && farm.longitude != null,
  );

  const center: LatLng = useMemo(() => {
    const farm =
      currentFarm?.latitude != null ? currentFarm : located[0];

    return farm
      ? [Number(farm.latitude), Number(farm.longitude)]
      : [20.59, 78.96];
  }, [currentFarm, located]);

  const areaAcres = useMemo(
    () => calculateAreaAcres(points),
    [points],
  );

  const areaHectares = useMemo(
    () => calculateAreaHectares(points),
    [points],
  );

  const save = useMutation({
    mutationFn: () => {
      if (points.length < 3) {
        throw new Error("Add at least three points to draw a boundary.");
      }

      const ring = [
        ...points,
        points[0],
      ].map(([lat, lng]) => [lng, lat]);

      return updateRow("plots", plotId, {
        boundary_geojson: {
          type: "Polygon",
          coordinates: [ring],
        },
      });
    },
    onSuccess: () => {
      toast.success("Plot boundary saved");
      setPoints([]);
      setPlotId("");
      queryClient.invalidateQueries();
    },
    onError: (error: Error) => {
      toast.error(error.message);
    },
  });

  const manage = canManage(session.roles);

  return (
    <div className="space-y-4">
      {manage && (
        <div className="flex flex-wrap items-center gap-3 rounded-2xl border bg-card p-4 shadow-soft">
          <select
            aria-label="Plot to draw"
            value={plotId}
            onChange={(event) => {
              setPlotId(event.target.value);
              setPoints([]);
            }}
            className="h-9 rounded-lg border bg-card px-3 text-sm"
          >
            <option value="">Choose a plot to draw…</option>

            {plots.map((plot) => (
              <option key={plot.id} value={plot.id}>
                {plot.name}
              </option>
            ))}
          </select>

          <div className="text-sm text-muted-foreground">
            {plotId
              ? `${points.length} boundary point${points.length === 1 ? "" : "s"}`
              : "Select a plot first"}

            {points.length >= 3 && (
              <div className="mt-1 font-medium text-foreground">
                Estimated area: {areaAcres.toFixed(3)} acres
                {" "}({areaHectares.toFixed(3)} hectares)
              </div>
            )}

            {plotId && points.length < 3 && (
              <div className="mt-1">
                Click the map to add at least 3 corners.
              </div>
            )}
          </div>

          <div className="ml-auto flex gap-2">
            <Button
              size="sm"
              variant="outline"
              disabled={points.length === 0}
              onClick={() => setPoints((previous) => previous.slice(0, -1))}
            >
              Undo
            </Button>

            <Button
              size="sm"
              variant="outline"
              disabled={points.length === 0}
              onClick={() => setPoints([])}
            >
              Clear
            </Button>

            <Button
              size="sm"
              disabled={!plotId || points.length < 3}
              loading={save.isPending}
              onClick={() => save.mutate()}
            >
              Save boundary
            </Button>
          </div>
        </div>
      )}

      <div className="h-[560px] overflow-hidden rounded-2xl border shadow-soft">
        <MapContainer
          key={center.join(",")}
          center={center}
          zoom={located.length ? 14 : 5}
          className="h-full w-full"
        >
          <TileLayer
            attribution='&copy; OpenStreetMap contributors'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />

          {located.map((farm) => (
            <Marker
              key={farm.id}
              position={[
                Number(farm.latitude),
                Number(farm.longitude),
              ]}
              icon={pin}
            >
              <Popup>
                <strong>{farm.name}</strong>
                <br />
                {farm.location ?? ""}
                <br />
                {farm.total_area_acres ?? "?"} acres
              </Popup>
            </Marker>
          ))}

          {plots.map((plot) => {
            const ring = ringOf(plot.boundary_geojson);

            if (!ring || ring.length < 3) {
              return null;
            }

            return (
              <Polygon
                key={plot.id}
                positions={ring}
                pathOptions={{
                  color: "var(--primary)",
                  fillColor: "var(--lime)",
                  fillOpacity: 0.25,
                }}
              >
                <Popup>
                  <strong>{plot.name}</strong>
                  <br />
                  Area: {plot.area_acres ?? "Not calculated"} acres
                </Popup>
              </Polygon>
            );
          })}

          {plotId && (
            <ClickCapture
              onClick={(point) => {
                setPoints((previous) => [...previous, point]);
              }}
            />
          )}

          {points.length > 1 && (
            <Polyline
              positions={[
                ...points,
                ...(points.length > 2 ? [points[0]] : []),
              ]}
              pathOptions={{
                color: "var(--lime)",
                dashArray: "6 6",
              }}
            />
          )}

          {points.map((point, index) => (
            <CircleMarker
              key={`${point[0]}-${point[1]}-${index}`}
              center={point}
              radius={5}
              pathOptions={{ color: "var(--lime)" }}
            >
              <Popup>Boundary point {index + 1}</Popup>
            </CircleMarker>
          ))}
        </MapContainer>
      </div>

      {!located.length && (
        <p className="text-sm text-muted-foreground">
          Add latitude and longitude to a farm to see it on the map.
        </p>
      )}

      {plotId && points.length >= 3 && (
        <div className="rounded-xl border bg-card p-4">
          <h3 className="font-semibold">Boundary area estimate</h3>
          <div className="mt-2 grid grid-cols-2 gap-4">
            <div>
              <p className="text-sm text-muted-foreground">Acres</p>
              <p className="text-xl font-semibold">
                {areaAcres.toFixed(3)}
              </p>
            </div>
            <div>
              <p className="text-sm text-muted-foreground">Hectares</p>
              <p className="text-xl font-semibold">
                {areaHectares.toFixed(3)}
              </p>
            </div>
          </div>
          <p className="mt-2 text-xs text-muted-foreground">
            This is an approximate area calculated from the boundary
            coordinates, not a survey-grade measurement.
          </p>
        </div>
      )}
    </div>
  );
}
