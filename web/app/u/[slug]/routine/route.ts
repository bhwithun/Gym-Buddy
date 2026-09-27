import { html, json, run } from "../../../../lib/http";
import { renderRoutineEditor } from "../../../../lib/routine-editor";
import { getProfile } from "../../../../lib/users";

export const dynamic = "force-dynamic";

type Context = { params: Promise<{ slug: string }> };

export function GET(request: Request, context: Context) {
  return run(request, async () => {
    const { slug: raw } = await context.params;
    const profile = await getProfile(decodeURIComponent(raw));
    if (!profile) return json({ error: "not found" }, 404);
    return html(
      renderRoutineEditor({
        apiBase: `/u/${profile.id}`,
        calendarHref: `/u/${profile.id}`,
        slug: profile.id,
      }),
    );
  });
}

export function OPTIONS(request: Request) {
  return run(request, async () => json({}));
}
