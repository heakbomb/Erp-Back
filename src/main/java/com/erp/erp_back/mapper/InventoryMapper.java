package com.erp.erp_back.mapper;

import com.erp.erp_back.dto.erp.InventoryRequest;
import com.erp.erp_back.dto.erp.InventoryResponse;
import com.erp.erp_back.entity.erp.Inventory;
import com.erp.erp_back.entity.store.Store;
import org.mapstruct.*;

import java.math.BigDecimal;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InventoryMapper {

    // 생성
    @Mapping(target = "itemId", ignore = true)
    @Mapping(target = "store", source = "store")
    @Mapping(target = "lastUnitCost", expression = "java(java.math.BigDecimal.ZERO)")
    // ⭐️ status 모호성 해결
    @Mapping(target = "status", source = "req.status", defaultValue = "ACTIVE")
    // ⭐️ [수정] req.getSafetyQty() -> req.safetyQty (필드명 사용)
    @Mapping(target = "stockQty", source = "req.stockQty", qualifiedByName = "defaultZero")
    @Mapping(target = "safetyQty", source = "req.safetyQty", qualifiedByName = "defaultZero")
    Inventory toEntity(InventoryRequest req, Store store);

    // 조회
    @Mapping(source = "store.storeId", target = "storeId")
    @Mapping(source = "stockQty", target = "stockQty", qualifiedByName = "defaultZero")
    @Mapping(source = "safetyQty", target = "safetyQty", qualifiedByName = "defaultZero")
    @Mapping(source = "lastUnitCost", target = "lastUnitCost", qualifiedByName = "defaultZero")
    InventoryResponse toResponse(Inventory inventory);

    // 수정
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "itemId", ignore = true)
    @Mapping(target = "store", ignore = true)
    @Mapping(target = "lastUnitCost", ignore = true)
    @Mapping(target = "stockQty", source = "req.stockQty")
    @Mapping(target = "safetyQty", source = "req.safetyQty")
    void updateFromDto(InventoryRequest req, @MappingTarget Inventory inventory);

    @Named("defaultZero")
    default BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}