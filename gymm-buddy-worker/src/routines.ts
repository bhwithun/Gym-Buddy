import { corsHeaders, json, readJsonBody } from "./http";
import { renderRoutineEditor } from "./routine-editor";

export const DAY_NAMES = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"] as const;
const RATINGS = new Set(["easy", "good", "hard"]);
const INDEX_KEY = "routine:index";
const ID_RE = /^[a-z0-9][a-z0-9-]{0,62}$/;
const MAX_EXERCISES = 40;

export type RoutineExercise = {
  title: string;
  weight: number;
  reps: number;
  sets: number;
  notes: string;
  easyGoodOrHard: string;
};

export type RoutineDay = {
  dayOfWeek: string;
  exercises: RoutineExercise[];
};

export type RoutineRecord = {
  id: string;
  name: string;
  updatedAt: string;
  days: RoutineDay[];
};

type RoutineIndexEntry = {
  id: string;
  name: string;
  updatedAt: string;
};

function valueKey(id: string): string {
  return `routine:v:${id}`;
}

export function renderRoutineEditorPage(): Response {
  return new Response(renderRoutineEditor(), {
    status: 200,
    headers: { "Content-Type": "text/html; charset=utf-8", "Cache-Control": "no-store" },
  });
}

export async function handleRoutineApi(request: Request, env: Env, url: URL): Promise<Response | null> {
  if (url.pathname === "/routines") {
    if (request.method === "GET") return listRoutines(env);
    if (request.method === "POST") return saveRoutine(request, env, null);
    return json({ error: "method not allowed" }, 405);
  }

  const match = url.pathname.match(/^\/routines\/([^/]+)$/);
  if (!match) return null;
  const id = decodeURIComponent(match[1]);
  if (!ID_RE.test(id)) {
    return json({ error: "invalid routine id" }, 400);
  }
  if (request.method === "GET") return getRoutine(env, id);
  if (request.method === "PUT") return saveRoutine(request, env, id);
  if (request.method === "DELETE") return deleteRoutine(env, id);
  return json({ error: "method not allowed" }, 405);
}

async function listRoutines(env: Env): Promise<Response> {
  const index = await loadRoutineIndex(env);
  return json({ routines: index });
}

