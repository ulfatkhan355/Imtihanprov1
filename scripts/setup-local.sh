#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "=== Imtihan Pro Stage 2 Local Setup (Ubuntu) ==="

command -v node >/dev/null 2>&1 || { echo "Error: node is required (Node 20+ / 22+)."; exit 1; }
command -v npm >/dev/null 2>&1 || { echo "Error: npm is required."; exit 1; }

if [ ! -f ".env" ] && [ -f ".env.example" ]; then
  cp .env.example .env
  echo "Created .env from .env.example (fill in any local overrides if needed)."
fi

if [ ! -f "backend/.dev.vars" ] && [ -f "backend/.dev.vars.example" ]; then
  cp backend/.dev.vars.example backend/.dev.vars
  echo "Created backend/.dev.vars from backend/.dev.vars.example."
  echo "ACTION REQUIRED: Edit backend/.dev.vars with your real ADMIN_SESSION_SECRET, ADMIN_BOOTSTRAP_SECRET, AGENT_INGEST_SECRET, and Backblaze B2 keys before testing B2 uploads."
fi

echo "Installing root dependencies..."
npm install

echo "Installing backend Worker dependencies..."
npm --prefix backend install

echo "Running Stage 2 local unit & integration tests (Node SQLite D1)..."
npx --yes tsx --test tests/*.test.ts

if command -v npx >/dev/null 2>&1; then
  echo "Applying local D1 migrations to Wrangler local state..."
  npm --prefix backend run d1:migrate:local
  npm --prefix backend run d1:seed:local
fi

echo ""
echo "Local setup complete!"
echo "Next terminals:"
echo "  Terminal 1 (Cloudflare Worker local): npm --prefix backend run dev -- --ip 0.0.0.0 --port 8787"
echo "  Terminal 2 (Admin Dashboard):         npm run dev"
