package com.trackit.data.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WorkoutExerciseRequest(
        @NotNull Long exerciseId,
        @NotNull @Min(0) Integer position,
        @Min(1) Integer targetSets,
        @Min(1) Integer targetReps,
        @Size(max = 255) String notes
) {}