async function getRoutine(env: Env, id: string): Promise<Response> {
  const stored = await env.WORKOUTS.get(valueKey(id));
  if (stored === null) {
    return json({ error: "not found" }, 404);
  }
  return new Response(stored, {
    status: 200,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

async function saveRoutine(request: Request, env: Env, pathId: string | null): Promise<Response> {
  const parsed = await readJsonBody(request);
  if (!parsed.ok) return parsed.response;
  if (!parsed.value || typeof parsed.value !== "object" || Array.isArray(parsed.value)) {
    return json({ error: "body must be an object" }, 400);
  }
  const body = parsed.value as Record<string, unknown>;
  const daysResult = normalizeDays(body.days ?? body.routine);
  if (!daysResult.ok) {
    return json({ error: daysResult.error }, 400);
  }

  const name = typeof body.name === "string" ? body.name.trim() : "";
  if (!name) {
    return json({ error: "name is required" }, 400);
  }
  if (name.length > 80) {
    return json({ error: "name is too long" }, 400);
  }

  const index = await loadRoutineIndex(env);
  const overwriteByName = body.overwriteByName === true;
  let id = pathId;
  if (!id && typeof body.id === "string" && body.id) {
    id = body.id;
  }
  if (!id && overwriteByName) {
    const existing = index.find((entry) => entry.name.toLowerCase() === name.toLowerCase());
    if (existing) id = existing.id;
  }
  if (id && !ID_RE.test(id)) {
    return json({ error: "invalid routine id" }, 400);
  }
  if (!id) {
    id = makeId(name, new Set(index.map((entry) => entry.id)));
  }

  const replacing = index.some((entry) => entry.id === id);
  const record: RoutineRecord = {
    id,
    name,
    updatedAt: new Date().toISOString(),
    days: daysResult.days,
  };
  await env.WORKOUTS.put(valueKey(id), JSON.stringify(record));
  const nextIndex = index.filter((entry) => entry.id !== id);
  nextIndex.push({ id, name, updatedAt: record.updatedAt });
  nextIndex.sort((a, b) => b.updatedAt.localeCompare(a.updatedAt));
  await env.WORKOUTS.put(INDEX_KEY, JSON.stringify(nextIndex));
  console.log(JSON.stringify({ message: "routine stored", id, name, replaced: replacing }));
  return json(record, replacing ? 200 : 201);
}

async function deleteRoutine(env: Env, id: string): Promise<Response> {
  const stored = await env.WORKOUTS.get(valueKey(id));
  if (stored === null) {
    return json({ error: "not found" }, 404);
  }
  await env.WORKOUTS.delete(valueKey(id));
  const index = (await loadRoutineIndex(env)).filter((entry) => entry.id !== id);
  await env.WORKOUTS.put(INDEX_KEY, JSON.stringify(index));
  console.log(JSON.stringify({ message: "routine deleted", id }));
  return json({ ok: true, id });
}

async function readStoredRoutineIndex(env: Env): Promise<RoutineIndexEntry[]> {
  const raw = await env.WORKOUTS.get(INDEX_KEY);
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw) as RoutineIndexEntry[];
    return Array.isArray(parsed) ? parsed.filter((item) => item && typeof item.id === "string") : [];
  } catch {
    return [];
  }
}

async function listRoutineIds(env: Env): Promise<string[]> {
  const listed: string[] = [];
  let cursor: string | undefined;
  do {
    const page = await env.WORKOUTS.list({ prefix: "routine:v:", cursor });
    for (const key of page.keys) {
      listed.push(key.name.slice("routine:v:".length));
    }
    cursor = page.list_complete ? undefined : page.cursor;
  } while (cursor);
  return listed;
}

async function entryFromStored(env: Env, id: string): Promise<RoutineIndexEntry | null> {
  const stored = await env.WORKOUTS.get(valueKey(id));
  if (!stored) return null;
  try {
    const rec = JSON.parse(stored) as RoutineRecord;
    return {
      id,
      name: typeof rec.name === "string" ? rec.name : id,
      updatedAt: typeof rec.updatedAt === "string" ? rec.updatedAt : new Date(0).toISOString(),
    };
  } catch {
    return null;
  }
}

async function loadRoutineIndex(env: Env): Promise<RoutineIndexEntry[]> {
  let index = await readStoredRoutineIndex(env);
  const listed = await listRoutineIds(env);
  const indexed = new Set(index.map((item) => item.id));
  let changed = false;
  // KV list() can lag behind puts. Never drop index entries just because
  // they are missing from list(); only add keys list() knows about.
  for (const id of listed) {
    if (indexed.has(id)) continue;
    const entry = await entryFromStored(env, id);
    if (!entry) continue;
    index.push(entry);
    changed = true;
  }
  index.sort((a, b) => b.updatedAt.localeCompare(a.updatedAt));
  if (changed) {
    await env.WORKOUTS.put(INDEX_KEY, JSON.stringify(index));
  }
  return index;
}

function makeId(name: string, existing: Set<string>): string {
  let base = name
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 40);
  if (!base) base = "routine";
  if (!existing.has(base)) return base;
  let id = `${base.slice(0, 32)}-${crypto.randomUUID().slice(0, 8)}`;
  while (existing.has(id)) {
    id = `${base.slice(0, 32)}-${crypto.randomUUID().slice(0, 8)}`;
  }
  return id;
}

