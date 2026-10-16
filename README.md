# Task & Worker Scheduler

Full-stack application to manage **tasks** and **workers**, assign tasks to workers manually or
automatically, and plan the work on a Monday–Friday calendar.

**Stack:** Java 21 · Spring Boot 4 · Spring Security (JWT) · PostgreSQL 16 · Flyway · Angular 22 · Angular Material · Docker Compose

| Requirement | Where |
|---|---|
| **Req1** CRUD tasks (name, effort in hours, skills, status done / not-done) | `GET/POST /api/tasks`, `GET/PUT/DELETE /api/tasks/{id}` · *Tasks* page |
| **Req2** CRUD workers (name, availability hours/day, skills) | `GET/POST /api/workers`, `GET/PUT/DELETE /api/workers/{id}` · *Workers* page |
| **Req3** Manual assignment (worker, tasks, start date) | `POST /api/assignments` · *Assignments* page |
| **Req3a** Only if task skills ⊆ worker skills | Checked on every assign / reassign (HTTP 422), incompatible tasks disabled in the UI |
| **Req4** Auto-assignment | `POST /api/assignments/auto` (with dry-run preview) · *Auto-assign* button |
| Scripts: build / run / test / populate | [`scripts/`](scripts) |

### Requirements glossary

The code and tests refer to the requirements of the technical test by id:

| Id | Meaning |
|---|---|
| **Req1** | CRUD of tasks: name, effort in hours, required skills, status done / not done. |
| **Req2** | CRUD of workers: name, available hours per working day, skills. |
| **Req3** | Manual assignment: a worker, one or more tasks and a start date. |
| **Req3a** | Skills rule: a task can only go to a worker who has *all* of its skills (task skills ⊆ worker skills). |
| **Req4** | Auto-assignment: the app distributes the pending tasks among the compatible workers. |

---

## Quick start

Requirements: **Docker Desktop** (running), `curl`, `jq` (`brew install jq`; the seed and demo scripts use it to
build and read JSON) and `python3` for the demo script.

```bash
./scripts/demo.sh
```

That single command builds the images, starts PostgreSQL + API + UI, loads the demo data and walks through
the requirements against the real API. Then open **http://localhost:4200**.

On the first run the scripts create `.env` from [`.env.example`](.env.example) with a random database
password, JWT secret and demo passwords. `scripts/run.sh` prints the two demo accounts:

| User | Role |
|---|---|
| `admin` | ADMIN |
| `user` | USER |

Everything (UI, API, database) is published on `127.0.0.1` only, so the demo accounts are not reachable from
other machines.

### Scripts

| Script | What it does |
|---|---|
| `scripts/build.sh` | Builds the Docker images. `--local` builds the jar (JDK 21) and the Angular bundle (Node 22+) instead. |
| `scripts/run.sh` | `docker compose up`, waits until the API is healthy and prints the URLs. |
| `scripts/seed.sh` | Resets the data and loads 6 workers and 14 tasks through the API (as admin). |
| `scripts/demo.sh` | `run` + `seed` + scripted walkthrough: manual assignment, Req3a rejection, validation, roles, auto-assignment. |
| `scripts/test.sh` | Backend tests (unit + integration against a real PostgreSQL via Testcontainers) and frontend tests (Vitest). Needs JDK 21, Node 22+ and Docker. `backend` / `frontend` runs one side. |
| `scripts/stop.sh` | Stops the containers. `--clean` also deletes the database volume. |

| URL | |
|---|---|
| http://localhost:4200 | Angular app |
| http://localhost:8080/swagger-ui.html | Swagger UI (log in with `POST /api/auth/login`, then *Authorize* with the token) |

### Local development (without the app containers)

```bash
docker compose up -d db                       # only PostgreSQL
cd backend && set -a && . ../.env && set +a && DB_URL=jdbc:postgresql://localhost:5432/scheduler ./mvnw spring-boot:run
cd frontend && npm ci && npm start            # http://localhost:4200, proxies /api to :8080
```

---

## How it works

### Domain rules

- Hours are integers. A task's effort is 1–1000 h; a worker's availability is 1–24 h per working day.
- **Working days are Monday to Friday.** A start date on a weekend moves to the next Monday.
- A task can be assigned to **one** worker. `DONE` tasks cannot be assigned, moved or unassigned (their
  assignment records who did them); mark the task as not done first.
- Start dates go from today up to 2 years ahead (`Limits.PLANNING_HORIZON_YEARS`).
- **Req3a:** a worker can take a task only if *every* task skill is one of the worker's skills
  (case-insensitive: `java` = `Java`). Otherwise the API answers `422` listing the missing skills.
