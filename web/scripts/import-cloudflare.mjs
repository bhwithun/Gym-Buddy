/**
 * Copy workouts and routines from the public Cloudflare worker into Neon.
 * Requires DATABASE_URL. Does not print the connection string.
 *
 *   node scripts/import-cloudflare.mjs
 *   node scripts/import-cloudflare.mjs https://gymm-buddy-worker.example.workers.dev
 */
import { neon } from "@neondatabase/serverless";

const source = (process.argv[2] || "https://gymm-buddy-worker.brian-952.workers.dev").replace(/\/$/, "");
const databaseUrl = process.env.DATABASE_URL;
if (!databaseUrl) {
  console.error("DATABASE_URL is required");
  process.exit(1);
}

const sql = neon(databaseUrl);

async function getJson(path) {
  const response = await fetch(`${source}${path}`);
  if (!response.ok) {
    throw new Error(`${path} returned ${response.status}`);
  }
  return response.json();
}

async function mapPool(items, limit, fn) {
  const results = new Array(items.length);
  let next = 0;
  async function worker() {
    while (next < items.length) {
      const index = next++;
      results[index] = await fn(items[index], index);
    }
  }
  await Promise.all(Array.from({ length: Math.min(limit, items.length) }, () => worker()));
  return results;
}

const listed = await getJson("/workouts");
const dates = Array.isArray(listed.dates) ? listed.dates : [];
console.log(`fetching ${dates.length} workouts from ${source}`);

const workouts = await mapPool(dates, 16, async (date) => {
  const record = await getJson(`/workouts/${encodeURIComponent(date)}`);
  return record;
});

const routinesBody = await getJson("/routines");
const routineSummaries = Array.isArray(routinesBody.routines) ? routinesBody.routines : [];
const routines = await mapPool(routineSummaries, 4, async (entry) => getJson(`/routines/${encodeURIComponent(entry.id)}`));

console.log(`inserting ${workouts.length} workouts and ${routines.length} routines`);

const batchSize = 40;
for (let offset = 0; offset < workouts.length; offset += batchSize) {
  const batch = workouts.slice(offset, offset + batchSize);
  await sql.transaction(
    batch.map((record) => {
      const date = String(record.date);
      const receivedAt = typeof record.receivedAt === "string" ? record.receivedAt : new Date().toISOString();
      return sql`
        INSERT INTO workouts (
          date, payload, duration_ms, set_count, exercise_count, is_makeup, estimated, received_at
        ) VALUES (
          ${date},
          ${JSON.stringify(record)}::jsonb,
          ${typeof record.durationMs === "number" ? record.durationMs : 0},
          ${typeof record.setCount === "number" ? record.setCount : 0},
          ${typeof record.exerciseCount === "number" ? record.exerciseCount : 0},
          ${record.isMakeup === true},
          ${record.estimated === true},
          ${receivedAt}
        )
        ON CONFLICT (date) DO UPDATE SET
          payload = EXCLUDED.payload,
          duration_ms = EXCLUDED.duration_ms,
          set_count = EXCLUDED.set_count,
          exercise_count = EXCLUDED.exercise_count,
          is_makeup = EXCLUDED.is_makeup,
          estimated = EXCLUDED.estimated,
          received_at = EXCLUDED.received_at
      `;
    }),
  );
  console.log(`workouts ${Math.min(offset + batch.length, workouts.length)}/${workouts.length}`);
}

for (const record of routines) {
  await sql`
    INSERT INTO routines (id, name, updated_at, days)
    VALUES (
      ${record.id},
      ${record.name},
      ${record.updatedAt},
      ${JSON.stringify(record.days)}::jsonb
    )
    ON CONFLICT (id) DO UPDATE SET
      name = EXCLUDED.name,
      updated_at = EXCLUDED.updated_at,
      days = EXCLUDED.days
  `;
  console.log(`routine ${record.id} (${record.name})`);
}

const counts = await sql`
  SELECT
    (SELECT count(*) FROM workouts) AS workouts,
    (SELECT count(*) FROM routines) AS routines
`;
console.log(JSON.stringify(counts[0]));
