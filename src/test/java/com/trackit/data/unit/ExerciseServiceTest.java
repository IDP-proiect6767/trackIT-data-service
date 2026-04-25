package com.trackit.data.unit;

import com.trackit.data.domain.Exercise;
import com.trackit.data.domain.enums.Difficulty;
import com.trackit.data.domain.enums.Equipment;
import com.trackit.data.domain.enums.MuscleGroup;
import com.trackit.data.dto.*;
import com.trackit.data.exception.ResourceNotFoundException;
import com.trackit.data.repository.ExerciseRepository;
import com.trackit.data.service.ExerciseService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExerciseServiceTest {

    @Mock
    private ExerciseRepository repository;

    private ExerciseService service;

    @BeforeEach
    void setUp() {
        MeterRegistry registry = new SimpleMeterRegistry();
        service = new ExerciseService(repository, registry);
    }

    @Test
    void list_returnsAllExercisesOnEmptyFilters() {
        Exercise ex = buildExercise(1L, "Bench Press", MuscleGroup.CHEST, Equipment.BARBELL, Difficulty.INTERMEDIATE);
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ex)));

        PageResponse<ExerciseResponse> result = service.list(0, 20, null, null, null, null);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).name()).isEqualTo("Bench Press");
    }

    @Test
    void get_throwsWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_throwsOnInvalidMuscleGroup() {
        CreateExerciseRequest req = new CreateExerciseRequest("Test", "INVALID", "BARBELL", "BEGINNER", null);
        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("muscleGroup");
    }

    @Test
    void update_appliesPartialFields() {
        Exercise ex = buildExercise(1L, "Old Name", MuscleGroup.CHEST, Equipment.BARBELL, Difficulty.BEGINNER);
        when(repository.findById(1L)).thenReturn(Optional.of(ex));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateExerciseRequest req = new UpdateExerciseRequest("New Name", null, null, null, null);
        ExerciseResponse response = service.update(1L, req);

        assertThat(response.name()).isEqualTo("New Name");
        assertThat(response.muscleGroup()).isEqualTo("CHEST");
    }

    private Exercise buildExercise(Long id, String name, MuscleGroup mg, Equipment eq, Difficulty diff) {
        return Exercise.builder()
                .id(id).name(name).muscleGroup(mg).equipment(eq).difficulty(diff)
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
    }
}
