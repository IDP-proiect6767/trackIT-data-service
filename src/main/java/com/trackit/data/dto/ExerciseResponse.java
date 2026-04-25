package com.trackit.data.dto;

import java.time.Instant;

public record ExerciseResponse(
        Long id,
        String name,
        String muscleGroup,
        String equipment,
        String difficulty,
        String description,
        Instant createdAt,
        Instant updatedAt
) {}
