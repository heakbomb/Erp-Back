package com.erp.erp_back.repository.erp;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.erp.erp_back.entity.enums.ActiveStatus;
import com.erp.erp_back.entity.erp.MenuItem;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    Optional<MenuItem> findByMenuIdAndStoreStoreId(Long menuId, Long storeId);
    boolean existsByMenuIdAndStoreStoreId(Long menuId, Long storeId);

    List<MenuItem> findByStoreStoreId(Long storeId);
    
    long countByStoreStoreId(Long storeId);
    long countByStoreStoreIdAndStatus(Long storeId, ActiveStatus status);
    
    boolean existsByStoreStoreIdAndMenuName(Long storeId, String menuName);

    // 목록/상태/검색
    Page<MenuItem> findByStoreStoreId(Long storeId, Pageable pageable);
    Page<MenuItem> findByStoreStoreIdAndStatus(Long storeId, ActiveStatus status, Pageable pageable);
    Page<MenuItem> findByStoreStoreIdAndMenuNameContainingIgnoreCase(
            Long storeId, String q, Pageable pageable);
    Page<MenuItem> findByStoreStoreIdAndMenuNameContainingIgnoreCaseAndStatus(
            Long storeId, String q, ActiveStatus status, Pageable pageable);
}
