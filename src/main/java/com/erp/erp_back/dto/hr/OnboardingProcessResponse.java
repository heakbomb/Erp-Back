package com.erp.erp_back.dto.hr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingProcessResponse {
    
    private Long storeId;
    private String dataType;
    private int totalCount;    // 총 시도 건수
    private int successCount;  // 성공 건수
    private int failedCount;   // 실패 건수
    private String message;    // 예: "메뉴 100건이 성공적으로 등록되었습니다."
    // private List<String> errorMessages; // (선택) 실패 시 간단한 오류 메시지 목록
}