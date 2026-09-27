import { json, run } from "../../../../lib/http";
import { listRoutines, saveRoutine } from "../../../../lib/routines";
import { requireProfile } from "../../../../lib/users";

export const dynamic = "force-dynamic";

type Context = { params: Promise<{ slug: string }> };

export function GET(request: Request, context: Context) {
  return run(request, async () => {
    const { slug } = await context.params;
    const auth = await requireProfile(request, decodeURIComponent(slug));
    if (!auth.ok) return auth.response;
    return listRoutines(auth.profile.id);
  });
}

export function POST(request: Request, context: Context) {
  return run(request, async () => {
    const { slug } = await context.params;
    const auth = await requireProfile(request, decodeURIComponent(slug));
    if (!auth.ok) return auth.response;
    return saveRoutine(auth.profile.id, request, null);
  });
}

export function OPTIONS(request: Request) {
  return run(request, async () => json({}));
}
