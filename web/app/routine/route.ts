import { html, run } from "../../lib/http";

export const dynamic = "force-dynamic";

export function GET(request: Request) {
  return run(request, async () =>
    html(`<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Gym Buddy</title>
</head>
<body style="margin:0;background:#121212;color:#eee;font-family:ui-sans-serif,system-ui,sans-serif">
  <main style="max-width:640px;margin:0 auto;padding:32px 16px">
    <h1 style="color:#f9f72e">Routines live on your profile</h1>
    <p>Open the calendar link from your connect page, then choose Edit routines. <a style="color:#00ffff" href="/">Back to Gym Buddy</a></p>
  </main>
</body>
</html>`),
  );
}

export function OPTIONS(request: Request) {
  return run(request, async () => html(""));
}
