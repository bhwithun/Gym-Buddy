import { renderLanding } from "../../../../lib/landing";
import { html, json, run } from "../../../../lib/http";
import {
  SLUG_RE,
  acceptsToken,
  bearerToken,
  browserToken,
  deleteProfile,
  getProfile,
  queryToken,
  rememberedProfiles,
  withoutProfileCookie,
} from "../../../../lib/users";

export const dynamic = "force-dynamic";

type Context = { params: Promise<{ slug: string }> };

export function POST(request: Request, context: Context) {
  return run(request, async () => {
    const { slug: raw } = await context.params;
    let slug: string;
    try {
      slug = decodeURIComponent(raw);
    } catch {
      return json({ error: "not found" }, 404);
    }
    if (!SLUG_RE.test(slug)) return json({ error: "not found" }, 404);

    const profile = await getProfile(slug);
    if (!profile) {
      return withoutProfileCookie(
        new Response(null, { status: 303, headers: { Location: "/" } }),
        request,
        slug,
      );
    }

    const token = bearerToken(request) || queryToken(request) || browserToken(request, slug);
    if (!acceptsToken(token, profile)) {
      return html(
        renderLanding(
          "That profile cannot be deleted from this browser.",
          await rememberedProfiles(request),
        ),
      );
    }

    try {
      await deleteProfile(slug);
    } catch (error) {
      const detail = error instanceof Error ? error.message : "unknown";
      console.error(JSON.stringify({ message: "delete profile failed", slug, error: detail }));
      return html(
        renderLanding("Could not delete that profile.", await rememberedProfiles(request)),
      );
    }

    return withoutProfileCookie(
      new Response(null, { status: 303, headers: { Location: "/" } }),
      request,
      slug,
    );
  });
}

export function OPTIONS(request: Request) {
  return run(request, async () => json({}));
}
