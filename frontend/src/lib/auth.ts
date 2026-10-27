import { api, getToken, setToken } from "./api";

export type AppRole = "admin" | "farmer" | "field_worker";

export interface SessionInfo {
  userId: string;
  email: string;
  fullName: string | null;
  roles: AppRole[];
}

/** Re-validates the stored JWT with the backend and loads the user's roles. */
export async function loadSession(): Promise<SessionInfo | null> {
  if (!getToken()) return null;
  try {
    return await api<SessionInfo>("/api/auth/me");
  } catch {
    return null;
  }
}

export async function signIn(email: string, password: string) {
  const r = await api<{ token: string }>("/api/auth/login", { method: "POST", body: { email, password } });
  setToken(r.token);
}

export async function signUp(body: { email: string; password: string; fullName: string; role: string }) {
  const r = await api<{ token: string }>("/api/auth/signup", { method: "POST", body });
  setToken(r.token);
}

export function signOut() {
  setToken(null);
}

export const roleLabel: Record<AppRole, string> = {
  admin: "Admin",
  farmer: "Farmer",
  field_worker: "Field Worker",
};

export function canManage(roles: AppRole[]) {
  return roles.includes("admin") || roles.includes("farmer");
}
