
import { useEffect, useMemo, useState } from "react";
import {
  CircleMarker,
  MapContainer,
  Polygon,
  Popup,
  TileLayer,
  Tooltip,
  useMap,
  useMapEvents,
} from "react-leaflet";
import type { LeafletMouseEvent } from "leaflet";

import { farms, initialFarmPlots } from "../data/farmData";
import { calculateAreaHectares } from "../services/areaCalculator";
import { CROP_TYPES } from "../types";
import type { CropType, FarmPlot, GeoPoint } from "../types";
import "./GISMapping.css";

const STORAGE_KEY = "farmverse-gis-plots-v1";

const CROP_COLORS: Record<CropType, string> = {
  Rice: "#16a34a",
  Wheat: "#eab308",
  Cotton: "#9333ea",
  Maize: "#ea580c",
};

function createInitialPlots(): FarmPlot[] {
  return initialFarmPlots.map((plot) => ({
    ...plot,
    areaHectares: calculateAreaHectares(plot.boundary),
  }));
}

function loadPlots(): FarmPlot[] {
  const fallback = createInitialPlots();

  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved === null) return fallback;

    const parsed: unknown = JSON.parse(saved);
    if (!Array.isArray(parsed)) return fallback;

    const valid = parsed.filter((item: unknown): item is FarmPlot => {
      if (typeof item !== "object" || item === null) return false;

      const plot = item as Partial<FarmPlot>;

      return (
        typeof plot.id === "string" &&
        typeof plot.farmId === "string" &&
        typeof plot.name === "string" &&
        CROP_TYPES.includes(plot.crop as CropType) &&
        Array.isArray(plot.boundary) &&
        plot.boundary.length === 4 &&
        plot.boundary.every(
          (point) =>
            Array.isArray(point) &&
            point.length === 2 &&
            typeof point[0] === "number" &&
            typeof point[1] === "number" &&
            Number.isFinite(point[0]) &&
            Number.isFinite(point[1]),
        ) &&
        farms.some((farm) => farm.id === plot.farmId)
      );
    });

    return valid.map((plot) => ({
      ...plot,
      areaHectares: calculateAreaHectares(plot.boundary),
    }));
  } catch {
    return fallback;
  }
}

interface MapClickHandlerProps {
  enabled: boolean;
  onAddPoint: (point: GeoPoint) => void;
}

function MapClickHandler({
  enabled,
  onAddPoint,
}: MapClickHandlerProps) {
  useMapEvents({
    click(event: LeafletMouseEvent) {
      if (!enabled) return;

      onAddPoint([
        event.latlng.lat,
        event.latlng.lng,
      ]);
    },
  });

  return null;
}

function MapFocus({ center }: { center: GeoPoint }) {
  const map = useMap();

  useEffect(() => {
    map.setView(center, 15, { animate: true });
    const timer = window.setTimeout(() => {
      map.invalidateSize();
    }, 100);

    return () => window.clearTimeout(timer);
  }, [center, map]);

  return null;
}

