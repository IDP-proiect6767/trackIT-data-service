package com.trackit.data.repository;

import com.trackit.data.domain.UserGoal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserGoalRepository extends JpaRepository<UserGoal, Long> {
}
