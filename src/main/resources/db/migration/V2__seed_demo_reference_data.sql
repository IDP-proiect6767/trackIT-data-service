-- Demo seed data; expand or replace as needed.

INSERT INTO data.exercises (name, muscle_group, equipment, difficulty, description) VALUES
    ('Bench Press',      'CHEST',     'BARBELL',    'INTERMEDIATE', 'Classic compound chest press on a flat bench'),
    ('Squat',            'LEGS',      'BARBELL',    'INTERMEDIATE', 'Barbell back squat — king of lower body exercises'),
    ('Deadlift',         'BACK',      'BARBELL',    'ADVANCED',     'Hip-hinge compound pull from the floor'),
    ('Pull-up',          'BACK',      'BODYWEIGHT', 'INTERMEDIATE', 'Bodyweight vertical pull to a fixed bar'),
    ('Overhead Press',   'SHOULDERS', 'BARBELL',    'INTERMEDIATE', 'Standing barbell press overhead'),
    ('Plank',            'CORE',      'BODYWEIGHT', 'BEGINNER',     'Isometric core stabilisation hold'),
    ('Push-up',          'CHEST',     'BODYWEIGHT', 'BEGINNER',     'Bodyweight horizontal push'),
    ('Barbell Row',      'BACK',      'BARBELL',    'INTERMEDIATE', 'Horizontal barbell pull to the lower chest'),
    ('Bicep Curl',       'ARMS',      'DUMBBELL',   'BEGINNER',     'Isolation curl for the biceps brachii'),
    ('Running',          'CARDIO',    'OTHER',      'BEGINNER',     'Steady-state or interval cardio running');

-- Macros per reference serving size (serving_size_g column)
INSERT INTO data.foods (name, brand, serving_size_g, calories_kcal, protein_g, carbs_g, fat_g) VALUES
    ('Chicken Breast',  NULL,           100,  165.00, 31.00,  0.00,  3.60),
    ('White Rice',      NULL,           100,  130.00,  2.70, 28.20,  0.30),
    ('Olive Oil',       NULL,           100,  884.00,  0.00,  0.00, 100.00),
    ('Banana',          NULL,           100,   89.00,  1.10, 22.80,  0.30),
    ('Whey Protein',    'Generic Brand', 30,  120.00, 25.00,  3.00,  2.00),
    ('Egg (whole)',     NULL,            50,   72.00,  6.30,  0.40,  5.00),
    ('Rolled Oats',     NULL,           100,  389.00, 16.90, 66.30,  6.90),
    ('Greek Yogurt',    NULL,           100,   59.00, 10.00,  3.60,  0.40),
    ('Almonds',         NULL,           100,  579.00, 21.20, 21.60, 49.90),
    ('Broccoli',        NULL,           100,   34.00,  2.80,  6.60,  0.40);
