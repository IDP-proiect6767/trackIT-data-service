package com.trackit.data.service;

import com.trackit.data.domain.Exercise;
import com.trackit.data.domain.WorkoutLog;
import com.trackit.data.domain.WorkoutLogSet;
import com.trackit.data.dto.*;
import com.trackit.data.exception.ResourceNotFoundException;
import com.trackit.data.repository.ExerciseRepository;
import com.trackit.data.repository.WorkoutLogRepository;
import com.trackit.data.repository.WorkoutRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class WorkoutLogService {

    private final WorkoutLogRepository logRepository;
    private final WorkoutRepository workoutRepository;
    private final ExerciseRepository exerciseRepository;
    private final MeterRegistry meterRegistry;

    public WorkoutLogService(WorkoutLogRepository logRepository, WorkoutRepository workoutRepository,
                             ExerciseRepository exerciseRepository, MeterRegistry meterRegistry) {
        this.logRepository = logRepository;
        this.workoutRepository = workoutRepository;
        this.exerciseRepository = exerciseRepository;
        this.meterRegistry = meterRegistry;
    }

    @Transactional(readOnly = true)
    public PageResponse<WorkoutLogResponse> list(Long userId, int page, int size, Instant from, Instant to) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "performedAt"));
        Page<WorkoutLog> result = (from != null && to != null)
                ? logRepository.findAllByUserIdAndPerformedAtBetween(userId, from, to, pageable)
                : logRepository.findAllByUserId(userId, pageable);
        List<WorkoutLogResponse> content = result.getContent().stream().map(this::toResponse).toList();
        return new PageResponse<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public WorkoutLogResponse get(Long userId, Long id) {
        return toResponse(find(userId, id));
    }

    public WorkoutLogResponse create(Long userId, CreateWorkoutLogRequest req) {
        WorkoutLog log = WorkoutLog.builder()
                .userId(userId)
                .performedAt(req.performedAt())
                .durationMinutes(req.durationMinutes())
                .notes(req.notes())
                .build();

        if (req.workoutId() != null) {
            if (!workoutRepository.existsByIdAndUserId(req.workoutId(), userId)) {
                throw new ResourceNotFoundException("workout", req.workoutId());
            }
            log.setWorkout(workoutRepository.getReferenceById(req.workoutId()));
        }

        if (req.sets() != null) {
            validateNoDuplicateSets(req.sets());
            for (WorkoutLogSetRequest s : req.sets()) {
                Exercise exercise = exerciseRepository.findById(s.exerciseId())
                        .orElseThrow(() -> new ResourceNotFoundException("exercise", s.exerciseId()));
                log.getSets().add(WorkoutLogSet.builder()
                        .workoutLog(log)
                        .exercise(exercise)
                        .setNumber(s.setNumber())
                        .reps(s.reps())
                        .weightKg(s.weightKg())
                        .build());
            }
        }

        WorkoutLog saved = logRepository.save(log);
        meterRegistry.counter("data_workout_logs_created_total").increment();
        return toResponse(saved);
    }

    public WorkoutLogResponse update(Long userId, Long id, UpdateWorkoutLogRequest req) {
        WorkoutLog log = find(userId, id);

        if (req.performedAt() != null) log.setPerformedAt(req.performedAt());
        if (req.durationMinutes() != null) log.setDurationMinutes(req.durationMinutes());
        if (req.notes() != null) log.setNotes(req.notes());
        if (req.sets() != null) {
            validateNoDuplicateSets(req.sets());
            log.getSets().clear();
            for (WorkoutLogSetRequest s : req.sets()) {
                Exercise exercise = exerciseRepository.findById(s.exerciseId())
                        .orElseThrow(() -> new ResourceNotFoundException("exercise", s.exerciseId()));
                log.getSets().add(WorkoutLogSet.builder()
                        .workoutLog(log)
                        .exercise(exercise)
                        .setNumber(s.setNumber())
                        .reps(s.reps())
                        .weightKg(s.weightKg())
                        .build());
            }
        }

        return toResponse(logRepository.save(log));
    }

    public void delete(Long userId, Long id) {
        WorkoutLog log = find(userId, id);
        logRepository.delete(log);
    }

    private WorkoutLog find(Long userId, Long id) {
        return logRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("workout log", id));
    }

    private void validateNoDuplicateSets(List<WorkoutLogSetRequest> sets) {
        Set<String> keys = sets.stream()
                .map(s -> s.exerciseId() + ":" + s.setNumber())
                .collect(Collectors.toSet());
        if (keys.size() != sets.size()) {
            throw new IllegalArgumentException("Duplicate (exerciseId, setNumber) pairs in sets list");
        }
    }

    private WorkoutLogResponse toResponse(WorkoutLog log) {
        List<WorkoutLogSetResponse> sets = log.getSets().stream()
                .map(s -> new WorkoutLogSetResponse(
                        s.getId(), s.getExercise().getId(), s.getExercise().getName(),
                        s.getSetNumber(), s.getReps(), s.getWeightKg()))
                .toList();
        Long workoutId = log.getWorkout() != null ? log.getWorkout().getId() : null;
        String workoutName = log.getWorkout() != null ? log.getWorkout().getName() : null;
        return new WorkoutLogResponse(log.getId(), log.getUserId(), workoutId, workoutName,
                log.getPerformedAt(), log.getDurationMinutes(), log.getNotes(), sets, log.getCreatedAt());
    }
}
