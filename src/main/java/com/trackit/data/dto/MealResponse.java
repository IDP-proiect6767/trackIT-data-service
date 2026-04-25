package com.trackit.data.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record MealResponse(
        Long id,
        Long userId,
        String mealType,
        Instant consumedAt,
        String notes,
        List<MealItemResponse> items,
        BigDecimal totalCaloriesKcal,
        BigDecimal totalProteinG,
        BigDecimal totalCarbsG,
        BigDecimal totalFatG,
        Instant createdAt
) {}
