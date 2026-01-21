package com.erp.erp_back.repository.ai;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.erp.erp_back.entity.ai.DemandForecast;

@Repository
public interface DemandForecastRepository extends JpaRepository<DemandForecast, Long> {
    
    // 특정 매장의 특정 기간(예: 내일~7일뒤) 예측 데이터 조회
    List<DemandForecast> findByStoreIdAndTargetDateBetween(Long storeId, LocalDate startDate, LocalDate endDate);
}