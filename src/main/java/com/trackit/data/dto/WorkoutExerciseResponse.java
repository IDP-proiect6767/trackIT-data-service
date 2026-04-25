package com.trackit.data.dto;

public record WorkoutExerciseResponse(
        Long id,
        Long exerciseId,
        String exerciseName,
        int position,
        Integer targetSets,
        Integer targetReps,
        String notes
) {}
