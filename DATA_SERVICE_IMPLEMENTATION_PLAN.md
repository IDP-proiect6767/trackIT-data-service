# TrackIT Data Service — Full Implementation Plan

> **For:** Claude Code agent. Implement **one phase at a time**. After each phase: stop, summarize what was done, update `[Unreleased]` in `CHANGELOG.md`, and wait for the user to review/commit before continuing to the next phase. Do not skip ahead.

## Project context

- **Stack:** Java 21, Spring Boot 3.4.x, Maven, PostgreSQL 16, Flyway, Spring Data JPA, Testcontainers, Lombok.
- **Role:** Pure data-access microservice. Exposes REST CRUD for workouts, exercises, workout logs, foods, meals. **Only this service touches `data_db`.** Called over HTTP by Business Logic Service over the internal `backend-net` network.
- **Trust model:** Internal-only. Lives on `backend-net` and `db-net`, not reachable from `frontend-net`. Has **no JWT validation** — it trusts the caller (Business Logic Service) to have authenticated the user. The caller passes the user ID via the `X-User-Id` HTTP header on every request.
- **Multi-tenancy:** Every user-owned entity (workouts, workout-logs, meals) carries a `user_id` column. Every endpoint that operates on user-owned data **scopes queries by the `X-User-Id` header**. A request to `GET /api/v1/workouts/500` from user 42 returns 404 if workout 500 belongs to user 99 — never leak existence across users. Reference data (exercises, foods) is global and not scoped.
- **Deployment target:** Docker image, runs on Docker Swarm worker labeled `role=app` (same node as Business Logic and `data_db`).
- **Repo:** `trackIT-data-service`.
- **Docker Hub:** `florinionita/trackit-data-service`.

## Conventions

- **Package base:** `com.trackit.data`
- **API prefix:** `/api/v1`
- **Commits:** Conventional Commits (`feat:`, `chore:`, `test:`, `docs:`, `ci:`, `build:`, `fix:`).
- **Branching:** Work on `main` (solo project). At least one commit per phase.
- **DB schema:** `data` (not `public`).
- **Required header:** `X-User-Id` (Long, > 0) on every endpoint that operates on user-owned data. Missing/invalid → 400 `MISSING_USER_CONTEXT`. Reference-data endpoints (exercises catalogue, foods catalogue) ignore the header.
- **Config style:** `application.yml` with profiles (`default`, `docker`, `test`). All secrets via env vars.
- **No JWT, no Spring Security filter chain.** Network isolation is the security boundary. Document this prominently in the README.

## High-level endpoint map (target state at end of plan)

User-scoped endpoints require `X-User-Id`. Reference endpoints do not.

### Exercises (reference data — global, read-only for clients)

| Method | Path                                  | X-User-Id | Notes |
|--------|---------------------------------------|-----------|-------|
| GET    | `/api/v1/exercises`                   | no        | List with filters |
| GET    | `/api/v1/exercises/{id}`              | no        | Get one |
| POST   | `/api/v1/exercises`                   | no        | Admin-only seed/create — used by migrations or admin tools |
| PATCH  | `/api/v1/exercises/{id}`              | no        | Edit reference exercise |
| DELETE | `/api/v1/exercises/{id}`              | no        | Remove reference exercise |

### Workouts (user-owned templates)

| Method | Path                                  | X-User-Id |
|--------|---------------------------------------|-----------|
| GET    | `/api/v1/workouts`                    | yes |
| GET    | `/api/v1/workouts/{id}`               | yes |
| POST   | `/api/v1/workouts`                    | yes |
| PATCH  | `/api/v1/workouts/{id}`               | yes |
| DELETE | `/api/v1/workouts/{id}`               | yes |

### Workout logs (user-owned session records)

| Method | Path                                  | X-User-Id |
|--------|---------------------------------------|-----------|
| GET    | `/api/v1/workout-logs`                | yes |
| GET    | `/api/v1/workout-logs/{id}`           | yes |
| POST   | `/api/v1/workout-logs`                | yes |
| PATCH  | `/api/v1/workout-logs/{id}`           | yes |
| DELETE | `/api/v1/workout-logs/{id}`           | yes |

### Foods (reference data — global, read-only for clients)

| Method | Path                                  | X-User-Id |
|--------|---------------------------------------|-----------|
| GET    | `/api/v1/foods`                       | no |
| GET    | `/api/v1/foods/{id}`                  | no |
| POST   | `/api/v1/foods`                       | no |
| PATCH  | `/api/v1/foods/{id}`                  | no |
| DELETE | `/api/v1/foods/{id}`                  | no |

### Meals (user-owned logged meals)

| Method | Path                                  | X-User-Id |
|--------|---------------------------------------|-----------|
| GET    | `/api/v1/meals`                       | yes |
| GET    | `/api/v1/meals/{id}`                  | yes |
| POST   | `/api/v1/meals`                       | yes |
| PATCH  | `/api/v1/meals/{id}`                  | yes |
| DELETE | `/api/v1/meals/{id}`                  | yes |

---

## Phase 0 — Project bootstrap

