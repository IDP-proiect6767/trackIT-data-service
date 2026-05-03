package com.trackit.data.dto.goals;

import java.math.BigDecimal;
import java.time.Instant;

public record GoalsResponse(
        Long userId,
        BigDecimal dailyCaloriesKcal,
        BigDecimal dailyProteinG,
        BigDecimal dailyCarbsG,
        BigDecimal dailyFatG,
        Instant updatedAt
) {}
