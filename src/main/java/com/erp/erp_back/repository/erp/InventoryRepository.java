package com.erp.erp_back.repository.erp;

import com.erp.erp_back.entity.erp.Inventory;
import com.erp.erp_back.entity.enums.ActiveStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByItemIdAndStoreStoreId(Long itemId, Long storeId);
    boolean existsByItemIdAndStoreStoreId(Long itemId, Long storeId);

    Page<Inventory> findByStoreStoreId(Long storeId, Pageable pageable);
    Page<Inventory> findByStoreStoreIdAndStatus(Long storeId, ActiveStatus status, Pageable pageable);

    Page<Inventory> findByStoreStoreIdAndItemNameContainingIgnoreCase(
            Long storeId, String q, Pageable pageable);
    Page<Inventory> findByStoreStoreIdAndItemNameContainingIgnoreCaseAndStatus(
            Long storeId, String q, ActiveStatus status, Pageable pageable);
}
