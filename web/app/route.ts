import { renderDashboard } from "../lib/dashboard";
import { html, run } from "../lib/http";
import { loadWorkoutIndex } from "../lib/workouts";

export const dynamic = "force-dynamic";

export function OPTIONS(request: Request) {
  return run(request, async () => html(""));
}

export function GET(request: Request) {
  return run(request, async () => html(renderDashboard(await loadWorkoutIndex())));
}
