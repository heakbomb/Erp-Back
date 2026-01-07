package com.erp.erp_back.entity.hr;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.entity.user.Employee;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "employee_shift")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeShift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shift_id")
    private Long shiftId;

    @Column(name = "shift_group_id")
    private Long shiftGroupId; // 동일한 스케줄 묶음 식별용 (옵션)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "shift_date", nullable = false)
    private LocalDate shiftDate; // 근무날짜

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime; // 시작 시간

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime; // 종료 시간

    // ✅ 휴게 시간(분) – 없으면 0 취급
    @Column(name = "break_minutes")
    private Integer breakMinutes;

    @Builder.Default
    @Column(name = "is_fixed", nullable = false)
    private Boolean isFixed = false; // 고정 스케줄 여부

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // =================================================================
    // ⚡️ 엔티티 생명주기(Lifecycle) 이벤트 메서드
    // =================================================================
    // 데이터 일관성 유지를 위해 @PrePersist, @PreUpdate 사용
    /**
     * @PrePersist: 데이터가 DB에 처음 저장(INSERT)되기 직전에 자동으로 실행됩니다.
     *              역할: 생성일자 자동 기록 및 필수값(기본값) 누락 방지
     */
    @PrePersist
    protected void onCreate() {
        // 1. 타임스탬프 자동 설정 (개발자가 수동으로 넣지 않아도 됨)
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt; // 생성 시점에는 수정일도 생성일과 같음

        // 2. NULL 방지 (Builder 패턴 사용 시 실수로 값을 안 넣었을 때 대비)
        if (isFixed == null) {
            isFixed = false; // 고정 여부 없으면 기본값 false
        }
        if (breakMinutes == null) {
            breakMinutes = 0; // 휴게 시간 없으면 기본값 0분
        }
    }

    /**
     * @PreUpdate: 데이터가 수정(UPDATE)되어 DB에 반영되기 직전에 자동으로 실행됩니다.
     *             역할: 수정일자 갱신 및 데이터 무결성 유지
     */
    @PreUpdate
    protected void onUpdate() {
        // 1. 수정 시간 갱신 (마지막으로 언제 수정됐는지 추적)
        this.updatedAt = LocalDateTime.now();

        // 2. 수정 과정에서 휴게 시간이 실수로 null이 되는 것을 방지
        if (breakMinutes == null) {
            breakMinutes = 0;
        }
    }
}