package com.erp.erp_back.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import com.erp.erp_back.dto.erp.PosOrderRequest;
import com.erp.erp_back.dto.erp.PosOrderResponse;
import com.erp.erp_back.dto.erp.RecentTransactionResponse;
import com.erp.erp_back.dto.erp.SalesSummaryResponse;
import com.erp.erp_back.dto.erp.SalesTransactionSummaryResponse;
import com.erp.erp_back.dto.erp.TopMenuStatsResponse;
import com.erp.erp_back.entity.erp.MenuItem;
import com.erp.erp_back.entity.erp.SalesLineItem;
import com.erp.erp_back.entity.erp.SalesTransaction;
import com.erp.erp_back.entity.store.Store;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SalesMapper {

    // ===========================================
    // 1. Entity 생성 (PosOrderRequest -> Entity)
    // ===========================================
    
    // SalesTransaction 생성 (총액은 계산된 값을 받음)
    @Mapping(target = "transactionId", ignore = true)
    @Mapping(target = "store", source = "store")
    @Mapping(target = "transactionTime", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "totalAmount", source = "totalAmount") // 계산된 총액 주입
    @Mapping(target = "totalDiscount", source = "req.totalDiscount", defaultValue = "0")
    @Mapping(target = "paymentMethod", source = "req.paymentMethod")
    @Mapping(target = "status", constant = "PAID")
    @Mapping(target = "idempotencyKey", source = "req.idempotencyKey")
    @Mapping(target = "lineItems", ignore = true) // 별도 설정
    SalesTransaction toEntity(PosOrderRequest req, Store store, BigDecimal totalAmount);

    // SalesLineItem 생성 (단가/금액 계산된 값 받음)
    @Mapping(target = "lineId", ignore = true)
    @Mapping(target = "salesTransaction", ignore = true) // 나중에 연관관계 설정
    @Mapping(target = "menuItem", source = "menuItem")
    @Mapping(target = "quantity", source = "req.quantity")
    @Mapping(target = "unitPrice", source = "req.unitPrice")
    @Mapping(target = "lineAmount", source = "lineAmount")
    SalesLineItem toLineItem(PosOrderRequest.PosOrderLine req, MenuItem menuItem, BigDecimal lineAmount);


    // ===========================================
    // 2. 조회 응답 (DTO 생성)
    // ===========================================

    @Mapping(source = "store.storeId", target = "storeId")
    @Mapping(source = "lineItems", target = "lines")
    PosOrderResponse toPosOrderResponse(SalesTransaction entity);

    @Mapping(source = "menuItem.menuId", target = "menuId")
    @Mapping(source = "menuItem.menuName", target = "menuName")
    PosOrderResponse.LineSummary toLineSummary(SalesLineItem entity);

    @Mapping(source = "transactionId", target = "transactionId")
    @Mapping(source = "transactionTime", target = "transactionTime", dateFormat = "yyyy-MM-dd HH:mm")
    @Mapping(target = "itemsSummary", expression = "java(makeItemsSummary(entity.getLineItems()))")
    SalesTransactionSummaryResponse toSummaryResponse(SalesTransaction entity);

    @Mapping(source = "transactionId", target = "id")
    @Mapping(source = "totalAmount", target = "amount")
    @Mapping(target = "time", expression = "java(formatTime(entity.getTransactionTime()))")
    @Mapping(target = "items", expression = "java(makeItemsSummary(entity.getLineItems()))")
    RecentTransactionResponse toRecentTransactionResponse(SalesTransaction entity);

    // ⭐️ [추가] TopMenuStatsResponse 생성 (개별 필드 -> DTO)
    TopMenuStatsResponse toTopMenuStats(Long menuId, String name, long quantity, BigDecimal revenue, double growth);

    // ⭐️ [추가] SalesSummaryResponse 생성 (8개 필드 -> DTO)
    SalesSummaryResponse toSalesSummary(BigDecimal todaySales, BigDecimal todaySalesChangeRate,
                                        BigDecimal weekSales, BigDecimal weekSalesChangeRate,
                                        BigDecimal monthSales, BigDecimal monthSalesChangeRate,
                                        BigDecimal avgTicket, BigDecimal avgTicketChangeRate);


    // ===========================================
    // 3. 헬퍼 메서드
    // ===========================================

    default String makeItemsSummary(List<SalesLineItem> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        return items.stream()
                .map(li -> li.getMenuItem().getMenuName() + " x " + li.getQuantity())
                .collect(Collectors.joining(", "));
    }

    default String formatTime(LocalDateTime time) {
        if (time == null) return "";
        return java.time.format.DateTimeFormatter.ofPattern("HH:mm").format(time);
    }
}