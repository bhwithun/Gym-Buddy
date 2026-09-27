import { renderDashboard, type DashboardView } from "../../../lib/dashboard";
import { html, json, run } from "../../../lib/http";
import { acceptsToken, openProfile, queryToken, renderPrivate, withProfileCookie } from "../../../lib/users";
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
    const token = queryToken(request);
    const routinePath =
      token && acceptsToken(token, profile)
        ? `/u/${profile.id}/routine?token=${encodeURIComponent(token)}`
        : `/u/${profile.id}/routine`;
    let response = html(
      renderDashboard(index, {
        displayName: profile.displayName,
        routinePath,
        view,
      }),
    );
    if (token && acceptsToken(token, profile)) {
      response = withProfileCookie(response, request, profile.id, token);
    }
    return response;
  });
}

export function OPTIONS(request: Request) {
  return run(request, async () => json({}));
}
