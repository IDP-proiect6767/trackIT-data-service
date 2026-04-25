package com.trackit.data.dto;

import jakarta.validation.Valid;

import java.time.Instant;
import java.util.List;

public record UpdateWorkoutLogRequest(
        Instant performedAt,
        Integer durationMinutes,
        String notes,
        @Valid List<WorkoutLogSetRequest> sets
) {}
