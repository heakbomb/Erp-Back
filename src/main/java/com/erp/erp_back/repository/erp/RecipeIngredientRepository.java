package com.erp.erp_back.repository.erp;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.erp.erp_back.entity.erp.RecipeIngredient;

// ✅ RecipeIngredientRepository.java
@Repository
public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, Long> {

    // 존재 여부
    boolean existsByMenuItemMenuIdAndInventoryItemId(Long menuId, Long itemId);
    boolean existsByMenuItemMenuId(Long menuId);
    boolean existsByInventoryItemId(Long itemId);

    // 조회
    List<RecipeIngredient> findByMenuItemMenuId(Long menuId);
    List<RecipeIngredient> findByInventoryItemId(Long itemId);

    // 매장 범위로 조회 (선택)
    List<RecipeIngredient> findByInventoryItemIdAndMenuItemStoreStoreId(Long itemId, Long storeId);

    // 삭제
    void deleteByMenuItemMenuId(Long menuId);
}
