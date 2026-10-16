#!/usr/bin/env bash
# Runs all the tests.
#   Backend: unit tests + integration tests against a real PostgreSQL started by Testcontainers
#            (needs JDK 21 and Docker running).
#   Frontend: Angular unit tests with Vitest (needs Node 22+).
#   scripts/test.sh backend|frontend runs only one side.
set -euo pipefail
. "$(dirname "$0")/lib.sh"
cd "$ROOT_DIR"
target="${1:-all}"

if [ "$target" = "all" ] || [ "$target" = "backend" ]; then
  use_java21
  use_docker
  info "Backend tests"
  (cd backend && ./mvnw -B test)
  ok "Backend tests passed"
fi

if [ "$target" = "all" ] || [ "$target" = "frontend" ]; then
  require npm "Install Node.js 22 or newer."
  info "Frontend tests"
  (cd frontend && { [ -d node_modules ] || npm ci --no-audit --no-fund; } && npx ng test --watch=false)
  ok "Frontend tests passed"
fi
