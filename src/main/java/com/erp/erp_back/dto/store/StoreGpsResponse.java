package com.erp.erp_back.dto.store;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StoreGpsResponse {
    private Long storeId;
    private Double latitude;
    private Double longitude;
    private Integer gpsRadiusM;
}