import { readJsonBody } from "./http";
import { SLUG_RE, slugifyName } from "./users";

export type ProfileRequest =
  | { ok: true; displayName: string; slug: string; json: boolean }
  | { ok: false; error: string; json: boolean };

export async function readProfileRequest(request: Request): Promise<ProfileRequest> {
  const type = request.headers.get("content-type") ?? "";
  const json = type.includes("application/json");
  let displayName = "";
  let slug = "";
  if (json) {
    const parsed = await readJsonBody(request);
    if (!parsed.ok) return { ok: false, error: "invalid json", json };
    if (!parsed.value || typeof parsed.value !== "object" || Array.isArray(parsed.value)) {
      return { ok: false, error: "body must be an object", json };
    }
    const body = parsed.value as Record<string, unknown>;
    displayName = typeof body.displayName === "string" ? body.displayName : "";
    slug = typeof body.slug === "string" ? body.slug : "";
  } else {
    const form = await request.formData();
    displayName = String(form.get("displayName") ?? "");
    slug = String(form.get("slug") ?? "");
  }
  displayName = displayName.trim();
  slug = slug.trim().toLowerCase();
  if (!displayName || displayName.length > 80) {
    return { ok: false, error: "Enter a name up to 80 characters.", json };
  }
  if (!slug) slug = slugifyName(displayName);
  if (!SLUG_RE.test(slug)) {
    return { ok: false, error: "Use letters, numbers, and hyphens for the profile address.", json };
  }
  return { ok: true, displayName, slug, json };
}
