import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { useQuery } from "@tanstack/react-query";
import { listRows } from "@/services/crud";
import type { SessionInfo } from "./auth";

export interface Farm {
  id: string;
  name: string;
  owner_id: string;
  location: string | null;
  latitude: number | null;
  longitude: number | null;
  total_area_acres: number | null;
  is_demo: boolean;
}

interface Ctx {
  session: SessionInfo;
  farms: Farm[];
  farmId: string | null; // null = all farms
  setFarmId: (id: string | null) => void;
  currentFarm: Farm | null;
  farmsLoading: boolean;
}

const FarmCtx = createContext<Ctx | null>(null);

export function FarmProvider({ session, children }: { session: SessionInfo; children: ReactNode }) {
  const [farmId, setFarmIdState] = useState<string | null>(null);
  const { data: farms = [], isLoading } = useQuery({
    queryKey: ["farms"],
    queryFn: () => listRows<Farm>("farms", { orderBy: "created_at", ascending: true }),
  });

  useEffect(() => {
    const saved = window.localStorage.getItem("farmId");
    if (saved) setFarmIdState(saved === "all" ? null : saved);
  }, []);
  useEffect(() => {
    if (farmId && farms.length && !farms.some((f) => f.id === farmId)) setFarmIdState(null);
  }, [farms, farmId]);

  const setFarmId = (id: string | null) => {
    setFarmIdState(id);
    window.localStorage.setItem("farmId", id ?? "all");
  };

  return (
    <FarmCtx.Provider
      value={{
        session,
        farms,
        farmId,
        setFarmId,
        currentFarm: farms.find((f) => f.id === farmId) ?? null,
        farmsLoading: isLoading,
      }}
    >
      {children}
    </FarmCtx.Provider>
  );
}

export function useFarm() {
  const c = useContext(FarmCtx);
  if (!c) throw new Error("useFarm must be used inside FarmProvider");
  return c;
}
