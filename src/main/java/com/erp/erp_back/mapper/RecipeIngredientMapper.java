package com.erp.erp_back.mapper;

import com.erp.erp_back.dto.erp.RecipeIngredientResponse;
import com.erp.erp_back.entity.erp.RecipeIngredient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RecipeIngredientMapper {

    @Mapping(source = "menuItem.menuId", target = "menuId")
    @Mapping(source = "inventory.itemId", target = "itemId")
    RecipeIngredientResponse toResponse(RecipeIngredient entity);
}