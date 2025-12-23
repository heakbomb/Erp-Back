package com.erp.erp_back.repository.log;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
     * - (필요시) employeeId 순서가 먼저가 아니라 recordTime이 먼저 되도록 수정
     */
    @Query("""
            select l
            from AttendanceLog l
            where l.store.storeId = :storeId
              and DATE(l.recordTime) = :date
            order by l.recordTime asc, l.employee.employeeId asc
           """)
    List<AttendanceLog> findByStoreAndDate(
            @Param("storeId") Long storeId,
            @Param("date") LocalDate date
    );

    /**
     * 날짜 범위 조회 (사장페이지용)
     * ✅ 핵심 수정: 직원ID 우선 정렬 → 기록시간(recordTime) 우선 정렬로 변경
     */
    @Query("""
            select l
            from AttendanceLog l
            where l.store.storeId = :storeId
              and l.recordTime between :from and :to
            order by l.recordTime asc, l.employee.employeeId asc
           """)
    List<AttendanceLog> findByStoreAndDateTimeRange(
            @Param("storeId") Long storeId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    // =========================
    // ✅ [추가] shiftId 연동(출퇴근-근무시간표 묶기) + 자동퇴근용
    // =========================

    // ✅ shift 기준 IN/OUT 존재 여부 (중복/선후관계 체크)
    boolean existsByEmployee_EmployeeIdAndStore_StoreIdAndShift_ShiftIdAndRecordType(
            Long employeeId, Long storeId, Long shiftId, String recordType);

    // ✅ shift 기준 최신 기록 1건 (필요 시)
    AttendanceLog findTopByEmployee_EmployeeIdAndStore_StoreIdAndShift_ShiftIdOrderByRecordTimeDesc(
            Long employeeId, Long storeId, Long shiftId);

    // ✅ (서비스에서 요구) shift 기준 전체 로그(최신순)
    List<AttendanceLog> findByEmployee_EmployeeIdAndStore_StoreIdAndShift_ShiftIdOrderByRecordTimeDesc(
            Long employeeId, Long storeId, Long shiftId);

    // ✅ 자동퇴근 대상: OUT 없는 IN 로그(shift 단위로 OUT 존재 여부 판단)
    @Query("""
            select l
            from AttendanceLog l
            where l.recordType = 'IN'
              and l.shift is not null
              and l.shift.shiftDate = :date
              and l.store.storeId = :storeId
              and not exists (
                  select 1
                  from AttendanceLog o
                  where o.shift.shiftId = l.shift.shiftId
                    and o.employee.employeeId = l.employee.employeeId
                    and o.store.storeId = l.store.storeId
                    and o.recordType = 'OUT'
              )
           """)
    List<AttendanceLog> findOpenInLogsByStoreAndDate(
            @Param("storeId") Long storeId,
            @Param("date") LocalDate date
    );

    // ✅ [추가] shiftId 여러 개에 대한 로그 한 번에 조회 (상태 API 성능용)
    @Query("""
        select l
        from AttendanceLog l
        where l.employee.employeeId = :employeeId
          and l.store.storeId = :storeId
          and l.shift.shiftId in :shiftIds
        order by l.recordTime desc
    """)
    List<AttendanceLog> findByEmployeeAndStoreAndShiftIds(
            @Param("employeeId") Long employeeId,
            @Param("storeId") Long storeId,
            @Param("shiftIds") List<Long> shiftIds
    );

    // =========================
    // ✅ [추가] shift 삭제/기간삭제 안전장치
    // =========================

    // shift를 참조하는 로그가 존재하는지(선택)
    boolean existsByShift_ShiftId(Long shiftId);

    // FK(SET NULL) 마이그레이션이 덜 되어도 안전하게: 삭제 전에 로그의 shift를 null로 "먼저" 끊어줌
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update AttendanceLog l
           set l.shift = null
         where l.shift.shiftId in :shiftIds
    """)
    int detachShiftRefs(@Param("shiftIds") List<Long> shiftIds);
}