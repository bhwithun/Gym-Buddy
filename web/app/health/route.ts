import { json, run } from "../../lib/http";

export const dynamic = "force-dynamic";

export function GET(request: Request) {
  return run(request, async () =>
    json({
      service: "gym-buddy",
      ok: true,
      authRequired: true,
    }),
  );
}

export function OPTIONS(request: Request) {
  return run(request, async () => json({}));
}
