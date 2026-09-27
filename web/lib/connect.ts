import QRCode from "qrcode";
import { escapeHtml } from "./http";

export async function renderConnect(options: {
  origin: string;
  slug: string;
  displayName: string;
  token: string;
}): Promise<string> {
  const profilePath = `/u/${encodeURIComponent(options.slug)}`;
  const connectUrl = `${options.origin}${profilePath}?token=${encodeURIComponent(options.token)}`;
  const calendarUrl = `${options.origin}${profilePath}`;
  const qr = await QRCode.toDataURL(connectUrl, { margin: 1, width: 320, errorCorrectionLevel: "M" });
  const rotateAction = `${profilePath}/connect?token=${encodeURIComponent(options.token)}`;
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <meta name="referrer" content="no-referrer" />
  <title>Connect ${escapeHtml(options.displayName)}</title>
  <style>
    :root { color-scheme: dark; }
    * { box-sizing: border-box; }
    body { margin: 0; font-family: ui-sans-serif, system-ui, sans-serif; background: #121212; color: #eee; }
    main { max-width: 640px; margin: 0 auto; padding: 32px 16px 64px; }
    h1 { color: #f9f72e; font-size: 32px; margin: 0 0 8px; }
    a { color: #00ffff; }
    p { line-height: 1.5; }
    img { width: min(320px, 100%); height: auto; background: #fff; border-radius: 12px; padding: 8px; }
    code, input { font-family: ui-monospace, monospace; font-size: 13px; }
    input { width: 100%; margin-top: 8px; background: #121212; color: #eee; border: 1px solid #555; border-radius: 8px; padding: 10px 12px; }
    button { margin-top: 16px; background: #2a2a2a; color: #f9f72e; border: 1px solid #555; border-radius: 10px; padding: 10px 14px; font: inherit; cursor: pointer; }
    .hint { color: #9e9e9e; }
  </style>
</head>
<body>
  <main>
    <h1>${escapeHtml(options.displayName)}</h1>
    <p>Install Gym Buddy from the <a href="https://github.com/bhwithun/Gym-Buddy/releases/latest">latest GitHub release</a>. In the app, open About and tap <strong>Scan profile</strong>, then point the camera at this code.</p>
    <p><img src="${qr}" alt="Profile QR code" /></p>
    <p>The code connects this phone to <a href="${escapeHtml(calendarUrl)}">${escapeHtml(calendarUrl)}</a>.</p>
    <p class="hint">Bookmark the calendar link above. It does not include your token. Anyone with that link can view the calendar. The token is what lets the app push workouts and change routines.</p>
    <p>For the routine editor on a computer, open <a href="${escapeHtml(profilePath)}/routine">Edit routines</a> and paste this token:</p>
    <input readonly value="${escapeHtml(options.token)}" />
    <form method="post" action="${escapeHtml(rotateAction)}">
      <button type="submit">Create a new token</button>
    </form>
    <p class="hint">A new token stops the previous one. Scan the new code in About to reconnect the phone.</p>
  </main>
</body>
</html>`;
}
