// src/main/java/com/erp/erp_back/repository/erp/PurchaseHistoryRepository.java
package com.erp.erp_back.repository.erp;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.erp.erp_back.entity.erp.PurchaseHistory;

public interface PurchaseHistoryRepository extends JpaRepository<PurchaseHistory, Long>, JpaSpecificationExecutor<PurchaseHistory> {

   Optional<PurchaseHistory> findTop1ByInventoryItemIdOrderByPurchaseDateDescPurchaseIdDesc(Long itemId);

   boolean existsByInventoryItemId(Long itemId);

   long countByInventoryItemId(Long itemId);
}
