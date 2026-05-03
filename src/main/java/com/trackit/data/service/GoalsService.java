package com.trackit.data.service;

import com.trackit.data.domain.UserGoal;
import com.trackit.data.dto.goals.GoalsResponse;
import com.trackit.data.dto.goals.UpdateGoalsRequest;
import com.trackit.data.repository.UserGoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@Transactional
public class GoalsService {

    private static final BigDecimal DEFAULT_CALORIES = new BigDecimal("2000");
    private static final BigDecimal DEFAULT_PROTEIN  = new BigDecimal("150");
    private static final BigDecimal DEFAULT_CARBS    = new BigDecimal("250");
    private static final BigDecimal DEFAULT_FAT      = new BigDecimal("65");

    private final UserGoalRepository repository;

    public GoalsService(UserGoalRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public GoalsResponse get(Long userId) {
        return repository.findById(userId)
                .map(this::toResponse)
                .orElseGet(() -> new GoalsResponse(userId, DEFAULT_CALORIES, DEFAULT_PROTEIN,
                        DEFAULT_CARBS, DEFAULT_FAT, Instant.EPOCH));
    }

    public GoalsResponse upsert(Long userId, UpdateGoalsRequest req) {
        UserGoal goal = repository.findById(userId).orElseGet(() -> UserGoal.builder().userId(userId).build());
        goal.setDailyCaloriesKcal(req.dailyCaloriesKcal());
        goal.setDailyProteinG(req.dailyProteinG());
        goal.setDailyCarbsG(req.dailyCarbsG());
        goal.setDailyFatG(req.dailyFatG());
        return toResponse(repository.save(goal));
    }

    private GoalsResponse toResponse(UserGoal g) {
        return new GoalsResponse(g.getUserId(), g.getDailyCaloriesKcal(), g.getDailyProteinG(),
                g.getDailyCarbsG(), g.getDailyFatG(), g.getUpdatedAt());
    }
}
