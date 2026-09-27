import { escapeHtml } from "./http";

const RELEASES = "https://github.com/bhwithun/Gym-Buddy/releases/latest";

export function renderLanding(
  error = "",
  profiles: { slug: string; displayName: string }[] = [],
): string {
  const errorHtml = error
    ? `<p class="status err">${escapeHtml(error)}</p>`
    : "";
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Gym Buddy</title>
  <style>
    :root { color-scheme: dark; }
    * { box-sizing: border-box; }
    body { margin: 0; font-family: ui-sans-serif, system-ui, sans-serif; background: #121212; color: #eee; }
    main { max-width: 640px; margin: 0 auto; padding: 32px 16px 64px; }
    h1 { color: #f9f72e; font-size: 36px; margin: 0 0 8px; }
    h2 { font-size: 20px; margin: 28px 0 8px; }
    a { color: #00ffff; }
    p { line-height: 1.5; color: #ddd; }
    .card { background: #1e1e1e; border: 1px solid #444; border-radius: 16px; padding: 16px 18px; margin-top: 16px; }
    label { display: block; margin-top: 12px; color: #bdbdbd; font-size: 13px; text-transform: uppercase; letter-spacing: .04em; }
    input { width: 100%; margin-top: 6px; background: #121212; color: #eee; border: 1px solid #555; border-radius: 8px; padding: 10px 12px; font: inherit; }
    button { margin-top: 16px; background: #5B2C6F; color: #fff; border: 0; border-radius: 10px; padding: 12px 16px; font: inherit; cursor: pointer; }
    .hint { color: #9e9e9e; font-size: 14px; }
    .status.err { color: #ff8a9a; }
    .download { display: inline-block; margin-top: 8px; background: #14331c; border: 1px solid #00aa44; color: #e8ffe8; border-radius: 10px; padding: 12px 16px; text-decoration: none; }
  </style>
</head>
<body>
  <main>
    <h1>Gym Buddy</h1>
    <p>An Android app for the week's routine, logging sets as you lift, protein for the day, and a home-screen exercise widget.</p>
    <p>Each person gets their own gym calendar and their own named routine backups. The phone stores workouts on the device. Cloud backup starts after you connect a profile.</p>
    ${
      profiles.length
        ? `<div class="card"><h2>Your profiles</h2>${profiles
            .map(
              (profile) =>
                `<p><strong>${escapeHtml(profile.displayName)}</strong><br><a href="/u/${escapeHtml(profile.slug)}?tab=log">Log</a> · <a href="/u/${escapeHtml(profile.slug)}?tab=stats">Stats</a> · <a href="/u/${escapeHtml(profile.slug)}?tab=routines">Routines</a></p>`,
            )
            .join("")}</div>`
        : ""
    }
    <p><a class="download" href="${RELEASES}">Download the latest release</a></p>
    <p class="hint">That link opens the newest GitHub release of Gym Buddy. Install the APK from there, then come back to this page and create a profile.</p>
    <div class="card">
      <h2>Create your profile</h2>
      <p class="hint">You will get a QR code. In the app, open About and tap Scan profile.</p>
      ${errorHtml}
      <form method="post" action="/users">
        <label>Your name
          <input name="displayName" type="text" required maxlength="80" autocomplete="name" />
        </label>
        <label>Profile address
          <input name="slug" type="text" required maxlength="63" pattern="[A-Za-z0-9][A-Za-z0-9-]{0,62}" autocapitalize="none" spellcheck="false" placeholder="your-name" />
        </label>
        <p class="hint">This browser will remember the profile. The gym log and routines stay private. The phone connects by scanning the QR code, and you never copy a token.</p>
        <button type="submit">Create profile</button>
      </form>
    </div>
  </main>
</body>
</html>`;
}
