package com.erp.erp_back.dto.erp;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MenuStatsResponse {
    private long totalMenus;
    private long inactiveMenus;
    
}
