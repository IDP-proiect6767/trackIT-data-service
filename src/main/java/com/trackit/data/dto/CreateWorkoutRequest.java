package com.trackit.data.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateWorkoutRequest(
        @NotBlank @Size(max = 128) String name,
        @Size(max = 2000) String description,
        @Valid List<WorkoutExerciseRequest> exercises
) {}
