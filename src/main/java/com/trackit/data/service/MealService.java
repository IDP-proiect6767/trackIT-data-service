package com.trackit.data.service;

import com.trackit.data.domain.Food;
import com.trackit.data.domain.Meal;
import com.trackit.data.domain.MealItem;
import com.trackit.data.domain.enums.MealType;
import com.trackit.data.dto.*;
import com.trackit.data.exception.ResourceNotFoundException;
import com.trackit.data.repository.FoodRepository;
import com.trackit.data.repository.MealRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class MealService {

    private final MealRepository mealRepository;
    private final FoodRepository foodRepository;
    private final MeterRegistry meterRegistry;

    public MealService(MealRepository mealRepository, FoodRepository foodRepository, MeterRegistry meterRegistry) {
        this.mealRepository = mealRepository;
        this.foodRepository = foodRepository;
        this.meterRegistry = meterRegistry;
    }

    @Transactional(readOnly = true)
    public PageResponse<MealResponse> list(Long userId, int page, int size, Instant from, Instant to) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "consumedAt"));
        Page<Meal> result = (from != null && to != null)
                ? mealRepository.findAllByUserIdAndConsumedAtBetween(userId, from, to, pageable)
                : mealRepository.findAllByUserId(userId, pageable);
        List<MealResponse> content = result.getContent().stream().map(this::toResponse).toList();
        return new PageResponse<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public MealResponse get(Long userId, Long id) {
        return toResponse(find(userId, id));
    }

    public MealResponse create(Long userId, CreateMealRequest req) {
        MealType mealType = parseMealType(req.mealType());

        Meal meal = Meal.builder()
                .userId(userId)
                .mealType(mealType)
                .consumedAt(req.consumedAt())
                .notes(req.notes())
                .build();

        if (req.items() != null) {
            for (MealItemRequest item : req.items()) {
                Food food = foodRepository.findById(item.foodId())
                        .orElseThrow(() -> new ResourceNotFoundException("food", item.foodId()));
                meal.getItems().add(MealItem.builder()
                        .meal(meal)
                        .food(food)
                        .quantityG(item.quantityG())
                        .build());
            }
        }

        Meal saved = mealRepository.save(meal);
        meterRegistry.counter("data_meals_created_total").increment();
        return toResponse(saved);
    }

    public MealResponse update(Long userId, Long id, UpdateMealRequest req) {
        Meal meal = find(userId, id);

        if (req.mealType() != null) meal.setMealType(parseMealType(req.mealType()));
        if (req.consumedAt() != null) meal.setConsumedAt(req.consumedAt());
        if (req.notes() != null) meal.setNotes(req.notes());
        if (req.items() != null) {
            meal.getItems().clear();
            for (MealItemRequest item : req.items()) {
                Food food = foodRepository.findById(item.foodId())
                        .orElseThrow(() -> new ResourceNotFoundException("food", item.foodId()));
                meal.getItems().add(MealItem.builder()
                        .meal(meal)
                        .food(food)
                        .quantityG(item.quantityG())
                        .build());
            }
        }

        return toResponse(mealRepository.save(meal));
    }

    public void delete(Long userId, Long id) {
        Meal meal = find(userId, id);
        mealRepository.delete(meal);
    }

    private Meal find(Long userId, Long id) {
        return mealRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("meal", id));
    }

    private MealType parseMealType(String value) {
        try {
            return MealType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid mealType: " + value);
        }
    }

    private MealResponse toResponse(Meal meal) {
        List<MealItemResponse> itemResponses = meal.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        BigDecimal totalCalories = itemResponses.stream()
                .map(MealItemResponse::caloriesKcal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalProtein = itemResponses.stream()
                .map(MealItemResponse::proteinG)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalCarbs = itemResponses.stream()
                .map(MealItemResponse::carbsG)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalFat = itemResponses.stream()
                .map(MealItemResponse::fatG)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new MealResponse(meal.getId(), meal.getUserId(), meal.getMealType().name(),
                meal.getConsumedAt(), meal.getNotes(), itemResponses,
                totalCalories, totalProtein, totalCarbs, totalFat, meal.getCreatedAt());
    }

    private MealItemResponse toItemResponse(MealItem item) {
        Food food = item.getFood();
        BigDecimal factor = item.getQuantityG().divide(food.getServingSizeG(), 10, RoundingMode.HALF_UP);

        BigDecimal calories = food.getCaloriesKcal().multiply(factor).setScale(2, RoundingMode.HALF_UP);
        BigDecimal protein = food.getProteinG().multiply(factor).setScale(2, RoundingMode.HALF_UP);
        BigDecimal carbs = food.getCarbsG().multiply(factor).setScale(2, RoundingMode.HALF_UP);
        BigDecimal fat = food.getFatG().multiply(factor).setScale(2, RoundingMode.HALF_UP);

        return new MealItemResponse(item.getId(), food.getId(), food.getName(),
                item.getQuantityG(), calories, protein, carbs, fat);
    }
}
