import { html, run } from "../../lib/http";
import { renderRoutineEditor } from "../../lib/routine-editor";

export const dynamic = "force-dynamic";

export function GET(request: Request) {
  return run(request, async () => html(renderRoutineEditor()));
}

export function OPTIONS(request: Request) {
  return run(request, async () => html(""));
}
