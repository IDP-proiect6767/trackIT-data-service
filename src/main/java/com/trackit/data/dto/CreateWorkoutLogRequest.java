package com.trackit.data.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record CreateWorkoutLogRequest(
        Long workoutId,
        @NotNull Instant performedAt,
        @Min(0) Integer durationMinutes,
        @Size(max = 2000) String notes,
        @Valid List<WorkoutLogSetRequest> sets
) {}