**Goal:** Empty Spring Boot project that builds and runs.

1. Generate `pom.xml`: parent `spring-boot-starter-parent:3.4.x`, Java 21, group `com.trackit`, artifact `data-service`, version `0.1.0-SNAPSHOT`.
2. Dependencies:
   - `spring-boot-starter-web`
   - `spring-boot-starter-data-jpa`
   - `spring-boot-starter-validation`
   - `spring-boot-starter-actuator`
   - `org.postgresql:postgresql` (runtime)
   - `org.flywaydb:flyway-core` + `flyway-database-postgresql`
   - `io.micrometer:micrometer-registry-prometheus`
   - `org.projectlombok:lombok` (provided)
   - `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.x`
   - **Test:** `spring-boot-starter-test`, `org.testcontainers:postgresql`, `org.testcontainers:junit-jupiter`
3. `DataServiceApplication.java` with `@SpringBootApplication`.
4. `application.yml` minimal: `spring.application.name=data-service`, `server.port=8083`.
5. `.gitignore` for Java/Maven/IntelliJ.
6. `mvn clean verify` must pass.

**Acceptance:** App starts on port 8083. `mvn verify` green.

---

## Phase 1 — Database schema (Flyway)

**Goal:** All tables we'll need throughout the project, created reproducibly.

1. `application.yml` datasource + JPA + Flyway sections:
   ```yaml
   spring:
     datasource:
       url: ${DATA_DB_URL:jdbc:postgresql://localhost:5432/data_db}
       username: ${DATA_DB_USER:data_user}
       password: ${DATA_DB_PASSWORD:data_pass}
     jpa:
       hibernate.ddl-auto: validate
       properties.hibernate.default_schema: data
       open-in-view: false
     flyway:
       schemas: data
       default-schema: data
       baseline-on-migrate: true
   ```
2. `V1__init_data_schema.sql`:
   - `CREATE SCHEMA IF NOT EXISTS data;`
   - `data.exercises` (reference, global):
     - `id BIGSERIAL PRIMARY KEY`
     - `name VARCHAR(128) NOT NULL UNIQUE`
     - `muscle_group VARCHAR(64) NOT NULL` — e.g. `CHEST`, `BACK`, `LEGS`, `SHOULDERS`, `ARMS`, `CORE`, `CARDIO`, `FULL_BODY`. Stored as string (validated at app layer).
     - `equipment VARCHAR(64) NOT NULL` — e.g. `BARBELL`, `DUMBBELL`, `MACHINE`, `BODYWEIGHT`, `CABLE`, `OTHER`.
     - `difficulty VARCHAR(20) NOT NULL CHECK (difficulty IN ('BEGINNER','INTERMEDIATE','ADVANCED'))`
     - `description TEXT NULL`
     - `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`
     - `updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`
   - `data.workouts` (user-owned templates):
     - `id BIGSERIAL PRIMARY KEY`
     - `user_id BIGINT NOT NULL`
     - `name VARCHAR(128) NOT NULL`
     - `description TEXT NULL`
     - `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`
     - `updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`
   - `data.workout_exercises` (join: which exercises are in which workout, with order):
     - `id BIGSERIAL PRIMARY KEY`
     - `workout_id BIGINT NOT NULL REFERENCES data.workouts(id) ON DELETE CASCADE`
     - `exercise_id BIGINT NOT NULL REFERENCES data.exercises(id)`
     - `position INT NOT NULL DEFAULT 0` — display order within workout
     - `target_sets INT NULL`
     - `target_reps INT NULL`
     - `notes VARCHAR(255) NULL`
     - `UNIQUE (workout_id, position)`
   - `data.workout_logs` (user-owned session records):
     - `id BIGSERIAL PRIMARY KEY`
     - `user_id BIGINT NOT NULL`
     - `workout_id BIGINT NULL REFERENCES data.workouts(id) ON DELETE SET NULL` — optional link to template; `NULL` for ad-hoc workouts
     - `performed_at TIMESTAMPTZ NOT NULL`
     - `duration_minutes INT NULL`
     - `notes TEXT NULL`
     - `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`
   - `data.workout_log_sets` (individual sets within a logged session):
     - `id BIGSERIAL PRIMARY KEY`
     - `workout_log_id BIGINT NOT NULL REFERENCES data.workout_logs(id) ON DELETE CASCADE`
     - `exercise_id BIGINT NOT NULL REFERENCES data.exercises(id)`
     - `set_number INT NOT NULL` — 1-indexed within the exercise within the log
     - `reps INT NOT NULL`
     - `weight_kg NUMERIC(6,2) NULL` — null for bodyweight
     - `UNIQUE (workout_log_id, exercise_id, set_number)`
   - `data.foods` (reference, global):
     - `id BIGSERIAL PRIMARY KEY`
     - `name VARCHAR(128) NOT NULL UNIQUE`
     - `brand VARCHAR(128) NULL`
     - `serving_size_g NUMERIC(7,2) NOT NULL` — reference serving weight in grams
     - `calories_kcal NUMERIC(7,2) NOT NULL` — kcal per serving
     - `protein_g NUMERIC(6,2) NOT NULL DEFAULT 0`
     - `carbs_g NUMERIC(6,2) NOT NULL DEFAULT 0`
     - `fat_g NUMERIC(6,2) NOT NULL DEFAULT 0`
     - `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`
     - `updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`
   - `data.meals` (user-owned meal log entries):
     - `id BIGSERIAL PRIMARY KEY`
     - `user_id BIGINT NOT NULL`
     - `meal_type VARCHAR(20) NOT NULL CHECK (meal_type IN ('BREAKFAST','LUNCH','DINNER','SNACK'))`
     - `consumed_at TIMESTAMPTZ NOT NULL`
     - `notes VARCHAR(255) NULL`
     - `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`
   - `data.meal_items` (food entries within a meal):
     - `id BIGSERIAL PRIMARY KEY`
     - `meal_id BIGINT NOT NULL REFERENCES data.meals(id) ON DELETE CASCADE`
     - `food_id BIGINT NOT NULL REFERENCES data.foods(id)`
     - `quantity_g NUMERIC(7,2) NOT NULL` — actual weight consumed; macros computed as `(quantity_g / serving_size_g) * food.<macro>`
   - Indexes:
     - `CREATE INDEX idx_workouts_user_id ON data.workouts(user_id);`
     - `CREATE INDEX idx_workout_logs_user_id ON data.workout_logs(user_id);`
     - `CREATE INDEX idx_workout_logs_performed_at ON data.workout_logs(user_id, performed_at DESC);`
     - `CREATE INDEX idx_meals_user_id ON data.meals(user_id);`
     - `CREATE INDEX idx_meals_consumed_at ON data.meals(user_id, consumed_at DESC);`
     - `CREATE INDEX idx_workout_exercises_workout_id ON data.workout_exercises(workout_id);`
     - `CREATE INDEX idx_meal_items_meal_id ON data.meal_items(meal_id);`
