package com.erp.erp_back.dto.erp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TradeAreaInfo {
    private String trdarCd;
    private String trdarCdNm;
    private String sigunguCdNm;
    private Integer distanceM;     // 상권 중심점까지 거리
    private String matchMethod;    // NEAREST / OUT_OF_RANGE 등
}
