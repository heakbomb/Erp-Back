package com.erp.erp_back.dto.ai;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EventDto {
    private String date;        // "2026-06-01"
    private String name;        // "월드컵 조별예선", "프로야구 개막전", "불꽃축제"
    private String type;        // "SPORTS", "FESTIVAL", "CONCERT" (AI가 유형별로 학습하기 좋음)
    private int importance;     // 1~5 (영향력 크기: 월드컵은 5, 동네 야구는 1)
}