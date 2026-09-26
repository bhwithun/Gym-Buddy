import { json, run } from "../../lib/http";
import { listRoutines, saveRoutine } from "../../lib/routines";

export const dynamic = "force-dynamic";

export function GET(request: Request) {
  return run(request, () => listRoutines(), true);
}

export function POST(request: Request) {
  return run(request, () => saveRoutine(request, null), true);
}

export function OPTIONS(request: Request) {
  return run(request, async () => json({}));
}
