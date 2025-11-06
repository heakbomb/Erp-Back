package com.erp.erp_back.dto.log;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreQrResponse {
    private Long storeId;
    private String qrToken;
    private LocalDateTime expireAt;
}