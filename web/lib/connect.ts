import { renderDotQr } from "./dot-qr";
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
  const qr = renderDotQr(connectUrl);
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
    .qr { width: min(320px, 100%); height: auto; background: #fff; border-radius: 16px; display: block; }
    .qr circle { fill: #121212; }
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
    <p>${qr.replace("<svg ", '<svg class="qr" ')}</p>
    <p>This browser can now open <a href="${escapeHtml(calendarUrl)}">your profile</a>. Log, stats, and routines are tabs on that page. Come back any time from <a href="/">the Gym Buddy home page</a>. Other people cannot open them.</p>
    <p class="hint">Scan the code with the phone. You do not need to copy a token.</p>
    <form method="post" action="${escapeHtml(rotateAction)}">
      <button type="submit">Create a new token</button>
    </form>
    <p class="hint">A new token stops the previous one. Scan the new code in About to reconnect the phone.</p>
  </main>
</body>
</html>`;
}
