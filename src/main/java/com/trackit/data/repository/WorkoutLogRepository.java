package com.trackit.data.repository;

import com.trackit.data.domain.WorkoutLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface WorkoutLogRepository extends JpaRepository<WorkoutLog, Long> {

    Page<WorkoutLog> findAllByUserId(Long userId, Pageable pageable);

    Page<WorkoutLog> findAllByUserIdAndPerformedAtBetween(Long userId, Instant from, Instant to, Pageable pageable);

    Optional<WorkoutLog> findByIdAndUserId(Long id, Long userId);
}
