package com.erp.erp_back.repository.erp;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.erp.erp_back.entity.erp.MenuItem;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    // 기본적인 CRUD 메소드가 이미 모두 구현되어 있음
    boolean existsByStoreStoreIdAndMenuName(Long storeId, String menuName);

     Page<MenuItem> findByStoreStoreIdAndMenuNameContaining(Long storeId, String q, Pageable pageable);

    boolean existsByStoreStoreIdAndMenuNameAndMenuIdNot(Long storeId, String menuName, Long menuId);
}