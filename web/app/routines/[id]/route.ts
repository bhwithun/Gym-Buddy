import { json, run } from "../../../lib/http";
import { deleteRoutine, getRoutine, isRoutineId, saveRoutine } from "../../../lib/routines";

export const dynamic = "force-dynamic";

type Context = { params: Promise<{ id: string }> };

async function idFrom(context: Context): Promise<string | Response> {
  const { id: raw } = await context.params;
  const id = decodeURIComponent(raw);
  if (!isRoutineId(id)) return json({ error: "invalid routine id" }, 400);
  return id;
}

export function GET(request: Request, context: Context) {
  return run(
    request,
    async () => {
      const id = await idFrom(context);
      if (typeof id !== "string") return id;
      return getRoutine(id);
    },
    true,
  );
}

export function PUT(request: Request, context: Context) {
  return run(
    request,
    async () => {
      const id = await idFrom(context);
      if (typeof id !== "string") return id;
      return saveRoutine(request, id);
    },
    true,
  );
}

export function DELETE(request: Request, context: Context) {
  return run(
    request,
    async () => {
      const id = await idFrom(context);
      if (typeof id !== "string") return id;
      return deleteRoutine(id);
    },
    true,
  );
}

export function OPTIONS(request: Request) {
  return run(request, async () => json({}));
}