3. `V2__seed_demo_reference_data.sql`: insert ~10 demo exercises (Bench Press, Squat, Deadlift, Pull-up, Overhead Press, Plank, Push-up, Row, Bicep Curl, Running) and ~10 demo foods (Chicken Breast, White Rice, Olive Oil, Banana, Whey Protein, Egg, Oats, Greek Yogurt, Almonds, Broccoli) with realistic per-100g values. Add SQL comment `-- Demo seed data; expand or replace as needed.`

**Acceptance:** With local Postgres running, `mvn spring-boot:run` applies both migrations. `\dt data.*` shows all expected tables. `SELECT COUNT(*) FROM data.exercises;` returns 10.

---

## Phase 2 — Domain & persistence

**Goal:** JPA entities + repositories.

1. Enums under `domain.enums.*`:
   - `MuscleGroup`: `CHEST, BACK, LEGS, SHOULDERS, ARMS, CORE, CARDIO, FULL_BODY`.
   - `Equipment`: `BARBELL, DUMBBELL, MACHINE, BODYWEIGHT, CABLE, OTHER`.
   - `Difficulty`: `BEGINNER, INTERMEDIATE, ADVANCED`.
   - `MealType`: `BREAKFAST, LUNCH, DINNER, SNACK`.
   - Use `@Enumerated(EnumType.STRING)` on entity fields. Validate input enum values explicitly in DTOs (return 400 `VALIDATION_FAILED` on bad value, not 500).
2. Entities (`@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`, `@CreationTimestamp`/`@UpdateTimestamp` where applicable):
   - `domain.Exercise` → `data.exercises`
   - `domain.Workout` → `data.workouts` with `@OneToMany(mappedBy="workout", cascade=ALL, orphanRemoval=true)` to `WorkoutExercise`. Always ordered by `position ASC`.
   - `domain.WorkoutExercise` → `data.workout_exercises`. `@ManyToOne` to `Workout` and `Exercise` (both LAZY).
   - `domain.WorkoutLog` → `data.workout_logs` with `@OneToMany` to `WorkoutLogSet` (cascade ALL, orphan removal). Optional `@ManyToOne` to `Workout`.
   - `domain.WorkoutLogSet` → `data.workout_log_sets`. `@ManyToOne` to `WorkoutLog` and `Exercise`.
   - `domain.Food` → `data.foods`.
   - `domain.Meal` → `data.meals` with `@OneToMany` to `MealItem` (cascade ALL, orphan removal).
   - `domain.MealItem` → `data.meal_items`. `@ManyToOne` to `Meal` and `Food`.
