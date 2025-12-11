package com.erp.erp_back.dto.admin;

import java.time.LocalDateTime;
import java.util.List;

import com.erp.erp_back.dto.store.StoreSimpleResponse;
import com.erp.erp_back.dto.subscription.OwnerSubscriptionResponse;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminOwnerDetailResponse {
    // 사장님 기본 정보
    private Long ownerId;
    private String username; 
    private String email;
    private LocalDateTime createdAt;
    
    // 연결된 사업장 목록
    private List<StoreSimpleResponse> stores;
    
    // 구독 정보
    private OwnerSubscriptionResponse subscription;
}