import { renderDashboard } from "../../../lib/dashboard";
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
    const index = await loadWorkoutIndex(profile.id);
    const token = queryToken(request);
    const accessToken = token && acceptsToken(token, profile) ? token : "";
    let response = html(
      renderDashboard(index, {
        displayName: profile.displayName,
        slug: profile.id,
        accessToken,
      }),
    );
    if (accessToken) {
      response = withProfileCookie(response, request, profile.id, accessToken);
    }
    return response;
  });
}

export function OPTIONS(request: Request) {
  return run(request, async () => json({}));
}