export default function FarmMap() {
  const [plots, setPlots] = useState<FarmPlot[]>(loadPlots);
  const [selectedFarmId, setSelectedFarmId] = useState(
    farms[0]?.id ?? "",
  );
  const [selectedPlotId, setSelectedPlotId] = useState<string | null>(
    null,
  );

  const [newPoints, setNewPoints] = useState<GeoPoint[]>([]);
  const [isDrawing, setIsDrawing] = useState(false);
  const [plotName, setPlotName] = useState("");
  const [crop, setCrop] = useState<CropType>("Rice");
  const [notice, setNotice] = useState(
    "Select a farm, then start drawing a plot.",
  );

  const selectedFarm = farms.find(
    (farm) => farm.id === selectedFarmId,
  ) ?? farms[0];

  const farmPlots = useMemo(
    () => plots.filter((plot) => plot.farmId === selectedFarmId),
    [plots, selectedFarmId],
  );

  const selectedPlot = plots.find(
    (plot) => plot.id === selectedPlotId,
  );

  const newArea = useMemo(
    () => calculateAreaHectares(newPoints),
    [newPoints],
  );

  const totalArea = farmPlots.reduce(
    (sum, plot) => sum + plot.areaHectares,
    0,
  );

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(plots));
    } catch {
      setNotice(
        "Browser storage is unavailable. Changes may not survive a refresh.",
      );
    }
  }, [plots]);

  function chooseFarm(farmId: string) {
    setSelectedFarmId(farmId);
    setSelectedPlotId(null);
    setIsDrawing(false);
    setNewPoints([]);
    setPlotName("");
    setNotice("Farm selected. You can draw a new plot.");
  }

  function startDrawing() {
    if (!selectedFarm) {
      setNotice("No farm is available to select.");
      return;
    }

    setNewPoints([]);
    setPlotName("");
    setCrop("Rice");
    setSelectedPlotId(null);
    setIsDrawing(true);
    setNotice("Click the map four times to define the plot boundary.");
  }

  // Updated function: collect exactly four map points.
  function addBoundaryPoint(point: GeoPoint) {
    if (!isDrawing || newPoints.length >= 4) return;

    const updatedPoints = [...newPoints, point];

    if (updatedPoints.length === 4) {
      setIsDrawing(false);
      setNotice("Boundary complete. Name your plot and choose a crop.");
    }

    setNewPoints(updatedPoints);
  }

  function cancelDrawing() {
    setIsDrawing(false);
    setNewPoints([]);
    setPlotName("");
    setNotice("Drawing cancelled.");
  }

  function savePlot() {
    if (!selectedFarm) {
      setNotice("Please select a farm first.");
      return;
    }

    if (newPoints.length !== 4) {
      setNotice("Click the map four times to complete the boundary.");
      return;
    }

    if (!plotName.trim()) {
      setNotice("Enter a name for your plot.");
      return;
    }

    const area = calculateAreaHectares(newPoints);

    if (!Number.isFinite(area) || area <= 0) {
      setNotice("This boundary has no measurable area. Try again.");
      return;
    }

    const newPlot: FarmPlot = {
      id: `plot-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
      farmId: selectedFarm.id,
      name: plotName.trim(),
      crop,
      areaHectares: area,
      boundary: newPoints,
    };

    setPlots((previous) => [...previous, newPlot]);
    setSelectedPlotId(newPlot.id);
    setNewPoints([]);
    setPlotName("");
    setIsDrawing(false);
    setNotice(`${newPlot.name} saved successfully.`);
  }

  function selectPlot(plot: FarmPlot) {
    setSelectedFarmId(plot.farmId);
    setSelectedPlotId(plot.id);
    setIsDrawing(false);
    setNewPoints([]);
    setNotice(`${plot.name} selected.`);
  }

  if (!selectedFarm) {
    return <main className="gis-page">No farms available.</main>;
  }

  return (
    <main className="gis-page">
      <div className="gis-layout">
        <section className="gis-main">
          <div className="gis-toolbar">
            <div>
              <h2>Farm Mapping</h2>
              <p>Map farm locations and manage plot boundaries.</p>
            </div>

            <div className="gis-field">
              <label htmlFor="farm-select">Selected farm</label>
              <select
                id="farm-select"
                value={selectedFarm.id}
                onChange={(event) => chooseFarm(event.target.value)}
              >
                {farms.map((farm) => (
                  <option key={farm.id} value={farm.id}>
                    {farm.name}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="gis-notice" role="status">
            {notice}
          </div>

          <div className="gis-map-frame">
            <MapContainer
              center={[
                selectedFarm.latitude,
                selectedFarm.longitude,
              ]}
              zoom={15}
              scrollWheelZoom
              className="gis-map"
            >
              <MapFocus
                center={[
                  selectedFarm.latitude,
                  selectedFarm.longitude,
                ]}
              />

              <MapClickHandler
                enabled={isDrawing}
                onAddPoint={addBoundaryPoint}
              />

              <TileLayer
                attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              />

              {/* Every farm has a marker. Select a marker to focus its farm. */}
              {farms.map((farm) => (
                <CircleMarker
                  key={farm.id}
                  center={[farm.latitude, farm.longitude]}
                  radius={farm.id === selectedFarm.id ? 9 : 7}
                  pathOptions={{
                    color: "#ffffff",
                    weight: 2,
                    fillColor:
                      farm.id === selectedFarm.id ? "#2563eb" : "#64748b",
                    fillOpacity: 1,
                  }}
                  eventHandlers={{
                    click: () => chooseFarm(farm.id),
                  }}
                >
                  <Tooltip>{farm.name}</Tooltip>
                  <Popup>
                    <strong>{farm.name}</strong>
                    <br />
                    {farm.location}
                    <br />
                    <button
                      type="button"
                      onClick={() => chooseFarm(farm.id)}
                    >
                      View this farm
                    </button>
                  </Popup>
                </CircleMarker>
              ))}

              {/* Saved plot boundaries for the selected farm. */}
              {farmPlots.map((plot) => (
                <Polygon
                  key={plot.id}
                  positions={plot.boundary}
                  pathOptions={{
                    color: CROP_COLORS[plot.crop],
                    fillColor: CROP_COLORS[plot.crop],
                    fillOpacity: selectedPlotId === plot.id ? 0.55 : 0.3,
                    weight: selectedPlotId === plot.id ? 4 : 2,
                  }}
                  eventHandlers={{
                    click: () => selectPlot(plot),
                  }}
                >
                  <Tooltip sticky>{plot.name} · {plot.crop}</Tooltip>
                  <Popup>
                    <strong>{plot.name}</strong>
                    <br />
                    Crop: {plot.crop}
                    <br />
                    Area: {plot.areaHectares.toFixed(3)} ha
                    <br />
                    Farm: {farms.find((farm) => farm.id === plot.farmId)?.name}
                  </Popup>
                </Polygon>
              ))}

              {/* Points and temporary boundary while drawing. */}
              {newPoints.length > 0 && (
                <>
                  {newPoints.length >= 2 && (
                    <Polygon
                      positions={newPoints}
                      pathOptions={{
                        color: "#2563eb",
                        fillColor: "#60a5fa",
                        fillOpacity: 0.2,
                        dashArray: "6 6",
                      }}
                    />
                  )}

                  {newPoints.map((point, index) => (
                    <CircleMarker
                      key={`${point[0]}-${point[1]}-${index}`}
                      center={point}
                      radius={6}
                      pathOptions={{
                        color: "#ffffff",
                        weight: 2,
                        fillColor: "#2563eb",
                        fillOpacity: 1,
                      }}
                    >
                      <Tooltip>Point {index + 1} of 4</Tooltip>
                    </CircleMarker>
                  ))}
                </>
              )}
            </MapContainer>
          </div>

          <div className="gis-drawing-panel">
            <div>
              <strong>
                {isDrawing
                  ? `Boundary points: ${newPoints.length}/4`
                  : "Plot boundary"}
              </strong>
              <p>
                {isDrawing
                  ? "Click four different locations on the map."
                  : "Start drawing to add a new plot to the selected farm."}
              </p>
            </div>

            {!isDrawing && newPoints.length === 0 && (
              <button
                type="button"
                className="gis-button gis-primary"
                onClick={startDrawing}
              >
                Draw new plot
              </button>
            )}

            {isDrawing && (
              <button
                type="button"
                className="gis-button"
                onClick={cancelDrawing}
              >
                Cancel drawing
              </button>
            )}
          </div>

          {newPoints.length === 4 && (
            <form
              className="gis-save-form"
              onSubmit={(event) => {
                event.preventDefault();
                savePlot();
              }}
            >
              <div className="gis-field">
                <label htmlFor="plot-name">Plot name</label>
                <input
                  id="plot-name"
                  value={plotName}
                  onChange={(event) => setPlotName(event.target.value)}
                  placeholder="Example: North Field"
                  required
                />
              </div>

              <div className="gis-field">
                <label htmlFor="crop-select">Crop type</label>
                <select
                  id="crop-select"
                  value={crop}
                  onChange={(event) => setCrop(event.target.value as CropType)}
                >
                  {CROP_TYPES.map((cropType) => (
                    <option key={cropType} value={cropType}>
                      {cropType}
                    </option>
                  ))}
                </select>
              </div>

              <div className="gis-area-preview">
                <span>Calculated area</span>
                <strong>{newArea.toFixed(3)} hectares</strong>
                <small>{(newArea * 2.47105).toFixed(3)} acres (approx.)</small>
              </div>

              <button
                type="submit"
                className="gis-button gis-primary"
              >
                Save plot
              </button>

              <button
                type="button"
                className="gis-button"
                onClick={cancelDrawing}
              >
                Discard
              </button>
            </form>
          )}
        </section>

        <aside className="gis-sidebar">
          <h3>{selectedFarm.name}</h3>
          <p>{selectedFarm.location}</p>

          <div className="gis-stats">
            <div>
              <span>Plots</span>
              <strong>{farmPlots.length}</strong>
            </div>
            <div>
              <span>Total area</span>
              <strong>{totalArea.toFixed(2)} ha</strong>
            </div>
          </div>

          <h4>Plots on this farm</h4>

          {farmPlots.length === 0 ? (
            <p>No plots yet. Draw a boundary to add one.</p>
          ) : (
            <ul className="gis-plot-list">
              {farmPlots.map((plot) => (
                <li key={plot.id}>
                  <button
                    type="button"
                    className={`gis-plot-item ${
                      selectedPlotId === plot.id ? "is-selected" : ""
                    }`}
                    onClick={() => selectPlot(plot)}
                  >
                    <span
                      className="gis-crop-dot"
                      style={{ backgroundColor: CROP_COLORS[plot.crop] }}
                    />
                    <span className="gis-plot-text">
                      <strong>{plot.name}</strong>
                      <small>{plot.crop}</small>
                    </span>
                    <span className="gis-plot-area">
                      {plot.areaHectares.toFixed(2)} ha
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          )}

          <h4>Crop legend</h4>
          <ul className="gis-legend">
            {CROP_TYPES.map((cropType) => {
              const count = farmPlots.filter(
                (plot) => plot.crop === cropType,
              ).length;

              return (
                <li key={cropType}>
                  <span
                    className="gis-crop-dot"
                    style={{ backgroundColor: CROP_COLORS[cropType] }}
                  />
                  <span>{cropType}</span>
                  <strong>{count}</strong>
                </li>
              );
            })}
          </ul>

          {selectedPlot && selectedPlot.farmId === selectedFarm.id && (
            <section className="gis-detail-panel">
              <h4>Selected plot details</h4>
              <p><strong>Name:</strong> {selectedPlot.name}</p>
              <p><strong>Crop:</strong> {selectedPlot.crop}</p>
              <p>
                <strong>Area:</strong>{" "}
                {selectedPlot.areaHectares.toFixed(3)} hectares
              </p>
              <p>
                <strong>Boundary points:</strong>{" "}
                {selectedPlot.boundary.length}
              </p>
            </section>
          )}

          <p className="gis-storage-note">
            Plot changes are saved in this browser only. They are not yet
            synchronized with a backend server.
          </p>
        </aside>
      </div>
    </main>
  );
}
