package com.erp.erp_back.repository.erp;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.erp.erp_back.entity.erp.RecipeIngredient;

@Repository
public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, Long> {
   
    boolean existsByMenuItemMenuIdAndInventoryItemId(Long menuId, Long itemId);

    List<RecipeIngredient> findByMenuItemMenuId(Long menuId);

    void deleteByMenuItemMenuId(Long menuId);
}