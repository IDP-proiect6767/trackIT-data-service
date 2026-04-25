package com.trackit.data.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record WorkoutLogSetRequest(
        @NotNull Long exerciseId,
        @NotNull @Min(1) Integer setNumber,
        @NotNull @Min(1) Integer reps,
        @DecimalMin("0.0") BigDecimal weightKg
) {}
