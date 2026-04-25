package com.trackit.data.dto;

import java.math.BigDecimal;

public record UpdateFoodRequest(
        String name,
        String brand,
        BigDecimal servingSizeG,
        BigDecimal caloriesKcal,
        BigDecimal proteinG,
        BigDecimal carbsG,
        BigDecimal fatG
) {}
