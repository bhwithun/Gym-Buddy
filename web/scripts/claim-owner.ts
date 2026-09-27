/**
 * Attach existing workouts and routines to the brian profile and print a
 * one-time connect URL. Re-running rotates that profile's token.
 * Requires DATABASE_URL, or web/.env.local. Does not print the connection string.
 *
 *   npm run claim-owner
 */
import { createHash, randomBytes } from "node:crypto";
import { existsSync, readFileSync } from "node:fs";
import { resolve } from "node:path";
import { neon } from "@neondatabase/serverless";
import { ensureSchema } from "../lib/schema.ts";

function loadEnvLocal(): void {
  const path = resolve(process.cwd(), ".env.local");
  if (!existsSync(path)) return;
  for (const line of readFileSync(path, "utf8").split(/\r?\n/)) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) continue;
    const eq = trimmed.indexOf("=");
    if (eq < 1) continue;
    const key = trimmed.slice(0, eq).trim();
    let value = trimmed.slice(eq + 1).trim();
    if (
      (value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      value = value.slice(1, -1);
    }
    if (!process.env[key]) process.env[key] = value;
  }
}

loadEnvLocal();

function newToken(): string {
  return randomBytes(32).toString("base64url");
}

function hashToken(token: string): string {
  return createHash("sha256").update(token).digest("hex");
}

const databaseUrl = process.env.DATABASE_URL;
if (!databaseUrl) {
  console.error("DATABASE_URL is required");
  process.exit(1);
}

const sql = neon(databaseUrl);
await ensureSchema(sql);

const token = newToken();
await sql`
  INSERT INTO users (id, display_name, token_hash, created_at)
  VALUES ('brian', 'Brian', ${hashToken(token)}, ${new Date().toISOString()})
  ON CONFLICT (id) DO UPDATE SET token_hash = EXCLUDED.token_hash
`;
await ensureSchema(sql);

const origin = process.env.PUBLIC_ORIGIN || "https://gym.brianandkathi.com";
console.log(`${origin}/u/brian/connect?token=${encodeURIComponent(token)}`);
