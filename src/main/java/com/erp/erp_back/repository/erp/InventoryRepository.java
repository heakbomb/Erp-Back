package com.erp.erp_back.repository.erp;

import com.erp.erp_back.entity.erp.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long>, JpaSpecificationExecutor<Inventory> {

    Optional<Inventory> findByItemIdAndStoreStoreId(Long itemId, Long storeId);
}
