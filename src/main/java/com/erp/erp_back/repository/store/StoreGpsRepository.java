package com.erp.erp_back.repository.store;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.erp.erp_back.entity.store.StoreGps;

public interface StoreGpsRepository extends JpaRepository<StoreGps, Long> {
    Optional<StoreGps> findByStore_StoreId(Long storeId);
    // ✅ [추가] 여러 Store ID에 해당하는 GPS 정보를 한 번에 조회
    List<StoreGps> findAllByStore_StoreIdIn(List<Long> storeIds);
}