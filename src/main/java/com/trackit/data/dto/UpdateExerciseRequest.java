package com.trackit.data.dto;

public record UpdateExerciseRequest(
        String name,
        String muscleGroup,
        String equipment,
        String difficulty,
        String description
) {}
