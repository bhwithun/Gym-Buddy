import { json, run } from "../../../../../lib/http";
import { deleteRoutine, getRoutine, isRoutineId, saveRoutine } from "../../../../../lib/routines";
import { requireProfile } from "../../../../../lib/users";

export const dynamic = "force-dynamic";

type Context = { params: Promise<{ slug: string; id: string }> };

export function GET(request: Request, context: Context) {
  return run(request, async () => {
    const { slug, id: raw } = await context.params;
    const id = decodeURIComponent(raw);
    if (!isRoutineId(id)) return json({ error: "invalid routine id" }, 400);
    const auth = await requireProfile(request, decodeURIComponent(slug));
    if (!auth.ok) return auth.response;
    return getRoutine(auth.profile.id, id);
  });
}

export function PUT(request: Request, context: Context) {
  return run(request, async () => {
    const { slug, id: raw } = await context.params;
    const id = decodeURIComponent(raw);
    if (!isRoutineId(id)) return json({ error: "invalid routine id" }, 400);
    const auth = await requireProfile(request, decodeURIComponent(slug));
    if (!auth.ok) return auth.response;
    return saveRoutine(auth.profile.id, request, id);
  });
}

export function DELETE(request: Request, context: Context) {
  return run(request, async () => {
    const { slug, id: raw } = await context.params;
    const id = decodeURIComponent(raw);
    if (!isRoutineId(id)) return json({ error: "invalid routine id" }, 400);
    const auth = await requireProfile(request, decodeURIComponent(slug));
    if (!auth.ok) return auth.response;
    return deleteRoutine(auth.profile.id, id);
  });
}

export function OPTIONS(request: Request) {
  return run(request, async () => json({}));
}
