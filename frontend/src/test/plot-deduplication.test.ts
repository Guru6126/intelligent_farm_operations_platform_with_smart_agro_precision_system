import { describe, expect, it } from "vitest";
import type { Plot } from "@/lib/types";

describe("Plot duplicate prevention and validation", () => {
  const existingPlots: Plot[] = [
    {
      id: "p1",
      farm_id: "farm-1",
      name: "Plot 1",
      area_acres: 10,
      soil_ph: 6.5,
      boundary_geojson: null,
      created_at: "2026-01-01T00:00:00Z",
    },
    {
      id: "p2",
      farm_id: "farm-1",
      name: "Plot 2",
      area_acres: 5,
      soil_ph: 5.0,
      boundary_geojson: null,
      created_at: "2026-01-02T00:00:00Z",
    },
    {
      id: "p3",
      farm_id: "farm-2",
      name: "F2 Plot 1",
      area_acres: 2,
      soil_ph: 5.0,
      boundary_geojson: null,
      created_at: "2026-01-03T00:00:00Z",
    },
  ];

  function validatePlot(
    values: { id?: string; farm_id?: string; name?: string },
    plots: Plot[],
  ): string | null {
    const name = String(values.name ?? "").trim();
    if (!name) return "Plot name is required.";
    const farmId = String(values.farm_id ?? "");
    if (!farmId) return "Please choose a farm.";
    const isDuplicate = plots.some(
      (p) =>
        String(p.farm_id) === farmId &&
        p.name.trim().toLowerCase() === name.toLowerCase() &&
        p.id !== values.id,
    );
    if (isDuplicate) {
      return `A plot named "${name}" already exists on this farm. Duplication of plots is not allowed.`;
    }
    return null;
  }

  it("blocks duplicate plot names on the same farm", () => {
    const err = validatePlot({ farm_id: "farm-1", name: "Plot 2" }, existingPlots);
    expect(err).toBe('A plot named "Plot 2" already exists on this farm. Duplication of plots is not allowed.');
  });

  it("blocks duplicate plot names with different casing or whitespace", () => {
    const err = validatePlot({ farm_id: "farm-1", name: "  plot 2  " }, existingPlots);
    expect(err).toBe('A plot named "plot 2" already exists on this farm. Duplication of plots is not allowed.');
  });

  it("allows same plot name on a different farm", () => {
    const err = validatePlot({ farm_id: "farm-2", name: "Plot 2" }, existingPlots);
    expect(err).toBeNull();
  });

  it("allows updating existing plot keeping its own name", () => {
    const err = validatePlot({ id: "p2", farm_id: "farm-1", name: "Plot 2" }, existingPlots);
    expect(err).toBeNull();
  });

  it("deduplicates plot list prioritizing mapped boundaries", () => {
    const rawPlots: Plot[] = [
      {
        id: "dup-1",
        farm_id: "farm-1",
        name: "Plot 2",
        area_acres: 5,
        soil_ph: 5.0,
        boundary_geojson: null,
        created_at: "2026-01-01T00:00:00Z",
      },
      {
        id: "dup-2",
        farm_id: "farm-1",
        name: "Plot 2",
        area_acres: 5,
        soil_ph: 5.0,
        boundary_geojson: { type: "Polygon", coordinates: [[[0, 0], [1, 0], [1, 1], [0, 0]]] },
        created_at: "2026-01-02T00:00:00Z",
      },
    ];

    const map = new Map<string, Plot>();
    for (const p of rawPlots) {
      const key = `${p.farm_id}:${p.name.trim().toLowerCase()}`;
      const existing = map.get(key);
      if (!existing || (p.boundary_geojson && !existing.boundary_geojson)) {
        map.set(key, p);
      }
    }
    const result = Array.from(map.values());
    expect(result.length).toBe(1);
    expect(result[0].id).toBe("dup-2");
    expect(result[0].boundary_geojson).not.toBeNull();
  });
});

