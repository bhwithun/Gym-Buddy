import { renderLanding } from "../lib/landing";
import { html, run } from "../lib/http";

export const dynamic = "force-dynamic";

export function GET(request: Request) {
  return run(request, async () => html(renderLanding()));
}

export function OPTIONS(request: Request) {
  return run(request, async () => html(""));
}
