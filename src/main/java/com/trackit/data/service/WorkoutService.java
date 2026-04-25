package com.trackit.data.service;

import com.trackit.data.domain.Exercise;
import com.trackit.data.domain.Workout;
import com.trackit.data.domain.WorkoutExercise;
import com.trackit.data.dto.*;
import com.trackit.data.exception.ResourceNotFoundException;
import com.trackit.data.repository.ExerciseRepository;
import com.trackit.data.repository.WorkoutRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class WorkoutService {

    private final WorkoutRepository workoutRepository;
    private final ExerciseRepository exerciseRepository;
    private final MeterRegistry meterRegistry;

    public WorkoutService(WorkoutRepository workoutRepository, ExerciseRepository exerciseRepository,
                          MeterRegistry meterRegistry) {
        this.workoutRepository = workoutRepository;
        this.exerciseRepository = exerciseRepository;
        this.meterRegistry = meterRegistry;
    }

    @Transactional(readOnly = true)
    public PageResponse<WorkoutResponse> list(Long userId, int page, int size) {
        Page<Workout> result = workoutRepository.findAllByUserId(userId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<WorkoutResponse> content = result.getContent().stream().map(this::toResponse).toList();
        return new PageResponse<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public WorkoutResponse get(Long userId, Long id) {
        return toResponse(find(userId, id));
    }

    public WorkoutResponse create(Long userId, CreateWorkoutRequest req) {
        validateNoDuplicatePositions(req.exercises());

        Workout workout = Workout.builder()
                .userId(userId)
                .name(req.name())
                .description(req.description())
                .build();

        if (req.exercises() != null) {
            for (WorkoutExerciseRequest we : req.exercises()) {
                Exercise exercise = exerciseRepository.findById(we.exerciseId())
                        .orElseThrow(() -> new ResourceNotFoundException("exercise", we.exerciseId()));
                workout.getExercises().add(buildWorkoutExercise(workout, exercise, we));
            }
        }

        Workout saved = workoutRepository.save(workout);
        meterRegistry.counter("data_workouts_created_total").increment();
        return toResponse(saved);
    }

    public WorkoutResponse update(Long userId, Long id, UpdateWorkoutRequest req) {
        Workout workout = find(userId, id);

        if (req.name() != null) workout.setName(req.name());
        if (req.description() != null) workout.setDescription(req.description());
        if (req.exercises() != null) {
            validateNoDuplicatePositions(req.exercises());
            workout.getExercises().clear();
            for (WorkoutExerciseRequest we : req.exercises()) {
                Exercise exercise = exerciseRepository.findById(we.exerciseId())
                        .orElseThrow(() -> new ResourceNotFoundException("exercise", we.exerciseId()));
                workout.getExercises().add(buildWorkoutExercise(workout, exercise, we));
            }
        }

        return toResponse(workoutRepository.save(workout));
    }

    public void delete(Long userId, Long id) {
        Workout workout = find(userId, id);
        workoutRepository.delete(workout);
        meterRegistry.counter("data_workouts_deleted_total").increment();
    }

    private Workout find(Long userId, Long id) {
        return workoutRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("workout", id));
    }

    private WorkoutExercise buildWorkoutExercise(Workout workout, Exercise exercise, WorkoutExerciseRequest req) {
        return WorkoutExercise.builder()
                .workout(workout)
                .exercise(exercise)
                .position(req.position())
                .targetSets(req.targetSets())
                .targetReps(req.targetReps())
                .notes(req.notes())
                .build();
    }

    private void validateNoDuplicatePositions(List<WorkoutExerciseRequest> exercises) {
        if (exercises == null) return;
        Set<Integer> positions = exercises.stream()
                .map(WorkoutExerciseRequest::position)
                .collect(Collectors.toSet());
        if (positions.size() != exercises.size()) {
            throw new IllegalArgumentException("Duplicate position values in exercises list");
        }
    }

    private WorkoutResponse toResponse(Workout w) {
        List<WorkoutExerciseResponse> exercises = w.getExercises().stream()
                .map(we -> new WorkoutExerciseResponse(
                        we.getId(), we.getExercise().getId(), we.getExercise().getName(),
                        we.getPosition(), we.getTargetSets(), we.getTargetReps(), we.getNotes()))
                .toList();
        return new WorkoutResponse(w.getId(), w.getUserId(), w.getName(), w.getDescription(),
                exercises, w.getCreatedAt(), w.getUpdatedAt());
    }
}
