import { useMemo, useState } from "react";
import {
  MapContainer,
  TileLayer,
  Marker,
  Popup,
  Polygon,
  Polyline,
  CircleMarker,
  useMap,
  useMapEvents,
} from "react-leaflet";
import L from "leaflet";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { useFarm } from "@/lib/farm-context";
import { useRows } from "@/components/ResourceTable";
import { insertRow, listRows } from "@/services/crud";
import { canManage } from "@/lib/auth";
import type { Plot, Crop } from "@/lib/types";
import { calculateAreaAcres, calculateAreaHectares } from "@/lib/areaCalculator";

type LatLng = [number, number];
type CropName = "Rice" | "Wheat" | "Cotton" | "Maize";

const EMPTY_PLOTS: Plot[] = [];
const EMPTY_CROPS: Crop[] = [];

const CROP_OPTIONS: { name: CropName; color: string }[] = [
  { name: "Rice", color: "#16a34a" },
  { name: "Wheat", color: "#eab308" },
  { name: "Cotton", color: "#8b5cf6" },
  { name: "Maize", color: "#f97316" },
];

const DEFAULT_CENTER: LatLng = [16.83037, 79.57004];

const farmPin = L.divIcon({
  className: "",
  html: `<div style="width:18px;height:18px;border-radius:50%;background:#16a34a;border:3px solid white;box-shadow:0 0 0 4px #16a34a55"></div>`,
  iconSize: [18, 18],
  iconAnchor: [9, 9],
});

function getCropColor(name?: string | null): string {
  return (
    CROP_OPTIONS.find((crop) => crop.name.toLowerCase() === name?.toLowerCase())?.color ?? "#64748b"
  );
}

function getCropForPlot(plot: Plot, crops: Crop[]) {
  return crops.find((crop) => String(crop.plot_id) === String(plot.id));
}

function getBoundary(geometry: unknown): LatLng[] | null {
  const coordinates = (
    geometry as {
      type?: string;
      coordinates?: number[][][];
    } | null
  )?.coordinates?.[0];

  if (!Array.isArray(coordinates)) return null;

  return coordinates
    .map(([lng, lat]) => [Number(lat), Number(lng)] as LatLng)
    .filter(([lat, lng]) => Number.isFinite(lat) && Number.isFinite(lng));
}

function MapClickCapture({
  enabled,
  onPoint,
}: {
  enabled: boolean;
  onPoint: (point: LatLng) => void;
}) {
  useMapEvents({
    click(event) {
      if (enabled) {
        onPoint([event.latlng.lat, event.latlng.lng]);
      }
    },
  });

  return null;
}

function MapFocus({ position }: { position: LatLng }) {
  const map = useMap();

  map.setView(position, map.getZoom() < 13 ? 14 : map.getZoom(), {
    animate: true,
  });

  return null;
}

