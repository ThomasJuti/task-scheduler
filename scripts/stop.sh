#!/usr/bin/env bash
# Detiene los contenedores. Con --clean también borra el volumen de la base de datos.
set -euo pipefail
. "$(dirname "$0")/lib.sh"
cd "$ROOT_DIR"
use_docker
if [ "${1:-}" = "--clean" ]; then
  docker compose down --volumes
  ok "Stopped and removed the database volume"
else
  docker compose down
  ok "Stopped (data kept; use --clean to delete it)"
fi
