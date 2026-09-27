import { renderConnect } from "../../../../lib/connect";
import { html, json, run } from "../../../../lib/http";
import { connectPath, getProfile, rotateToken, tokenMatches, withProfileCookie } from "../../../../lib/users";

export const dynamic = "force-dynamic";

type Context = { params: Promise<{ slug: string }> };

function tokenFrom(request: Request): string {
  return new URL(request.url).searchParams.get("token")?.trim() ?? "";
}

async function authorized(request: Request, context: Context) {
  const { slug: raw } = await context.params;
  const slug = decodeURIComponent(raw);
  const profile = await getProfile(slug);
  const token = tokenFrom(request);
  if (!profile || !tokenMatches(token, profile.tokenHash)) return null;
  return profile;
}

export function GET(request: Request, context: Context) {
  return run(request, async () => {
    const profile = await authorized(request, context);
    if (!profile) {
      return new Response("This connect link is not valid.", {
        status: 404,
        headers: { "Content-Type": "text/plain; charset=utf-8", "Cache-Control": "no-store" },
      });
    }
    const token = tokenFrom(request);
    return withProfileCookie(
      html(
        await renderConnect({
          origin: new URL(request.url).origin,
          slug: profile.id,
          displayName: profile.displayName,
          token,
        }),
      ),
      request,
      profile.id,
      token,
    );
  });
}

export function POST(request: Request, context: Context) {
  return run(request, async () => {
    const profile = await authorized(request, context);
    if (!profile) {
      return new Response("This connect link is not valid.", {
        status: 404,
        headers: { "Content-Type": "text/plain; charset=utf-8", "Cache-Control": "no-store" },
      });
    }
    const token = await rotateToken(profile.id);
    if (!token) return json({ error: "not found" }, 404);
    return withProfileCookie(
      new Response(null, {
        status: 303,
        headers: { Location: connectPath(profile.id, token) },
      }),
      request,
      profile.id,
      token,
    );
  });
}

export function OPTIONS(request: Request) {
  return run(request, async () => json({}));
}
