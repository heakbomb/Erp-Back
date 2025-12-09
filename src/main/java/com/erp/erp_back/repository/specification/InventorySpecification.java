package com.erp.erp_back.repository.specification;

import org.springframework.data.jpa.domain.Specification;

import com.erp.erp_back.entity.enums.ActiveStatus;
import com.erp.erp_back.entity.enums.IngredientCategory;
import com.erp.erp_back.entity.erp.Inventory;


public class InventorySpecification {

    // 인스턴스화 방지
    private InventorySpecification() {}

    /**
     * 매장 ID로 필터링
     * SQL: WHERE store_id = ?
     */
    public static Specification<Inventory> byStoreId(Long storeId) {
        return (root, query, cb) -> cb.equal(root.get("store").get("storeId"), storeId);
    }

    /**
     * 아이템 이름 포함 여부 (대소문자 무시)
     * SQL: WHERE lower(item_name) LIKE %lower(q)%
     */
    public static Specification<Inventory> itemNameContains(String q) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("itemName")), "%" + q.toLowerCase() + "%");
    }

    /**
     * 활성 상태 필터링
     * SQL: WHERE status = ?
     */
    public static Specification<Inventory> hasStatus(ActiveStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    // 재고 품목 타입 
     public static Specification<Inventory> hasItemType(IngredientCategory itemType) {
        return (root, query, cb) ->
                itemType == null ? null : cb.equal(root.get("itemType"), itemType);
    }
}