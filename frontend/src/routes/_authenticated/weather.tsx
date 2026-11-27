import { createFileRoute } from "@tanstack/react-router";
import { useState, useMemo, useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { CloudSun, Sun, Cloud, CloudRain, Wind, MapPin, AlertCircle, Compass, Bot } from "lucide-react";
import { PageHeader } from "@/components/AppShell";
import { ResourceTable, type ResourceConfig } from "@/components/ResourceTable";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useFarm, type Farm } from "@/lib/farm-context";

export const Route = createFileRoute("/_authenticated/weather")({
  head: () => ({ meta: [{ title: "Weather — AgroPrecision" }, { name: "description", content: "Live forecasts and recorded farm weather." }] }),
  component: WeatherPage,
});

type R = Record<string, unknown> & { id: string };
const cfg: ResourceConfig<R> = {
  table: "weather_records", title: "Recorded observations", singular: "Observation", orderBy: "recorded_on", writeAccess: "manage",
  validate: (values) => {
    if (values.recorded_on) {
      const d = new Date(String(values.recorded_on));
      d.setHours(0, 0, 0, 0);
      const today = new Date();
      today.setHours(23, 59, 59, 999);
      if (d > today) {
        return "Weather observation date cannot be in the future.";
      }
    }
    return null;
  },
  fields: [
    { key: "recorded_on", label: "Date", type: "date", required: true },
    { key: "temperature_c", label: "Temperature °C", type: "number" },
    { key: "humidity_pct", label: "Humidity %", type: "number" },
    { key: "rainfall_mm", label: "Rainfall mm", type: "number" },
    { key: "wind_kmh", label: "Wind km/h", type: "number" },
    { key: "condition", label: "Condition", type: "select", options: ["sunny", "cloudy", "rain", "storm", "windy"] },
  ],
  columns: [
    { key: "recorded_on", label: "Date" }, { key: "condition", label: "Condition" }, { key: "temperature_c", label: "°C" },
    { key: "humidity_pct", label: "Hum %" }, { key: "rainfall_mm", label: "Rain mm" }, { key: "wind_kmh", label: "Wind" },
  ],
};

interface Forecast { daily: { time: string[]; temperature_2m_max: number[]; temperature_2m_min: number[]; precipitation_sum: number[]; weather_code: number[]; wind_speed_10m_max: number[] }; current: { temperature_2m: number; relative_humidity_2m: number; wind_speed_10m: number; weather_code: number } }

const kind = (code: number) => (code >= 51 ? "rain" : code >= 2 ? "cloudy" : "sunny");

function WeatherFx({ k }: { k: string }) {
  if (k === "sunny") return <Sun className="absolute -right-6 -top-6 size-40 text-sun/30 animate-spin-slow" />;
  if (k === "cloudy") return <><Cloud className="absolute right-10 top-2 size-24 text-primary-foreground/15 animate-cloud" /><Cloud className="absolute right-40 top-10 size-16 text-primary-foreground/10 animate-cloud" style={{ animationDelay: "1.5s" }} /></>;
  return <>{Array.from({ length: 18 }).map((_, i) => <span key={i} className="absolute top-0 h-3 w-px bg-water/60 animate-rain" style={{ left: `${40 + i * 3.2}%`, animationDelay: `${(i % 6) * 0.18}s` }} />)}</>;
}

