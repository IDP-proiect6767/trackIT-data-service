package com.trackit.data.service;

import com.trackit.data.domain.Food;
import com.trackit.data.dto.*;
import com.trackit.data.exception.ConflictException;
import com.trackit.data.exception.ResourceNotFoundException;
import com.trackit.data.repository.FoodRepository;
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
public class FoodService {

    private final FoodRepository repository;
    private final MeterRegistry meterRegistry;

    public FoodService(FoodRepository repository, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.meterRegistry = meterRegistry;
    }

    @Transactional(readOnly = true)
    public PageResponse<FoodResponse> list(int page, int size, String search) {
        Specification<Food> spec = buildSpec(search);
        Page<Food> result = repository.findAll(spec, PageRequest.of(page, size, Sort.by("name")));
        List<FoodResponse> content = result.getContent().stream().map(this::toResponse).toList();
        return new PageResponse<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public FoodResponse get(Long id) {
        return toResponse(find(id));
    }

    public FoodResponse create(CreateFoodRequest req) {
        Food food = Food.builder()
                .name(req.name())
                .brand(req.brand())
                .servingSizeG(req.servingSizeG())
                .caloriesKcal(req.caloriesKcal())
                .proteinG(req.proteinG() != null ? req.proteinG() : java.math.BigDecimal.ZERO)
                .carbsG(req.carbsG() != null ? req.carbsG() : java.math.BigDecimal.ZERO)
                .fatG(req.fatG() != null ? req.fatG() : java.math.BigDecimal.ZERO)
                .build();
        try {
            Food saved = repository.save(food);
            meterRegistry.counter("data_foods_mutations_total", "action", "create").increment();
            return toResponse(saved);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new ConflictException("Food with that name already exists");
        }
    }

    public FoodResponse update(Long id, UpdateFoodRequest req) {
        Food food = find(id);

        if (req.name() != null) food.setName(req.name());
        if (req.brand() != null) food.setBrand(req.brand());
        if (req.servingSizeG() != null) food.setServingSizeG(req.servingSizeG());
        if (req.caloriesKcal() != null) food.setCaloriesKcal(req.caloriesKcal());
        if (req.proteinG() != null) food.setProteinG(req.proteinG());
        if (req.carbsG() != null) food.setCarbsG(req.carbsG());
        if (req.fatG() != null) food.setFatG(req.fatG());

        meterRegistry.counter("data_foods_mutations_total", "action", "update").increment();
        return toResponse(repository.save(food));
    }

    public void delete(Long id) {
        Food food = find(id);
        try {
            repository.delete(food);
            repository.flush();
            meterRegistry.counter("data_foods_mutations_total", "action", "delete").increment();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new ConflictException("Food is referenced by existing meals");
        }
    }

    private Food find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("food", id));
    }

    private Specification<Food> buildSpec(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) return cb.conjunction();
            String pattern = "%" + search.toLowerCase() + "%";
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.like(cb.lower(root.get("name")), pattern));
            predicates.add(cb.like(cb.lower(root.get("brand")), pattern));
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }

    private FoodResponse toResponse(Food f) {
        return new FoodResponse(f.getId(), f.getName(), f.getBrand(), f.getServingSizeG(),
                f.getCaloriesKcal(), f.getProteinG(), f.getCarbsG(), f.getFatG(),
                f.getCreatedAt(), f.getUpdatedAt());
    }
}
