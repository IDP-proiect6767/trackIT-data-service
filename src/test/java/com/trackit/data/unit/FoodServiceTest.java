package com.trackit.data.unit;

import com.trackit.data.domain.Food;
import com.trackit.data.dto.*;
import com.trackit.data.exception.ResourceNotFoundException;
import com.trackit.data.repository.FoodRepository;
import com.trackit.data.service.FoodService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FoodServiceTest {

    @Mock private FoodRepository repository;
    private FoodService service;

    @BeforeEach
    void setUp() {
        service = new FoodService(repository, new SimpleMeterRegistry());
    }

    @Test
    void get_throwsWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void list_returnsPagedResults() {
        Food food = buildFood(1L, "Chicken Breast");
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(food)));

        PageResponse<FoodResponse> result = service.list(0, 20, null);
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).name()).isEqualTo("Chicken Breast");
    }

    @Test
    void update_appliesPartialFields() {
        Food food = buildFood(1L, "Old Name");
        when(repository.findById(1L)).thenReturn(Optional.of(food));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FoodResponse response = service.update(1L, new UpdateFoodRequest("New Name", null, null, null, null, null, null));
        assertThat(response.name()).isEqualTo("New Name");
    }

    private Food buildFood(Long id, String name) {
        return Food.builder().id(id).name(name)
                .servingSizeG(new BigDecimal("100")).caloriesKcal(new BigDecimal("150"))
                .proteinG(BigDecimal.ZERO).carbsG(BigDecimal.ZERO).fatG(BigDecimal.ZERO)
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
    }
}
