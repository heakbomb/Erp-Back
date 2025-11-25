package com.erp.erp_back.mapper;

import com.erp.erp_back.dto.erp.PurchaseHistoryRequest;
import com.erp.erp_back.dto.erp.PurchaseHistoryResponse;
import com.erp.erp_back.entity.erp.Inventory;
import com.erp.erp_back.entity.erp.PurchaseHistory;
import com.erp.erp_back.entity.store.Store;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PurchaseMapper {

    // Entity -> Response DTO
    @Mapping(source = "purchaseId", target = "purchaseId")
    @Mapping(source = "store.storeId", target = "storeId")
    @Mapping(source = "inventory.itemId", target = "itemId")
    @Mapping(source = "inventory.itemName", target = "itemName")
    PurchaseHistoryResponse toResponse(PurchaseHistory entity);

    // Request -> Entity (생성 시)
    // totalPrice는 Service에서 계산하여 넣으므로 ignore
    @Mapping(target = "purchaseId", ignore = true)
    @Mapping(target = "store", source = "store")
    @Mapping(target = "inventory", source = "inventory")
    PurchaseHistory toEntity(PurchaseHistoryRequest req, Store store, Inventory inventory);

    // Request -> Entity (수정 시)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "purchaseId", ignore = true)
    @Mapping(target = "store", ignore = true)
    @Mapping(target = "inventory", ignore = true) // 재고 품목 변경 불가 정책
    void updateFromDto(PurchaseHistoryRequest req, @MappingTarget PurchaseHistory entity);
}