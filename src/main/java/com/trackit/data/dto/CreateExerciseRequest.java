package com.trackit.data.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateExerciseRequest(
        @NotBlank @Size(max = 128) String name,
        @NotBlank String muscleGroup,
        @NotBlank String equipment,
        @NotBlank String difficulty,
        @Size(max = 2000) String description
) {}
