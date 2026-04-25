package com.trackit.data.unit;

import com.trackit.data.domain.Exercise;
import com.trackit.data.domain.Workout;
import com.trackit.data.domain.enums.Difficulty;
import com.trackit.data.domain.enums.Equipment;
import com.trackit.data.domain.enums.MuscleGroup;
import com.trackit.data.dto.*;
import com.trackit.data.exception.ResourceNotFoundException;
import com.trackit.data.repository.ExerciseRepository;
import com.trackit.data.repository.WorkoutRepository;
import com.trackit.data.service.WorkoutService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkoutServiceTest {

    @Mock private WorkoutRepository workoutRepository;
    @Mock private ExerciseRepository exerciseRepository;

    private WorkoutService service;

    @BeforeEach
    void setUp() {
        service = new WorkoutService(workoutRepository, exerciseRepository, new SimpleMeterRegistry());
    }

    @Test
    void create_withNonExistentExercise_throws() {
        when(exerciseRepository.findById(999L)).thenReturn(Optional.empty());

        CreateWorkoutRequest req = new CreateWorkoutRequest("My Workout", null,
                List.of(new WorkoutExerciseRequest(999L, 0, null, null, null)));

        assertThatThrownBy(() -> service.create(42L, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_withDuplicatePositions_throws() {
        CreateWorkoutRequest req = new CreateWorkoutRequest("Workout", null, List.of(
                new WorkoutExerciseRequest(1L, 0, null, null, null),
                new WorkoutExerciseRequest(2L, 0, null, null, null)
        ));

        assertThatThrownBy(() -> service.create(42L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate position");
    }

    @Test
    void get_crossUser_throws() {
        when(workoutRepository.findByIdAndUserId(1L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_crossUser_throws() {
        when(workoutRepository.findByIdAndUserId(1L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Exercise buildExercise(Long id) {
        return Exercise.builder().id(id).name("Ex").muscleGroup(MuscleGroup.CHEST)
                .equipment(Equipment.BARBELL).difficulty(Difficulty.BEGINNER)
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
    }
}
