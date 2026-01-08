package com.erp.erp_back.dto.erp;

import java.math.BigDecimal;

public record WeeklyAreaAvgPoint( 
    int weekIndex,
    BigDecimal areaAvgSales,
    int nearStoreCount
    
){}
