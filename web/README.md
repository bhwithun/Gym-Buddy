# Gym Buddy web

The HTTP API the Android app uses, hosted as a Next.js app on Vercel. Each profile has its own workouts and named routines in Neon Postgres.

## Onboarding

1. Open the site home page. It introduces the app and links to the [latest GitHub release](https://github.com/bhwithun/Gym-Buddy/releases/latest) for the APK.
2. Create a profile with your name and a profile address. The calendar is `https://gym.brianandkathi.com/u/your-name`. Anyone with that link can view it.
3. The connect page shows a QR code and a token. In the app, open **About** and tap **Scan profile**. The phone saves the profile URL and token. You can also paste those two fields and tap **Save worker**.
4. Push gym time, backup, and restore then use that profile. **Edit routines** on the calendar asks for the same token.

Creating a new token on the connect page invalidates the previous one. Scan the new code to reconnect the phone.

The home page is shared. Calendars and routines are not.

## Existing history

The first request after deploy attaches existing workouts and routines to a `brian` profile. That profile cannot be written until you print a token:

```bash
cd web
npm run claim-owner
```

That needs `DATABASE_URL` (the pooled Neon connection). It prints a connect URL and does not print the connection string. Running it again rotates Brian’s token. Scan that URL’s QR before relying on the phone’s old site address. Requests to `/workouts` and `/routines` without a profile are rejected.

## API

Profile base: `https://gym.brianandkathi.com/u/<slug>`

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/` | Intro, download link, create-profile form |
| `POST` | `/users` | Create a profile. Form post redirects to the connect page. JSON body `{ "displayName", "slug" }` returns `connectUrl`. |
| `GET` | `/u/:slug` | That person’s calendar (HTML) |
| `GET` | `/u/:slug/routine` | Routine editor (HTML; APIs need the token) |
| `GET` | `/u/:slug/connect?token=` | QR and token. Wrong or missing token is not found. |
| `POST` | `/u/:slug/connect?token=` | Rotate the token |
| `GET` | `/health` | Health check (`authRequired` is true) |
| `POST` | `/u/:slug/workouts` | Store or replace the snapshot for that start date |
| `GET` | `/u/:slug/workouts` | List stored dates |
| `GET` | `/u/:slug/workouts/YYYY-MM-DD` | Fetch one day |
| `GET` | `/u/:slug/routines` | List named routine versions |
| `POST` | `/u/:slug/routines` | Create a version. Body: `{ "name", "days", "overwriteByName": true }` |
| `GET` | `/u/:slug/routines/:id` | Fetch one version |
| `PUT` | `/u/:slug/routines/:id` | Create or replace that id |
| `DELETE` | `/u/:slug/routines/:id` | Delete a version |

`days` is a 7-day array (`Sun` through `Sat`). A pushed day with unfinished sets is stored with `partial: true`.

Every profile path except the calendar, the routine editor page, the connect page, and `GET /health` requires `Authorization: Bearer <token>`.

## Local

```bash
cd web
npm install
cp .env.example .env.local
# fill DATABASE_URL with the pooled Neon connection string
npm run dev
```

Copy an existing public worker into the brian profile (run `claim-owner` first):

```bash
DATABASE_URL="postgresql://…" npm run import:cloudflare -- https://your-worker.workers.dev
```

## Vercel

The project `gym-buddy` uses root directory `web`. `DATABASE_URL` is the pooled Neon URL for the `gymbuddy` database. Phone clients open `https://gym.brianandkathi.com`, so Vercel Authentication stays off for this project.
