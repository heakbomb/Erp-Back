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

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface MenuItemMapper {

    // Entity + 계산된 원가(calculatedCost) -> DTO
    @Mapping(source = "menu.menuId", target = "menuId")
    @Mapping(source = "menu.store.storeId", target = "storeId")
    @Mapping(source = "menu.menuName", target = "menuName")
    @Mapping(source = "menu.price", target = "price")
    @Mapping(source = "menu.status", target = "status")
    @Mapping(source = "cost", target = "calculatedCost")

    // ✅ [추가] 카테고리 문자열 내려주기
    @Mapping(source = "menu.categoryName", target = "categoryName")
    @Mapping(source = "menu.subCategoryName", target = "subCategoryName")
    MenuItemResponse toResponse(MenuItem menu, BigDecimal cost);

    // 생성용 (Request -> Entity)
    @Mapping(target = "menuId", ignore = true)
    @Mapping(target = "store", source = "store")
    // ✅ 상태값이 없으면 기본값 ACTIVE 설정
    @Mapping(source = "req.status", target = "status", defaultValue = "ACTIVE")
    @Mapping(target = "calculatedCost", expression = "java(java.math.BigDecimal.ZERO)")
    @Mapping(target = "price", source = "req.price")

    // ✅ [추가] Request의 categoryName/subCategoryName을 Entity에 매핑
    @Mapping(source = "req.categoryName", target = "categoryName")
    @Mapping(source = "req.subCategoryName", target = "subCategoryName")
    MenuItem toEntity(MenuItemRequest req, Store store);

    // 수정용 (Request -> Entity Partial Update)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "menuId", ignore = true)
    @Mapping(target = "store", ignore = true)
    @Mapping(target = "calculatedCost", ignore = true)
    void updateFromDto(MenuItemRequest req, @MappingTarget MenuItem menu);

    // Aspect 전용 Helper 메소드
    default MenuItemResponse toResponse(MenuItem menu) {
        return toResponse(menu, menu.getCalculatedCost());
    }
}