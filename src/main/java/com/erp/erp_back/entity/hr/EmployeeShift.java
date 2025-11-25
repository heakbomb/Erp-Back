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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "shift_date", nullable = false)
    private LocalDate shiftDate;      // 근무날짜

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;      // 시작 시간

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;        // 종료 시간

    // ✅ 휴게 시간(분) – 없으면 0 취급
    @Column(name = "break_minutes")
    private Integer breakMinutes;

    @Builder.Default
    @Column(name = "is_fixed", nullable = false)
    private Boolean isFixed = false;  // 고정 스케줄 여부

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (isFixed == null) {
            isFixed = false;
        }
        if (breakMinutes == null) {
            breakMinutes = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (breakMinutes == null) {
            breakMinutes = 0;
        }
    }
}