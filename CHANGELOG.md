# Changelog

All notable changes to the TrackIT Data Service will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2026-04-26

### Added

- **Phase 0 — Project bootstrap**: Spring Boot 3.4.1 Maven project (`com.trackit:data-service`), Java 21, `DataServiceApplication`, `application.yml` (port 8083, three profiles: default/docker/test), `.gitignore`.
- **Phase 1 — Flyway migrations**: `V1__init_data_schema.sql` creates the full `data` schema with tables `exercises`, `workouts`, `workout_exercises`, `workout_logs`, `workout_log_sets`, `foods`, `meals`, `meal_items` and all required indexes. `V2__seed_demo_reference_data.sql` seeds 10 exercises and 10 foods with realistic macro values.
- **Phase 2 — Domain & persistence**: JPA entities (`Exercise`, `Workout`, `WorkoutExercise`, `WorkoutLog`, `WorkoutLogSet`, `Food`, `Meal`, `MealItem`) with Lombok, `@CreationTimestamp`/`@UpdateTimestamp`. Enums `MuscleGroup`, `Equipment`, `Difficulty`, `MealType`. Repositories: `ExerciseRepository`, `WorkoutRepository`, `WorkoutLogRepository`, `FoodRepository`, `MealRepository` with custom queries for user-scoped and range lookups.
- **Phase 3 — DTOs**: Java records for all request/response shapes. `PageResponse<T>` generic page wrapper. `ErrorResponse` with stable codes (`VALIDATION_FAILED`, `NOT_FOUND`, `CONFLICT`, `MISSING_USER_CONTEXT`, `INTERNAL_ERROR`).
- **Phase 4 — Cross-cutting concerns**: `ResourceNotFoundException`, `MissingUserContextException`, `ConflictException`. `@CurrentUser` parameter annotation + `UserContextArgumentResolver` (extracts and validates `X-User-Id` header → Long > 0). `WebMvcConfig` registers the resolver. `GlobalExceptionHandler` maps all exceptions to standard `ErrorResponse` JSON.
- **Phase 5 — Exercise CRUD**: `ExerciseService` with Specification-based filtering (name ILIKE, muscleGroup, equipment, difficulty). `ExerciseController` at `/api/v1/exercises` — full CRUD, 201 on create, 204 on delete, 409 on duplicate name or FK violation.
- **Phase 6 — Workout CRUD**: `WorkoutService` with user-scoped queries, embedded exercise list (replace strategy on update), duplicate-position validation. `WorkoutController` at `/api/v1/workouts` — all methods require `X-User-Id`.
- **Phase 7 — Workout log CRUD**: `WorkoutLogService` with optional `from`/`to` range filter, per-set (exerciseId, setNumber) uniqueness validation, replace strategy. `WorkoutLogController` at `/api/v1/workout-logs` — both `from` and `to` must be provided together or neither.
- **Phase 8 — Food CRUD**: `FoodService` with name-or-brand ILIKE search. `FoodController` at `/api/v1/foods` — same pattern as exercises.
- **Phase 9 — Meal CRUD with computed macros**: `MealService` computes per-item and total macros (`factor = quantityG / servingSizeG`, rounded to 2 decimals using `HALF_UP`). `MealController` at `/api/v1/meals` — requires `X-User-Id`.
- **Phase 10 — Observability**: Prometheus actuator endpoint at `/actuator/prometheus`. Custom Micrometer counters: `data_workouts_created_total`, `data_workouts_deleted_total`, `data_workout_logs_created_total`, `data_meals_created_total`, `data_exercises_mutations_total{action}`, `data_foods_mutations_total{action}`.
- **Phase 11 — Tests**: Unit tests (`ExerciseServiceTest`, `WorkoutServiceTest`, `FoodServiceTest`, `MealServiceTest`) with Mockito and `SimpleMeterRegistry`. Integration tests with Testcontainers/PostgreSQL: `ExerciseFlowIT` (full CRUD), `CrossUserIsolationIT` (user 99 cannot access user 42 resources), `MissingUserContextIT` (all user-scoped endpoints return 400 without header), `MealMacroIT` (end-to-end macro computation), `WorkoutLogRangeIT` (date range filtering).
- **Phase 12 — Docker**: Multi-stage `Dockerfile` (Maven builder → `eclipse-temurin:21-jre-alpine`, non-root `appuser`, health check via `wget /actuator/health`). `.dockerignore`. `docker-compose.yml` with `data-db` (postgres:16-alpine with health check), `data-service` (depends_on healthy DB), optional `pgadmin` under `debug` profile.
- **Phase 13 — CI/CD**: `.github/workflows/ci.yml` — build and test on every push, upload surefire/failsafe reports on failure. `.github/workflows/release.yml` — on push to `main`, build jar, push Docker image to Docker Hub as `florinionita/trackit-data-service:latest` and `:<sha>`.
- **Phase 14 — Documentation**: `README.md` with security model warning, quick start, full API reference table, configuration table, build/test instructions, deployment notes, and required GitHub secrets.

<!--
Section types you can use under each release (omit empty ones):

  ### Added       — for new features.
  ### Changed     — for changes in existing functionality.
  ### Deprecated  — for soon-to-be removed features.
  ### Removed     — for now removed features.
  ### Fixed       — for any bug fixes.
  ### Security    — in case of vulnerabilities.

Workflow:
  1. Add entries under [Unreleased] as you work.
  2. When releasing, replace [Unreleased] heading with [X.Y.Z] - YYYY-MM-DD
     and start a fresh empty [Unreleased] section above it.
-->
