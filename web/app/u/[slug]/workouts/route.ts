import { json, run } from "../../../../lib/http";
import { requireProfile } from "../../../../lib/users";
import { listWorkoutDates, saveWorkout } from "../../../../lib/workouts";

export const dynamic = "force-dynamic";

type Context = { params: Promise<{ slug: string }> };

export function GET(request: Request, context: Context) {
  return run(request, async () => {
    const { slug } = await context.params;
    const auth = await requireProfile(request, decodeURIComponent(slug));
    if (!auth.ok) return auth.response;
    return json({ dates: await listWorkoutDates(auth.profile.id) });
  });
}

export function POST(request: Request, context: Context) {
  return run(request, async () => {
    const { slug } = await context.params;
    const auth = await requireProfile(request, decodeURIComponent(slug));
    if (!auth.ok) return auth.response;
    return saveWorkout(auth.profile.id, request);
  });
}

export function OPTIONS(request: Request) {
  return run(request, async () => json({}));
}
