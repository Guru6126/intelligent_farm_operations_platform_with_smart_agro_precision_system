// Strongly typed row models matching database/schema.sql
type Base = { id: string; farm_id: string; created_at: string; [k: string]: unknown };
export type Plot = Base & { name: string; area_acres: number | null; soil_ph: number | null; boundary_geojson: unknown };
export type Crop = Base & { crop_name: string; variety: string | null; season: string | null; stage: string; plot_id: string | null; sowing_date: string | null; expected_harvest: string | null };
export type Harvest = Base & { crop_id: string | null; harvest_date: string; quantity_kg: number; revenue: number };
export type InventoryItem = Base & { name: string; category: string; unit: string; quantity: number; reorder_level: number; unit_cost: number; expiry_date: string | null };
export type Irrigation = Base & { plot_id: string | null; scheduled_at: string; duration_min: number; water_litres: number; status: string; method: string; water_source: string | null };
export type Equipment = Base & { name: string; type: string | null; status: string; next_service: string | null };
export type Task = Base & { title: string; status: string; priority: string; due_date: string | null; assignee_id: string | null };
export type Expense = Base & { category: string; amount: number; spent_on: string; season: string | null };
export type Budget = Base & { category: string; season: string; planned_amount: number };
export type Weather = Base & { recorded_on: string; temperature_c: number; humidity_pct: number; rainfall_mm: number; wind_kmh: number; condition: string | null };
export type Notification = { id: string; title: string; body: string | null; severity: string; is_read: boolean; created_at: string };
