import { html, json, run } from "../../../../lib/http";
import { acceptsToken, openProfile, queryToken, renderPrivate, withProfileCookie } from "../../../../lib/users";

export const dynamic = "force-dynamic";

type Context = { params: Promise<{ slug: string }> };

export function GET(request: Request, context: Context) {
  return run(request, async () => {
    const { slug: raw } = await context.params;
    const profile = await openProfile(request, decodeURIComponent(raw));
    if (!profile) return html(renderPrivate());
    const token = queryToken(request);
    const accessToken = token && acceptsToken(token, profile) ? token : "";
    const dest = new URL(request.url);
    dest.pathname = `/u/${profile.id}`;
    dest.searchParams.set("tab", "routines");
    let response = new Response(null, { status: 302, headers: { Location: dest.pathname + dest.search } });
    if (accessToken) response = withProfileCookie(response, request, profile.id, accessToken);
    return response;
  });
}

export function OPTIONS(request: Request) {
  return run(request, async () => json({}));
}
