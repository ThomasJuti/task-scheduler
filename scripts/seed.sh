#!/usr/bin/env bash
# Loads demo data through the REST API (as admin): wipes tasks/workers/assignments and creates
# 6 workers and 14 tasks. Two tasks need skills nobody has, to show what auto-assignment reports.
# Requires the app running (scripts/run.sh) and ADMIN_RESET_ENABLED=true in .env.
set -euo pipefail
. "$(dirname "$0")/lib.sh"
require curl ""
ensure_env

TOKEN=$(login "${APP_ADMIN_USERNAME:-admin}" "$APP_ADMIN_PASSWORD")
ok "Logged in as ${APP_ADMIN_USERNAME:-admin}"

info "Resetting data"
api DELETE /api/admin/reset "$TOKEN" >/dev/null || fail "Reset failed: set ADMIN_RESET_ENABLED=true in .env and restart (scripts/run.sh)"

worker() { # name hours skills-json
  api POST /api/workers "$TOKEN" "{\"name\":\"$1\",\"availabilityHoursPerDay\":$2,\"skills\":$3}" >/dev/null
  printf '  %-16s %2s h/day  %s\n' "$1" "$2" "$3"
}
task() { # name effort skills-json [status]
  api POST /api/tasks "$TOKEN" "{\"name\":\"$1\",\"effortHours\":$2,\"skills\":$3,\"status\":\"${4:-NOT_DONE}\"}" >/dev/null
  printf '  %-34s %3s h  %-9s %s\n' "$1" "$2" "${4:-NOT_DONE}" "$3"
}

info "Creating workers"
worker "Ana Gómez"     8 '["Java","Spring Boot","SQL"]'
worker "Bruno Díaz"    6 '["Angular","TypeScript","CSS"]'
worker "Carla Ruiz"    8 '["Java","Spring Boot","Angular","TypeScript","SQL"]'
worker "David Torres"  4 '["Docker","Kubernetes","CI/CD"]'
worker "Elena Castro"  8 '["QA","Selenium","SQL"]'
worker "Felipe Mora"   5 '["UX","Figma","CSS"]'

info "Creating tasks"
task "Design login and dashboard screens" 6  '["Figma","UX"]'
task "REST API for tasks and workers"     16 '["Java","Spring Boot"]'
task "Database schema and migrations"     8  '["SQL"]'
task "Angular task list page"             12 '["Angular","TypeScript"]'
task "Responsive styles"                  5  '["CSS"]'
task "Assignment form with validation"    10 '["Angular","TypeScript"]'
task "JWT authentication"                 12 '["Java","Spring Boot"]'
task "Dockerize the application"          6  '["Docker"]'
task "CI pipeline"                        8  '["CI/CD","Docker"]'
task "End-to-end tests"                   10 '["QA","Selenium"]'
task "Reporting queries"                  6  '["Java","SQL"]'
task "Kick-off workshop notes"            2  '["UX"]' DONE
task "iOS app prototype"                  20 '["Swift","iOS"]'
task "Legacy data migration script"       8  '["Python","SQL"]'

ok "Demo data loaded. Open $UI_URL and sign in."
