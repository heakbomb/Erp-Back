package com.erp.erp_back.mapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import com.erp.erp_back.dto.erp.PosOrderResponse;
import com.erp.erp_back.dto.erp.RecentTransactionResponse;
import com.erp.erp_back.dto.erp.SalesTransactionSummaryResponse;
import com.erp.erp_back.entity.erp.SalesLineItem;
import com.erp.erp_back.entity.erp.SalesTransaction;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SalesMapper {

    // ===========================================
    // 1. POS 주문 응답 (PosOrderResponse)
    // ===========================================
    @Mapping(source = "store.storeId", target = "storeId")
    @Mapping(source = "lineItems", target = "lines") // List<SalesLineItem> -> List<LineSummary> 자동 매핑
    PosOrderResponse toPosOrderResponse(SalesTransaction entity);

    // 내부 라인 아이템 매핑
    @Mapping(source = "menuItem.menuId", target = "menuId")
    @Mapping(source = "menuItem.menuName", target = "menuName")
    PosOrderResponse.LineSummary toLineSummary(SalesLineItem entity);


    // ===========================================
    // 2. 거래 내역 요약 (SalesTransactionSummaryResponse)
    // ===========================================
    @Mapping(source = "transactionId", target = "transactionId")
    @Mapping(source = "totalAmount", target = "totalAmount")
    @Mapping(source = "totalDiscount", target = "totalDiscount")
    @Mapping(source = "transactionTime", target = "transactionTime", dateFormat = "yyyy-MM-dd HH:mm")
    @Mapping(source = "paymentMethod", target = "paymentMethod")
    @Mapping(source = "status", target = "status")
    @Mapping(target = "itemsSummary", expression = "java(makeItemsSummary(entity.getLineItems()))") // 커스텀 로직
    SalesTransactionSummaryResponse toSummaryResponse(SalesTransaction entity);


    // ===========================================
    // 3. 최근 거래 (RecentTransactionResponse)
    // ===========================================
    @Mapping(source = "transactionId", target = "id")
    @Mapping(source = "totalAmount", target = "amount")
    @Mapping(target = "time", expression = "java(formatTime(entity.getTransactionTime()))") // HH:mm 포맷
    @Mapping(target = "items", expression = "java(makeItemsSummary(entity.getLineItems()))") // 커스텀 로직
    RecentTransactionResponse toRecentTransactionResponse(SalesTransaction entity);


    // ===========================================
    // 4. 공통 헬퍼 메서드 (Java Expression용)
    // ===========================================

    // 메뉴명 x 수량, ... 형태로 요약 문자열 생성
    default String makeItemsSummary(List<SalesLineItem> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        return items.stream()
                .map(li -> li.getMenuItem().getMenuName() + " x " + li.getQuantity())
                .collect(Collectors.joining(", "));
    }

    // 시간 포맷팅 (HH:mm)
    default String formatTime(LocalDateTime time) {
        if (time == null) return "";
        return time.format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}