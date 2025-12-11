package com.erp.erp_back.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import com.erp.erp_back.dto.admin.AdminOwnerDetailResponse;
import com.erp.erp_back.dto.admin.AdminStoreDashboardItem;
import com.erp.erp_back.dto.admin.AdminUserStatsResponse;
import com.erp.erp_back.dto.store.StoreSimpleResponse;
import com.erp.erp_back.dto.subscription.OwnerSubscriptionResponse;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.entity.user.Owner;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AdminMapper {

    // 1. 통계 (기존 유지)
    default AdminUserStatsResponse toStatsResponse(long totalUsers, long totalOwners, 
                                           long totalEmployees, long newSignupsThisMonth) {
        return AdminUserStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalOwners(totalOwners)
                .totalEmployees(totalEmployees)
                .newSignupsThisMonth(newSignupsThisMonth)
                .build();
    }

    // 2. 대시보드 아이템 (기존 유지)
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

    // ✅ 3. [수정] 사장님 상세 정보 변환 (username 매핑 수정)
    @Mapping(source = "owner.ownerId", target = "ownerId")
    @Mapping(source = "owner.username", target = "username") // ✅ 이름 일치
    @Mapping(source = "owner.email", target = "email")
    @Mapping(source = "owner.createdAt", target = "createdAt")
    @Mapping(source = "stores", target = "stores")
    @Mapping(source = "subscription", target = "subscription")
    AdminOwnerDetailResponse toOwnerDetailResponse(Owner owner, List<StoreSimpleResponse> stores, OwnerSubscriptionResponse subscription);
}