3. Repositories under `repository.*`:
   - `ExerciseRepository extends JpaRepository<Exercise, Long>`:
     - `Page<Exercise> findAll(Specification<Exercise>, Pageable)` (extend `JpaSpecificationExecutor` too) — for filtering by `muscleGroup`, `equipment`, `difficulty`, `name LIKE`.
   - `WorkoutRepository extends JpaRepository<Workout, Long>`:
     - `Page<Workout> findAllByUserId(Long userId, Pageable)`
     - `Optional<Workout> findByIdAndUserId(Long id, Long userId)`
     - `boolean existsByIdAndUserId(Long id, Long userId)`
   - `WorkoutLogRepository extends JpaRepository<WorkoutLog, Long>`:
     - `Page<WorkoutLog> findAllByUserId(Long userId, Pageable)`
     - `Page<WorkoutLog> findAllByUserIdAndPerformedAtBetween(Long userId, Instant from, Instant to, Pageable)`
     - `Optional<WorkoutLog> findByIdAndUserId(Long id, Long userId)`
   - `FoodRepository extends JpaRepository<Food, Long>, JpaSpecificationExecutor<Food>`.
   - `MealRepository extends JpaRepository<Meal, Long>`:
     - `Page<Meal> findAllByUserId(Long userId, Pageable)`
     - `Page<Meal> findAllByUserIdAndConsumedAtBetween(Long userId, Instant from, Instant to, Pageable)`
     - `Optional<Meal> findByIdAndUserId(Long id, Long userId)`

**Acceptance:** Hibernate validates schema on startup with `ddl-auto: validate` (no errors). A throwaway `@DataJpaTest` against Testcontainers loads all entities without exception.

---

## Phase 3 — DTOs

**Goal:** Request/response records under `dto.*` (Java records).

**Common:**
- `PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages)` — generic page wrapper.
- `ErrorResponse(String code, String message, Instant timestamp, Map<String,String> details)` — same shape as Auth Service for consistency. Stable codes: `VALIDATION_FAILED`, `NOT_FOUND`, `CONFLICT`, `MISSING_USER_CONTEXT`, `INTERNAL_ERROR`.

**Exercise:**
- `ExerciseResponse(Long id, String name, String muscleGroup, String equipment, String difficulty, String description, Instant createdAt, Instant updatedAt)`
- `CreateExerciseRequest(@NotBlank @Size(max=128) String name, @NotBlank String muscleGroup, @NotBlank String equipment, @NotBlank String difficulty, @Size(max=2000) String description)`
- `UpdateExerciseRequest(String name, String muscleGroup, String equipment, String difficulty, String description)` — all nullable; null = don't change.

**Workout:**
- `WorkoutExerciseRequest(@NotNull Long exerciseId, @NotNull @Min(0) Integer position, @Min(1) Integer targetSets, @Min(1) Integer targetReps, @Size(max=255) String notes)`
- `WorkoutExerciseResponse(Long id, Long exerciseId, String exerciseName, int position, Integer targetSets, Integer targetReps, String notes)`
- `WorkoutResponse(Long id, Long userId, String name, String description, List<WorkoutExerciseResponse> exercises, Instant createdAt, Instant updatedAt)`
- `CreateWorkoutRequest(@NotBlank @Size(max=128) String name, @Size(max=2000) String description, @Valid List<WorkoutExerciseRequest> exercises)` — `exercises` may be empty.
- `UpdateWorkoutRequest(String name, String description, @Valid List<WorkoutExerciseRequest> exercises)` — if `exercises != null`, replace the entire list (delete-all-and-re-add semantics).

**Workout log:**
- `WorkoutLogSetRequest(@NotNull Long exerciseId, @NotNull @Min(1) Integer setNumber, @NotNull @Min(1) Integer reps, @DecimalMin("0.0") BigDecimal weightKg)`
- `WorkoutLogSetResponse(Long id, Long exerciseId, String exerciseName, int setNumber, int reps, BigDecimal weightKg)`
- `WorkoutLogResponse(Long id, Long userId, Long workoutId, String workoutName, Instant performedAt, Integer durationMinutes, String notes, List<WorkoutLogSetResponse> sets, Instant createdAt)`
- `CreateWorkoutLogRequest(Long workoutId, @NotNull Instant performedAt, @Min(0) Integer durationMinutes, @Size(max=2000) String notes, @Valid List<WorkoutLogSetRequest> sets)`
- `UpdateWorkoutLogRequest(Instant performedAt, Integer durationMinutes, String notes, @Valid List<WorkoutLogSetRequest> sets)`

**Food:**
- `FoodResponse(Long id, String name, String brand, BigDecimal servingSizeG, BigDecimal caloriesKcal, BigDecimal proteinG, BigDecimal carbsG, BigDecimal fatG, Instant createdAt, Instant updatedAt)`
- `CreateFoodRequest(@NotBlank @Size(max=128) String name, @Size(max=128) String brand, @NotNull @DecimalMin("0.01") BigDecimal servingSizeG, @NotNull @DecimalMin("0.0") BigDecimal caloriesKcal, @DecimalMin("0.0") BigDecimal proteinG, @DecimalMin("0.0") BigDecimal carbsG, @DecimalMin("0.0") BigDecimal fatG)`
- `UpdateFoodRequest(String name, String brand, BigDecimal servingSizeG, BigDecimal caloriesKcal, BigDecimal proteinG, BigDecimal carbsG, BigDecimal fatG)`

