import { authorize, corsHeaders, json, readJsonBody } from "./http";
import { handleRoutineApi, renderRoutineEditorPage } from "./routines";

const INDEX_KEY = "index";

type IndexEntry = {
  date: string;
  durationMs: number;
  setCount: number;
  exerciseCount: number;
  isMakeup: boolean;
  estimated?: boolean;
};

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    try {
      if (request.method === "OPTIONS") {
        return new Response(null, { status: 204, headers: corsHeaders });
      }

      const url = new URL(request.url);
      if (request.method === "GET" && url.pathname === "/") {
        return await renderDashboard(env);
      }
      if (request.method === "GET" && url.pathname === "/health") {
        return json({ service: "gymm-buddy-worker", ok: true, authRequired: Boolean(env.INGEST_TOKEN) });
      }
      if (request.method === "GET" && url.pathname === "/routine") {
        return renderRoutineEditorPage();
      }

      const authorized = await authorize(request, env);
      if (!authorized) {
        return json({ error: "unauthorized" }, 401);
      }

      const routineResponse = await handleRoutineApi(request, env, url);
      if (routineResponse) {
        return routineResponse;
      }

      if (request.method === "POST" && url.pathname === "/workouts") {
        return await saveWorkout(request, env);
      }

      if (request.method === "GET" && url.pathname === "/workouts") {
        const index = await loadIndex(env);
        return json({ dates: index.map((entry) => entry.date) });
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

  const existing = await env.WORKOUTS.get(`workout:${date}`);
  const record = {
    ...payload,
    date,
    receivedAt: new Date().toISOString(),
  };
  await env.WORKOUTS.put(`workout:${date}`, JSON.stringify(record));
  await upsertIndex(env, {
    date,
    durationMs: typeof payload.durationMs === "number" ? payload.durationMs : 0,
    setCount: typeof payload.setCount === "number" ? payload.setCount : 0,
    exerciseCount: typeof payload.exerciseCount === "number" ? payload.exerciseCount : 0,
    isMakeup: payload.isMakeup === true,
    estimated: false,
  });
  console.log(JSON.stringify({ message: "workout stored", date, replaced: existing !== null }));
  return json({ ok: true, date, replaced: existing !== null }, existing !== null ? 200 : 201);
}

function entryFromRecord(date: string, rec: Record<string, unknown>): IndexEntry {
  return {
    date,
    durationMs: typeof rec.durationMs === "number" ? rec.durationMs : 0,
    setCount: typeof rec.setCount === "number" ? rec.setCount : 0,
    exerciseCount: typeof rec.exerciseCount === "number" ? rec.exerciseCount : 0,
    isMakeup: rec.isMakeup === true,
    estimated: rec.estimated === true,
  };
}

async function readStoredIndex(env: Env): Promise<IndexEntry[]> {
  const raw = await env.WORKOUTS.get(INDEX_KEY);
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw) as IndexEntry[];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

async function listWorkoutDates(env: Env): Promise<string[]> {
  const dates: string[] = [];
  let cursor: string | undefined;
  do {
    const page = await env.WORKOUTS.list({ prefix: "workout:", cursor });
    for (const key of page.keys) {
      const date = key.name.slice("workout:".length);
      if (/^\d{4}-\d{2}-\d{2}$/.test(date)) dates.push(date);
    }
    cursor = page.list_complete ? undefined : page.cursor;
  } while (cursor);
  return dates;
}

async function loadIndex(env: Env): Promise<IndexEntry[]> {
  const listed = await listWorkoutDates(env);
  let index = await readStoredIndex(env);
  const indexed = new Set(index.map((item) => item.date));
  const missing = listed.filter((date) => !indexed.has(date));
  if (missing.length === 0) {
    return index;
  }
  for (const date of missing) {
    const stored = await env.WORKOUTS.get(`workout:${date}`);
    if (!stored) continue;
    try {
      const rec = JSON.parse(stored) as Record<string, unknown>;
      index.push(entryFromRecord(date, rec));
    } catch {
      // skip unreadable snapshots
    }
  }
  index.sort((a, b) => a.date.localeCompare(b.date));
  await env.WORKOUTS.put(INDEX_KEY, JSON.stringify(index));
  return index;
}

async function upsertIndex(env: Env, entry: IndexEntry): Promise<void> {
  const index = (await loadIndex(env)).filter((item) => item.date !== entry.date);
  index.push(entry);
  index.sort((a, b) => a.date.localeCompare(b.date));
  await env.WORKOUTS.put(INDEX_KEY, JSON.stringify(index));
}

function dateFromMillis(ms: number): string {
  const d = new Date(ms);
  const y = d.getUTCFullYear();
  const m = String(d.getUTCMonth() + 1).padStart(2, "0");
  const day = String(d.getUTCDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

async function renderDashboard(env: Env): Promise<Response> {
  const index = await loadIndex(env);
  const html = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Gym Buddy</title>
  <style>
    :root { color-scheme: dark; }
    * { box-sizing: border-box; }
    body { margin: 0; font-family: ui-sans-serif, system-ui, sans-serif; background: #121212; color: #eee; }
    main { max-width: 720px; margin: 0 auto; padding: 24px 16px 48px; }
    h1 { color: #f9f72e; font-size: 28px; margin: 0 0 8px; }
    .sub { color: #9e9e9e; margin-bottom: 24px; }
    .totals { display: grid; gap: 12px; grid-template-columns: 1fr; margin-bottom: 28px; }
    @media (min-width: 560px) { .totals { grid-template-columns: 1fr 1fr; } }
    .card { background: #1e1e1e; border: 1px solid #444; border-radius: 16px; padding: 16px 18px; }
    .label { color: #bdbdbd; font-size: 13px; text-transform: uppercase; letter-spacing: .04em; }
    .value { font-size: 28px; color: #00ff88; margin-top: 6px; font-variant-numeric: tabular-nums; }
    .meta { color: #9e9e9e; margin-top: 6px; font-size: 14px; }
    .nav { display: flex; align-items: center; justify-content: space-between; margin: 8px 0 12px; }
    .nav button { background: #2a2a2a; color: #f9f72e; border: 1px solid #555; border-radius: 10px; padding: 8px 14px; font-size: 18px; cursor: pointer; }
    .nav h2 { margin: 0; font-size: 20px; }
    .cal { display: grid; grid-template-columns: repeat(7, 1fr); gap: 6px; }
    .dow { text-align: center; color: #888; font-size: 12px; padding-bottom: 4px; }
    .day { aspect-ratio: 1; border-radius: 10px; background: #1a1a1a; border: 1px solid #333; display: flex; flex-direction: column; align-items: center; justify-content: center; font-size: 14px; color: #777; }
    .day.empty { background: transparent; border-color: transparent; }
    .day.future { opacity: .35; }
    .day.off { color: #666; }
    .day.hit { background: #14331c; border-color: #00aa44; color: #e8ffe8; cursor: pointer; }
    .day.hit.est { background: #1b2a1c; border-color: #2e7d4f; color: #cfe8d4; }
    .day.hit .dur { font-size: 10px; color: #00ff88; margin-top: 2px; }
    .day.hit.est .dur { color: #7dcc9a; }
    .day.today { outline: 2px solid #f9f72e; }
    .detail { margin-top: 16px; min-height: 1.5em; color: #ddd; }
  </style>
</head>
<body>
  <main>
    <h1>Gym Buddy</h1>
    <p class="sub">One workout per day. History from 1 Jan 2020 is estimated: Tue/Thu in 2020, then Mon/Tue/Thu/Fri from 2021, 1 hour, off the first week of July and the week of Christmas. A later push replaces that day. Sessions that cross midnight count on the start day. <a href="/routine" style="color:#00ffff">Edit routines</a></p>
    <div class="totals">
      <div class="card">
        <div class="label">Total gym time</div>
        <div class="value" id="totalTime">—</div>
        <div class="meta" id="totalSince"></div>
      </div>
      <div class="card">
        <div class="label">Year to date</div>
        <div class="value" id="ytdTime">—</div>
        <div class="meta" id="ytdMeta"></div>
      </div>
    </div>
    <div class="nav">
      <button type="button" id="prev">&lsaquo;</button>
      <h2 id="monthLabel"></h2>
      <button type="button" id="next">&rsaquo;</button>
    </div>
    <div class="cal" id="cal"></div>
    <div class="detail" id="detail">Tap a green day for that session.</div>
  </main>
  <script>
    const workouts = ${JSON.stringify(index)};
    const byDate = Object.fromEntries(workouts.map(w => [w.date, w]));
    const weekdays = ["Sun","Mon","Tue","Wed","Thu","Fri","Sat"];
    let view = new Date();
    view.setDate(1);

    function fmtDur(ms) {
      const s = Math.max(0, Math.floor(ms / 1000));
      const h = Math.floor(s / 3600);
      const m = Math.floor((s % 3600) / 60);
      if (h <= 0) return m + "m";
      if (m === 0) return h + "h";
      return h + "h " + String(m).padStart(2, "0") + "m";
    }
    function ymd(d) {
      return d.getFullYear() + "-" + String(d.getMonth()+1).padStart(2,"0") + "-" + String(d.getDate()).padStart(2,"0");
    }

    function totals() {
      const year = new Date().getFullYear();
      let allMs = 0, ytdMs = 0, ytdWorkouts = 0, ytdSets = 0;
      for (const w of workouts) {
        allMs += w.durationMs || 0;
        if (w.date.startsWith(String(year))) {
          ytdMs += w.durationMs || 0;
          ytdWorkouts += 1;
          ytdSets += w.setCount || 0;
        }
      }
      const first = workouts[0]?.date;
      document.getElementById("totalTime").textContent = workouts.length ? fmtDur(allMs) : "0m";
      document.getElementById("totalSince").textContent = first
        ? workouts.length + " workout" + (workouts.length === 1 ? "" : "s") + " since " + first
        : "No workouts pushed yet";
      document.getElementById("ytdTime").textContent = fmtDur(ytdMs);
      document.getElementById("ytdMeta").textContent = ytdWorkouts + " workout" + (ytdWorkouts === 1 ? "" : "s") + " · " + ytdSets + " sets in " + year;
    }

    function render() {
      const year = view.getFullYear();
      const month = view.getMonth();
      document.getElementById("monthLabel").textContent = view.toLocaleString(undefined, { month: "long", year: "numeric" });
      const cal = document.getElementById("cal");
      cal.innerHTML = weekdays.map(d => '<div class="dow">' + d + "</div>").join("");
      const firstDow = new Date(year, month, 1).getDay();
      const daysInMonth = new Date(year, month + 1, 0).getDate();
      const today = ymd(new Date());
      for (let i = 0; i < firstDow; i++) cal.insertAdjacentHTML("beforeend", '<div class="day empty"></div>');
      for (let day = 1; day <= daysInMonth; day++) {
        const date = year + "-" + String(month + 1).padStart(2, "0") + "-" + String(day).padStart(2, "0");
        const w = byDate[date];
        const future = date > today;
        const cls = ["day", w ? "hit" : "off", w && w.estimated ? "est" : "", future ? "future" : "", date === today ? "today" : ""].join(" ");
        const dur = w ? '<div class="dur">' + fmtDur(w.durationMs) + "</div>" : "";
        cal.insertAdjacentHTML("beforeend", '<div class="' + cls + '" data-date="' + date + '"><div>' + day + "</div>" + dur + "</div>");
      }
      cal.querySelectorAll(".day.hit").forEach((el) => {
        el.addEventListener("click", () => {
          const w = byDate[el.dataset.date];
          if (!w) return;
          document.getElementById("detail").textContent =
            w.date + " · " + fmtDur(w.durationMs) + " · " + w.exerciseCount + " exercises · " + w.setCount + " sets" + (w.isMakeup ? " · makeup" : "") + (w.estimated ? " · estimated" : "");
        });
      });
    }

    document.getElementById("prev").onclick = () => { view.setMonth(view.getMonth() - 1); render(); };
    document.getElementById("next").onclick = () => { view.setMonth(view.getMonth() + 1); render(); };
    totals();
    render();
  </script>
</body>
</html>`;
  return new Response(html, {
    status: 200,
    headers: { "Content-Type": "text/html; charset=utf-8", "Cache-Control": "no-store" },
  });
}
