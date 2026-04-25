package com.trackit.data.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "workout_log_sets", schema = "data")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class WorkoutLogSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workout_log_id", nullable = false)
    private WorkoutLog workoutLog;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(name = "set_number", nullable = false)
    private int setNumber;

    @Column(nullable = false)
    private int reps;

    @Column(name = "weight_kg", precision = 6, scale = 2)
    private BigDecimal weightKg;
}
