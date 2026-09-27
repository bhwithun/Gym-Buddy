import { html, json, run } from "../../../../lib/http";
import { renderRoutineEditor } from "../../../../lib/routine-editor";
import { openProfile, renderPrivate } from "../../../../lib/users";

export const dynamic = "force-dynamic";

type Context = { params: Promise<{ slug: string }> };

export function GET(request: Request, context: Context) {
  return run(request, async () => {
    const { slug: raw } = await context.params;
    const profile = await openProfile(request, decodeURIComponent(raw));
    if (!profile) return html(renderPrivate());
    return html(
      renderRoutineEditor({
        apiBase: `/u/${profile.id}`,
        calendarHref: `/u/${profile.id}`,
        slug: profile.id,
        signedIn: true,
      }),
    );
  });
}

export function OPTIONS(request: Request) {
  return run(request, async () => json({}));
}
