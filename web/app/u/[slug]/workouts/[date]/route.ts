import { json, run } from "../../../../../lib/http";
import { requireProfile } from "../../../../../lib/users";
import { getWorkout } from "../../../../../lib/workouts";

export const dynamic = "force-dynamic";

type Context = { params: Promise<{ slug: string; date: string }> };

export function GET(request: Request, context: Context) {
  return run(request, async () => {
    const { slug, date } = await context.params;
    const auth = await requireProfile(request, decodeURIComponent(slug));
    if (!auth.ok) return auth.response;
    return getWorkout(auth.profile.id, decodeURIComponent(date));
  });
}

export function OPTIONS(request: Request) {
  return run(request, async () => json({}));
}
