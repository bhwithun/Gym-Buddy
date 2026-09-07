# gymm-buddy-worker

A small [Cloudflare Worker](https://developers.cloudflare.com/workers/) that Gym Buddy can talk to.

The Android app does **not** ship with a worker URL. If you leave the About fields empty, the app never talks to the cloud. That is intentional: someone else running a copy of this app will not send their gym data to your account unless they paste *their* worker URL into *their* app.

## What the worker does

1. **Gym time log.** When you complete every set for the day, Gym Buddy can show a **Gym time** screen. If you have configured a worker, that screen offers **Push gym time**. Pushing stores one snapshot per calendar start-day in KV. Opening the worker URL in a browser shows a calendar, total gym time, and year-to-date stats.

2. **Named routines.** The app **Backup** / **Restore** buttons send the weekly routine to this worker under a name you choose (for example `hypertrophy` or `cut 2026`). Saving the same name again overwrites that version. **Restore** lists those names and pulls one into the phone. Clipboard export/import is gone.

3. **PC editor.** Open `{worker URL}/routine` in a browser. Enter the same token as **About → Worker token**. You can create, rename, duplicate, delete, and edit exercises for each day, or use **Edit as JSON** to change the current week as a 7-day array. Save, then Restore that name in the app.

It is a personal log in the cloud, not a social network and not a shared Gym Buddy backend.

## The two fields on the About page

Open **About** in the app. At the bottom:

**Worker URL (optional)**  
The HTTPS address of *your* deployed worker, for example:

`https://gymm-buddy-worker.<your-subdomain>.workers.dev`

Gym Buddy POSTs workouts to `{that URL}/workouts` and routines to `{that URL}/routines`. Leave this blank if you do not want any cloud features. Saving an empty URL turns them off.

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

5. On the **Routine** tab, tap **Backup** and name the version. Or open **Edit on PC** / `{worker URL}/routine`, use **Edit as JSON** if you want to change the week as text, save, then **Restore** in the app.

### Check that it worked

Open the worker URL in a browser for the calendar, or `/routine` for the editor. JSON:

```bash
# Health check (no token required)
curl https://gymm-buddy-worker.<account>.workers.dev/health

# List stored workout dates (send the token if you set one)
curl -H "Authorization: Bearer YOUR_TOKEN" \
  https://gymm-buddy-worker.<account>.workers.dev/workouts

# One day
curl -H "Authorization: Bearer YOUR_TOKEN" \
  https://gymm-buddy-worker.<account>.workers.dev/workouts/2026-09-06

# Named routines
curl -H "Authorization: Bearer YOUR_TOKEN" \
  https://gymm-buddy-worker.<account>.workers.dev/routines
```

## API (for your own tools)

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/` | Calendar dashboard (HTML) |
| `GET` | `/routine` | Routine editor (HTML; APIs still need the token) |
| `GET` | `/health` | Health check (`authRequired` is true if `INGEST_TOKEN` is set) |
| `POST` | `/workouts` | Store or **replace** the snapshot for that start date |
| `GET` | `/workouts` | List stored dates |
| `GET` | `/workouts/YYYY-MM-DD` | Fetch one day |
| `GET` | `/routines` | List named routine versions |
| `POST` | `/routines` | Create a version. Body: `{ "name", "days", "overwriteByName": true }` |
| `GET` | `/routines/:id` | Fetch one version |
| `PUT` | `/routines/:id` | Create or replace that id |
| `DELETE` | `/routines/:id` | Delete a version |

`days` is an array of **exactly 7** objects, one each for `Sun` `Mon` `Tue` `Wed` `Thu` `Fri` `Sat`:

```json
{
  "dayOfWeek": "Mon",
  "exercises": [
    {
      "title": "Bench Press",
      "weight": 135,
      "reps": 8,
      "sets": 3,
      "notes": "",
      "easyGoodOrHard": "good"
    }
  ]
}
```

An empty `exercises` array is a rest day. `easyGoodOrHard` is `easy`, `good`, or `hard`.

If `INGEST_TOKEN` is set on the worker, every path except `GET /`, `GET /routine`, and `GET /health` requires `Authorization: Bearer <token>`.
