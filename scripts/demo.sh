#!/usr/bin/env bash
# End-to-end demo for the presentation: starts the app, loads the demo data and walks through the
# requirements against the real API (manual assignment, Req3a rejection, roles, auto-assignment).
set -euo pipefail
. "$(dirname "$0")/lib.sh"
require python3 "It is used to pretty-print the JSON responses."

"$ROOT_DIR/scripts/run.sh"
echo
"$ROOT_DIR/scripts/seed.sh"
ensure_env

pretty() { python3 -m json.tool; }
# id_of COLLECTION NAME -> id of the item with that name
id_of() {
  api GET "/api/$1" "$ADMIN" | python3 -c "import json,sys; print(next(x['id'] for x in json.load(sys.stdin) if x['name']==sys.argv[1]))" "$2"
}
# expect STATUS METHOD PATH TOKEN [JSON] -> prints the response and checks the HTTP status
expect() {
  local want="$1"; shift
  local method="$1" path="$2" token="$3" body="${4:-}" out code
  out=$(mktemp)
  code=$(curl -s -o "$out" -w '%{http_code}' -X "$method" "$API_URL$path" -H "Authorization: Bearer $token" \
    -H 'Content-Type: application/json' ${body:+-d "$body"})
  if [ "$code" = "$want" ]; then ok "HTTP $code (expected)"; else warn "HTTP $code (expected $want)"; fi
  if [ -s "$out" ]; then pretty <"$out" | head -40; fi
  rm -f "$out"
}

ADMIN=$(login "${APP_ADMIN_USERNAME:-admin}" "$APP_ADMIN_PASSWORD")
USER_TOKEN=$(login "${APP_USER_USERNAME:-user}" "$APP_USER_PASSWORD")
START=$(python3 -c "import datetime as d; t=d.date.today(); print(t + d.timedelta(days=(7 - t.weekday()) % 7 or 7))")

ANA=$(id_of workers "Ana Gómez")
BRUNO=$(id_of workers "Bruno Díaz")
API_TASK=$(id_of tasks "REST API for tasks and workers")
DB_TASK=$(id_of tasks "Database schema and migrations")
JWT_TASK=$(id_of tasks "JWT authentication")

echo
bold "1. Req3 — manual assignment (as USER): Ana gets the REST API + DB schema from $START"
expect 201 POST /api/assignments "$USER_TOKEN" "{\"workerId\":$ANA,\"taskIds\":[$API_TASK,$DB_TASK],\"startDate\":\"$START\"}"

echo
bold "2. Req3a — Bruno (Angular, TypeScript, CSS) cannot take 'JWT authentication' (Java, Spring Boot)"
expect 422 POST /api/assignments "$USER_TOKEN" "{\"workerId\":$BRUNO,\"taskIds\":[$JWT_TASK],\"startDate\":\"$START\"}"

echo
bold "3. Validation — invalid task fields are reported per field"
expect 400 POST /api/tasks "$ADMIN" '{"name":" ","effortHours":0,"skills":[],"status":"NOT_DONE"}'

echo
bold "4. Security — USER cannot edit tasks or run the auto-assignment, and no token means 401"
expect 403 POST /api/tasks "$USER_TOKEN" '{"name":"X","effortHours":1,"skills":["Java"],"status":"NOT_DONE"}'
expect 403 POST /api/assignments/auto "$USER_TOKEN" '{}'
expect 401 GET /api/tasks "invalid-token"

echo
bold "5. Req4 — auto-assignment preview (dry run, nothing saved)"
expect 200 POST /api/assignments/auto "$ADMIN" "{\"startDate\":\"$START\",\"dryRun\":true}"

echo
bold "6. Req4 — auto-assignment"
api POST /api/assignments/auto "$ADMIN" "{\"startDate\":\"$START\",\"dryRun\":false}" | python3 -c '
import json, sys
r = json.load(sys.stdin)
print("Assigned %d tasks:" % len(r["assigned"]))
for a in r["assigned"]:
    print("  %-36s %3d h  -> %-14s %s .. %s" % (a["taskName"], a["effortHours"], a["workerName"], a["startDate"], a["endDate"]))
print("Could not assign %d:" % len(r["unassigned"]))
for u in r["unassigned"]:
    print("  %-36s %s" % (u["taskName"], u["reason"]))
'

echo
ok "Demo finished. Open $UI_URL (admin or user, passwords in .env) to continue in the UI."
