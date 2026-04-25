package com.trackit.data.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record FoodResponse(
        Long id,
        String name,
        String brand,
        BigDecimal servingSizeG,
        BigDecimal caloriesKcal,
        BigDecimal proteinG,
        BigDecimal carbsG,
        BigDecimal fatG,
        Instant createdAt,
        Instant updatedAt
) {}
