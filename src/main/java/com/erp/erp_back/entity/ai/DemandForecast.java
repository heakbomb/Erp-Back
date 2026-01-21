package com.erp.erp_back.entity.ai;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "demand_forecast")
@Data
@NoArgsConstructor
public class DemandForecast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "forecast_id")
    private Long forecastId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "menu_id", nullable = false)
    private Long menuId;

    // 예측 대상 날짜 (예: 내일, 모레)
    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    // 예측 수량
    @Column(name = "predicted_qty", nullable = false)
    private int predictedQty;

    // 예측을 수행한 날짜 (생성일)
    @Column(name = "forecast_date", nullable = false)
    private LocalDate forecastDate;

    // 실제 판매량 (검증용, 추후 업데이트)
    @Column(name = "actual_qty")
    private Integer actualQty;

    // 정확도 (검증용)
    @Column(name = "accuracy_rate")
    private Double accuracyRate;

    @Column(name = "is_reflected")
    private Boolean isReflected;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (isReflected == null) isReflected = false;
    }
}