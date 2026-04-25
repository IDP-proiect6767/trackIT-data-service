package com.trackit.data.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateFoodRequest(
        @NotBlank @Size(max = 128) String name,
        @Size(max = 128) String brand,
        @NotNull @DecimalMin("0.01") BigDecimal servingSizeG,
        @NotNull @DecimalMin("0.0") BigDecimal caloriesKcal,
        @DecimalMin("0.0") BigDecimal proteinG,
        @DecimalMin("0.0") BigDecimal carbsG,
        @DecimalMin("0.0") BigDecimal fatG
) {}
