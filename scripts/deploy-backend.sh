#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR/backend"

echo "=== Deploying Imtihan Pro Cloudflare Worker (imtihan-pro-api) ==="

if ! npx wrangler whoami >/dev/null 2>&1; then
  echo "Wrangler is not authenticated. Run: npx wrangler login"
  exit 1
fi

echo "1. Applying remote D1 schema migrations to imtihan-pro-db (079128c1-203b-477b-8c4b-d6b34daf8bbe)..."
npm run d1:migrate:remote

if [ "${SEED_REMOTE:-0}" = "1" ]; then
  echo "2. Seeding remote D1 with idempotent KPK sample dataset..."
  npm run d1:seed:remote
else
  echo "2. Skipping remote seed (set SEED_REMOTE=1 to execute 0002_seed_kpk_sample.sql remotely)."
fi

echo ""
echo "Reminder: Ensure these secrets are set via 'npx wrangler secret put <NAME>' before first use:"
echo "  - ADMIN_SESSION_SECRET"
echo "  - ADMIN_BOOTSTRAP_SECRET"
echo "  - AGENT_INGEST_SECRET"
echo "  - B2_KEY_ID"
echo "  - B2_APPLICATION_KEY"
echo "  - GOOGLE_OAUTH_CLIENT_ID (optional for Google Sign-In linking)"
echo "  - GEMINI_API_KEY (optional for server-side AI Tutor)"
echo ""

echo "3. Deploying Worker imtihan-pro-api..."
npm run deploy

echo "Deployment finished. Verify GET https://<your-worker-url>/v1/health"
