package com.erp.erp_back.dto.erp;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RefundRequest {
    private Long transactionId; 
    private Boolean isWaste;    // true: 폐기(재고복구X), false: 단순취소(재고복구O)
    private String reason;   
}