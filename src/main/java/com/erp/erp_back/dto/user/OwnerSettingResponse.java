package com.erp.erp_back.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerSettingResponse {
    private Long settingId;
    private Long ownerId;
    private String settingKey;
    private String settingValue;
}