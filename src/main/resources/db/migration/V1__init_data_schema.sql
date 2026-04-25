-- TrackIT Data Service — initial schema

CREATE SCHEMA IF NOT EXISTS data;

-- Reference data: exercises catalogue (global, not user-scoped)
CREATE TABLE data.exercises (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(128)  NOT NULL UNIQUE,
    muscle_group  VARCHAR(64)   NOT NULL,
    equipment     VARCHAR(64)   NOT NULL,
    difficulty    VARCHAR(20)   NOT NULL CHECK (difficulty IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    description   TEXT          NULL,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- User-owned workout templates
CREATE TABLE data.workouts (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT        NOT NULL,
    name        VARCHAR(128)  NOT NULL,
    description TEXT          NULL,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- Join: exercises within a workout, ordered by position
CREATE TABLE data.workout_exercises (
    id          BIGSERIAL PRIMARY KEY,
    workout_id  BIGINT  NOT NULL REFERENCES data.workouts(id) ON DELETE CASCADE,
    exercise_id BIGINT  NOT NULL REFERENCES data.exercises(id),
    position    INT     NOT NULL DEFAULT 0,
    target_sets INT     NULL,
    target_reps INT     NULL,
    notes       VARCHAR(255) NULL,
    UNIQUE (workout_id, position)
);

-- User-owned workout session records
CREATE TABLE data.workout_logs (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT      NOT NULL,
    workout_id       BIGINT      NULL REFERENCES data.workouts(id) ON DELETE SET NULL,
    performed_at     TIMESTAMPTZ NOT NULL,
    duration_minutes INT         NULL,
    notes            TEXT        NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Individual sets within a logged workout session
CREATE TABLE data.workout_log_sets (
    id             BIGSERIAL PRIMARY KEY,
    workout_log_id BIGINT         NOT NULL REFERENCES data.workout_logs(id) ON DELETE CASCADE,
    exercise_id    BIGINT         NOT NULL REFERENCES data.exercises(id),
    set_number     INT            NOT NULL,
    reps           INT            NOT NULL,
    weight_kg      NUMERIC(6, 2)  NULL,
    UNIQUE (workout_log_id, exercise_id, set_number)
);

-- Reference data: foods catalogue (global, not user-scoped)
CREATE TABLE data.foods (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(128)  NOT NULL UNIQUE,
    brand          VARCHAR(128)  NULL,
    serving_size_g NUMERIC(7, 2) NOT NULL,
    calories_kcal  NUMERIC(7, 2) NOT NULL,
    protein_g      NUMERIC(6, 2) NOT NULL DEFAULT 0,
    carbs_g        NUMERIC(6, 2) NOT NULL DEFAULT 0,
    fat_g          NUMERIC(6, 2) NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- User-owned meal log entries
CREATE TABLE data.meals (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL,
    meal_type   VARCHAR(20) NOT NULL CHECK (meal_type IN ('BREAKFAST', 'LUNCH', 'DINNER', 'SNACK')),
    consumed_at TIMESTAMPTZ NOT NULL,
    notes       VARCHAR(255) NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Food entries within a meal (macros computed from food reference data)
CREATE TABLE data.meal_items (
    id         BIGSERIAL PRIMARY KEY,
    meal_id    BIGINT        NOT NULL REFERENCES data.meals(id) ON DELETE CASCADE,
    food_id    BIGINT        NOT NULL REFERENCES data.foods(id),
    quantity_g NUMERIC(7, 2) NOT NULL
);

-- Indexes for hot query paths
CREATE INDEX idx_workouts_user_id            ON data.workouts(user_id);
CREATE INDEX idx_workout_logs_user_id        ON data.workout_logs(user_id);
CREATE INDEX idx_workout_logs_performed_at   ON data.workout_logs(user_id, performed_at DESC);
CREATE INDEX idx_meals_user_id               ON data.meals(user_id);
CREATE INDEX idx_meals_consumed_at           ON data.meals(user_id, consumed_at DESC);
CREATE INDEX idx_workout_exercises_workout_id ON data.workout_exercises(workout_id);
CREATE INDEX idx_meal_items_meal_id          ON data.meal_items(meal_id);
