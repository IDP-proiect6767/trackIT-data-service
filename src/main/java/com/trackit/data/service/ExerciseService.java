package com.trackit.data.service;

import com.trackit.data.domain.Exercise;
import com.trackit.data.domain.enums.Difficulty;
import com.trackit.data.domain.enums.Equipment;
import com.trackit.data.domain.enums.MuscleGroup;
import com.trackit.data.dto.*;
import com.trackit.data.exception.ConflictException;
import com.trackit.data.exception.ResourceNotFoundException;
import com.trackit.data.repository.ExerciseRepository;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class ExerciseService {

    private final ExerciseRepository repository;
    private final MeterRegistry meterRegistry;

    public ExerciseService(ExerciseRepository repository, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.meterRegistry = meterRegistry;
    }

    @Transactional(readOnly = true)
    public PageResponse<ExerciseResponse> list(int page, int size, String search,
                                                String muscleGroup, String equipment, String difficulty) {
        Specification<Exercise> spec = buildSpec(search, muscleGroup, equipment, difficulty);
        Page<Exercise> result = repository.findAll(spec, PageRequest.of(page, size, Sort.by("name")));
        return toPageResponse(result);
    }

    @Transactional(readOnly = true)
    public ExerciseResponse get(Long id) {
        return toResponse(find(id));
    }

    public ExerciseResponse create(CreateExerciseRequest req) {
        MuscleGroup mg = parseEnum(MuscleGroup.class, req.muscleGroup(), "muscleGroup");
        Equipment eq = parseEnum(Equipment.class, req.equipment(), "equipment");
        Difficulty diff = parseEnum(Difficulty.class, req.difficulty(), "difficulty");

        Exercise exercise = Exercise.builder()
                .name(req.name())
                .muscleGroup(mg)
                .equipment(eq)
                .difficulty(diff)
                .description(req.description())
                .build();

        try {
            Exercise saved = repository.save(exercise);
            meterRegistry.counter("data_exercises_mutations_total", "action", "create").increment();
            return toResponse(saved);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new ConflictException("Exercise with that name already exists");
        }
    }

    public ExerciseResponse update(Long id, UpdateExerciseRequest req) {
        Exercise exercise = find(id);

        if (req.name() != null) exercise.setName(req.name());
        if (req.muscleGroup() != null) exercise.setMuscleGroup(parseEnum(MuscleGroup.class, req.muscleGroup(), "muscleGroup"));
        if (req.equipment() != null) exercise.setEquipment(parseEnum(Equipment.class, req.equipment(), "equipment"));
        if (req.difficulty() != null) exercise.setDifficulty(parseEnum(Difficulty.class, req.difficulty(), "difficulty"));
        if (req.description() != null) exercise.setDescription(req.description());

        meterRegistry.counter("data_exercises_mutations_total", "action", "update").increment();
        return toResponse(repository.save(exercise));
    }

    public void delete(Long id) {
        Exercise exercise = find(id);
        try {
            repository.delete(exercise);
            repository.flush();
            meterRegistry.counter("data_exercises_mutations_total", "action", "delete").increment();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new ConflictException("Exercise is referenced by existing workouts or logs");
        }
    }

    private Exercise find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("exercise", id));
    }

    private Specification<Exercise> buildSpec(String search, String muscleGroup,
                                               String equipment, String difficulty) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"));
            }
            if (muscleGroup != null && !muscleGroup.isBlank()) {
                predicates.add(cb.equal(root.get("muscleGroup"),
                        parseEnum(MuscleGroup.class, muscleGroup, "muscleGroup")));
            }
            if (equipment != null && !equipment.isBlank()) {
                predicates.add(cb.equal(root.get("equipment"),
                        parseEnum(Equipment.class, equipment, "equipment")));
            }
            if (difficulty != null && !difficulty.isBlank()) {
                predicates.add(cb.equal(root.get("difficulty"),
                        parseEnum(Difficulty.class, difficulty, "difficulty")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private <T extends Enum<T>> T parseEnum(Class<T> enumClass, String value, String field) {
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid value '" + value + "' for field " + field);
        }
    }

    private PageResponse<ExerciseResponse> toPageResponse(Page<Exercise> page) {
        List<ExerciseResponse> content = page.getContent().stream().map(this::toResponse).toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    private ExerciseResponse toResponse(Exercise e) {
        return new ExerciseResponse(e.getId(), e.getName(), e.getMuscleGroup().name(),
                e.getEquipment().name(), e.getDifficulty().name(), e.getDescription(),
                e.getCreatedAt(), e.getUpdatedAt());
    }
}
