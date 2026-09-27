import { renderDashboard, type DashboardView } from "../../../lib/dashboard";
import { html, json, run } from "../../../lib/http";
import { openProfile, renderPrivate } from "../../../lib/users";
import { loadWorkoutIndex } from "../../../lib/workouts";

export const dynamic = "force-dynamic";

type Context = { params: Promise<{ slug: string }> };

export function GET(request: Request, context: Context) {
  return run(request, async () => {
    const { slug } = await context.params;
    const profile = await openProfile(request, decodeURIComponent(slug));
    if (!profile) return html(renderPrivate());
    const requested = new URL(request.url).searchParams.get("view");
    const view: DashboardView =
      requested === "log" || requested === "totals" || requested === "averages" ? requested : "all";
    const index = await loadWorkoutIndex(profile.id);
    return html(
      renderDashboard(index, {
        displayName: profile.displayName,
        routinePath: `/u/${profile.id}/routine`,
        view,
      }),
    );
  });
}

export function OPTIONS(request: Request) {
  return run(request, async () => json({}));
}
