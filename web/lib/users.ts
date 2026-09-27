import { createHash, randomBytes, timingSafeEqual } from "node:crypto";
import { ready, sql } from "./db";
import { json } from "./http";

export const SLUG_RE = /^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$/;

export type Profile = {
  id: string;
  displayName: string;
  tokenHash: string;
};

export function newToken(): string {
  return randomBytes(32).toString("base64url");
}

export function hashToken(token: string): string {
  return createHash("sha256").update(token).digest("hex");
}

export function tokenMatches(token: string, hash: string): boolean {
  if (!token || !/^[a-f0-9]{64}$/.test(hash)) return false;
  const provided = createHash("sha256").update(token).digest();
  const expected = Buffer.from(hash, "hex");
  if (provided.length !== expected.length) return false;
  return timingSafeEqual(provided, expected);
}

export function bearerToken(request: Request): string {
  const header = request.headers.get("authorization") ?? "";
  return header.startsWith("Bearer ") ? header.slice(7).trim() : "";
}

export function slugifyName(name: string): string {
  return name
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 63);
}

export async function getProfile(slug: string): Promise<Profile | null> {
  await ready();
  if (!SLUG_RE.test(slug)) return null;
  const rows = await sql()`
    SELECT id, display_name, token_hash FROM users WHERE id = ${slug}
  `;
  const row = rows[0];
  if (!row) return null;
  return {
    id: String(row.id),
    displayName: String(row.display_name),
    tokenHash: String(row.token_hash),
  };
}

export async function requireProfile(
  request: Request,
  slug: string,
): Promise<{ ok: true; profile: Profile } | { ok: false; response: Response }> {
  const profile = await getProfile(slug);
  if (!profile) return { ok: false, response: json({ error: "not found" }, 404) };
  if (!tokenMatches(bearerToken(request), profile.tokenHash)) {
    return { ok: false, response: json({ error: "unauthorized" }, 401) };
  }
  return { ok: true, profile };
}

export async function createProfile(
  displayName: string,
  slug: string,
): Promise<{ ok: true; token: string } | { ok: false; error: string }> {
  await ready();
  const token = newToken();
  const rows = await sql()`
    INSERT INTO users (id, display_name, token_hash, created_at)
    VALUES (${slug}, ${displayName}, ${hashToken(token)}, ${new Date().toISOString()})
    ON CONFLICT (id) DO NOTHING
    RETURNING id
  `;
  if (rows.length === 0) return { ok: false, error: "That profile address is already taken." };
  return { ok: true, token };
}

export async function rotateToken(slug: string): Promise<string | null> {
  await ready();
  const token = newToken();
  const rows = await sql()`
    UPDATE users SET token_hash = ${hashToken(token)} WHERE id = ${slug} RETURNING id
  `;
  if (rows.length === 0) return null;
  return token;
}

export function connectPath(slug: string, token: string): string {
  return `/u/${encodeURIComponent(slug)}/connect?token=${encodeURIComponent(token)}`;
}
