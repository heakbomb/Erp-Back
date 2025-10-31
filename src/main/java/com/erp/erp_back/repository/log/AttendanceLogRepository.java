package com.erp.erp_back.repository.log;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.erp.erp_back.entity.log.AttendanceLog;

public interface AttendanceLogRepository extends JpaRepository<AttendanceLog, Long> {

    @Query("""
        select a
        from AttendanceLog a
        where a.employee.employeeId = :employeeId
          and a.store.storeId = :storeId
        order by a.recordTime desc
    """)
    List<AttendanceLog> findLatestLogs(Long employeeId, Long storeId);

    @Query("""
        select a
        from AttendanceLog a
        where a.employee.employeeId = :employeeId
          and a.store.storeId = :storeId
          and a.recordTime >= :from
        order by a.recordTime desc
    """)
    List<AttendanceLog> findSince(Long employeeId, Long storeId, LocalDateTime from);

    @Query("""
        select a
        from AttendanceLog a
        where a.employee.employeeId = :employeeId
          and a.store.storeId = :storeId
          and a.recordTime between :start and :end
        order by a.recordTime desc
    """)
    List<AttendanceLog> findDayLogs(Long employeeId, Long storeId,
                                    LocalDateTime start, LocalDateTime end);

                                    // 마지막 기록 1건(직원+매장 기준, 최신순)
    AttendanceLog findTopByEmployee_EmployeeIdAndStore_StoreIdOrderByRecordTimeDesc(Long employeeId, Long storeId);
}