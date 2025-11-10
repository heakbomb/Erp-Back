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

    @NotNull(message = "구독 상품 ID(subId)는 필수입니다.")
    private Long subId;
}
