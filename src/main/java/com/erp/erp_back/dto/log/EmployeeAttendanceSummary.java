package com.erp.erp_back.dto.log;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사장페이지 - 직원별 출결 "월간 요약" DTO
 *
 * - 직원 출결 현황 카드에서 사용하는 용도
 *   (직원 / 이번 달 총 근무일수 / 이번 달 총 근무시간)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeAttendanceSummary {

    private Long employeeId;
    private String employeeName;

    private Long storeId;
    private String storeName; // UI에서 안 쓰면 null이어도 상관 없음

    /** 이번 달 근무일 수 (출근 기록이 1번이라도 있는 날짜 수) */
    private int workDaysThisMonth;

    /** 이번 달 총 근무시간(시간 단위, 예: 12.5h) */
    private double workHoursThisMonth;
}