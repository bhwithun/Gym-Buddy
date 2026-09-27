import { renderDashboard } from "../../../lib/dashboard";
import { html, json, run } from "../../../lib/http";
import { ensureShareToken, openProfile, renderPrivate, withProfileCookie } from "../../../lib/users";
import { loadWorkoutIndex } from "../../../lib/workouts";

export const dynamic = "force-dynamic";

type Context = { params: Promise<{ slug: string }> };

export function GET(request: Request, context: Context) {
  return run(request, async () => {
    const { slug } = await context.params;
    const profile = await openProfile(request, decodeURIComponent(slug));
    if (!profile) return html(renderPrivate());
    const index = await loadWorkoutIndex(profile.id);
    const shareToken = await ensureShareToken(profile);
    const agentUrl = `${new URL(request.url).origin}/u/${profile.id}?token=${encodeURIComponent(shareToken)}`;
    const requested = new URL(request.url).searchParams;
    const view = requested.get("view");
    const fromPhone = (request.headers.get("user-agent") ?? "").includes("; wv");
    const phone = fromPhone || requested.get("app") === "1" || view === "log" || view === "totals" || view === "averages";
    const locked = view === "log" || view === "totals" || view === "averages";
    const response = withProfileCookie(
      html(
        renderDashboard(index, {
          displayName: profile.displayName,
          slug: profile.id,
          accessToken: shareToken,
          agentUrl,
          embed: locked ? "locked" : phone ? "app" : "full",
          lockTab: view === "log" ? "log" : locked ? "stats" : undefined,
        }),
      ),
      request,
      profile.id,
      shareToken,
    );
    return response;
  });
}

export function OPTIONS(request: Request) {
  return run(request, async () => json({}));
}