function WeatherPage() {
  const { farms, session } = useFarm();
  const isAdmin = session.roles.includes("admin");

  // Default to first farm with coordinates, or first available farm
  const [selectedFarmId, setSelectedFarmId] = useState<string>("");

  useEffect(() => {
    if (farms.length > 0 && !selectedFarmId) {
      const preferred = farms.find((f) => f.latitude && f.longitude) ?? farms[0];
      setSelectedFarmId(preferred.id);
    }
  }, [farms, selectedFarmId]);

  const farm = useMemo(() => {
    return farms.find((f) => f.id === selectedFarmId) ?? null;
  }, [farms, selectedFarmId]);

  const hasCoords = Boolean(farm?.latitude && farm?.longitude);

  const { data, isLoading, error } = useQuery({
    queryKey: ["forecast", farm?.id, farm?.latitude, farm?.longitude],
    enabled: hasCoords,
    queryFn: async () => {
      // Open-Meteo: free, no API key required.
      const u = `https://api.open-meteo.com/v1/forecast?latitude=${farm!.latitude}&longitude=${farm!.longitude}&current=temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code&daily=temperature_2m_max,temperature_2m_min,precipitation_sum,weather_code,wind_speed_10m_max&timezone=auto`;
      const r = await fetch(u);
      if (!r.ok) throw new Error("Weather service unavailable for this location.");
      return (await r.json()) as Forecast;
    },
  });

  const k = data ? kind(data.current.weather_code) : "sunny";
  const alerts = data ? data.daily.time.map((d, i) => ({ d, rain: data.daily.precipitation_sum[i], wind: data.daily.wind_speed_10m_max[i], hi: data.daily.temperature_2m_max[i] }))
    .filter((x) => x.rain > 20 || x.wind > 40 || x.hi > 40) : [];

  function buildWeatherMessage(alertList: { d: string; rain: number; wind: number; hi: number }[]): string {
    if (alertList.length === 0) return "";
    const parts: string[] = [];
    for (const a of alertList) {
      const dateLabel = new Date(a.d).toLocaleDateString([], { weekday: "long", month: "long", day: "numeric" });
      const reasons: string[] = [];
      if (a.hi > 40) reasons.push(`temperatures expected to reach ${Math.round(a.hi)}°C`);
      if (a.rain > 20) reasons.push(`heavy rainfall of ${Math.round(a.rain)} mm is forecast`);
      if (a.wind > 40) reasons.push(`strong winds of up to ${Math.round(a.wind)} km/h are expected`);
      parts.push(`On ${dateLabel}, ${reasons.join(" and ")}.`);
    }
    return parts.join(" ") + " It is advisable to postpone spraying and irrigation on these days.";
  }

  return (
    <div className="space-y-6">
      {/* Farm Location Selector */}
      <div className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border bg-card p-4 shadow-soft">
        <div className="flex items-center gap-2.5">
          <div className="grid size-9 place-items-center rounded-xl bg-primary/10 text-primary">
            <MapPin className="size-5" />
          </div>
          <div>
            <h2 className="text-sm font-semibold text-foreground">Select Farm Location</h2>
            <p className="text-xs text-muted-foreground">
              {isAdmin
                ? "Admin view: access live weather across all system farms."
                : "View live weather for your owned and assigned farms."}
            </p>
          </div>
        </div>

        <div className="w-full sm:w-72">
          <Select
            value={selectedFarmId}
            onValueChange={(val) => setSelectedFarmId(val)}
          >
            <SelectTrigger className="h-10 w-full rounded-xl border border-[#b7d8bd] bg-[#f0f8f1] px-3 text-sm text-[#123b2a] focus:ring-2 focus:ring-[#16834a]">
              <SelectValue placeholder="Choose a farm..." />
            </SelectTrigger>
            <SelectContent className="max-h-72">
              {farms.map((f) => (
                <SelectItem key={f.id} value={f.id}>
                  {f.name} {f.location ? `(${f.location})` : ""}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      <PageHeader
        icon={CloudSun}
        title="Weather"
        subtitle={
          farm
            ? hasCoords
              ? `Live forecast for ${farm.name}${farm.location ? ` · ${farm.location}` : ""} (Open-Meteo)`
              : `${farm.name} has no GPS coordinates configured.`
            : "Select a farm to view its live weather forecast."
        }
        decoration={data ? <WeatherFx k={k} /> : null}
      >
        {data && (
          <div className="text-right">
            <div className="font-display text-4xl font-bold">{Math.round(data.current.temperature_2m)}°C</div>
            <div className="text-sm capitalize text-primary-foreground/75">
              {k} · {data.current.relative_humidity_2m}% · <Wind className="inline size-3" /> {data.current.wind_speed_10m} km/h
            </div>
          </div>
        )}
      </PageHeader>

      {farm && !hasCoords && (
        <div className="rounded-xl border border-sun/40 bg-sun/15 p-4 text-sm text-foreground flex items-center gap-3">
          <Compass className="size-5 shrink-0 text-sun" />
          <span>
            <strong>Coordinates missing:</strong> {farm.name} does not have latitude and longitude set yet. You can add coordinates in <em>Farms &amp; Plots</em> to unlock live Open-Meteo forecasts for this location.
          </span>
        </div>
      )}

      {isLoading && <p className="text-sm text-muted-foreground">Loading forecast for {farm?.name}…</p>}
      {error && (
        <div className="rounded-xl border border-destructive/30 bg-destructive-soft p-4 text-sm text-destructive flex items-center gap-2">
          <AlertCircle className="size-4 shrink-0" />
          <span>{(error as Error).message}</span>
        </div>
      )}

      {alerts.length > 0 && (
        <div className="flex items-center gap-3 rounded-xl border border-[#b7d8bd] bg-[#f0f8f1] px-4 py-3 text-sm text-[#123b2a]">
          <div className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-[#16834a]/15 text-[#16834a]">
            <Bot className="size-4" />
          </div>
          <span className="self-center">
            {buildWeatherMessage(alerts)}
          </span>
        </div>
      )}

      {data && (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 lg:grid-cols-7">
          {data.daily.time.map((d, i) => {
            const kk = kind(data.daily.weather_code[i]);
            const Icon = kk === "rain" ? CloudRain : kk === "cloudy" ? Cloud : Sun;
            return (
              <div
                key={d}
                className="lift animate-rise rounded-2xl border bg-card p-4 text-center shadow-soft"
                style={{ animationDelay: `${i * 50}ms` }}
              >
                <div className="text-xs text-muted-foreground">
                  {new Date(d).toLocaleDateString([], { weekday: "short", day: "numeric" })}
                </div>
                <Icon
                  className={`mx-auto my-2 size-7 ${
                    kk === "rain" ? "text-water" : kk === "cloudy" ? "text-muted-foreground" : "text-sun"
                  }`}
                />
                <div className="text-sm font-semibold">
                  {Math.round(data.daily.temperature_2m_max[i])}° / {Math.round(data.daily.temperature_2m_min[i])}°
                </div>
                <div className="font-tech text-xs text-muted-foreground">{data.daily.precipitation_sum[i]} mm</div>
              </div>
            );
          })}
        </div>
      )}

      <ResourceTable config={cfg} />
    </div>
  );
}
