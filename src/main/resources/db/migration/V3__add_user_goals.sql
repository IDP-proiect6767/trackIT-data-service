-- User fitness goals (one row per user, upserted on PUT)
CREATE TABLE data.user_goals (
    user_id           BIGINT        NOT NULL PRIMARY KEY,
    daily_calories_kcal NUMERIC(7, 2) NOT NULL DEFAULT 2000,
    daily_protein_g   NUMERIC(6, 2) NOT NULL DEFAULT 150,
    daily_carbs_g     NUMERIC(6, 2) NOT NULL DEFAULT 250,
    daily_fat_g       NUMERIC(6, 2) NOT NULL DEFAULT 65,
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
