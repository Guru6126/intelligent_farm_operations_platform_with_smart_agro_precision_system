import { createFileRoute, Link } from "@tanstack/react-router";
import { Sprout, Droplets, Map, Package, Wrench, BarChart3, CloudSun, Users, Bell, Tractor, ShieldCheck, Leaf } from "lucide-react";
import { Button } from "@/components/ui/button";
import hero from "@/assets/hero-farm.jpg";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "AgroPrecision — Smart Farm Operations Platform" },
      { name: "description", content: "One connected platform for farms, plots, crops, inventory, irrigation, equipment, labour, weather and reports." },
      { property: "og:title", content: "AgroPrecision — Smart Farm Operations Platform" },
      { property: "og:description", content: "One connected platform for farms, crops, irrigation, equipment and analytics." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: Landing,
});

const modules = [
  { icon: ShieldCheck, t: "Users & Access", d: "Admin, Farmer and Field Worker roles enforced by the database." },
  { icon: Tractor, t: "Farms & Plots", d: "Register farms, split them into plots, add field workers." },
  { icon: Map, t: "GIS Mapping", d: "See farms and plot boundaries on OpenStreetMap." },
  { icon: Sprout, t: "Crop Lifecycle", d: "Plan seasons, track stages, activities and harvests." },
  { icon: Package, t: "Inventory", d: "Stock in/out that can never go negative, low-stock alerts." },
  { icon: CloudSun, t: "Weather", d: "Live forecasts for every farm plus recorded history." },
  { icon: Droplets, t: "Irrigation", d: "Schedule watering by plot, source and method." },
  { icon: Wrench, t: "Equipment", d: "Bookings without double-booking, maintenance logs." },
  { icon: Users, t: "Resource Planning", d: "Workers, tasks, seasonal budgets and expenses." },
  { icon: Bell, t: "Notifications", d: "Automatic alerts for stock and task assignments." },
  { icon: BarChart3, t: "Analytics", d: "Yield, cost and water reports with CSV export." },
];

function Landing() {
  return (
    <div className="min-h-screen bg-background">
      <section className="relative isolate overflow-hidden">
        <img src={hero} alt="Aerial view of green terraced farm fields at sunrise" width={1600} height={912} className="absolute inset-0 -z-10 h-full w-full object-cover" />
        <div className="absolute inset-0 -z-10 bg-hero" />
        <nav className="mx-auto flex max-w-6xl items-center gap-2 px-6 py-6 text-primary-foreground">
          <div className="grid size-9 place-items-center rounded-xl bg-accent text-accent-foreground"><Leaf className="size-5" /></div>
          <span className="font-display text-lg font-bold">AgroPrecision</span>
          <div className="ml-auto flex gap-2">
            <Button asChild variant="outline" size="sm"><Link to="/auth">Sign in</Link></Button>
          </div>
        </nav>
        <div className="mx-auto max-w-6xl px-6 pb-28 pt-16 text-primary-foreground md:pt-24">
          <p className="animate-rise font-tech text-xs uppercase tracking-[0.3em] text-lime">Smart Agro Precision System</p>
          <h1 className="animate-rise mt-4 max-w-3xl text-4xl font-extrabold leading-[1.05] md:text-6xl" style={{ animationDelay: "80ms" }}>
            Run every field, crop and drop of water from one place.
          </h1>
          <p className="animate-rise mt-5 max-w-xl text-lg text-primary-foreground/80" style={{ animationDelay: "160ms" }}>
            An intelligent farm operations platform connecting plots, crops, inventory, irrigation, machinery, labour and reports.
          </p>
          <div className="animate-rise mt-8 flex flex-wrap gap-3" style={{ animationDelay: "240ms" }}>
            <Button asChild size="lg" variant="lime"><Link to="/auth">Get started</Link></Button>
            <Button asChild size="lg" variant="outline"><Link to="/auth">Create account</Link></Button>
          </div>
        </div>
      </section>
      <section className="mx-auto max-w-6xl px-6 py-20">
        <h2 className="text-3xl font-bold">11 connected modules</h2>
        <p className="mt-2 max-w-xl text-muted-foreground">Data entered in one module flows into the others — stock used by an activity shows up in costs, harvests feed yield reports.</p>
        <div className="mt-10 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {modules.map((m, i) => (
            <div key={m.t} className="lift animate-rise rounded-2xl border bg-card p-6 shadow-soft" style={{ animationDelay: `${i * 50}ms` }}>
              <div className="grid size-10 place-items-center rounded-xl bg-secondary text-primary"><m.icon className="size-5" /></div>
              <h3 className="mt-4 text-lg font-semibold">{m.t}</h3>
              <p className="mt-1 text-sm text-muted-foreground">{m.d}</p>
            </div>
          ))}
        </div>
      </section>
      <footer className="border-t py-8 text-center text-sm text-muted-foreground">AgroPrecision · Intelligent Farm Operations Platform</footer>
    </div>
  );
}
