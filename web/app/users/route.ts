import { renderLanding } from "../../lib/landing";
import { html, json, run } from "../../lib/http";
import { readProfileRequest } from "../../lib/profile-form";
import { connectPath, createProfile } from "../../lib/users";

export const dynamic = "force-dynamic";

export function POST(request: Request) {
  return run(request, async () => {
    const parsed = await readProfileRequest(request);
    if (!parsed.ok) {
      return parsed.json
        ? json({ error: parsed.error }, 400)
        : html(renderLanding(parsed.error));
    }
    let created: Awaited<ReturnType<typeof createProfile>>;
    try {
      created = await createProfile(parsed.displayName, parsed.slug);
    } catch (error) {
      const detail = error instanceof Error ? error.message : "unknown";
      console.error(JSON.stringify({ message: "create profile failed", error: detail }));
      const message = "Could not create that profile.";
      return parsed.json ? json({ error: message }, 500) : html(renderLanding(message));
    }
    if (!created.ok) {
      return parsed.json ? json({ error: created.error }, 409) : html(renderLanding(created.error));
    }
    const path = connectPath(parsed.slug, created.token);
    if (parsed.json) {
      const origin = new URL(request.url).origin;
      return json({ slug: parsed.slug, displayName: parsed.displayName, connectUrl: `${origin}${path}` }, 201);
    }
    return new Response(null, { status: 303, headers: { Location: path } });
  });
}

export function OPTIONS(request: Request) {
  return run(request, async () => json({}));
}