export function normalizeDays(input: unknown): { ok: true; days: RoutineDay[] } | { ok: false; error: string } {
  if (!Array.isArray(input)) {
    return { ok: false, error: "routine must be an array of 7 days" };
  }
  if (input.length !== 7) {
    return { ok: false, error: "routine must contain exactly 7 days (Sun–Sat)" };
  }

  const byName = new Map<string, RoutineDay>();
  for (const item of input) {
    if (!item || typeof item !== "object") {
      return { ok: false, error: "each day must be an object" };
    }
    const raw = item as Record<string, unknown>;
    const dayName = normalizeDayName(raw.dayOfWeek);
    if (!dayName) {
      return { ok: false, error: 'dayOfWeek must be Sun, Mon, Tue, Wed, Thu, Fri, or Sat' };
    }
    if (byName.has(dayName)) {
      return { ok: false, error: `duplicate day: ${dayName}` };
    }
    const exercisesRaw = raw.exercises;
    if (exercisesRaw !== undefined && !Array.isArray(exercisesRaw)) {
      return { ok: false, error: `${dayName}: exercises must be an array` };
    }
    const exercises: RoutineExercise[] = [];
    for (const exercise of (exercisesRaw as unknown[]) ?? []) {
      const parsed = normalizeExercise(exercise);
      if (!parsed.ok) {
        return { ok: false, error: `${dayName}: ${parsed.error}` };
      }
      exercises.push(parsed.exercise);
    }
    if (exercises.length > MAX_EXERCISES) {
      return { ok: false, error: `${dayName}: too many exercises (max ${MAX_EXERCISES})` };
    }
    byName.set(dayName, { dayOfWeek: dayName, exercises });
  }

  const days: RoutineDay[] = [];
  for (const name of DAY_NAMES) {
    const day = byName.get(name);
    if (!day) {
      return { ok: false, error: `missing ${name}` };
    }
    days.push(day);
  }
  return { ok: true, days };
}

function normalizeDayName(value: unknown): string | null {
  if (typeof value === "number" && value >= 1 && value <= 7) {
    return DAY_NAMES[value - 1];
  }
  if (typeof value !== "string") return null;
  const trimmed = value.trim();
  const short = trimmed.slice(0, 3);
  const match = DAY_NAMES.find((name) => name.toLowerCase() === short.toLowerCase());
  return match ?? null;
}

function normalizeExercise(value: unknown): { ok: true; exercise: RoutineExercise } | { ok: false; error: string } {
  if (!value || typeof value !== "object") {
    return { ok: false, error: "exercise must be an object" };
  }
  const raw = value as Record<string, unknown>;
  const title = typeof raw.title === "string" ? raw.title.trim() : "";
  if (!title) {
    return { ok: false, error: "title is required" };
  }
  const weight = asInt(raw.weight, 0);
  const reps = asInt(raw.reps, 10);
  const sets = asInt(raw.sets, 3);
  if (weight === null || reps === null || sets === null) {
    return { ok: false, error: `${title}: weight, reps, and sets must be numbers` };
  }
  if (weight < 0 || reps < 0 || sets < 0) {
    return { ok: false, error: `${title}: weight, reps, and sets cannot be negative` };
  }
  const notes = typeof raw.notes === "string" ? raw.notes : "";
  const ratingRaw = typeof raw.easyGoodOrHard === "string" ? raw.easyGoodOrHard : typeof raw.rating === "string" ? raw.rating : "good";
  const easyGoodOrHard = RATINGS.has(ratingRaw.toLowerCase()) ? ratingRaw.toLowerCase() : "good";
  return {
    ok: true,
    exercise: { title, weight, reps, sets, notes, easyGoodOrHard },
  };
}

function asInt(value: unknown, fallback: number): number | null {
  if (value === undefined || value === null || value === "") return fallback;
  if (typeof value === "number" && Number.isFinite(value)) return Math.trunc(value);
  if (typeof value === "string" && Number.isFinite(Number(value))) return Math.trunc(Number(value));
  return null;
}
