package com.trackit.data.dto;

import jakarta.validation.Valid;

import java.util.List;

public record UpdateWorkoutRequest(
        String name,
        String description,
        @Valid List<WorkoutExerciseRequest> exercises
) {}
