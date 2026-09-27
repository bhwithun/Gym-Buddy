import { renderDashboard } from "../../../lib/dashboard";
import { html, json, run } from "../../../lib/http";
import { getProfile } from "../../../lib/users";
import { loadWorkoutIndex } from "../../../lib/workouts";

export const dynamic = "force-dynamic";

type Context = { params: Promise<{ slug: string }> };

export function GET(request: Request, context: Context) {
  return run(request, async () => {
    const { slug } = await context.params;
    const profile = await getProfile(decodeURIComponent(slug));
    if (!profile) return json({ error: "not found" }, 404);
    const index = await loadWorkoutIndex(profile.id);
    return html(
      renderDashboard(index, {
        displayName: profile.displayName,
        routinePath: `/u/${profile.id}/routine`,
      }),
    );
  });
}

export function OPTIONS(request: Request) {
  return run(request, async () => json({}));
}
