package com.erp.erp_back.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import com.erp.erp_back.dto.admin.DashboardStatsResponse;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DashboardMapper {

    /**
     * 여러 개의 개별 데이터를 받아서 DashboardStatsResponse 객체 생성
     * 파라미터 이름과 DTO 필드명이 일치하면 @Mapping 생략 가능하지만,
     * 명시적으로 적어두는 것이 유지보수에 좋습니다.
     */
    @Mapping(source = "totalStores", target = "totalStores")
    @Mapping(source = "totalUsers", target = "totalUsers")
    @Mapping(source = "activeSubscriptions", target = "activeSubscriptions")
    @Mapping(source = "pendingStoreCount", target = "pendingStoreCount")
    @Mapping(source = "pendingInquiryCount", target = "pendingInquiryCount")
    DashboardStatsResponse toResponse(long totalStores, 
                                      long totalUsers, 
                                      long activeSubscriptions, 
                                      long pendingStoreCount, 
                                      long pendingInquiryCount);
}