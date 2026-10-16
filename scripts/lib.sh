#!/usr/bin/env bash
# Funciones compartidas por los scripts de esta carpeta. Se cargan con source, no se ejecutan.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
API_URL="${API_URL:-http://localhost:8080}"
UI_URL="${UI_URL:-http://localhost:4200}"

bold() { printf '\033[1m%s\033[0m\n' "$*"; }
info() { printf '\033[34m==>\033[0m %s\n' "$*"; }
ok()   { printf '\033[32m✔\033[0m %s\n' "$*"; }
warn() { printf '\033[33m!\033[0m %s\n' "$*"; }
fail() { printf '\033[31m✖ %s\033[0m\n' "$*" >&2; exit 1; }

require() {
  command -v "$1" >/dev/null 2>&1 || fail "'$1' is required. $2"
}

# Docker Desktop en macOS instala la CLI en ~/.docker/bin, que puede no estar aún en el PATH.
use_docker() {
  if ! command -v docker >/dev/null 2>&1 && [ -x "$HOME/.docker/bin/docker" ]; then
    export PATH="$HOME/.docker/bin:$PATH"
  fi
  require docker "Install Docker Desktop: https://docs.docker.com/get-docker/"
  docker info >/dev/null 2>&1 || fail "Docker is installed but not running. Start Docker Desktop and try again."
}

# Busca un JDK 21 para el build local de Maven (java_home de macOS, Homebrew o el JAVA_HOME actual).
use_java21() {
  if [ -n "${JAVA_HOME:-}" ] && "$JAVA_HOME/bin/java" -version 2>&1 | grep -q 'version "21'; then return; fi
  for candidate in "$(/usr/libexec/java_home -v 21 2>/dev/null || true)" /opt/homebrew/opt/openjdk@21 /usr/local/opt/openjdk@21 /usr/lib/jvm/java-21-openjdk*; do
    if [ -n "$candidate" ] && [ -x "$candidate/bin/java" ]; then export JAVA_HOME="$candidate"; return; fi
  done
  if java -version 2>&1 | grep -q 'version "21'; then return; fi
  fail "JDK 21 is required for the local build/tests (e.g. 'brew install openjdk@21'). Use 'scripts/build.sh' without --local to build in Docker instead."
}

random_secret() {
  if command -v openssl >/dev/null 2>&1; then openssl rand -base64 48 | tr -d '\n/+=' | cut -c1-48
  else LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c 48; fi
}

# Crea .env desde .env.example la primera vez, reemplazando los secretos de ejemplo por otros aleatorios.
ensure_env() {
  local env_file="$ROOT_DIR/.env"
  if [ ! -f "$env_file" ]; then
    info "Creating .env from .env.example with random DB password and JWT secret"
    sed -e "s|^POSTGRES_PASSWORD=.*|POSTGRES_PASSWORD=$(random_secret)|" \
        -e "s|^JWT_SECRET=.*|JWT_SECRET=$(random_secret)|" \
        "$ROOT_DIR/.env.example" > "$env_file"
    chmod 600 "$env_file"
  fi
  set -a
  # shellcheck disable=SC1090
  . "$env_file"
  set +a
}

wait_for_backend() {
  local timeout="${1:-180}" waited=0
  info "Waiting for the backend at $API_URL ..."
  until curl -fs "$API_URL/actuator/health" 2>/dev/null | grep -q '"UP"'; do
    sleep 3; waited=$((waited + 3))
    [ "$waited" -ge "$timeout" ] && fail "Backend not healthy after ${timeout}s. Check: docker compose logs backend"
  done
  ok "Backend is up"
}

# jq construye y lee el JSON, así los nombres y contraseñas con comillas o barras invertidas se escapan bien.
JQ_HINT="Install it with 'brew install jq' (macOS) or 'sudo apt-get install jq' (Debian/Ubuntu)."

# Inicia sesión e imprime el JWT. Uso: TOKEN=$(login usuario contraseña)
login() {
  require jq "$JQ_HINT"
  local body response
  body=$(jq -n --arg u "$1" --arg p "$2" '{username: $u, password: $p}')
  response=$(curl -fsS -X POST "$API_URL/api/auth/login" -H 'Content-Type: application/json' \
    -d "$body") || fail "Login failed for '$1'"
  printf '%s' "$response" | jq -r '.token'
}

# api METODO RUTA TOKEN [JSON] -> imprime el cuerpo; devuelve distinto de cero en errores HTTP
api() {
  local method="$1" path="$2" token="$3" body="${4:-}"
  if [ -n "$body" ]; then
    curl -fsS -X "$method" "$API_URL$path" -H "Authorization: Bearer $token" -H 'Content-Type: application/json' -d "$body"
  else
    curl -fsS -X "$method" "$API_URL$path" -H "Authorization: Bearer $token"
  fi
}

json_id() {
  jq -r '.id'
}
