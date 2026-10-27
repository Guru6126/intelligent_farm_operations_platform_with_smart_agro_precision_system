import { createFileRoute, Outlet, redirect } from "@tanstack/react-router";
import { loadSession } from "@/lib/auth";
import { FarmProvider } from "@/lib/farm-context";
import { AppShell } from "@/components/AppShell";

export const Route = createFileRoute("/_authenticated")({
  ssr: false,
  beforeLoad: async ({ location }) => {
    const session = await loadSession();
    if (!session) throw redirect({ to: "/auth", search: { redirect: location.href } });
    return { session };
  },
  component: Layout,
});

function Layout() {
  const { session } = Route.useRouteContext();
  return (
    <FarmProvider session={session}>
      <AppShell>
        <Outlet />
      </AppShell>
    </FarmProvider>
  );
}
