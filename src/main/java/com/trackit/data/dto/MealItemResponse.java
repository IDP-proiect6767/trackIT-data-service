package com.trackit.data.dto;

import java.math.BigDecimal;

public record MealItemResponse(
        Long id,
        Long foodId,
        String foodName,
        BigDecimal quantityG,
        BigDecimal caloriesKcal,
        BigDecimal proteinG,
        BigDecimal carbsG,
        BigDecimal fatG
) {}
