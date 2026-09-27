/**
 * Attach existing workouts and routines to the brian profile and print a
 * one-time connect URL. Re-running rotates that profile's token.
 * Requires DATABASE_URL. Does not print the connection string.
 *
 *   npm run claim-owner
 */
import { neon } from "@neondatabase/serverless";
import { ensureSchema } from "../lib/schema";
import { hashToken, newToken } from "../lib/users";

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
