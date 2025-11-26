package com.erp.erp_back.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.erp.erp_back.dto.erp.RecipeIngredientRequest;
import com.erp.erp_back.dto.erp.RecipeIngredientResponse;
import com.erp.erp_back.dto.erp.RecipeIngredientUpdateRequest;
import com.erp.erp_back.entity.erp.Inventory;
import com.erp.erp_back.entity.erp.MenuItem;
import com.erp.erp_back.entity.erp.RecipeIngredient;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RecipeIngredientMapper {

    // Entity -> DTO 변환
    @Mapping(source = "menuItem.menuId", target = "menuId")
    @Mapping(source = "inventory.itemId", target = "itemId")
    RecipeIngredientResponse toResponse(RecipeIngredient entity);

    // 생성 (Request -> Entity)
    @Mapping(target = "recipeId", ignore = true)
    @Mapping(target = "menuItem", source = "menuItem")
    @Mapping(target = "inventory", source = "inventory")
    @Mapping(target = "consumptionQty", source = "req.consumptionQty")
    RecipeIngredient toEntity(RecipeIngredientRequest req, MenuItem menuItem, Inventory inventory);

    // 수정 (Request -> Entity 업데이트)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "recipeId", ignore = true)
    @Mapping(target = "menuItem", ignore = true)
    @Mapping(target = "inventory", ignore = true)
    @Mapping(target = "consumptionQty", source = "req.consumptionQty")
    void updateFromDto(RecipeIngredientUpdateRequest req, @MappingTarget RecipeIngredient entity);
}