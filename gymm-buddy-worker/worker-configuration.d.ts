// Binding types for gymm-buddy-worker.
// Re-generate with `npm run types` after wrangler config changes.
interface Env {
  WORKOUTS: KVNamespace;
  INGEST_TOKEN?: string;
}