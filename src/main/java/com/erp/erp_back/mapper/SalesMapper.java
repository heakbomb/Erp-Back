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
import com.erp.erp_back.entity.enums.TransactionStatus; // ✅ Enum Import 필수
import com.erp.erp_back.entity.erp.MenuItem;
import com.erp.erp_back.entity.erp.SalesLineItem;
import com.erp.erp_back.entity.erp.SalesTransaction;
import com.erp.erp_back.entity.store.Store;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = {TransactionStatus.class, LocalDateTime.class}) // ✅ imports 추가
public interface SalesMapper {

    // ===========================================
    // 1. Entity 생성 (PosOrderRequest -> Entity)
    // ===========================================
    
    // SalesTransaction 생성
    @Mapping(target = "transactionId", ignore = true)
    @Mapping(target = "store", source = "store")
    @Mapping(target = "transactionTime", expression = "java(LocalDateTime.now())")
    @Mapping(target = "totalAmount", source = "totalAmount")
    @Mapping(target = "totalDiscount", source = "req.totalDiscount", defaultValue = "0")
    @Mapping(target = "paymentMethod", source = "req.paymentMethod")
    @Mapping(target = "idempotencyKey", source = "req.idempotencyKey")
    @Mapping(target = "lineItems", ignore = true)
    @Mapping(target = "cancelReason", ignore = true) // 생성 시점엔 취소 사유 없음
    // ✅ [변경] 문자열 "PAID" -> Enum Type으로 매핑
    @Mapping(target = "status", expression = "java(TransactionStatus.PAID)") 
    SalesTransaction toEntity(PosOrderRequest req, Store store, BigDecimal totalAmount);

    // SalesLineItem 생성
    @Mapping(target = "lineId", ignore = true)
    @Mapping(target = "salesTransaction", ignore = true)
    @Mapping(target = "menuItem", source = "menuItem")
    @Mapping(target = "quantity", source = "req.quantity")
    @Mapping(target = "unitPrice", source = "realUnitPrice") 
    @Mapping(target = "lineAmount", source = "lineAmount")
    SalesLineItem toLineItem(PosOrderRequest.PosOrderLine req, MenuItem menuItem, BigDecimal realUnitPrice, BigDecimal lineAmount);


    // ===========================================
    // 2. 조회 응답 (DTO 생성)
    // ===========================================

    @Mapping(source = "store.storeId", target = "storeId")
    @Mapping(source = "lineItems", target = "lines")
    @Mapping(source = "status", target = "status") // Enum 그대로 내보내거나 String으로 변환됨
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
    @Mapping(source = "status", target = "status") // 상태값 추가 (취소 여부 확인용)
    RecentTransactionResponse toRecentTransactionResponse(SalesTransaction entity);

    // TopMenuStatsResponse 생성
    TopMenuStatsResponse toTopMenuStats(Long menuId, String menuName, long quantity, BigDecimal revenue, double growth);

    // ✅ [변경] SalesSummaryResponse 생성 (증감률 제거 -> 비교군 데이터 추가)
    // 파라미터 이름과 DTO 필드명이 같으면 @Mapping 생략 가능 (자동 매핑됨)
    SalesSummaryResponse toSalesSummary(
            BigDecimal todaySales, BigDecimal yesterdaySales,
            BigDecimal thisWeekSales, BigDecimal lastWeekSales,
            BigDecimal thisMonthSales, BigDecimal lastMonthSales,
            BigDecimal avgTicket, BigDecimal prevAvgTicket
    );


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