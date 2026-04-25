package com.trackit.data.dto;

import jakarta.validation.Valid;

import java.time.Instant;
import java.util.List;

public record UpdateMealRequest(
        String mealType,
        Instant consumedAt,
        String notes,
        @Valid List<MealItemRequest> items
) {}
