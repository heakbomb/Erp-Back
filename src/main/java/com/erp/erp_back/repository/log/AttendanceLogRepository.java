package com.erp.erp_back.repository.log;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable; // ✅ 추가
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import com.erp.erp_back.entity.log.AttendanceLog;

public interface AttendanceLogRepository extends JpaRepository<AttendanceLog, Long> {

    // 마지막 기록 1건(직원+매장 기준, 최신순)
    AttendanceLog findTopByEmployee_EmployeeIdAndStore_StoreIdOrderByRecordTimeDesc(Long employeeId, Long storeId);

    // =========================
    // ✅ 직원 본인 조회용 추가
    // =========================

    // 직원 전체 최근 N건 (매장 무관)
    Page<AttendanceLog> findByEmployee_EmployeeIdOrderByRecordTimeDesc(Long employeeId, Pageable pageable);

    // 직원+매장 최근 N건
    Page<AttendanceLog> findByEmployee_EmployeeIdAndStore_StoreIdOrderByRecordTimeDesc(
            Long employeeId, Long storeId, Pageable pageable);

    // 직원 기간 조회 (매장 무관)
    List<AttendanceLog> findByEmployee_EmployeeIdAndRecordTimeBetweenOrderByRecordTimeDesc(
            Long employeeId, LocalDateTime start, LocalDateTime end);

    // 직원+매장 기간 조회
    List<AttendanceLog> findByEmployee_EmployeeIdAndStore_StoreIdAndRecordTimeBetweenOrderByRecordTimeDesc(
            Long employeeId, Long storeId, LocalDateTime start, LocalDateTime end);

        /**
     * 특정 매장의 특정 날짜 로그 전체 조회 (시간 오름차순)
     */
    @Query("""
            select l
            from AttendanceLog l
            where l.store.storeId = :storeId
              and DATE(l.recordTime) = :date
            order by l.employee.employeeId asc, l.recordTime asc
           """)
    List<AttendanceLog> findByStoreAndDate(
            @Param("storeId") Long storeId,
            @Param("date") LocalDate date
    );

    /**
     * 필요하면 날짜 범위용도 추가 가능
     */
    @Query("""
            select l
            from AttendanceLog l
            where l.store.storeId = :storeId
              and l.recordTime between :from and :to
            order by l.employee.employeeId asc, l.recordTime asc
           """)
    List<AttendanceLog> findByStoreAndDateTimeRange(
            @Param("storeId") Long storeId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

}