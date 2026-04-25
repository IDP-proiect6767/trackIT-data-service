package com.trackit.data.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record CreateMealRequest(
        @NotBlank String mealType,
        @NotNull Instant consumedAt,
        @Size(max = 255) String notes,
        @Valid List<MealItemRequest> items
) {}
