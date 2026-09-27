/**
 * Print a share URL for the brian profile. Re-running replaces the share
 * secret and leaves the phone token alone.
 * Requires DATABASE_URL, or web/.env.local. Does not print the connection string.
 *
 *   npm run share-link
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

const databaseUrl = process.env.DATABASE_URL;
if (!databaseUrl) {
  console.error("DATABASE_URL is required");
  process.exit(1);
}

const sql = neon(databaseUrl);
await ensureSchema(sql);
const token = randomBytes(32).toString("base64url");
const tokenHash = createHash("sha256").update(token).digest("hex");
const rows = await sql`
  UPDATE users SET share_token_hash = ${tokenHash} WHERE id = 'brian' RETURNING id
`;
if (rows.length === 0) {
  console.error("No brian profile yet. Run npm run claim-owner first.");
  process.exit(1);
}
const origin = process.env.PUBLIC_ORIGIN || "https://gym.brianandkathi.com";
const secret = encodeURIComponent(token);
console.log(`${origin}/u/brian?token=${secret}`);
