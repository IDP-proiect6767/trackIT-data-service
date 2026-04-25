package com.trackit.data.dto;

import java.time.Instant;
import java.util.List;

public record WorkoutLogResponse(
        Long id,
        Long userId,
        Long workoutId,
        String workoutName,
        Instant performedAt,
        Integer durationMinutes,
        String notes,
        List<WorkoutLogSetResponse> sets,
        Instant createdAt
) {}
