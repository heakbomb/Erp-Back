package com.erp.erp_back.repository.ai;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.erp.erp_back.entity.ai.MlProfitForecast;

public interface MlProfitForecastRepository extends JpaRepository<MlProfitForecast, Long> {

    Optional<MlProfitForecast> findTopByStoreIdAndFeatureYmAndTarget(Long storeId, String featureYm, String target);
}
