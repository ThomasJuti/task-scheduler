#!/usr/bin/env bash
# Compila la aplicación.
#   scripts/build.sh          -> construye las imágenes Docker (solo requiere Docker)
#   scripts/build.sh --local  -> compila el jar del backend (JDK 21) y el bundle de Angular (Node 22+)
set -euo pipefail
. "$(dirname "$0")/lib.sh"
cd "$ROOT_DIR"

if [ "${1:-}" = "--local" ]; then
  use_java21
  require npm "Install Node.js 22 or newer."
  info "Backend: mvnw package"
  (cd backend && ./mvnw -B -q package -DskipTests)
  ok "backend/target/$(cd backend/target && ls scheduler-*.jar)"
  info "Frontend: npm ci && ng build"
  (cd frontend && npm ci --no-audit --no-fund && npm run build)
  ok "frontend/dist/frontend/browser"
else
  use_docker
  ensure_env
  info "Building Docker images"
  docker compose build
  ok "Images built"
fi
