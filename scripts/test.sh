#!/usr/bin/env bash
# Ejecuta todas las pruebas.
#   Backend: pruebas unitarias + de integración contra un PostgreSQL real levantado por Testcontainers
#            (requiere JDK 21 y Docker en ejecución).
#   Frontend: pruebas unitarias de Angular con Vitest (requiere Node 22+).
#   scripts/test.sh backend|frontend ejecuta solo un lado.
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
