package com.erp.erp_back.repository.erp;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.erp.erp_back.entity.erp.Inventory;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    boolean existsByStoreStoreIdAndItemName(Long storeId, String itemName);
    boolean existsByStoreStoreIdAndItemNameAndItemIdNot(Long storeId, String itemName, Long itemId);
    Page<Inventory> findByStoreStoreIdAndItemNameContaining(Long storeId, String q, Pageable pageable);

}