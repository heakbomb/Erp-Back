package com.erp.erp_back.dto.store;

import com.erp.erp_back.entity.store.BusinessNumber;   // ✅ 추가 import
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessNumberResponse {
    private Long bizId;
    private Long ownerId;
    private String phone;
    private String bizNum;

    // ✅ 편의 메서드: 엔티티 -> DTO
    public static BusinessNumberResponse fromEntity(BusinessNumber entity) {
        Long ownerId = (entity.getOwner() != null)
                ? entity.getOwner().getOwnerId()   // Owner 엔티티 PK 이름에 맞게 수정
                : null;

        return BusinessNumberResponse.builder()
                .bizId(entity.getBizId())
                .ownerId(ownerId)
                .phone(entity.getPhone())
                .bizNum(entity.getBizNum())
                .build();
    }
}