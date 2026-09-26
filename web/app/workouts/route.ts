import { json, run } from "../../lib/http";
import { listWorkoutDates, saveWorkout } from "../../lib/workouts";

export const dynamic = "force-dynamic";

export function GET(request: Request) {
  return run(request, async () => json({ dates: await listWorkoutDates() }), true);
}

export function POST(request: Request) {
  return run(request, () => saveWorkout(request), true);
}

export function OPTIONS(request: Request) {
  return run(request, async () => json({}));
}
