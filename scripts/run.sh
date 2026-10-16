#!/usr/bin/env bash
# Arranca PostgreSQL + backend + frontend con Docker Compose y espera a que la API esté saludable.
set -euo pipefail
. "$(dirname "$0")/lib.sh"
cd "$ROOT_DIR"
use_docker
require curl ""
ensure_env

info "Starting containers (first run builds the images, it can take a few minutes)"
docker compose up -d --build
wait_for_backend 300

echo
bold "Task Scheduler is running"
echo "  App (Angular):   $UI_URL"
echo "  API:             $API_URL/api"
echo "  Swagger UI:      $API_URL/swagger-ui.html"
echo "  Admin login:     ${APP_ADMIN_USERNAME:-admin} / (APP_ADMIN_PASSWORD in .env)"
echo "  User login:      ${APP_USER_USERNAME:-user} / (APP_USER_PASSWORD in .env)"
echo
echo "  Load demo data:  scripts/seed.sh      Stop: scripts/stop.sh"
