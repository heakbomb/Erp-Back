package com.erp.erp_back.dto.erp;

import java.math.BigDecimal;

public record GuIndustrySalesResponse(
        String sigunguCdNm,
        String industry,
        BigDecimal guIndustrySales
) {}