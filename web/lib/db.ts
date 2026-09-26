import { neon, type NeonQueryFunction } from "@neondatabase/serverless";

type Sql = NeonQueryFunction<false, false>;

let cached: Sql | null = null;

export function sql(): Sql {
  const url = process.env.DATABASE_URL;
  if (!url) {
    throw new Error("DATABASE_URL is not set");
  }
  if (!cached) cached = neon(url);
  return cached;
}

export function asNumber(value: unknown): number {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string" && value !== "" && Number.isFinite(Number(value))) {
    return Number(value);
  }
  return 0;
}

export function asObject(value: unknown): Record<string, unknown> | null {
  if (typeof value === "string") {
    try {
      const parsed = JSON.parse(value) as unknown;
      if (parsed && typeof parsed === "object" && !Array.isArray(parsed)) {
        return parsed as Record<string, unknown>;
      }
    } catch {
      return null;
    }
    return null;
  }
  if (value && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

export function asIso(value: unknown): string {
  if (value instanceof Date) return value.toISOString();
  if (typeof value === "string" && value) {
    const parsed = new Date(value);
    if (!Number.isNaN(parsed.getTime())) return parsed.toISOString();
  }
  return new Date(0).toISOString();
}
