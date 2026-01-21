package com.erp.erp_back.entity.ai;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "ml_profit_forecast",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ml_profit_forecast", columnNames = {"store_id", "feature_ym", "target"})
        },
        indexes = {
                @Index(name = "ix_ml_profit_forecast_store", columnList = "store_id"),
                @Index(name = "ix_ml_profit_forecast_predfor", columnList = "pred_for_ym")
        }
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MlProfitForecast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "feature_ym", nullable = false, length = 7)
    private String featureYm;

    @Column(name = "pred_for_ym", nullable = false, length = 7)
    private String predForYm;

    @Column(name = "target", nullable = false, length = 50)
    private String target;

    @Column(name = "pred_value", nullable = false)
    private Long predValue;

    @Column(name = "model_path", nullable = false, length = 255)
    private String modelPath;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        var now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
