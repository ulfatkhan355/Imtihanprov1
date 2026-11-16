#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "=== Building Imtihan Pro Admin Dashboard ==="

if [ -n "${VITE_API_BASE_URL:-}" ]; then
  echo "Using VITE_API_BASE_URL=$VITE_API_BASE_URL"
fi

npm run lint
npm run build

echo ""
echo "Admin bundle generated in ./dist"
echo "Deploy to Cloudflare Pages with:"
echo "  npx wrangler pages deploy dist --project-name=imtihan-pro-admin"
