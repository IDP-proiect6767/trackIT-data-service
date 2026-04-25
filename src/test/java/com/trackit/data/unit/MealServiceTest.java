package com.trackit.data.unit;

import com.trackit.data.domain.Food;
import com.trackit.data.domain.Meal;
import com.trackit.data.domain.MealItem;
import com.trackit.data.domain.enums.MealType;
import com.trackit.data.dto.*;
import com.trackit.data.repository.FoodRepository;
import com.trackit.data.repository.MealRepository;
import com.trackit.data.service.MealService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MealServiceTest {

    @Mock private MealRepository mealRepository;
    @Mock private FoodRepository foodRepository;

    private MealService service;

    @BeforeEach
    void setUp() {
        service = new MealService(mealRepository, foodRepository, new SimpleMeterRegistry());
    }

    @Test
    void create_computesMacrosCorrectly() {
        // Chicken breast: 100g serving = 165 kcal, 31g protein, 0 carbs, 3.6 fat
        Food chicken = buildFood(1L, "Chicken Breast", new BigDecimal("100"),
                new BigDecimal("165"), new BigDecimal("31"), BigDecimal.ZERO, new BigDecimal("3.6"));
        // White rice: 100g serving = 130 kcal, 2.7g protein, 28.2 carbs, 0.3 fat
        Food rice = buildFood(2L, "White Rice", new BigDecimal("100"),
                new BigDecimal("130"), new BigDecimal("2.7"), new BigDecimal("28.2"), new BigDecimal("0.3"));

        when(foodRepository.findById(1L)).thenReturn(Optional.of(chicken));
        when(foodRepository.findById(2L)).thenReturn(Optional.of(rice));

        Meal savedMeal = buildMeal(chicken, new BigDecimal("200"), rice, new BigDecimal("100"));
        when(mealRepository.save(any())).thenReturn(savedMeal);

        CreateMealRequest req = new CreateMealRequest("LUNCH", Instant.now(), null, List.of(
                new MealItemRequest(1L, new BigDecimal("200")),
                new MealItemRequest(2L, new BigDecimal("100"))
        ));

        MealResponse response = service.create(42L, req);

        // 200g chicken: factor=2 → 330 kcal, 62g protein
        // 100g rice: factor=1 → 130 kcal, 2.7g protein
        // Total: 460 kcal, 64.7g protein
        assertThat(response.totalCaloriesKcal()).isEqualByComparingTo("460.00");
        assertThat(response.totalProteinG()).isEqualByComparingTo("64.70");
    }

    private Food buildFood(Long id, String name, BigDecimal serving,
                            BigDecimal cal, BigDecimal protein, BigDecimal carbs, BigDecimal fat) {
        return Food.builder().id(id).name(name).servingSizeG(serving)
                .caloriesKcal(cal).proteinG(protein).carbsG(carbs).fatG(fat)
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
    }

    private Meal buildMeal(Food food1, BigDecimal qty1, Food food2, BigDecimal qty2) {
        Meal meal = Meal.builder().id(1L).userId(42L).mealType(MealType.LUNCH)
                .consumedAt(Instant.now()).createdAt(Instant.now()).build();
        MealItem item1 = MealItem.builder().id(1L).meal(meal).food(food1).quantityG(qty1).build();
        MealItem item2 = MealItem.builder().id(2L).meal(meal).food(food2).quantityG(qty2).build();
        meal.getItems().addAll(List.of(item1, item2));
        return meal;
    }
}
