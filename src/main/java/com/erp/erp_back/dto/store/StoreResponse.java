package com.erp.erp_back.dto.store;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter

@NoArgsConstructor
@AllArgsConstructor
public class StoreResponse {
    private Long storeId;
    private Long bizId;
    private String storeName;
    private String industry;
    private String posVendor;
    private String status;
    private LocalDateTime approvedAt;
    private Double latitude;
    private Double longitude;

    // --- 상세 정보 필드 ---
    private String ownerName;
    private String ownerEmail;
    private String bizNum; 
    private String openStatus; 
    private String taxType; 
    private String startDt; 
    private String phone;

    // --- 직원 목록 리스트 ---
    private List<StoreEmployeeDto> employees;

    // ⭐️ [수정] Inner Class에도 생성자 어노테이션 추가
    @Getter
    @Setter 
    @NoArgsConstructor  // 추가: 기본 생성자
    @AllArgsConstructor // 추가: 모든 필드 생성자 (public으로 생성됨)
    public static class StoreEmployeeDto {
        private String name;
        private String role;
        private String status;
    }
}