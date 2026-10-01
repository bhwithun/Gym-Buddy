import { createHash, randomBytes, timingSafeEqual } from "node:crypto";
import { ready, sql } from "./db";
import { json } from "./http";

export const SLUG_RE = /^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$/;

export type Profile = {
  id: string;
  displayName: string;
  tokenHash: string;
  shareTokenHash: string | null;
  shareToken: string | null;
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

export function acceptsToken(token: string, profile: Profile): boolean {
  if (tokenMatches(token, profile.tokenHash)) return true;
  return Boolean(profile.shareTokenHash && tokenMatches(token, profile.shareTokenHash));
}

export function queryToken(request: Request): string {
  try {
    return new URL(request.url).searchParams.get("token")?.trim() ?? "";
  } catch {
    return "";
  }
}

const PROFILE_COOKIE = "gb_profiles";

export type BrowserProfile = { slug: string; token: string };

export function bearerToken(request: Request): string {
  const header = request.headers.get("authorization") ?? "";
  return header.startsWith("Bearer ") ? header.slice(7).trim() : "";
}

export function readBrowserProfiles(request: Request): BrowserProfile[] {
  const header = request.headers.get("cookie") ?? "";
  const parts = header.split(";").map((part) => part.trim());
  const raw = parts.find((part) => part.startsWith(`${PROFILE_COOKIE}=`));
  if (!raw) return [];
  let decoded = raw.slice(PROFILE_COOKIE.length + 1);
  try {
    decoded = decodeURIComponent(decoded);
  } catch {
    return [];
  }
  const profiles: BrowserProfile[] = [];
  for (const entry of decoded.split(",")) {
    const dot = entry.indexOf(".");
    if (dot <= 0) continue;
    const slug = entry.slice(0, dot);
    const token = entry.slice(dot + 1);
    if (SLUG_RE.test(slug) && token) profiles.push({ slug, token });
  }
  return profiles;
}

export function browserToken(request: Request, slug: string): string {
  return readBrowserProfiles(request).find((profile) => profile.slug === slug)?.token ?? "";
}

function writeProfileCookie(response: Response, request: Request, profiles: BrowserProfile[]): Response {
  const secure = new URL(request.url).protocol === "https:" ? "; Secure" : "";
  const value = encodeURIComponent(profiles.map((profile) => `${profile.slug}.${profile.token}`).join(","));
  const maxAge = profiles.length === 0 ? 0 : 31536000;
  const headers = new Headers(response.headers);
  headers.append(
    "Set-Cookie",
    `${PROFILE_COOKIE}=${value}; HttpOnly; Path=/; Max-Age=${maxAge}; SameSite=Lax${secure}`,
  );
  return new Response(response.body, { status: response.status, headers });
}

export function withProfileCookie(
  response: Response,
  request: Request,
  slug: string,
  token: string,
): Response {
  const profiles = readBrowserProfiles(request).filter((profile) => profile.slug !== slug);
  profiles.push({ slug, token });
  return writeProfileCookie(response, request, profiles);
}

export function withoutProfileCookie(response: Response, request: Request, slug: string): Response {
  const profiles = readBrowserProfiles(request).filter((profile) => profile.slug !== slug);
  return writeProfileCookie(response, request, profiles);
}

export function renderPrivate(): string {
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Gym Buddy</title>
</head>
<body style="margin:0;background:#121212;color:#eee;font-family:ui-sans-serif,system-ui,sans-serif">
  <main style="max-width:640px;margin:0 auto;padding:32px 16px">
    <h1 style="color:#f9f72e">Private</h1>
    <p>Open <a style="color:#00ffff" href="/">gym.brianandkathi.com</a> in this browser. Your profile is listed there after you have opened its connect page once.</p>
  </main>
</body>
</html>`;
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
    SELECT id, display_name, token_hash, share_token_hash, share_token FROM users WHERE id = ${slug}
  `;
  const row = rows[0];
  if (!row) return null;
  return {
    id: String(row.id),
    displayName: String(row.display_name),
    tokenHash: String(row.token_hash),
    shareTokenHash: row.share_token_hash ? String(row.share_token_hash) : null,
    shareToken: row.share_token ? String(row.share_token) : null,
  };
}

export async function ensureShareToken(profile: Profile): Promise<string> {
  if (profile.shareToken) return profile.shareToken;
  const token = newToken();
  const hash = hashToken(token);
  await sql()`
    UPDATE users SET share_token = ${token}, share_token_hash = ${hash} WHERE id = ${profile.id}
  `;
  profile.shareToken = token;
  profile.shareTokenHash = hash;
  return token;
}

export async function requireProfile(
  request: Request,
  slug: string,
): Promise<{ ok: true; profile: Profile } | { ok: false; response: Response }> {
  const profile = await getProfile(slug);
  if (!profile) return { ok: false, response: json({ error: "not found" }, 404) };
  const token = bearerToken(request) || queryToken(request) || browserToken(request, profile.id);
  if (!acceptsToken(token, profile)) {
    return { ok: false, response: json({ error: "unauthorized" }, 401) };
  }
  return { ok: true, profile };
}

export async function openProfile(request: Request, slug: string): Promise<Profile | null> {
  const profile = await getProfile(slug);
  if (!profile) return null;
  const token = bearerToken(request) || queryToken(request) || browserToken(request, profile.id);
  if (!acceptsToken(token, profile)) return null;
  return profile;
}

export async function rememberedProfiles(
  request: Request,
): Promise<{ slug: string; displayName: string }[]> {
  const found: { slug: string; displayName: string }[] = [];
  for (const entry of readBrowserProfiles(request)) {
    const profile = await getProfile(entry.slug);
    if (profile && acceptsToken(entry.token, profile)) {
      found.push({ slug: profile.id, displayName: profile.displayName });
    }
  }
  return found;
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

export async function deleteProfile(slug: string): Promise<boolean> {
  await ready();
  if (!SLUG_RE.test(slug)) return false;
  const db = sql();
  const results = await db.transaction((txn) => [
    txn`DELETE FROM workouts WHERE user_id = ${slug}`,
    txn`DELETE FROM routines WHERE user_id = ${slug}`,
    txn`DELETE FROM users WHERE id = ${slug} RETURNING id`,
  ]);
  const removed = results[2];
  return Array.isArray(removed) && removed.length > 0;
}
