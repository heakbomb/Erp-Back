package com.erp.erp_back.repository.erp;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.erp.erp_back.entity.erp.RecipeIngredient;

@Repository
public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, Long> {

    // 존재 여부
    boolean existsByMenuItemMenuIdAndInventoryItemId(Long menuId, Long itemId);

    // 조회
    List<RecipeIngredient> findByMenuItemMenuId(Long menuId);
    List<RecipeIngredient> findByInventoryItemId(Long itemId);

}
