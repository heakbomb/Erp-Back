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

    // 1. 통계
    default AdminUserStatsResponse toStatsResponse(long totalUsers, long totalOwners, 
                                           long totalEmployees, long newSignupsThisMonth) {
        return AdminUserStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalOwners(totalOwners)
                .totalEmployees(totalEmployees)
                .newSignupsThisMonth(newSignupsThisMonth)
                .build();
    }

    // 2. 대시보드 아이템
    @Mapping(source = "store.storeId", target = "storeId")
    @Mapping(source = "store.storeName", target = "storeName")
    @Mapping(source = "store.industry", target = "industry") // Enum to Enum (자동 매핑)
    @Mapping(source = "store.status", target = "status")
    @Mapping(source = "store.businessNumber.owner.username", target = "ownerName")
    @Mapping(source = "store.businessNumber.owner.email", target = "ownerEmail")
    @Mapping(source = "store.businessNumber.bizNum", target = "bizNum")
    @Mapping(source = "employeeCount", target = "employeeCount")
    
    // ✅ [수정] 타겟 필드명 변경 (totalSalesMonth -> totalSales)
    @Mapping(source = "totalSales", target = "totalSales")
    
    // ✅ [수정] 타겟 필드명 변경 (lastSalesDate -> lastTransaction)
    @Mapping(source = "lastTransaction", target = "lastTransaction")
    AdminStoreDashboardItem toDashboardItem(Store store, long employeeCount, 
                                            BigDecimal totalSales, LocalDateTime lastTransaction);

    // 3. 사장님 상세 정보 변환
    @Mapping(source = "owner.ownerId", target = "ownerId")
    @Mapping(source = "owner.username", target = "username")
    @Mapping(source = "owner.email", target = "email")
    @Mapping(source = "owner.createdAt", target = "createdAt")
    @Mapping(source = "stores", target = "stores")
    @Mapping(source = "subscription", target = "subscription")
    AdminOwnerDetailResponse toOwnerDetailResponse(Owner owner, List<StoreSimpleResponse> stores, OwnerSubscriptionResponse subscription);
}