export default function FarmMap() {
  const { farms, session, currentFarm } = useFarm();

  const plotsQuery = useRows<Plot>("plots");
  const cropsQuery = useRows<Crop>("crops");

  // Stable empty arrays prevent unnecessary useMemo dependency changes.
  const plots = plotsQuery.data ?? EMPTY_PLOTS;
  const crops = cropsQuery.data ?? EMPTY_CROPS;

  const queryClient = useQueryClient();
  const manage = canManage(session.roles);

  const [farmId, setFarmId] = useState(String(currentFarm?.id ?? farms[0]?.id ?? ""));
  const [points, setPoints] = useState<LatLng[]>([]);
  const [plotName, setPlotName] = useState("");
  const [cropName, setCropName] = useState<CropName>("Rice");
  const [soilPH, setSoilPH] = useState("6.5");
  const [soilType, setSoilType] = useState("Red Sandy Soil");
  const [selectedPlotId, setSelectedPlotId] = useState("");

  const selectedFarm = farms.find((farm) => String(farm.id) === farmId) ?? currentFarm ?? farms[0];

  const farmPlots = useMemo(() => {
    const filtered = plots.filter((plot) => !farmId || String(plot.farm_id) === farmId);
    const map = new Map<string, Plot>();
    for (const p of filtered) {
      const key = `${p.farm_id}:${String(p.name ?? "").trim().toLowerCase()}`;
      const existing = map.get(key);
      if (!existing || (p.boundary_geojson && !existing.boundary_geojson)) {
        map.set(key, p);
      }
    }
    return Array.from(map.values());
  }, [plots, farmId]);

  const locatedFarms = useMemo(
    () =>
      farms.filter(
        (farm) =>
          farm.latitude != null &&
          farm.longitude != null &&
          Number.isFinite(Number(farm.latitude)) &&
          Number.isFinite(Number(farm.longitude)),
      ),
    [farms],
  );

  const center: LatLng =
    selectedFarm?.latitude != null && selectedFarm?.longitude != null
      ? [Number(selectedFarm.latitude), Number(selectedFarm.longitude)]
      : DEFAULT_CENTER;

  const areaAcres = useMemo(() => calculateAreaAcres(points), [points]);

  const areaHectares = useMemo(() => calculateAreaHectares(points), [points]);

  const selectedPlot = farmPlots.find((plot) => String(plot.id) === selectedPlotId);

  const selectedCrop = selectedPlot ? getCropForPlot(selectedPlot, crops) : undefined;

  const savePlot = useMutation({
    mutationFn: async () => {
      if (!selectedFarm) {
        throw new Error("Please create or select a farm first.");
      }

      if (points.length !== 4) {
        throw new Error("Mark exactly four boundary points.");
      }

      const trimmedName = plotName.trim();
      if (!trimmedName) {
        throw new Error("Enter a plot name.");
      }

      const duplicate = plots.some(
        (p) =>
          String(p.farm_id) === String(selectedFarm.id) &&
          p.name.trim().toLowerCase() === trimmedName.toLowerCase(),
      );
      if (duplicate) {
        throw new Error(
          `A plot named "${trimmedName}" already exists on ${selectedFarm.name}. Duplication of plots is not allowed.`,
        );
      }

      const pH = Number(soilPH);

      if (soilPH.trim() === "" || !Number.isFinite(pH) || pH < 0 || pH > 14) {
        throw new Error("Enter a valid soil pH between 0 and 14.");
      }

      // GeoJSON uses [longitude, latitude].
      const ring = [...points.map(([lat, lng]) => [lng, lat]), [points[0][1], points[0][0]]];

      const boundary = {
        type: "Polygon",
        coordinates: [ring],
      };

      const createdPlot = await insertRow<Plot>("plots", {
        farm_id: selectedFarm.id,
        name: trimmedName,
        area_acres: Number(areaAcres.toFixed(4)),
        soil_ph: pH,
        soil_type: soilType,
        boundary_geojson: boundary,
      });

      const plotId = createdPlot?.id;
      if (!plotId) {
        throw new Error("Plot created, but its ID was missing.");
      }

      await insertRow("crops", {
        farm_id: selectedFarm.id,
        plot_id: plotId,
        crop_name: cropName,
        variety: null,
        season: null,
        stage: "Planned",
        sowing_date: null,
        expected_harvest: null,
      });
    },

    onSuccess: async () => {
      toast.success("Plot and crop saved successfully.");

      setPoints([]);
      setPlotName("");
      setCropName("Rice");
      setSoilPH("6.5");
      setSoilType("Red Sandy Soil");
      setSelectedPlotId("");

      await queryClient.invalidateQueries();
    },

    onError: (error: Error) => {
      toast.error(error.message || "Could not save the plot.");
      void queryClient.invalidateQueries();
    },
  });

  function resetDrawing() {
    setPoints([]);
    setPlotName("");
    setCropName("Rice");
    setSoilPH("6.5");
    setSoilType("Red Sandy Soil");
    setSelectedPlotId("");
  }

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="text-2xl font-bold tracking-tight">GIS Farm Mapping</h2>
          <p className="mt-1 text-sm text-muted-foreground">
            View farm locations, explore crop plots, and draw farm boundaries.
          </p>
        </div>

        <Button variant="outline" onClick={resetDrawing} disabled={savePlot.isPending}>
          + New plot
        </Button>
      </div>

      {/* Summary cards */}
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
        <div className="rounded-xl border bg-card p-4">
          <p className="text-sm text-muted-foreground">All farms</p>
          <p className="mt-1 text-2xl font-bold">{farms.length}</p>
        </div>

        <div className="rounded-xl border bg-card p-4">
          <p className="text-sm text-muted-foreground">Mapped plots</p>
          <p className="mt-1 text-2xl font-bold">{plots.length}</p>
        </div>

        <div className="rounded-xl border bg-card p-4">
          <p className="text-sm text-muted-foreground">Crop types</p>
          <p className="mt-1 text-2xl font-bold">
            {new Set(crops.map((crop) => crop.crop_name)).size}
          </p>
        </div>
      </div>

      {/* Farm selection */}
      <div className="space-y-4 rounded-2xl border bg-card p-4">
        <label className="block space-y-1.5">
          <span className="text-sm font-medium">Select farm</span>
          <select
            className="h-10 w-full rounded-lg border bg-background px-3 text-sm"
            value={farmId}
            onChange={(event) => {
              setFarmId(event.target.value);
              resetDrawing();
            }}
          >
            <option value="">Choose a farm</option>
            {farms.map((farm) => (
              <option key={farm.id} value={String(farm.id)}>
                {farm.name}
              </option>
            ))}
          </select>
        </label>

        {selectedFarm && (
          <p className="text-sm text-muted-foreground">
            {selectedFarm.name} · {selectedFarm.location ?? "Location not specified"}
          </p>
        )}

        {manage && (
          <>
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <h3 className="font-semibold">Draw a new plot</h3>
                <p className="text-sm text-muted-foreground">
                  Click four corners of the plot on the map.
                </p>
              </div>
              <span className="rounded-full bg-muted px-3 py-1 text-sm">
                {points.length}/4 points
              </span>
            </div>

            <div className="flex flex-wrap gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={!points.length || savePlot.isPending}
                onClick={() => setPoints((previous) => previous.slice(0, -1))}
              >
                Undo point
              </Button>

              <Button
                variant="outline"
                size="sm"
                disabled={!points.length || savePlot.isPending}
                onClick={() => setPoints([])}
              >
                Clear points
              </Button>
            </div>

            <p className="text-sm text-muted-foreground">
              {points.length < 4
                ? `Click the map to add point ${points.length + 1} of 4.`
                : "Boundary complete. Enter the plot details below."}
            </p>

            {points.length >= 3 && (
              <div className="grid grid-cols-2 gap-3">
                <div className="rounded-lg border p-3">
                  <p className="text-sm text-muted-foreground">Area (acres)</p>
                  <p className="text-xl font-bold">{areaAcres.toFixed(3)}</p>
                </div>
                <div className="rounded-lg border p-3">
                  <p className="text-sm text-muted-foreground">Area (hectares)</p>
                  <p className="text-xl font-bold">{areaHectares.toFixed(3)}</p>
                </div>
              </div>
            )}

            {points.length === 4 && (
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <label className="space-y-1.5">
                  <span className="text-sm font-medium">Plot name *</span>
                  <input
                    className="h-10 w-full rounded-lg border bg-background px-3 text-sm"
                    value={plotName}
                    onChange={(event) => setPlotName(event.target.value)}
                    placeholder="e.g. Ramu Rice Plot"
                    maxLength={100}
                  />
                </label>

                <label className="space-y-1.5">
                  <span className="text-sm font-medium">Crop *</span>
                  <select
                    className="h-10 w-full rounded-lg border bg-background px-3 text-sm"
                    value={cropName}
                    onChange={(event) => setCropName(event.target.value as CropName)}
                  >
                    {CROP_OPTIONS.map((crop) => (
                      <option key={crop.name} value={crop.name}>
                        {crop.name}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="space-y-1.5">
                  <span className="text-sm font-medium">Soil pH</span>
                  <input
                    type="number"
                    min="0"
                    max="14"
                    step="0.1"
                    className="h-10 w-full rounded-lg border bg-background px-3 text-sm"
                    value={soilPH}
                    onChange={(event) => setSoilPH(event.target.value)}
                  />
                </label>

                <label className="space-y-1.5">
                  <span className="text-sm font-medium">Soil type</span>
                  <select
                    className="h-10 w-full rounded-lg border bg-background px-3 text-sm"
                    value={soilType}
                    onChange={(event) => setSoilType(event.target.value)}
                  >
                    <option>Red Sandy Soil</option>
                    <option>Sandy Loam</option>
                    <option>Black Soil</option>
                    <option>Alluvial Soil</option>
                    <option>Clay Soil</option>
                  </select>
                </label>

                <div className="flex items-center gap-2 text-sm md:col-span-2">
                  <span
                    className="h-4 w-4 rounded-sm"
                    style={{ backgroundColor: getCropColor(cropName) }}
                  />
                  {cropName} plots will use this colour on the map.
                </div>

                <div className="md:col-span-2">
                  <Button
                    className="w-full sm:w-auto"
                    disabled={
                      !selectedFarm || !plotName.trim() || points.length !== 4 || savePlot.isPending
                    }
                    loading={savePlot.isPending}
                    onClick={() => savePlot.mutate()}
                  >
                    Save plot and crop
                  </Button>
                </div>
              </div>
            )}
          </>
        )}
      </div>

      {/* Interactive map */}
      <div className="h-[560px] overflow-hidden rounded-2xl border shadow-sm">
        <MapContainer
          center={center}
          zoom={locatedFarms.length ? 14 : 11}
          className="h-full w-full"
          scrollWheelZoom
        >
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />

          <MapFocus position={center} />

          {/* All farms with coordinates */}
          {locatedFarms.map((farm) => (
            <Marker
              key={farm.id}
              position={[Number(farm.latitude), Number(farm.longitude)]}
              icon={farmPin}
              eventHandlers={{
                click: () => setFarmId(String(farm.id)),
              }}
            >
              <Popup>
                <strong>{farm.name}</strong>
                <br />
                {farm.location ?? "Location not specified"}
                <br />
                Area: {farm.total_area_acres ?? "—"} acres
                <br />
                <button type="button" onClick={() => setFarmId(String(farm.id))}>
                  Select this farm
                </button>
              </Popup>
            </Marker>
          ))}

          {/* Saved crop plots */}
          {farmPlots.map((plot) => {
            const boundary = getBoundary(plot.boundary_geojson);
            if (!boundary || boundary.length < 4) return null;

            const crop = getCropForPlot(plot, crops);
            const color = getCropColor(crop?.crop_name);

            return (
              <Polygon
                key={plot.id}
                positions={boundary}
                pathOptions={{
                  color,
                  fillColor: color,
                  fillOpacity: 0.35,
                  weight: String(plot.id) === selectedPlotId ? 4 : 2,
                }}
                eventHandlers={{
                  click: () => setSelectedPlotId(String(plot.id)),
                }}
              >
                <Popup>
                  <strong>{plot.name}</strong>
                  <br />
                  Crop: {crop?.crop_name ?? "Not assigned"}
                  <br />
                  Area: {plot.area_acres ?? "—"} acres
                  <br />
                  Soil pH: {plot.soil_ph ?? "—"}
                </Popup>
              </Polygon>
            );
          })}

          <MapClickCapture
            enabled={manage && points.length < 4 && !savePlot.isPending}
            onPoint={(point) =>
              setPoints((previous) => (previous.length < 4 ? [...previous, point] : previous))
            }
          />

          {points.length >= 2 && (
            <Polyline
              positions={points}
              pathOptions={{
                color: "#0f766e",
                weight: 3,
                dashArray: "6 6",
              }}
            />
          )}

          {points.length === 4 && (
            <Polygon
              positions={points}
              pathOptions={{
                color: getCropColor(cropName),
                fillColor: getCropColor(cropName),
                fillOpacity: 0.4,
                weight: 3,
              }}
            />
          )}

          {points.map((point, index) => (
            <CircleMarker
              key={`${point[0]}-${point[1]}-${index}`}
              center={point}
              radius={6}
              pathOptions={{
                color: "#115e59",
                fillColor: "#5eead4",
                fillOpacity: 1,
                weight: 2,
              }}
            >
              <Popup>Boundary point {index + 1} of 4</Popup>
            </CircleMarker>
          ))}
        </MapContainer>
      </div>

      {/* Crop legend */}
      <div className="rounded-2xl border bg-card p-4">
        <h3 className="font-semibold">Crop legend</h3>
        <div className="mt-3 flex flex-wrap gap-x-6 gap-y-3">
          {CROP_OPTIONS.map((crop) => (
            <div key={crop.name} className="flex items-center gap-2">
              <span className="h-4 w-4 rounded-sm" style={{ backgroundColor: crop.color }} />
              <span className="text-sm">{crop.name}</span>
              <span className="text-sm text-muted-foreground">
                {
                  crops.filter((item) => item.crop_name?.toLowerCase() === crop.name.toLowerCase())
                    .length
                }
              </span>
            </div>
          ))}
        </div>
      </div>

      {/* Selected plot information */}
      {selectedPlot && (
        <div className="rounded-2xl border bg-card p-5">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <p className="text-sm text-muted-foreground">Selected plot</p>
              <h3 className="mt-1 text-xl font-bold">{selectedPlot.name}</h3>
            </div>

            <span
              className="rounded-full px-3 py-1 text-sm font-medium text-white"
              style={{
                backgroundColor: getCropColor(selectedCrop?.crop_name),
              }}
            >
              {selectedCrop?.crop_name ?? "Crop not assigned"}
            </span>
          </div>

          <div className="mt-4 grid grid-cols-2 gap-4 sm:grid-cols-4">
            <div>
              <p className="text-sm text-muted-foreground">Area</p>
              <p className="font-semibold">{selectedPlot.area_acres ?? "—"} acres</p>
            </div>

            <div>
              <p className="text-sm text-muted-foreground">Hectares</p>
              <p className="font-semibold">
                {selectedPlot.area_acres != null
                  ? (Number(selectedPlot.area_acres) * 0.404686).toFixed(2)
                  : "—"}{" "}
                ha
              </p>
            </div>

            <div>
              <p className="text-sm text-muted-foreground">Soil pH</p>
              <p className="font-semibold">{selectedPlot.soil_ph ?? "—"}</p>
            </div>

            <div>
              <p className="text-sm text-muted-foreground">Soil type</p>
              <p className="font-semibold">{String(selectedPlot["soil_type"] ?? "Not recorded")}</p>
            </div>
          </div>
        </div>
      )}

      {!farms.length && (
        <div className="rounded-xl border border-dashed p-4 text-sm text-muted-foreground">
          No farms were returned by the app. Create a farm using the existing farm-management page
          before saving plots. The default map location is for demonstration only.
        </div>
      )}

      {farms.length > 0 && !locatedFarms.length && (
        <p className="text-sm text-muted-foreground">
          Farms exist, but none has usable coordinates. Add latitude and longitude through the
          existing farm-management workflow.
        </p>
      )}

      <p className="text-xs text-muted-foreground">
        Polygon area is an estimate, not a survey-grade measurement. Sample soil values should be
        verified before being treated as real farm data.
      </p>
    </div>
  );
}
