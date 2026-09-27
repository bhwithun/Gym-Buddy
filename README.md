# Gym Buddy

Android app for routines, daily workouts, protein tracking, and a home-screen exercise widget.

## Cloud log (optional)

Gym Buddy can talk to a small web app that **you** deploy: push finished gym-time sessions, and backup/restore **named routine versions**. Edit those versions in a browser at `{URL}/routine`. Clipboard export/import is gone. Nothing is sent unless you paste that URL into **About**. Other people who install this app do not use your database.

The current app is a Next.js service on Vercel with workouts and routines stored in Neon. Each person has a profile: a public calendar, their own routine backups, and a token the phone stores. Setup starts at the site’s home page. Details: **[web/README.md](web/README.md)**.

The previous Cloudflare Worker is still in [gymm-buddy-worker](gymm-buddy-worker/README.md). The phone keeps using whichever profile URL is saved on the About screen.
