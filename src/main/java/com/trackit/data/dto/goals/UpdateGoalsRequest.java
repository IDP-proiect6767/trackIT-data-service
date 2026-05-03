package com.trackit.data.dto.goals;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateGoalsRequest(
        @NotNull @DecimalMin("0.0") BigDecimal dailyCaloriesKcal,
        @NotNull @DecimalMin("0.0") BigDecimal dailyProteinG,
        @NotNull @DecimalMin("0.0") BigDecimal dailyCarbsG,
        @NotNull @DecimalMin("0.0") BigDecimal dailyFatG
) {}
