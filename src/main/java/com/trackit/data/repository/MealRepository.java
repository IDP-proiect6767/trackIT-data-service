package com.trackit.data.repository;

import com.trackit.data.domain.Meal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface MealRepository extends JpaRepository<Meal, Long> {

    Page<Meal> findAllByUserId(Long userId, Pageable pageable);

    Page<Meal> findAllByUserIdAndConsumedAtBetween(Long userId, Instant from, Instant to, Pageable pageable);

    Optional<Meal> findByIdAndUserId(Long id, Long userId);
}
