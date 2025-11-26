package com.erp.erp_back.dto.subscription;

import lombok.Data;

@Data
public class OwnerSubscriptionRequest {
    private Long subId;          // 구독할 상품

    // [Case A] 기존 카드 선택 시
    private Long paymentMethodId; 

    // [Case B] 새 카드 입력 시 (빌링키 즉시 발급됨)
    private String customerUid;   
    private String newCardName;   // (선택) 카드 별칭
}