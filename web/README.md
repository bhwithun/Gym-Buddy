# Gym Buddy web

The same HTTP API the Android app already uses, hosted as a Next.js app on Vercel. Workouts and named routines live in Neon Postgres instead of Cloudflare KV.

The phone does not ship with a URL. Paste this deployment’s address into **About → Worker URL**. Leave **Worker token** blank unless `INGEST_TOKEN` is set on the Vercel project.

## API

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/` | Calendar dashboard (HTML) |
| `GET` | `/routine` | Routine editor (HTML; APIs still need the token) |
| `GET` | `/health` | Health check (`authRequired` is true if `INGEST_TOKEN` is set) |
| `POST` | `/workouts` | Store or replace the snapshot for that start date |
| `GET` | `/workouts` | List stored dates |
| `GET` | `/workouts/YYYY-MM-DD` | Fetch one day |
| `GET` | `/routines` | List named routine versions |
| `POST` | `/routines` | Create a version. Body: `{ "name", "days", "overwriteByName": true }` |
| `GET` | `/routines/:id` | Fetch one version |
| `PUT` | `/routines/:id` | Create or replace that id |
| `DELETE` | `/routines/:id` | Delete a version |

`days` is the same 7-day array the Cloudflare worker accepted (`Sun` through `Sat`).

If `INGEST_TOKEN` is set, every path except `GET /`, `GET /routine`, and `GET /health` requires `Authorization: Bearer <token>`.

## Local

```bash
cd web
npm install
cp .env.example .env.local
# fill DATABASE_URL with the pooled Neon connection string
npm run dev
```

Copy an existing public worker into the database:

```bash
DATABASE_URL="postgresql://…" npm run import:cloudflare -- https://your-worker.workers.dev
```

## Vercel

Create the project in the BrianAndKathi team with root directory `web`. Set `DATABASE_URL` to the pooled Neon URL for the `gymbuddy` database (Neon project `gym-buddy`). Turn Vercel Authentication off for this project, or the phone cannot reach `*.vercel.app`. The BrianAndKathi team protects those hostnames by default; a custom domain is the other way through, which is how mixology is published.
