import { asNumber, asObject, ready, sql } from "./db";
import type { IndexEntry } from "./dashboard";
import { json, readJsonBody } from "./http";

export async function listWorkoutDates(userId: string): Promise<string[]> {
  await ready();
  const rows = await sql()`
    SELECT date FROM workouts WHERE user_id = ${userId} ORDER BY date ASC
  `;
  return rows.map((row) => String(row.date));
}

export async function loadWorkoutIndex(userId: string): Promise<IndexEntry[]> {
  await ready();
  const rows = await sql()`
    SELECT date, duration_ms, set_count, exercise_count, is_makeup, estimated, partial
    FROM workouts
    WHERE user_id = ${userId}
    ORDER BY date ASC
  `;
  return rows.map((row) => ({
    date: String(row.date),
    durationMs: asNumber(row.duration_ms),
    setCount: asNumber(row.set_count),
    exerciseCount: asNumber(row.exercise_count),
    isMakeup: row.is_makeup === true,
    estimated: row.estimated === true,
    partial: row.partial === true,
  }));
}

export async function getWorkout(userId: string, date: string): Promise<Response> {
  await ready();
  if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) {
    return json({ error: "not found" }, 404);
  }
  const rows = await sql()`
    SELECT payload FROM workouts WHERE user_id = ${userId} AND date = ${date}
  `;
  const payload = asObject(rows[0]?.payload);
  if (!payload) return json({ error: "not found" }, 404);
  return json(payload);
}

function dateFromMillis(ms: number): string {
  const d = new Date(ms);
  const y = d.getUTCFullYear();
  const m = String(d.getUTCMonth() + 1).padStart(2, "0");
  const day = String(d.getUTCDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

function payloadIsPartial(payload: Record<string, unknown>): boolean {
  if (!Array.isArray(payload.exercises)) return payload.partial === true;
  return payload.exercises.some((exercise) => {
    if (!exercise || typeof exercise !== "object") return false;
    const raw = exercise as Record<string, unknown>;
    const sets = typeof raw.sets === "number" ? raw.sets : Number(raw.sets);
    const done = typeof raw.completedSets === "number" ? raw.completedSets : Number(raw.completedSets);
    return Number.isFinite(sets) && sets > 0 && Number.isFinite(done) && done < sets;
  });
}

export async function saveWorkout(userId: string, request: Request): Promise<Response> {
  await ready();
  const parsed = await readJsonBody(request);
  if (!parsed.ok) return parsed.response;
  if (!parsed.value || typeof parsed.value !== "object" || Array.isArray(parsed.value)) {
    return json({ error: "invalid json" }, 400);
  }
  const payload = parsed.value as Record<string, unknown>;
  let date = typeof payload.date === "string" ? payload.date : "";
  const startMs = typeof payload.startMs === "number" ? payload.startMs : 0;
  if (!/^\d{4}-\d{2}-\d{2}$/.test(date) && startMs > 0) {
    date = dateFromMillis(startMs);
  }
  if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) {
    return json({ error: "date (YYYY-MM-DD) is required" }, 400);
  }

  const partial = payloadIsPartial(payload);
  const existing = await sql()`
    SELECT 1 AS found FROM workouts WHERE user_id = ${userId} AND date = ${date}
  `;
  const record = {
    ...payload,
    date,
    partial,
    receivedAt: new Date().toISOString(),
  };
  await sql()`
    INSERT INTO workouts (
      user_id, date, payload, duration_ms, set_count, exercise_count, is_makeup, estimated, partial, received_at
    ) VALUES (
      ${userId},
      ${date},
      ${JSON.stringify(record)}::jsonb,
      ${typeof payload.durationMs === "number" ? payload.durationMs : 0},
      ${typeof payload.setCount === "number" ? payload.setCount : 0},
      ${typeof payload.exerciseCount === "number" ? payload.exerciseCount : 0},
      ${payload.isMakeup === true},
      ${payload.estimated === true},
      ${partial},
      ${record.receivedAt}
    )
    ON CONFLICT (user_id, date) DO UPDATE SET
      payload = EXCLUDED.payload,
      duration_ms = EXCLUDED.duration_ms,
      set_count = EXCLUDED.set_count,
      exercise_count = EXCLUDED.exercise_count,
      is_makeup = EXCLUDED.is_makeup,
      estimated = EXCLUDED.estimated,
      partial = EXCLUDED.partial,
      received_at = EXCLUDED.received_at
  `;
  const replaced = existing.length > 0;
  console.log(JSON.stringify({ message: "workout stored", date, replaced }));
  return json({ ok: true, date, replaced }, replaced ? 200 : 201);
}
