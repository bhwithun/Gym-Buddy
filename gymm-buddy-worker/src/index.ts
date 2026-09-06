const corsHeaders: Record<string, string> = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, Authorization",
};

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    try {
      if (request.method === "OPTIONS") {
        return new Response(null, { status: 204, headers: corsHeaders });
      }

      const url = new URL(request.url);
      if (request.method === "GET" && url.pathname === "/") {
        return json({ service: "gymm-buddy-worker", ok: true });
      }

      const authorized = await authorize(request, env);
      if (!authorized) {
        return json({ error: "unauthorized" }, 401);
      }

      if (request.method === "POST" && url.pathname === "/workouts") {
        return await saveWorkout(request, env);
      }

      if (request.method === "GET" && url.pathname === "/workouts") {
        const listed = await env.WORKOUTS.list({ prefix: "workout:" });
        const keys = listed.keys.map((key) => key.name.replace(/^workout:/, ""));
        return json({ dates: keys });
      }

      const workoutMatch = url.pathname.match(/^\/workouts\/([^/]+)$/);
      if (request.method === "GET" && workoutMatch) {
        const date = decodeURIComponent(workoutMatch[1]);
        const stored = await env.WORKOUTS.get(`workout:${date}`);
        if (stored === null) {
          return json({ error: "not found" }, 404);
        }
        return new Response(stored, {
          status: 200,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
      }

      return json({ error: "not found" }, 404);
    } catch (error) {
      const message = error instanceof Error ? error.message : "unknown";
      console.error(JSON.stringify({ message: "unhandled error", error: message }));
      return json({ error: "internal error" }, 500);
    }
  },
} satisfies ExportedHandler<Env>;

async function saveWorkout(request: Request, env: Env): Promise<Response> {
  const payload = (await request.json()) as Record<string, unknown>;
  const date = typeof payload.date === "string" ? payload.date : "";
  if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) {
    return json({ error: "date (YYYY-MM-DD) is required" }, 400);
  }

  const record = {
    ...payload,
    receivedAt: new Date().toISOString(),
  };
  await env.WORKOUTS.put(`workout:${date}`, JSON.stringify(record));
  console.log(JSON.stringify({ message: "workout stored", date }));
  return json({ ok: true, date }, 201);
}

async function authorize(request: Request, env: Env): Promise<boolean> {
  const expected = env.INGEST_TOKEN;
  if (!expected) {
    return true;
  }
  const header = request.headers.get("Authorization") ?? "";
  const provided = header.startsWith("Bearer ") ? header.slice(7) : header;
  return timingSafeEqualString(provided, expected);
}

async function timingSafeEqualString(provided: string, expected: string): Promise<boolean> {
  const encoder = new TextEncoder();
  const [providedHash, expectedHash] = await Promise.all([
    crypto.subtle.digest("SHA-256", encoder.encode(provided)),
    crypto.subtle.digest("SHA-256", encoder.encode(expected)),
  ]);
  return crypto.subtle.timingSafeEqual(providedHash, expectedHash);
}

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}