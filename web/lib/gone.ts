import { json } from "./http";

export function profileRequired(): Response {
  return json({ error: "Open your profile at /u/your-name" }, 404);
}
