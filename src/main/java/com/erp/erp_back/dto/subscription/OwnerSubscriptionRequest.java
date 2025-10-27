package com.erp.erp_back.dto.subscription;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OwnerSubscriptionRequest {
    // ownerId는 인증 토큰에서 추출

    @NotNull
    private Long subId; // 신청할 구독 상품 ID
}
