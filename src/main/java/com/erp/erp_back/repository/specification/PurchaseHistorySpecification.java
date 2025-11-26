package com.erp.erp_back.repository.specification;

import org.springframework.data.jpa.domain.Specification;
import com.erp.erp_back.entity.erp.PurchaseHistory;
import java.time.LocalDate;

public class PurchaseHistorySpecification {

    private PurchaseHistorySpecification() {}

    public static Specification<PurchaseHistory> byStoreId(Long storeId) {
        return (root, query, cb) -> cb.equal(root.get("store").get("storeId"), storeId);
    }

    public static Specification<PurchaseHistory> byItemId(Long itemId) {
        return (root, query, cb) -> cb.equal(root.get("inventory").get("itemId"), itemId);
    }

    public static Specification<PurchaseHistory> dateGte(LocalDate from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("purchaseDate"), from);
    }

    public static Specification<PurchaseHistory> dateLte(LocalDate to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("purchaseDate"), to);
    }
}