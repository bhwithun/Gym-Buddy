import { html, json, run } from "../../../../lib/http";
import { renderRoutineEditor } from "../../../../lib/routine-editor";
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
    let response = html(
      renderRoutineEditor({
        apiBase: `/u/${profile.id}`,
        calendarHref: accessToken
          ? `/u/${profile.id}?token=${encodeURIComponent(accessToken)}`
          : `/u/${profile.id}`,
        slug: profile.id,
        signedIn: true,
        accessToken,
      }),
    );
    if (accessToken) response = withProfileCookie(response, request, profile.id, accessToken);
    return response;
  });
}

export function OPTIONS(request: Request) {
  return run(request, async () => json({}));
}
