package com.erp.erp_back.repository.store;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.erp.erp_back.entity.store.StoreGps;

public interface StoreGpsRepository extends JpaRepository<StoreGps, Long> {
    Optional<StoreGps> findByStore_StoreId(Long storeId);
}