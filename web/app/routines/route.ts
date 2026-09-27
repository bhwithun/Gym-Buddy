import { profileRequired } from "../../lib/gone";
import { json, run } from "../../lib/http";

export const dynamic = "force-dynamic";

export function GET(request: Request) {
  return run(request, async () => profileRequired());
}

export function POST(request: Request) {
  return run(request, async () => profileRequired());
}

export function OPTIONS(request: Request) {
  return run(request, async () => json({}));
}
