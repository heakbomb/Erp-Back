package com.erp.erp_back.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor  // 기본 생성자 (JSON 변환 등에 안전)
@AllArgsConstructor 
public class SalesDataDto {
    private String date;       // "2025-01-01"
    private Long storeId;      // 매장 ID
    private Long menuId;       // 메뉴 ID
    private int quantity;      // 판매 수량
    
    // 날씨 매핑을 위한 좌표 정보 (Store 테이블에서 가져옴)
    private int nx;            
    private int ny;            
}