# gymm-buddy-worker

A small [Cloudflare Worker](https://developers.cloudflare.com/workers/) that Gym Buddy can talk to after you finish a workout.

The Android app does **not** ship with a worker URL. If you leave the About fields empty, Gym time never offers to push anywhere. That is intentional: someone else running a copy of this app will not send their gym data to your account unless they paste *their* worker URL into *their* app.

## What the worker does

When you complete every set for the day, Gym Buddy can show a **Gym time** screen (duration, start/finish, exercise list). If you have configured a worker, that screen also offers **Push gym time**.

Pushing sends a JSON snapshot of that session to your worker:

- **Date the workout started** (if you train past midnight, it still counts as that start day)
- Whether it was a makeup day
- Start time, end time, and duration
- Each exercise (name, weight, reps, sets, completed sets, rating, notes)

There is **one workout per calendar start-day**. Pushing again for the same start date **replaces** the previous snapshot.

The worker stores that snapshot in Cloudflare KV. Opening the worker URL in a browser shows a **calendar** of days you did and did not train, plus:

- **Total gym time** since your first pushed workout
- **Year-to-date** workouts, gym time, and sets

It is a personal log in the cloud, not a social network and not a shared Gym Buddy backend.

## The two fields on the About page

Open **About** in the app. At the bottom:

**Worker URL (optional)**  
The HTTPS address of *your* deployed worker, for example:

`https://gymm-buddy-worker.<your-subdomain>.workers.dev`

Gym Buddy POSTs to `{that URL}/workouts`. Leave this blank if you do not want any cloud push. Saving an empty URL turns the feature off.

**Worker token (optional)**  
A shared secret. Only needed if you set `INGEST_TOKEN` on the worker (recommended if the URL is public). The app sends it as `Authorization: Bearer <token>`. If the worker has no token configured, leave this blank.

Tap **Save worker** after you change either field.

## Set up a worker for yourself

You need a [Cloudflare](https://dash.cloudflare.com/sign-up) account and [Node.js](https://nodejs.org/).

1. Clone this repo and open the worker folder:

   ```bash
   git clone https://github.com/bhwithun/Gym-Buddy.git
   cd Gym-Buddy/gymm-buddy-worker
   npm install
   ```

2. Log in and deploy:

   ```bash
   npx wrangler login
   npx wrangler deploy
   ```

   Wrangler prints a URL like `https://gymm-buddy-worker.<account>.workers.dev`.

3. (Recommended) Protect ingest with a token you invent. Do not commit it.

   ```bash
   npx wrangler secret put INGEST_TOKEN
   ```

4. In Gym Buddy → **About**, paste the workers.dev URL into **Worker URL**. If you set a secret, paste the same value into **Worker token**. Save.

5. Finish a workout. On the Gym time screen, tap **Push gym time**. A successful push stores that day on your worker.

### Check that it worked

Open the worker URL in a browser for the calendar. JSON:

```bash
# Health check (no token required)
curl https://gymm-buddy-worker.<account>.workers.dev/health

# List stored dates (send the token if you set one)
curl -H "Authorization: Bearer YOUR_TOKEN" \
  https://gymm-buddy-worker.<account>.workers.dev/workouts

# One day
curl -H "Authorization: Bearer YOUR_TOKEN" \
  https://gymm-buddy-worker.<account>.workers.dev/workouts/2026-09-06
```

## API (for your own tools)

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/` | Calendar dashboard (HTML) |
| `GET` | `/health` | Health check |
| `POST` | `/workouts` | Store or **replace** the snapshot for that start date |
| `GET` | `/workouts` | List stored dates |
| `GET` | `/workouts/YYYY-MM-DD` | Fetch one day |

If `INGEST_TOKEN` is set on the worker, every path except `GET /` and `GET /health` requires `Authorization: Bearer <token>`.
