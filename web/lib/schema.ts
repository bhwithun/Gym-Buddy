import { createHash, randomBytes } from "node:crypto";
import type { NeonQueryFunction } from "@neondatabase/serverless";

type Sql = NeonQueryFunction<false, false>;

let pending: Promise<void> | null = null;

export function ensureSchema(sql: Sql): Promise<void> {
  if (!pending) {
    pending = migrate(sql).catch((error) => {
      pending = null;
      throw error;
    });
  }
  return pending;
}

async function migrate(sql: Sql): Promise<void> {
  await sql`
    CREATE TABLE IF NOT EXISTS users (
      id text PRIMARY KEY,
      display_name text NOT NULL,
      token_hash text NOT NULL,
      created_at timestamptz NOT NULL DEFAULT now()
    )
  `;
  await sql`ALTER TABLE users ADD COLUMN IF NOT EXISTS share_token_hash text`;
  await sql`
    CREATE TABLE IF NOT EXISTS workouts (
      user_id text NOT NULL,
      date text NOT NULL,
      payload jsonb NOT NULL,
      duration_ms integer NOT NULL DEFAULT 0,
      set_count integer NOT NULL DEFAULT 0,
      exercise_count integer NOT NULL DEFAULT 0,
      is_makeup boolean NOT NULL DEFAULT false,
      estimated boolean NOT NULL DEFAULT false,
      partial boolean NOT NULL DEFAULT false,
      received_at timestamptz NOT NULL DEFAULT now(),
      PRIMARY KEY (user_id, date)
    )
  `;
  await sql`
    CREATE TABLE IF NOT EXISTS routines (
      id text PRIMARY KEY,
      user_id text NOT NULL,
      name text NOT NULL,
      updated_at timestamptz NOT NULL,
      days jsonb NOT NULL
    )
  `;
  await sql`ALTER TABLE workouts ADD COLUMN IF NOT EXISTS user_id text`;
  await sql`ALTER TABLE workouts ADD COLUMN IF NOT EXISTS partial boolean NOT NULL DEFAULT false`;
  await sql`ALTER TABLE routines ADD COLUMN IF NOT EXISTS user_id text`;
  const orphans = await sql`
    SELECT
      (SELECT count(*)::int FROM workouts WHERE user_id IS NULL) AS workouts,
      (SELECT count(*)::int FROM routines WHERE user_id IS NULL) AS routines
  `;
  const orphanCount = Number(orphans[0]?.workouts ?? 0) + Number(orphans[0]?.routines ?? 0);
  if (orphanCount > 0) {
    const tokenHash = createHash("sha256").update(randomBytes(32)).digest("hex");
    await sql`
      INSERT INTO users (id, display_name, token_hash, created_at)
      VALUES ('brian', 'Brian', ${tokenHash}, ${new Date().toISOString()})
      ON CONFLICT (id) DO NOTHING
    `;
  }
  await sql`
    UPDATE workouts
    SET user_id = 'brian'
    WHERE user_id IS NULL
      AND EXISTS (SELECT 1 FROM users WHERE id = 'brian')
  `;
  await sql`
    UPDATE routines
    SET user_id = 'brian'
    WHERE user_id IS NULL
      AND EXISTS (SELECT 1 FROM users WHERE id = 'brian')
  `;
  await sql`CREATE INDEX IF NOT EXISTS routines_user_id_idx ON routines (user_id)`;
  await sql`
    DO $$
    DECLARE
      cname text;
      cols text;
    BEGIN
      SELECT con.conname,
             (
               SELECT string_agg(a.attname, ',' ORDER BY u.ord)
               FROM unnest(i.indkey) WITH ORDINALITY AS u(attnum, ord)
               JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attnum = u.attnum
             )
        INTO cname, cols
      FROM pg_constraint con
      JOIN pg_class rel ON rel.oid = con.conrelid
      JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
      JOIN pg_index i ON i.indexrelid = con.conindid
      WHERE nsp.nspname = 'public' AND rel.relname = 'workouts' AND con.contype = 'p';

      IF cols = 'date' AND NOT EXISTS (SELECT 1 FROM workouts WHERE user_id IS NULL) THEN
        EXECUTE format('ALTER TABLE workouts DROP CONSTRAINT %I', cname);
        ALTER TABLE workouts ALTER COLUMN user_id SET NOT NULL;
        ALTER TABLE workouts ADD PRIMARY KEY (user_id, date);
      END IF;

      IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'routines'
          AND column_name = 'user_id'
          AND is_nullable = 'YES'
      ) AND NOT EXISTS (SELECT 1 FROM routines WHERE user_id IS NULL) THEN
        ALTER TABLE routines ALTER COLUMN user_id SET NOT NULL;
      END IF;
    END $$
  `;
}
