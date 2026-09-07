# Gym Buddy

Android app for routines, daily workouts, protein tracking, and a home-screen exercise widget.

## Cloud worker (optional)

Gym Buddy can talk to a Cloudflare Worker that **you** deploy: push finished gym-time sessions, and backup/restore **named routine versions**. Edit those versions in a browser at `{worker URL}/routine`. Clipboard export/import is gone. Nothing is sent unless you paste a worker URL into **About**. Other people who install this app do not use your worker.

How the worker works, what the About fields mean, and how to deploy your own copy: **[gymm-buddy-worker/README.md](gymm-buddy-worker/README.md)**.
