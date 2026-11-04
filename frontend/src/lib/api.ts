// Central HTTP client for the Spring Boot backend (see PROJECT_GUIDE.md).
// The browser only stores a short-lived JWT; passwords are hashed with BCrypt on the server.
const BASE = ((import.meta.env.VITE_API_URL as string | undefined) ?? "http://localhost:8081").replace(/\/$/, "");
const TOKEN_KEY = "agro_token";

export const getToken = () => (typeof window === "undefined" ? null : window.localStorage.getItem(TOKEN_KEY));
export const setToken = (t: string | null) => {
  if (t) window.localStorage.setItem(TOKEN_KEY, t);
  else window.localStorage.removeItem(TOKEN_KEY);
};

export class ApiError extends Error {
  constructor(message: string, public status: number) {
    super(message);
  }
}

export async function api<T = unknown>(path: string, init: { method?: string; body?: unknown; query?: Record<string, unknown> } = {}): Promise<T> {
  const qs = init.query
    ? "?" + new URLSearchParams(Object.entries(init.query).filter(([, v]) => v !== undefined && v !== null && v !== "").map(([k, v]) => [k, String(v)])).toString()
    : "";
  const headers: Record<string, string> = { Accept: "application/json" };
  if (init.body !== undefined) headers["Content-Type"] = "application/json";
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;
  let res: Response;
  try {
    res = await fetch(`${BASE}${path}${qs}`, { method: init.method ?? "GET", headers, body: init.body !== undefined ? JSON.stringify(init.body) : undefined });
  } catch {
    throw new ApiError("Cannot reach the server. Make sure the backend is running.", 0);
  }
  if (res.status === 401) setToken(null);
  const text = await res.text();
  const data = text ? JSON.parse(text) : null;
  if (!res.ok) throw new ApiError(data?.message ?? `Request failed (${res.status})`, res.status);
  return data as T;
}
