package com.erp.erp_back.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import com.erp.erp_back.dto.admin.AdminStoreDashboardItem;
import com.erp.erp_back.dto.admin.AdminUserStatsResponse;
import com.erp.erp_back.entity.store.Store;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AdminMapper {

    /**
     * 1. 사용자 통계 KPI 변환
     * [FIX] MapStruct가 primitive 타입들만 있을 때 'if ( )' 같은 잘못된 코드를 생성하는 버그가 있어
     * default 메서드로 직접 구현하여 우회합니다.
     */
    default AdminUserStatsResponse toStatsResponse(long totalUsers, long totalOwners, 
                                           long totalEmployees, long newSignupsThisMonth) {
        return AdminUserStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalOwners(totalOwners)
                .totalEmployees(totalEmployees)
                .newSignupsThisMonth(newSignupsThisMonth)
                .build();
    }

    /**
     * 2. 사업장 대시보드 아이템 변환
     * - Store 엔티티의 연관 관계(BusinessNumber, Owner)를 탐색하여 정보 매핑
     * - 별도로 집계된 데이터(직원수, 매출, 최근거래일)도 함께 매핑
     */
    @Mapping(source = "store.storeId", target = "storeId")
    @Mapping(source = "store.storeName", target = "storeName")
    @Mapping(source = "store.industry", target = "industry")
    @Mapping(source = "store.status", target = "status")
    @Mapping(source = "store.businessNumber.owner.username", target = "ownerName")
    @Mapping(source = "store.businessNumber.owner.email", target = "ownerEmail")
    @Mapping(source = "store.businessNumber.bizNum", target = "bizNum")
    @Mapping(source = "employeeCount", target = "employeeCount")
    @Mapping(source = "totalSalesMonth", target = "totalSalesMonth")
    @Mapping(source = "lastSalesDate", target = "lastSalesDate")
    AdminStoreDashboardItem toDashboardItem(Store store, long employeeCount, 
                                            BigDecimal totalSalesMonth, LocalDateTime lastSalesDate);
}