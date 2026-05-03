package com.trackit.data.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "user_goals", schema = "data")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class UserGoal {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "daily_calories_kcal", nullable = false)
    private BigDecimal dailyCaloriesKcal;

    @Column(name = "daily_protein_g", nullable = false)
    private BigDecimal dailyProteinG;

    @Column(name = "daily_carbs_g", nullable = false)
    private BigDecimal dailyCarbsG;

    @Column(name = "daily_fat_g", nullable = false)
    private BigDecimal dailyFatG;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
