package com.trackit.data.dto;

import java.time.Instant;
import java.util.List;

public record WorkoutResponse(
        Long id,
        Long userId,
        String name,
        String description,
        List<WorkoutExerciseResponse> exercises,
        Instant createdAt,
        Instant updatedAt
) {}
