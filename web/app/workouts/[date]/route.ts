import { json, run } from "../../../lib/http";
import { getWorkout } from "../../../lib/workouts";

export const dynamic = "force-dynamic";

type Context = { params: Promise<{ date: string }> };

export function GET(request: Request, context: Context) {
  return run(
    request,
    async () => {
      const { date } = await context.params;
      return getWorkout(decodeURIComponent(date));
    },
    true,
  );
}

export function OPTIONS(request: Request) {
  return run(request, async () => json({}));
}
