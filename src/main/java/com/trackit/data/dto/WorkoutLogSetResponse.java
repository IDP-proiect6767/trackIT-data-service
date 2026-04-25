package com.trackit.data.dto;

import java.math.BigDecimal;

public record WorkoutLogSetResponse(
        Long id,
        Long exerciseId,
        String exerciseName,
        int setNumber,
        int reps,
        BigDecimal weightKg
) {}
