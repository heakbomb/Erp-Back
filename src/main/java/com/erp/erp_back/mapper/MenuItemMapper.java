package com.erp.erp_back.mapper;

import java.math.BigDecimal;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.erp.erp_back.dto.erp.MenuItemRequest;
import com.erp.erp_back.dto.erp.MenuItemResponse;
import com.erp.erp_back.entity.erp.MenuItem;
import com.erp.erp_back.entity.store.Store;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MenuItemMapper {

    // Entity + 계산된 원가(calculatedCost) -> DTO
    @Mapping(source = "menu.menuId", target = "menuId")
    @Mapping(source = "menu.store.storeId", target = "storeId")
    @Mapping(source = "menu.menuName", target = "menuName")
    @Mapping(source = "menu.price", target = "price")
    @Mapping(source = "menu.status", target = "status")
    @Mapping(source = "cost", target = "calculatedCost")
    MenuItemResponse toResponse(MenuItem menu, BigDecimal cost);

    // 생성용
    @Mapping(target = "menuId", ignore = true)
    @Mapping(target = "store", source = "store")
    @Mapping(source = "req.status", target = "status") 
    MenuItem toEntity(MenuItemRequest req, Store store);

    // 수정용
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "menuId", ignore = true)
    @Mapping(target = "store", ignore = true)
    void updateFromDto(MenuItemRequest req, @MappingTarget MenuItem menu);
}