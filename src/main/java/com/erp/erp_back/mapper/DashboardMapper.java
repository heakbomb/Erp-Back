package com.erp.erp_back.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import com.erp.erp_back.dto.admin.DashboardStatsResponse;

// componentModel = "spring" : 스프링 빈으로 등록하여 주입받을 수 있게 함
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DashboardMapper {

    /**
     * [해결책] MapStruct 자동 생성(@Mapping) 대신 Java 'default' 메서드로 직접 구현합니다.
     * 이유: VSCode/Maven 환경에서 파라미터 이름 인식 오류로 인한 빌드 실패 방지
     */
    default DashboardStatsResponse toResponse(long totalStores, 
                                              long totalUsers, 
                                              long activeSubscriptions, 
                                              long pendingStoreCount, 
                                              long pendingInquiryCount) {
        
        // 빌더 패턴을 사용하여 직접 객체 생성 (오류 발생 가능성 0%)
        return DashboardStatsResponse.builder()
                .totalStores(totalStores)
                .totalUsers(totalUsers)
                .activeSubscriptions(activeSubscriptions)
                .pendingStoreCount(pendingStoreCount)
                .pendingInquiryCount(pendingInquiryCount)
                .build();
    }
}