- Skills are created automatically the first time they are used; names are trimmed and de-duplicated.
- Integrity: an assigned task cannot change effort or skills, and a worker cannot lose a skill that one of their
  assigned tasks needs (`409`, unassign first). Deleting a task or worker removes its assignments.

### Scheduling (shared by manual and automatic assignment)

Each worker has a capacity of *availability* hours per working day. Every assignment stores how many hours it
uses on each day (`assignment_allocation`). To plan a task from a start date, [`WorkCalendar`](backend/src/main/java/com/jutinico/scheduler/scheduling/WorkCalendar.java)
walks the working days and fills the **free** hours of each day (availability − hours already booked) until
the effort is covered. That gives the real start and end date, e.g. 10 h with 4 h/day starting Thursday →
Thu 4 h, Fri 4 h, Mon 2 h.

Manual assignment of several tasks schedules them one after the other in the given order, and is **all or
nothing**: if one task breaks a rule, none is assigned.

Concurrent requests cannot overbook a worker or break the rules: every operation locks the rows it relies on
(`SELECT … FOR UPDATE`) before reading them, always workers first and then tasks by id, so they cannot deadlock.
Assigning locks the worker and the tasks; editing a worker locks the worker; editing a task or changing its status
locks the task; a real auto-assignment locks all workers and the pending tasks. New skills are created with
`INSERT … ON CONFLICT DO NOTHING`, so two requests adding the same new skill do not collide.

### Auto-assignment (Req4) — greedy, capacity-aware

[`AutoAssignmentService`](backend/src/main/java/com/jutinico/scheduler/assignment/AutoAssignmentService.java):

1. Take every `NOT_DONE` task without an assignment, **largest effort first** (ties: more skills first), because
   big and specialised tasks are the hardest to place.
2. Candidates are the workers whose skills contain all the task skills (Req3a).
3. For each candidate, simulate the schedule on its current load — including what this same run already
   assigned — and pick the worker who **finishes the task earliest**. Ties go to the worker with fewer
   assigned hours, then the lowest id.
4. Tasks without any compatible worker are returned in `unassigned` with the reason.

`dryRun: true` returns the plan without saving it; the UI shows that preview and asks for confirmation.
It is a heuristic (the optimal problem is NP-hard), deterministic and easy to explain; it balances the load
because a busy worker finishes later and loses the task to a free one.

### Security

- **JWT (HS256)**, stateless. `POST /api/auth/login` returns a token valid for 1 hour; every other endpoint
  needs `Authorization: Bearer <token>`. Passwords are stored with **BCrypt**.
- **Roles**

  | Action | ADMIN | USER |
  |---|:-:|:-:|
  | View tasks, workers, skills, assignments | ✅ | ✅ |
  | Create / edit / delete tasks and workers | ✅ | ❌ |
  | Move tasks: change status (`PATCH /api/tasks/{id}/status`) | ✅ | ✅ |
  | Move tasks: assign, reassign to another worker/date, unassign | ✅ | ✅ |
  | Run the auto-assignment, reset data | ✅ | ❌ |

  A USER can move tasks but cannot change their fields: the status endpoint only accepts `status`.
  Rules are enforced in the `SecurityFilterChain` **and** with `@PreAuthorize` in the services; the UI hides
  what the role cannot do.
- Login: generic "Invalid username or password" (no user enumeration, constant-time check against a dummy
  hash) and temporary lock after 5 failed attempts per username + IP within 5 minutes (`429`). Behind nginx the
  client IP comes from `X-Forwarded-For` (`server.forward-headers-strategy: native`, trusted only from private
  networks), so one client cannot lock everybody out. nginx overwrites that header with the real client IP
  (`$remote_addr`), so a client cannot send a fake one to get around the lock.
- No secrets in the code: database password, JWT secret and demo passwords come from environment variables
  (`.env`, git-ignored). The app refuses to start with a JWT secret shorter than 32 characters or with the example value from
  `.env.example`.
- CORS limited to the frontend origin; errors never include stack traces or SQL; Actuator exposes only `health`;
  the UI, the API and PostgreSQL are published on `127.0.0.1` only; the backend and nginx containers run as
  non-root users (nginx uses the official `nginx-unprivileged` image on port 8080).
- nginx adds `Content-Security-Policy`, `X-Frame-Options: DENY`, `X-Content-Type-Options`, `Referrer-Policy`
  and `Permissions-Policy`.

### Validation

