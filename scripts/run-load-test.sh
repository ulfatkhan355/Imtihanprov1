#!/usr/bin/env bash
set -euo pipefail

# Runs progressive concurrency stages (10, 25, 50, 100 simulated students) against
# an isolated local SQLite + Worker router and records measured metrics cleanly.
# Never hits live Cloudflare production infrastructure.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$ROOT_DIR"
npx --yes tsx tests/loadTest.ts --write-snapshot "$@"
