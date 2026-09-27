import { renderLanding } from "../lib/landing";
import { html, run } from "../lib/http";
import { rememberedProfiles } from "../lib/users";

export const dynamic = "force-dynamic";

export function GET(request: Request) {
  return run(request, async () => html(renderLanding("", await rememberedProfiles(request))));
}

export function OPTIONS(request: Request) {
  return run(request, async () => html(""));
}
