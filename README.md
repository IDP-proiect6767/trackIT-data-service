# TrackIT Data Service

Pure data-access microservice for the TrackIT fitness tracker. Provides REST CRUD for workouts, exercises, workout logs, foods, and meals. **Only this service has direct access to `data_db`.**

---

## ⚠️ Security Model

**This service has NO authentication and NO JWT validation.**

It trusts the `X-User-Id` HTTP header passed by the Business Logic Service (the only authorized caller). User-owned endpoints scope all queries by that header — cross-user access returns 404, not 403, to avoid leaking existence.

**This service must NEVER be exposed to the public internet or to Kong.** It must only be reachable on the internal `backend-net` Docker network from the Business Logic Service.

---

## Quick Start (local)

```bash
docker compose up
```

### Example requests

```bash
# List exercises (no auth needed)
curl http://localhost:8083/api/v1/exercises

# Create a workout (X-User-Id required)
curl -X POST http://localhost:8083/api/v1/workouts \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 42" \
  -d '{"name":"Push Day","exercises":[]}'

# List your workouts
curl http://localhost:8083/api/v1/workouts -H "X-User-Id: 42"

# Log a meal
curl -X POST http://localhost:8083/api/v1/meals \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 42" \
  -d '{"mealType":"LUNCH","consumedAt":"2026-04-26T12:00:00Z","items":[]}'
```

Swagger UI: http://localhost:8083/swagger-ui.html

---

## API Reference

### Exercises (reference data — no `X-User-Id`)

| Method | Path | Notes |
|--------|------|-------|
| GET | `/api/v1/exercises` | List with optional filters: `search`, `muscleGroup`, `equipment`, `difficulty`, `page`, `size` |
| GET | `/api/v1/exercises/{id}` | Get one |
| POST | `/api/v1/exercises` | Create |
| PATCH | `/api/v1/exercises/{id}` | Partial update |
| DELETE | `/api/v1/exercises/{id}` | Delete (fails with 409 if referenced) |

### Workouts (user-owned — `X-User-Id` required)

| Method | Path | Notes |
|--------|------|-------|
| GET | `/api/v1/workouts` | List own workouts (`page`, `size`) |
| GET | `/api/v1/workouts/{id}` | Get one |
| POST | `/api/v1/workouts` | Create with exercises |
| PATCH | `/api/v1/workouts/{id}` | Partial update; passing `exercises` replaces the full list |
| DELETE | `/api/v1/workouts/{id}` | Delete |

### Workout Logs (user-owned — `X-User-Id` required)

| Method | Path | Notes |
|--------|------|-------|
| GET | `/api/v1/workout-logs` | List own logs; optional `from`/`to` ISO-8601 filter (both or neither) |
| GET | `/api/v1/workout-logs/{id}` | Get one |
| POST | `/api/v1/workout-logs` | Create with sets |
| PATCH | `/api/v1/workout-logs/{id}` | Partial update |
| DELETE | `/api/v1/workout-logs/{id}` | Delete |

### Foods (reference data — no `X-User-Id`)

| Method | Path | Notes |
|--------|------|-------|
| GET | `/api/v1/foods` | List with optional `search`, `page`, `size` |
| GET | `/api/v1/foods/{id}` | Get one |
| POST | `/api/v1/foods` | Create |
| PATCH | `/api/v1/foods/{id}` | Partial update |
| DELETE | `/api/v1/foods/{id}` | Delete (fails with 409 if referenced by meals) |

### Meals (user-owned — `X-User-Id` required)

| Method | Path | Notes |
|--------|------|-------|
| GET | `/api/v1/meals` | List own meals; optional `from`/`to` filter |
| GET | `/api/v1/meals/{id}` | Get one (includes computed macro totals) |
| POST | `/api/v1/meals` | Create with items; macros auto-computed |
| PATCH | `/api/v1/meals/{id}` | Partial update |
| DELETE | `/api/v1/meals/{id}` | Delete |

---

## Configuration

| Environment Variable | Default | Description |
|---------------------|---------|-------------|
| `DATA_DB_URL` | `jdbc:postgresql://localhost:5432/data_db` | JDBC URL |
| `DATA_DB_USER` | `data_user` | DB username |
| `DATA_DB_PASSWORD` | `data_pass` | DB password |

---

## Build & Test

```bash
mvn verify
```

Integration tests use Testcontainers (Docker required).

---

## Deployment

Docker Swarm placement constraint: `node.labels.role == app`

Stack file: see `trackIT-infrastructure` repository.

### GitHub Secrets required for CI/CD

| Secret | Purpose |
|--------|---------|
| `DOCKERHUB_USERNAME` | Docker Hub login |
| `DOCKERHUB_TOKEN` | Docker Hub access token |
