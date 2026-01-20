package com.erp.erp_back.repository.erp;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.erp.erp_back.entity.enums.ActiveStatus;
import com.erp.erp_back.entity.erp.MenuItem;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long>, JpaSpecificationExecutor<MenuItem> {

    // ✅ [추가] 특정 매장의 모든 메뉴 조회 (List 반환)
    List<MenuItem> findByStoreStoreId(Long storeId);

    Optional<MenuItem> findByMenuIdAndStoreStoreId(Long menuId, Long storeId);

    long countByStoreStoreId(Long storeId);

    long countByStoreStoreIdAndStatus(Long storeId, ActiveStatus status);

    boolean existsByStoreStoreIdAndMenuName(Long storeId, String menuName);

    boolean existsByStoreStoreIdAndMenuNameAndMenuIdNot(Long storeId, String menuName, Long menuId);

    Page<MenuItem> findByStoreStoreIdAndStatus(Long storeId, ActiveStatus status, Pageable pageable);

}