**Meal:**
- `MealItemRequest(@NotNull Long foodId, @NotNull @DecimalMin("0.01") BigDecimal quantityG)`
- `MealItemResponse(Long id, Long foodId, String foodName, BigDecimal quantityG, BigDecimal caloriesKcal, BigDecimal proteinG, BigDecimal carbsG, BigDecimal fatG)` — macros computed at response time from the food's per-serving values.
- `MealResponse(Long id, Long userId, String mealType, Instant consumedAt, String notes, List<MealItemResponse> items, BigDecimal totalCaloriesKcal, BigDecimal totalProteinG, BigDecimal totalCarbsG, BigDecimal totalFatG, Instant createdAt)`
- `CreateMealRequest(@NotBlank String mealType, @NotNull Instant consumedAt, @Size(max=255) String notes, @Valid List<MealItemRequest> items)`
- `UpdateMealRequest(String mealType, Instant consumedAt, String notes, @Valid List<MealItemRequest> items)`

---

## Phase 4 — Cross-cutting concerns: user context + exception handling

**Goal:** Consistent `X-User-Id` extraction and error responses.

1. Custom exceptions in `exception.*` (all extend `RuntimeException`):
   - `ResourceNotFoundException(String resource, Long id)` — used when a user-owned entity isn't found OR doesn't belong to the calling user. Same exception either way (don't leak existence).
   - `MissingUserContextException`
   - `ConflictException(String message)` — for unique-constraint violations surfacing as 409.
2. `web.UserContext` — small holder + helper:
   - `web.UserContextArgumentResolver implements HandlerMethodArgumentResolver`: extracts `X-User-Id` header → parses to Long → injects into controller params annotated `@CurrentUser Long userId`.
   - `web.CurrentUser` — `@interface` (parameter annotation, runtime retention).
   - If header missing or not a positive Long → throw `MissingUserContextException`.
3. Register the resolver in `config.WebMvcConfig implements WebMvcConfigurer`.
4. `controller.GlobalExceptionHandler` (`@RestControllerAdvice`):
   - `MethodArgumentNotValidException` → 400, `code=VALIDATION_FAILED`, `details` map field→message.
   - `ConstraintViolationException` → 400, same pattern.
   - `MissingServletRequestParameterException` → 400, `code=VALIDATION_FAILED`.
   - `MethodArgumentTypeMismatchException` → 400, `code=VALIDATION_FAILED`.
   - `HttpMessageNotReadableException` → 400, `code=VALIDATION_FAILED`, message `"Malformed JSON request"`.
   - `MissingUserContextException` → 400, `code=MISSING_USER_CONTEXT`, `message="X-User-Id header missing or invalid"`.
   - `ResourceNotFoundException` → 404, `code=NOT_FOUND`, `message="<Resource> not found"`. **Do not include the requested ID in the message** (avoid confirming existence).
   - `ConflictException` → 409, `code=CONFLICT`.
   - `DataIntegrityViolationException` → 409, `code=CONFLICT`, `message="Data integrity violation"`. Log full cause; return generic message (don't leak DB details).
   - `Exception` (fallback) → 500, log full stack trace, generic `code=INTERNAL_ERROR, message="Internal server error"`.

**Acceptance:** A throwaway controller method with `@CurrentUser Long userId` returns the parsed user ID. Hitting it without the header → 400 JSON with `MISSING_USER_CONTEXT`. Hitting it with `X-User-Id: notanumber` → 400 same code. Hitting it with `X-User-Id: -5` → 400 same code.

---

## Phase 5 — Exercise CRUD (reference data)

**Goal:** Reference catalogue endpoints. Global, no `X-User-Id`.

### 5a. `service.ExerciseService`

- `PageResponse<ExerciseResponse> list(int page, int size, String search, String muscleGroup, String equipment, String difficulty)`:
  - Build a `Specification<Exercise>` with optional `name ILIKE %search%`, `muscleGroup =`, `equipment =`, `difficulty =`.
  - `PageRequest.of(page, size, Sort.by("name"))`.
  - Map to `PageResponse<ExerciseResponse>`.
- `ExerciseResponse get(Long id)` — `ResourceNotFoundException("exercise", id)` if absent.
- `ExerciseResponse create(CreateExerciseRequest req)`:
  - Parse + validate enum values for `muscleGroup`, `equipment`, `difficulty`. Invalid → `IllegalArgumentException` → handled by global handler as 400.
  - Save and return mapped response.
  - On unique-constraint violation (`name`) → `ConflictException("Exercise with that name already exists")`.
- `ExerciseResponse update(Long id, UpdateExerciseRequest req)`:
  - Load. For each non-null field in `req`, validate (enums) and apply.
  - Save and return.
- `void delete(Long id)`:
  - **Hard delete.** If FK violation (referenced by workout_exercises or workout_log_sets) → `ConflictException("Exercise is referenced by existing workouts or logs")`.

### 5b. `controller.ExerciseController` (`@RequestMapping("/api/v1/exercises")`)

- `GET /` (query params: `page=0`, `size=20`, `search=`, `muscleGroup=`, `equipment=`, `difficulty=`) → `200`, body `PageResponse<ExerciseResponse>`.
- `GET /{id}` → `200`, body `ExerciseResponse`.
- `POST /` → `201`, body `ExerciseResponse`.
- `PATCH /{id}` → `200`, body `ExerciseResponse`.
- `DELETE /{id}` → `204`.

**Acceptance:** Full CRUD via curl works end-to-end. `GET` with filters narrows results. Creating duplicate name → 409. Deleting an exercise referenced by a workout → 409.

---

## Phase 6 — Workout CRUD (user-owned)

**Goal:** User-scoped workout templates with embedded exercises.

### 6a. `service.WorkoutService`

- `PageResponse<WorkoutResponse> list(Long userId, int page, int size)`:
  - `workoutRepository.findAllByUserId(userId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))`.
  - Map each to `WorkoutResponse` (includes nested exercises sorted by `position`).
- `WorkoutResponse get(Long userId, Long id)`:
  - `findByIdAndUserId(id, userId)` → `ResourceNotFoundException("workout", id)` if absent.
- `WorkoutResponse create(Long userId, CreateWorkoutRequest req)`:
  - Build `Workout` with `userId`, `name`, `description`.
  - For each `WorkoutExerciseRequest` in `req.exercises`:
    - Verify `exerciseRepository.existsById(exerciseId)`; if not → `ResourceNotFoundException("exercise", exerciseId)`.
    - Build `WorkoutExercise` and add to workout's collection.
  - Validate no duplicate `position` values within the request → 400 `VALIDATION_FAILED`.
  - Save workout (cascade saves exercises). Return mapped response.
- `WorkoutResponse update(Long userId, Long id, UpdateWorkoutRequest req)`:
  - Load with `findByIdAndUserId` → 404 if absent.
  - For each non-null scalar field (`name`, `description`), apply.
  - If `req.exercises != null`:
    - **Replace strategy:** clear the existing collection (orphanRemoval will delete them), then add new ones (validate exercise IDs exist, validate no duplicate positions).
  - Save and return.
- `void delete(Long userId, Long id)`:
  - `findByIdAndUserId` → 404 if absent.
  - Delete (cascade removes `workout_exercises`). `workout_logs.workout_id` is `ON DELETE SET NULL`, so logs survive.

### 6b. `controller.WorkoutController` (`@RequestMapping("/api/v1/workouts")`)

All methods take `@CurrentUser Long userId`.

- `GET /` (query params: `page=0`, `size=20`) → `200`, body `PageResponse<WorkoutResponse>`.
- `GET /{id}` → `200`, body `WorkoutResponse`.
- `POST /` → `201`, body `WorkoutResponse`.
- `PATCH /{id}` → `200`, body `WorkoutResponse`.
- `DELETE /{id}` → `204`.

**Acceptance:**
- Create workout for user 42 with 3 exercises → 201, response includes exercises ordered by position.
- `GET /workouts` with `X-User-Id: 42` lists only user 42's workouts.
- `GET /workouts/{id}` with the wrong user ID → 404 (not 403 — don't leak existence).
- PATCH with `exercises: [...]` replaces the full list cleanly.
- Without `X-User-Id` → 400 `MISSING_USER_CONTEXT`.

---

## Phase 7 — Workout log CRUD (user-owned)

**Goal:** Persisted workout sessions with sets.

### 7a. `service.WorkoutLogService`

- `PageResponse<WorkoutLogResponse> list(Long userId, int page, int size, Instant from, Instant to)`:
  - If `from` and `to` are both non-null, use the range query; else `findAllByUserId`.
  - Sort by `performedAt DESC`.
- `WorkoutLogResponse get(Long userId, Long id)` — same pattern as workouts.
- `WorkoutLogResponse create(Long userId, CreateWorkoutLogRequest req)`:
  - If `req.workoutId != null`, verify it exists and belongs to `userId` (`workoutRepository.existsByIdAndUserId`); if not → `ResourceNotFoundException("workout", workoutId)`.
  - Build `WorkoutLog`. For each `WorkoutLogSetRequest`:
    - Verify exercise exists.
    - Build `WorkoutLogSet`, add to log's collection.
  - Validate `(exerciseId, setNumber)` is unique within the request → 400.
  - Save and return.
- `WorkoutLogResponse update(Long userId, Long id, UpdateWorkoutLogRequest req)`:
  - Load with `findByIdAndUserId`.
  - Apply non-null scalars.
  - If `req.sets != null`: replace strategy (clear + re-add).
- `void delete(Long userId, Long id)` — load + delete.

### 7b. `controller.WorkoutLogController` (`@RequestMapping("/api/v1/workout-logs")`)

- `GET /` (query params: `page=0`, `size=20`, `from=`, `to=`) — `from`/`to` are ISO-8601 instants, both required together (return 400 if only one provided).
- `GET /{id}`, `POST /`, `PATCH /{id}`, `DELETE /{id}` — same shapes as workouts.

**Acceptance:**
- Log a workout with 3 exercises × 3 sets → 201, all sets persisted.
- List logs for user 42 → only their logs, newest first.
- Filter by `from=2026-01-01T00:00:00Z&to=2026-01-31T23:59:59Z` → only January logs.
- Cross-user access returns 404.

---

## Phase 8 — Food CRUD (reference data)

**Goal:** Reference foods catalogue. Global, no `X-User-Id`. Same pattern as exercises.

### 8a. `service.FoodService`

- `PageResponse<FoodResponse> list(int page, int size, String search)`:
  - Specification: optional `name ILIKE %search%` OR `brand ILIKE %search%`.
  - Sort by `name`.
- `FoodResponse get(Long id)`.
- `FoodResponse create(CreateFoodRequest req)` — unique name → 409 on conflict.
- `FoodResponse update(Long id, UpdateFoodRequest req)`.
- `void delete(Long id)` — FK violation (referenced by `meal_items`) → `ConflictException("Food is referenced by existing meals")`.

### 8b. `controller.FoodController` (`@RequestMapping("/api/v1/foods")`)

- `GET /` (query params: `page=0`, `size=20`, `search=`).
- `GET /{id}`, `POST /`, `PATCH /{id}`, `DELETE /{id}`.

**Acceptance:** Full CRUD works. Can't delete a food referenced by a meal → 409.

---

## Phase 9 — Meal CRUD (user-owned, with computed macros)

**Goal:** Meal logs with auto-computed totals.

### 9a. `service.MealService`

- `PageResponse<MealResponse> list(Long userId, int page, int size, Instant from, Instant to)` — analogous to workout logs.
- `MealResponse get(Long userId, Long id)`.
- `MealResponse create(Long userId, CreateMealRequest req)`:
  - Validate `mealType` enum value.
  - For each `MealItemRequest`: verify food exists → 404 if not.
  - Build `Meal` with items. Save. Return.
- `MealResponse update(Long userId, Long id, UpdateMealRequest req)` — replace strategy on items.
- `void delete(Long userId, Long id)`.
- `MealResponse` mapping logic: for each `MealItem`, compute `factor = quantityG / food.servingSizeG`, then per-item `caloriesKcal = food.caloriesKcal * factor` (same for protein/carbs/fat). Sum to get totals. **Round to 2 decimals using `BigDecimal.setScale(2, RoundingMode.HALF_UP)`.**

### 9b. `controller.MealController` (`@RequestMapping("/api/v1/meals")`)

- Same shapes as workout logs.

**Acceptance:**
- Log a meal: 200g chicken (per 100g: 165 kcal, 31g protein) + 100g rice (per 100g: 130 kcal, 2.7g protein) → totals 460 kcal, 64.7g protein.
- Daily totals can be computed by Business Logic Service summing `GET /meals?from=...&to=...`.

---

## Phase 10 — Observability

**Goal:** Prometheus scraping works with custom metrics.

1. `application.yml` actuator section:
   ```yaml
   management:
     endpoints.web.exposure.include: health,info,prometheus,metrics
     endpoint.health.probes.enabled: true
     metrics.tags.application: ${spring.application.name}
   ```
2. Custom counters via `MeterRegistry`:
   - `data_workouts_created_total`
   - `data_workouts_deleted_total`
   - `data_workout_logs_created_total`
   - `data_meals_created_total`
   - `data_exercises_mutations_total{action}` — tag `action` ∈ {`create`, `update`, `delete`}.
   - `data_foods_mutations_total{action}` — same tags.
   - `data_cross_user_access_blocked_total` — increment whenever a user-owned lookup returns `ResourceNotFoundException` because the row exists but belongs to a different user. (Track in `WorkoutService.get`/`update`/`delete` etc. by checking `findById` separately when needed for the metric. Skip if too invasive — primary signal can come from logs.)
3. Verify `GET /actuator/prometheus` returns metrics.

---

## Phase 11 — Tests

**Goal:** Decent coverage, especially of cross-user isolation.

### Unit tests

- `ExerciseServiceTest` (Mockito): list with/without filters; create duplicate-name throws; update partial fields; delete with FK violation throws.
- `WorkoutServiceTest`: create with exercises; create with non-existent exercise ID throws 404; update with replace-exercises strategy; cross-user `get`/`update`/`delete` returns 404.
- `WorkoutLogServiceTest`: log with sets; from/to filter; reference to non-existent workout throws 404.
- `FoodServiceTest`: same shape as exercise.
- `MealServiceTest`: macro computation correctness — feed known per-100g foods and quantities, assert exact totals (`BigDecimal` equals with scale 2).

### Integration tests

Under `src/test/java/.../it/`. `@SpringBootTest(webEnvironment=RANDOM_PORT)` + `@Testcontainers` with `PostgreSQLContainer` (one container reused via `static` field with manual `start()`). Use `MockMvc`. Override datasource via `@DynamicPropertySource`.

- `ExerciseFlowIT`: full CRUD via HTTP.
- `WorkoutFlowIT`: create → list (only own) → get → update (replace exercises) → delete.
- `CrossUserIsolationIT`: **critical.** User 42 creates a workout. User 99 attempts `GET /workouts/{id}` → 404. User 99 attempts `PATCH` → 404. User 99 attempts `DELETE` → 404. Verify nothing was modified.
- `MissingUserContextIT`: every user-scoped endpoint without `X-User-Id` → 400 `MISSING_USER_CONTEXT`.
- `MealMacroIT`: log a meal end-to-end via HTTP, verify computed totals in response.
- `WorkoutLogRangeIT`: insert logs across two months, query with `from`/`to`, verify only in-range returned.

**Acceptance:** `mvn verify` runs all tests green.

---

## Phase 12 — Docker

**Goal:** Production-ready image.

1. `Dockerfile` multi-stage:
   - **Builder:** `maven:3.9-eclipse-temurin-21` → copy `pom.xml`, run `mvn -B dependency:go-offline`, copy `src/`, run `mvn -B -DskipTests package`.
   - **Runtime:** `eclipse-temurin:21-jre-alpine` → create non-root user `appuser`, copy jar to `/app/app.jar`, `EXPOSE 8083`, `HEALTHCHECK CMD wget -qO- http://localhost:8083/actuator/health || exit 1`, `USER appuser`, `ENTRYPOINT ["java","-jar","/app/app.jar"]`.
2. `.dockerignore`: `target`, `.git`, `.idea`, `*.iml`, `*.md`, `Dockerfile`, etc.
3. `docker-compose.yml` (local dev only — prod stack lives in `trackIT-infrastructure`):
   - `data-db` (postgres:16-alpine), healthcheck `pg_isready`, named volume.
   - `data-service` depends_on `data-db` (`condition: service_healthy`), env vars wired up, ports `8083:8083`.
   - `pgadmin` (optional).
4. `docker compose up --build` → curl through `http://localhost:8083` works for full CRUD flow.

---

## Phase 13 — CI/CD (GitHub Actions)

**Goal:** Build/test on every push, publish image on `main`.

1. `.github/workflows/ci.yml`:
   - Trigger: `push` to any branch, `pull_request` to `main`.
   - Job `build-test`: `actions/checkout@v4`, `actions/setup-java@v4` (temurin 21), `actions/cache@v4` for `~/.m2`, run `mvn -B verify`.
   - Upload surefire/failsafe reports as artifact on failure.
2. `.github/workflows/release.yml`:
   - Trigger: `push` to `main`.
   - Steps: checkout, setup-java 21, build jar, `docker/setup-buildx-action@v3`, `docker/login-action@v3` with `${{ secrets.DOCKERHUB_USERNAME }}` / `${{ secrets.DOCKERHUB_TOKEN }}`, `docker/build-push-action@v5` with tags `florinionita/trackit-data-service:latest` and `florinionita/trackit-data-service:${{ github.sha }}`.
3. Document required GitHub secrets in `README.md`: `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`.

---

## Phase 14 — Documentation & release

**Goal:** Repo is self-explanatory and tagged.

1. `README.md`:
   - One-paragraph description (link to TrackIT umbrella project).
   - **⚠️ Security model section:** prominently document that this service has **no authentication** and trusts the `X-User-Id` header from callers. Must only be exposed on the internal `backend-net` network. **Never expose to the public internet or to Kong.**
   - **Quick start:** `docker compose up` + curl examples (every example must include `-H "X-User-Id: 42"` for user-scoped endpoints).
   - **API reference:** the endpoint table from the top of this plan, plus link to Swagger UI at `/swagger-ui.html`.
   - **Configuration:** table of env vars (`DATA_DB_URL`, `DATA_DB_USER`, `DATA_DB_PASSWORD`).
   - **Build & test:** `mvn verify`.
   - **Deployment:** Docker Swarm placement constraint `node.labels.role == app`. Reference to the stack file in `trackIT-infrastructure`.
   - **GitHub secrets required for CI:** `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`.
2. Final pass on `CHANGELOG.md`: ensure `[Unreleased]` accurately reflects everything done.
3. Bump `pom.xml` version `0.1.0-SNAPSHOT` → `0.1.0`. In `CHANGELOG.md`, replace `## [Unreleased]` heading with `## [0.1.0] - YYYY-MM-DD` and add a fresh empty `## [Unreleased]` section above. Commit, tag `v0.1.0`, push tag.

---

## Versioning notes

- Pre-1.0: API may break between minors. Bump minor for new endpoints, patch for fixes.
- After Etapa 1 ships and the API is stable, cut `1.0.0`.

## Out of scope (intentionally)

- JWT validation / Spring Security — by design. Network isolation + trusted `X-User-Id` from Business Logic Service.
- Rate limiting — Kong handles this at the gateway, and this service isn't reachable from outside `backend-net`.
- Aggregations / analytics — that's Business Logic Service's job. This service exposes raw data; Business Logic computes daily totals, weekly trends, etc.
- Soft deletes — hard delete with cascades is fine for user-owned data. Reference data (exercises/foods) refuses delete if FK-referenced.
- File uploads (e.g. exercise demo videos, food images) — would need object storage; out of scope for Etapa 1.
- Bulk endpoints — single-resource CRUD is sufficient for the demo.

If any of these become required later, add them as new phases here, bump the minor version, and document in `CHANGELOG.md`.
