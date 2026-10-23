import { createFileRoute } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { CloudSun, Sun, Cloud, CloudRain, Wind } from "lucide-react";
import { PageHeader } from "@/components/AppShell";
import { ResourceTable, type ResourceConfig } from "@/components/ResourceTable";
import { useFarm } from "@/lib/farm-context";

export const Route = createFileRoute("/_authenticated/weather")({
  head: () => ({ meta: [{ title: "Weather — AgroPrecision" }, { name: "description", content: "Live forecasts and recorded farm weather." }] }),
  component: WeatherPage,
});

type R = Record<string, unknown> & { id: string };
const cfg: ResourceConfig<R> = {
  table: "weather_records", title: "Recorded observations", singular: "Observation", orderBy: "recorded_on", writeAccess: "manage",
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
  const { farms, currentFarm } = useFarm();
  const farm = currentFarm ?? farms.find((f) => f.latitude && f.longitude) ?? null;
  const { data, isLoading, error } = useQuery({
    queryKey: ["forecast", farm?.id],
    enabled: !!(farm?.latitude && farm?.longitude),
    queryFn: async () => {
      // Open-Meteo: free, no API key required.
      const u = `https://api.open-meteo.com/v1/forecast?latitude=${farm!.latitude}&longitude=${farm!.longitude}&current=temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code&daily=temperature_2m_max,temperature_2m_min,precipitation_sum,weather_code,wind_speed_10m_max&timezone=auto`;
      const r = await fetch(u);
      if (!r.ok) throw new Error("Weather service unavailable");
      return (await r.json()) as Forecast;
    },
  });
  const k = data ? kind(data.current.weather_code) : "sunny";
  const alerts = data ? data.daily.time.map((d, i) => ({ d, rain: data.daily.precipitation_sum[i], wind: data.daily.wind_speed_10m_max[i], hi: data.daily.temperature_2m_max[i] }))
    .filter((x) => x.rain > 20 || x.wind > 40 || x.hi > 40) : [];

  return (
    <div className="space-y-6">
      <PageHeader icon={CloudSun} title="Weather" subtitle={farm ? `Live forecast for ${farm.name} (Open-Meteo)` : "Add coordinates to a farm to see its live forecast."}
        decoration={data ? <WeatherFx k={k} /> : null}>
        {data && (
          <div className="text-right">
            <div className="font-display text-4xl font-bold">{Math.round(data.current.temperature_2m)}°C</div>
            <div className="text-sm capitalize text-primary-foreground/75">{k} · {data.current.relative_humidity_2m}% · <Wind className="inline size-3" /> {data.current.wind_speed_10m} km/h</div>
          </div>
        )}
      </PageHeader>
      {isLoading && <p className="text-sm text-muted-foreground">Loading forecast…</p>}
      {error && <p className="text-sm text-destructive">{(error as Error).message}</p>}
      {alerts.length > 0 && (
        <div role="alert" className="rounded-xl border border-destructive/30 bg-destructive-soft px-4 py-3 text-sm">
          <strong>Weather advisory:</strong> {alerts.map((a) => `${a.d}: ${a.rain > 20 ? `heavy rain ${a.rain}mm ` : ""}${a.wind > 40 ? `strong wind ${a.wind}km/h ` : ""}${a.hi > 40 ? `heat ${a.hi}°C` : ""}`).join(" · ")}. Consider postponing spraying and irrigation.
        </div>
      )}
      {data && (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 lg:grid-cols-7">
          {data.daily.time.map((d, i) => {
            const kk = kind(data.daily.weather_code[i]);
            const Icon = kk === "rain" ? CloudRain : kk === "cloudy" ? Cloud : Sun;
            return (
              <div key={d} className="lift animate-rise rounded-2xl border bg-card p-4 text-center shadow-soft" style={{ animationDelay: `${i * 50}ms` }}>
                <div className="text-xs text-muted-foreground">{new Date(d).toLocaleDateString([], { weekday: "short", day: "numeric" })}</div>
                <Icon className={`mx-auto my-2 size-7 ${kk === "rain" ? "text-water" : kk === "cloudy" ? "text-muted-foreground" : "text-sun"}`} />
                <div className="text-sm font-semibold">{Math.round(data.daily.temperature_2m_max[i])}° / {Math.round(data.daily.temperature_2m_min[i])}°</div>
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
