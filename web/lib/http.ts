import { createHash, timingSafeEqual } from "node:crypto";

export const corsHeaders: Record<string, string> = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, PUT, DELETE, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, Authorization",
};

const MAX_JSON_BYTES = 256 * 1024;

export function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

export function html(body: string): Response {
  return new Response(body, {
    status: 200,
    headers: {
      "Content-Type": "text/html; charset=utf-8",
      "Cache-Control": "no-store",
    },
  });
}

export function options(): Response {
  return new Response(null, { status: 204, headers: corsHeaders });
}

export function authorize(request: Request): boolean {
  const expected = process.env.INGEST_TOKEN ?? "";
  if (!expected) return true;
  const header = request.headers.get("Authorization") ?? "";
  const provided = header.startsWith("Bearer ") ? header.slice(7) : header;
  const providedHash = createHash("sha256").update(provided).digest();
  const expectedHash = createHash("sha256").update(expected).digest();
  return timingSafeEqual(providedHash, expectedHash);
}

export async function readJsonBody(
  request: Request,
): Promise<{ ok: true; value: unknown } | { ok: false; response: Response }> {
  const declared = Number(request.headers.get("content-length") ?? "0");
  if (Number.isFinite(declared) && declared > MAX_JSON_BYTES) {
    return { ok: false, response: json({ error: "payload too large" }, 413) };
  }
  const text = await request.text();
  if (text.length > MAX_JSON_BYTES) {
    return { ok: false, response: json({ error: "payload too large" }, 413) };
  }
  try {
    return { ok: true, value: JSON.parse(text) as unknown };
  } catch {
    return { ok: false, response: json({ error: "invalid json" }, 400) };
  }
}

export async function run(
  request: Request,
  fn: () => Promise<Response>,
  auth = false,
): Promise<Response> {
  try {
    if (request.method === "OPTIONS") return options();
    if (auth && !authorize(request)) return json({ error: "unauthorized" }, 401);
    return await fn();
  } catch (error) {
    const message = error instanceof Error ? error.message : "unknown";
    console.error(JSON.stringify({ message: "unhandled error", error: message }));
    return json({ error: "internal error" }, 500);
  }
}
