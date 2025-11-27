package com.erp.erp_back.dto.store;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessNumberResponse {
    private Long bizId;
    private Long ownerId;
    private String phone;
    private String bizNum;
}