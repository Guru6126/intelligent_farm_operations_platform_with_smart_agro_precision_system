import { api } from "@/lib/api";

// Reusable data-access helpers. The Spring Boot backend enforces farm-level authorization on every call.

export interface ListOpts {
  farmId?: string | null;
  orderBy?: string;
  ascending?: boolean;
  limit?: number;
}

export async function listRows<T>(table: string, opts: ListOpts = {}): Promise<T[]> {
  return api<T[]>(`/api/data/${table}`, {
    query: { farmId: opts.farmId, orderBy: opts.orderBy, ascending: opts.ascending ?? false, limit: opts.limit },
  });
}

export async function insertRow<T = Record<string, unknown>>(table: string, values: Record<string, unknown>): Promise<T> {
  return api<T>(`/api/data/${table}`, { method: "POST", body: values });
}

export async function updateRow<T = Record<string, unknown>>(table: string, id: string, values: Record<string, unknown>): Promise<T> {
  return api<T>(`/api/data/${table}/${id}`, { method: "PUT", body: values });
}

export async function deleteRow(table: string, id: string) {
  await api(`/api/data/${table}/${id}`, { method: "DELETE" });
}

export async function rpc<T = unknown>(fn: string, args?: Record<string, unknown>): Promise<T> {
  return api<T>(`/api/rpc/${fn}`, { method: "POST", body: args ?? {} });
}

export function toCsv(rows: Record<string, unknown>[]) {
  if (!rows.length) return "";
  const keys = Object.keys(rows[0]);
  const esc = (v: unknown) => `"${String(v ?? "").replace(/"/g, '""')}"`;
  return [keys.join(","), ...rows.map((r) => keys.map((k) => esc(r[k])).join(","))].join("\n");
}

export function downloadCsv(name: string, rows: Record<string, unknown>[]) {
  const blob = new Blob([toCsv(rows)], { type: "text/csv" });
  const a = document.createElement("a");
  a.href = URL.createObjectURL(blob);
  a.download = name;
  a.click();
  URL.revokeObjectURL(a.href);
}
