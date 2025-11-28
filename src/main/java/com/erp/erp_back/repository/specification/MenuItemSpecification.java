package com.erp.erp_back.repository.specification;

import org.springframework.data.jpa.domain.Specification;
import com.erp.erp_back.entity.enums.ActiveStatus;
import com.erp.erp_back.entity.erp.MenuItem;

public class MenuItemSpecification {

    // 인스턴스화 방지
    private MenuItemSpecification() {}

    /**
     * 매장 ID 필터 (필수)
     */
    public static Specification<MenuItem> byStoreId(Long storeId) {
        return (root, query, cb) -> cb.equal(root.get("store").get("storeId"), storeId);
    }

    /**
     * 메뉴 이름 검색 (대소문자 무시, 부분 일치)
     */
    public static Specification<MenuItem> menuNameContains(String q) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("menuName")), "%" + q.toLowerCase() + "%");
    }

    /**
     * 활성 상태 필터
     */
    public static Specification<MenuItem> hasStatus(ActiveStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}