- **Backend (source of truth):** Bean Validation on every request DTO — required fields, lengths, ranges
  (effort 1–1000, availability 1–24), 1–20 skills per task with a safe character set
  (`letters digits space + # . - /`), unique task ids, start date from today up to 2 years ahead, no control characters in names. Unknown JSON fields, wrong
  types, invalid enum values and malformed JSON are rejected with `400`. Database `CHECK` / `UNIQUE`
  constraints are a second barrier.
- Errors follow **RFC 9457** (`application/problem+json`); validation errors include a field map:

  ```json
  { "status": 400, "detail": "Validation failed",
    "errors": { "name": "must not be blank", "effortHours": "must be greater than or equal to 1" } }
  ```
- **Frontend:** reactive forms mirror the same rules (plus whole hours only and weekday-only date pickers),
  show the message under each field, and also map the backend `errors` onto the fields.

---

## API

| Method | Path | Role | Description |
|---|---|---|---|
| POST | `/api/auth/login` | public | `{username, password}` → `{token, username, role, expiresAt}` |
| GET | `/api/tasks?status=&unassigned=` | any | List tasks (with their assignment) |
| POST / PUT / DELETE | `/api/tasks`, `/api/tasks/{id}` | ADMIN | Create / update / delete a task |
| PATCH | `/api/tasks/{id}/status` | any | `{status}` — mark done / not done |
| GET | `/api/workers` | any | List workers with assigned tasks and hours |
| POST / PUT / DELETE | `/api/workers`, `/api/workers/{id}` | ADMIN | Create / update / delete a worker |
| GET | `/api/skills` | any | Known skills (autocomplete) |
| GET | `/api/assignments?workerId=` | any | Assignments with daily allocations |
| POST | `/api/assignments` | any | `{workerId, taskIds[], startDate}` — manual assignment |
| PATCH | `/api/assignments/{id}` | any | `{workerId, startDate}` — move to another worker / date |
| DELETE | `/api/assignments/{id}` | any | Unassign |
| POST | `/api/assignments/auto` | ADMIN | `{startDate?, dryRun}` — auto-assignment |
| DELETE | `/api/admin/reset` | ADMIN | Wipe business data (only if `ADMIN_RESET_ENABLED=true`) |

Status codes: `400` validation, `401` no/invalid token, `403` role, `404` not found, `409` state conflict,
`422` Req3a, `429` too many login attempts.

---

## Project structure

```
backend/                Spring Boot API
  src/main/java/.../
    task/ worker/ skill/        entities, DTOs, services, controllers
    assignment/                 manual + auto assignment
    scheduling/WorkCalendar     Mon–Fri capacity planning (pure Java)
    auth/ config/               JWT login, users, security, CORS, OpenAPI
    common/                     error handling (problem+json), validation patterns
  src/main/resources/db/migration   Flyway SQL
  src/test/                     unit tests + Testcontainers integration tests
frontend/               Angular 22 + Material (standalone components, signals)
  src/app/core/                 API clients, auth (service, guards, interceptor, role directive), validators
  src/app/pages/                login, tasks, workers, assignments (+ dialogs)
  nginx.conf                    static hosting, /api proxy
  security-headers.conf         security headers, included by every nginx location that sets headers
scripts/                build / run / stop / test / seed / demo
docker-compose.yml      db + backend + frontend
```

## Tests

- **Backend (54 tests):** `WorkCalendarTest` (weekends, partial days, existing load), `AutoAssignmentServiceTest`
  (earliest finish, load balancing, unassignable tasks, dry run), `LoginAttemptServiceTest` (lock, unlock, expiry),
  `ProblemJsonHandlersTest` (401/403 body, escaping) and API integration tests with a real
  PostgreSQL: CRUD, validation errors, Req3a, all-or-nothing, reassign, integrity rules, auto-assign, and
  security (401 / 403 per role, generic login errors, lock-out). `ConcurrencyApiTest` keeps a transaction open
  in the middle of an operation and checks that the competing request waits and then gets the right answer
  (Req3a kept, no effort/hours mismatch, no duplicate-skill error). `SecurityConfigTest` (weak or example JWT
  secret) and `NginxConfigTest` (nginx overwrites `X-Forwarded-For`, unprivileged port).
- **Frontend (25 tests):** validators, dates, skills, task sorting, role directive, server-error mapping, auth service
  + interceptor (token header, logout on 401).

## Assumptions

- "Today" uses the `America/Bogota` time zone (`APP_TIMEZONE`), both in the services and in the `@FutureOrPresent`
  validation. Public holidays are not considered.
- Changing a worker's availability keeps the existing schedules (they were valid when planned); new
  assignments use the new value.
- One worker can have several tasks in parallel only up to their daily availability; tasks are not split
  between workers.
