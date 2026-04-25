package com.trackit.data.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MealItemRequest(
        @NotNull Long foodId,
        @NotNull @DecimalMin("0.01") BigDecimal quantityG
) {}
