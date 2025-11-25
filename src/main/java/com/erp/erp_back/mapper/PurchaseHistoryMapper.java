package com.erp.erp_back.mapper;

import com.erp.erp_back.dto.erp.PurchaseHistoryRequest;
import com.erp.erp_back.dto.erp.PurchaseHistoryResponse;
import com.erp.erp_back.entity.erp.Inventory;
import com.erp.erp_back.entity.erp.PurchaseHistory;
import com.erp.erp_back.entity.store.Store;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PurchaseHistoryMapper {

    // 생성 (Request -> Entity)
    @Mapping(target = "purchaseId", ignore = true)
    @Mapping(source = "store", target = "store")
    @Mapping(source = "inventory", target = "inventory")
    @Mapping(source = "req.purchaseQty", target = "purchaseQty")
    @Mapping(source = "req.unitPrice", target = "unitPrice")
    @Mapping(source = "req.purchaseDate", target = "purchaseDate")
    PurchaseHistory toEntity(PurchaseHistoryRequest req, Store store, Inventory inventory);

    // 조회 (Entity -> Response)
    @Mapping(source = "store.storeId", target = "storeId")
    @Mapping(source = "inventory.itemId", target = "itemId")
    PurchaseHistoryResponse toResponse(PurchaseHistory entity);
}